package com.example.photosmanager.data.repository

import android.content.Context
import android.util.Log
import com.example.photosmanager.data.database.AppDatabase
import com.example.photosmanager.data.model.ReviewedPhoto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 记录"已经判断过"的照片，供抽取时排除。
 *
 * 标记写入走自己的作用域：这些写入发生在用户点下保留/删除的瞬间，
 * 而那一刻之后悬浮窗可能马上就被销毁并取消掉 UI 协程。
 */
class ReviewRepository(context: Context) {
    private val dao = AppDatabase.getDatabase(context).reviewedPhotoDao()

    companion object {
        // 作用域共享：悬浮窗每轮都会 new 一个 ReviewRepository，
        // 每个实例一个 scope 会白白攒下一堆未取消的 Job。
        private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    suspend fun getReviewedUris(): Set<String> = try {
        dao.getReviewedUris().toSet()
    } catch (e: Exception) {
        Log.e("ReviewRepository", "读取已判断记录失败，本轮按未判断处理", e)
        emptySet()
    }

    /** 判断了一张照片，不阻塞界面 */
    fun markReviewedAsync(uri: String) {
        ioScope.launch {
            try {
                dao.insert(ReviewedPhoto(uri = uri))
            } catch (e: Exception) {
                Log.e("ReviewRepository", "写入已判断记录失败: $uri", e)
            }
        }
    }

    /** 照片实际上没被删掉（用户取消勾选、拒绝授权或删除失败），退回池子 */
    suspend fun release(uris: List<String>) {
        if (uris.isEmpty()) return
        try {
            dao.delete(uris)
        } catch (e: Exception) {
            Log.e("ReviewRepository", "退回已判断记录失败", e)
        }
    }

    /** 退回一批照片，不阻塞调用方（确认页可能马上就 finish 了） */
    fun releaseAsync(uris: List<String>) {
        ioScope.launch { release(uris) }
    }

    /** 整个池子都判断完了，重新开始一轮循环 */
    suspend fun startNewCycle() {
        try {
            dao.deleteAll()
        } catch (e: Exception) {
            Log.e("ReviewRepository", "重置整理循环失败", e)
        }
    }
}
