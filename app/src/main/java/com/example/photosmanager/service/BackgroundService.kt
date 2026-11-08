package com.example.photosmanager.service

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.photosmanager.MainActivity
import com.example.photosmanager.R
import com.example.photosmanager.data.repository.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 应用唯一的常驻前台服务，也是唯一一条通知。
 *
 * 之前有三个前台服务，各自的常驻通知就是三条——「后台服务运行中」「屏幕监听服务运行中」
 * 「照片管理界面」，用户只想知道"这个应用在后台活着"，三条只是噪音。
 * 而 Android 8 起前台服务必须挂一条通知，没法让它显示但不出现，
 * 所以只能把服务收成一个：亮屏这件事本来也只是 BackgroundService 里注册的一个广播接收器，
 * 单独给它开一个前台服务换来的只有多一条通知。
 * 悬浮窗服务留着，但降级成普通服务——它不需要常驻，只在亮屏时被拉起来挂一层界面，
 * 进程由这个前台服务保住，界面不会因为不是前台服务而消失。
 */
class BackgroundService : Service() {
    private var wakeLock: PowerManager.WakeLock? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var preferencesRepository: PreferencesRepository
    private var screenReceiver: BroadcastReceiver? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        try {
            startForeground(NOTIFICATION_ID, createNotification())
        } catch (e: Exception) {
            android.util.Log.e("BackgroundService", "启动前台服务失败", e)
        }
        acquireWakeLock()
        preferencesRepository = PreferencesRepository(this)
        registerScreenReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 收进来重新注册一次：START_STICKY 重启后 onCreate 已经跑过，
        // 但被 START_REDELIVER_INTENT 再拉起时不能假定接收器还在
        registerScreenReceiver()
        // 使用START_STICKY确保服务被杀死后自动重启
        // 同时使用START_REDELIVER_INTENT确保Intent被重新传递
        return START_STICKY or START_REDELIVER_INTENT
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // 当任务被移除时（如"全部清除"），尝试重启服务
        android.util.Log.d("BackgroundService", "任务被移除，尝试重启服务")
        val restartIntent = Intent(applicationContext, BackgroundService::class.java)
        restartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(restartIntent)
        } else {
            startService(restartIntent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.ch_bg),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.ch_bg_desc)
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationChannelCleanup(notificationManager)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 收掉多余的前台服务，它们那条渠道要一起删掉——留着的话系统「通知管理」里
     * 还会挂着两个永远不会响的渠道，用户点进去只会更糊涂。
     */
    private fun notificationChannelCleanup(notificationManager: NotificationManager) {
        OBSOLETE_CHANNEL_IDS.forEach { notificationManager.deleteNotificationChannel(it) }
    }

    private fun createNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle(getString(R.string.app_name))
        .setContentText(getString(R.string.notif_background_body))
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentIntent(
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        .setOngoing(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "PhotosManager::WakeLock"
        ).apply {
            acquire(10 * 60 * 60 * 1000L) // 10小时
        }
    }

    // ---- 亮屏时提醒整理照片（原先是独立的 ScreenMonitorService）----

    private fun registerScreenReceiver() {
        screenReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {
                android.util.Log.d("BackgroundService", "注销旧接收器失败（可能未注册）", e)
            }
        }

        screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_SCREEN_ON ||
                    intent.action == Intent.ACTION_USER_PRESENT
                ) {
                    serviceScope.launch {
                        try {
                            val isEnabled = preferencesRepository.isServiceEnabled.first()
                            if (!isEnabled) return@launch
                            if (inDoNotDisturbNow()) {
                                android.util.Log.d("BackgroundService", "处于免打扰时段，跳过")
                            } else {
                                // 延迟一小段时间确保屏幕完全亮起
                                kotlinx.coroutines.delay(300)
                                startFloatingWindow()
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("BackgroundService", "处理亮屏事件失败", e)
                        }
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT) // 也响应解锁，亮屏后直接进桌面的机器上这条更早到
        }
        try {
            // 只收系统自带广播，用 NOT_EXPORTED 注册；个别 ROM 上 registerReceiver 会抛，兜住别把服务带崩
            ContextCompat.registerReceiver(this, screenReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        } catch (e: Exception) {
            android.util.Log.e("BackgroundService", "注册亮屏广播失败", e)
        }
    }

    /**
     * 免打扰时段内不弹整理界面。时段可能跨午夜（默认 22:00-07:00），
     * 判断放在 PreferencesRepository 里，设置页要显示同样的区间。
     */
    private suspend fun inDoNotDisturbNow(): Boolean {
        if (!preferencesRepository.dndEnabled.first()) return false
        val now = java.util.Calendar.getInstance()
        val nowMinute = now.get(java.util.Calendar.HOUR_OF_DAY) * 60 + now.get(java.util.Calendar.MINUTE)
        return preferencesRepository.inDoNotDisturb(
            preferencesRepository.dndStartMinute.first(),
            preferencesRepository.dndEndMinute.first(),
            nowMinute
        )
    }

    private fun startFloatingWindow() {
        try {
            // 普通服务：进程由本前台服务保住，它只负责挂上/收起那一层界面，不再占一条常驻通知
            startService(Intent(this, FloatingWindowService::class.java))
        } catch (e: Exception) {
            android.util.Log.e("BackgroundService", "拉起整理界面失败", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        screenReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {
                android.util.Log.d("BackgroundService", "注销接收器失败", e)
            }
        }
        screenReceiver = null
        wakeLock?.release()
        wakeLock = null
    }

    companion object {
        private const val CHANNEL_ID = "background_service_channel"
        private const val NOTIFICATION_ID = 1003

        /** 上一版那两个前台服务留下的渠道，收到一条通知之后就没人管了 */
        private val OBSOLETE_CHANNEL_IDS = listOf("screen_monitor_channel", "floating_window_channel")
    }
}
