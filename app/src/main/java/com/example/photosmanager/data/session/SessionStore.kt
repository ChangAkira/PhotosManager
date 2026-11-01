package com.example.photosmanager.data.session

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.example.photosmanager.data.model.PhotoAction
import com.example.photosmanager.data.model.PhotoInfo
import com.example.photosmanager.data.model.PhotoSelection
import com.example.photosmanager.data.repository.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * 一轮询问的会话状态：队列、当前进度、每张照片的保留/删除标记。
 *
 * 悬浮窗是 Service 里的 ComposeView，窗口一重建，remember 里的状态就清零了。
 * 所以这些状态必须搬到窗口之外：内存态供窗口直接读取，磁盘态保证进程被杀后也能续上。
 *
 * 写盘走自己的作用域，不能挂在 UI 协程上——关闭悬浮窗会立刻取消 rememberCoroutineScope，
 * 那一笔状态就丢了。
 */
object SessionStore {

    private val lock = Any()
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var photos: MutableList<PhotoSelection> = mutableListOf()
    private var index: Int = 0
    private var restored = false

    /** 队列快照，仅供渲染 */
    val queue: List<PhotoSelection>
        get() = synchronized(lock) { photos.toList() }

    val currentIndex: Int
        get() = synchronized(lock) { index }

    val isActive: Boolean
        get() = synchronized(lock) { photos.isNotEmpty() }

    /**
     * 进程内第一次调用时尝试恢复未完成的一轮。
     * 已经被删掉或不在媒体库里的照片会被剔除；整轮都失效则丢弃。
     */
    suspend fun restoreIfNeeded(context: Context) {
        synchronized(lock) {
            if (restored) return
            restored = true
        }
        val appContext = context.applicationContext
        val json = PreferencesRepository(appContext).activeSessionJson.first() ?: return
        val parsed = deserialize(json) ?: return
        val (savedPhotos, savedIndex) = parsed
        if (savedPhotos.isEmpty()) return

        val alive = withContext(Dispatchers.IO) {
            savedPhotos.filter { existsInMediaStore(appContext, it.photoInfo.uri) }
        }
        if (alive.isEmpty()) {
            finish(appContext)
            return
        }
        val currentUri = savedPhotos.getOrNull(savedIndex)?.photoInfo?.uri
        val dropped = alive.size != savedPhotos.size
        synchronized(lock) {
            photos = alive.toMutableList()
            index = currentUri?.let { uri ->
                alive.indexOfFirst { it.photoInfo.uri == uri }
            }?.takeIf { it >= 0 } ?: 0
        }
        if (dropped) write(appContext, snapshot())
        Log.d("SessionStore", "恢复未完成的一轮：${alive.size} 张，进度 $currentIndex")
    }

    /** 开新一轮，覆盖任何遗留状态 */
    fun startRound(context: Context, selections: List<PhotoSelection>) {
        val appContext = context.applicationContext
        synchronized(lock) {
            photos = selections.toMutableList()
            index = 0
            restored = true
        }
        write(appContext, snapshot())
    }

    fun setIndex(context: Context, position: Int) {
        val appContext = context.applicationContext
        val json = synchronized(lock) {
            if (photos.isEmpty()) return
            index = position.coerceIn(0, photos.size - 1)
            snapshot()
        }
        write(appContext, json)
    }

    fun setAction(context: Context, position: Int, action: PhotoAction) {
        val appContext = context.applicationContext
        val json = synchronized(lock) {
            val target = photos.getOrNull(position) ?: return
            if (target.action == action) return
            target.action = action
            snapshot()
        }
        write(appContext, json)
    }

    /** 把"附近"拉来的照片插到当前照片之后，当前进度不受影响 */
    fun insertAfterCurrent(context: Context, extra: List<PhotoSelection>) {
        if (extra.isEmpty()) return
        val appContext = context.applicationContext
        val json = synchronized(lock) {
            if (photos.isEmpty()) return
            val before = photos.take(index + 1)
            val after = photos.drop(index + 1)
            photos = (before + extra + after).toMutableList()
            snapshot()
        }
        write(appContext, json)
    }

    /** 本轮结束（走完或作废），清空内存与磁盘状态 */
    fun finish(context: Context) {
        val appContext = context.applicationContext
        synchronized(lock) {
            photos = mutableListOf()
            index = 0
            restored = true
        }
        write(appContext, null)
    }

    /** 调用方必须已在 [lock] 内 */
    private fun snapshot(): String {
        val array = JSONArray()
        for (selection in photos) {
            val info = selection.photoInfo
            array.put(
                JSONObject()
                    .put("uri", info.uri)
                    .put("albumName", info.albumName)
                    .put("dateAdded", info.dateAdded)
                    .put("dateModified", info.dateModified)
                    .put("size", info.size)
                    .put("width", info.width)
                    .put("height", info.height)
                    .put("displayName", info.displayName)
                    .put("bucketId", info.bucketId)
                    .put("action", selection.action.name)
                    .put("fromNearby", selection.fromNearby)
            )
        }
        return JSONObject()
            .put("version", VERSION)
            .put("index", index)
            .put("photos", array)
            .toString()
    }

    private fun write(context: Context, json: String?) {
        ioScope.launch {
            try {
                val repository = PreferencesRepository(context)
                if (json == null) repository.clearActiveSession() else repository.setActiveSession(json)
            } catch (e: Exception) {
                Log.e("SessionStore", "写入会话状态失败", e)
            }
        }
    }

    private fun deserialize(raw: String): Pair<List<PhotoSelection>, Int>? {
        return try {
            val root = JSONObject(raw)
            if (root.optInt("version", -1) != VERSION) return null
            val array = root.getJSONArray("photos")
            val selections = ArrayList<PhotoSelection>(array.length())
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val info = PhotoInfo(
                    uri = item.getString("uri"),
                    albumName = item.optString("albumName", "未知相册"),
                    dateAdded = item.optLong("dateAdded"),
                    dateModified = item.optLong("dateModified"),
                    size = item.optLong("size"),
                    width = item.optInt("width"),
                    height = item.optInt("height"),
                    displayName = item.optString("displayName", ""),
                    bucketId = item.optLong("bucketId", -1L)
                )
                val action = runCatching { PhotoAction.valueOf(item.optString("action")) }
                    .getOrDefault(PhotoAction.KEEP)
                selections.add(
                    PhotoSelection(
                        photoInfo = info,
                        action = action,
                        fromNearby = item.optBoolean("fromNearby", false)
                    )
                )
            }
            val position = root.optInt("index", 0).coerceIn(0, maxOf(0, selections.size - 1))
            selections to position
        } catch (e: Exception) {
            Log.e("SessionStore", "会话数据解析失败，丢弃本轮", e)
            null
        }
    }

    /**
     * 照片是否还在媒体库里。查询失败时按"还在"处理——
     * 宁可多显示一张加载失败的照片，也不要把用户还没处理的照片悄悄丢掉。
     */
    private fun existsInMediaStore(context: Context, uriString: String): Boolean {
        return try {
            val projection = arrayOf(MediaStore.Images.Media._ID)
            context.contentResolver.query(Uri.parse(uriString), projection, null, null, null)?.use {
                it.moveToFirst()
            } ?: true
        } catch (e: Exception) {
            Log.d("SessionStore", "存在性检查失败，保留该照片: $uriString")
            true
        }
    }

    private const val VERSION = 1
}
