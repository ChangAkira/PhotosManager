package com.example.photosmanager.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.photosmanager.R
import com.example.photosmanager.data.repository.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ScreenMonitorService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var preferencesRepository: PreferencesRepository
    private var screenReceiver: BroadcastReceiver? = null

    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("ScreenMonitorService", "onCreate 开始")
        createNotificationChannel()
        preferencesRepository = PreferencesRepository(this)
        try {
            startForeground(NOTIFICATION_ID, createNotification())
            android.util.Log.d("ScreenMonitorService", "前台服务已启动")
        } catch (e: Exception) {
            android.util.Log.e("ScreenMonitorService", "启动前台服务失败", e)
        }
        // 在onCreate时就注册接收器，确保能立即监听
        registerScreenReceiver()
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                CHANNEL_ID,
                "屏幕监听",
                android.app.NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "屏幕监听服务通知"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(android.app.NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        registerScreenReceiver()
        // 使用START_STICKY确保服务被杀死后自动重启
        return START_STICKY
    }
    
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // 当任务被移除时（如"全部清除"），尝试重启服务
        android.util.Log.d("ScreenMonitorService", "任务被移除，尝试重启服务")
        val restartIntent = Intent(applicationContext, ScreenMonitorService::class.java)
        restartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(restartIntent)
        } else {
            startService(restartIntent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun registerScreenReceiver() {
        android.util.Log.d("ScreenMonitorService", "注册屏幕监听接收器")
        // 先注销旧的接收器（如果存在）
        screenReceiver?.let { 
            try {
                unregisterReceiver(it)
                android.util.Log.d("ScreenMonitorService", "已注销旧的接收器")
            } catch (e: Exception) {
                android.util.Log.d("ScreenMonitorService", "注销旧接收器失败（可能未注册）", e)
            }
        }
        
        screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                android.util.Log.d("ScreenMonitorService", "收到广播: ${intent.action}")
                if (intent.action == Intent.ACTION_SCREEN_ON) {
                    android.util.Log.d("ScreenMonitorService", "屏幕已亮起，准备显示悬浮窗")
                    serviceScope.launch {
                        try {
                            val isEnabled = preferencesRepository.isServiceEnabled.first()
                            android.util.Log.d("ScreenMonitorService", "服务启用状态: $isEnabled")
                            if (isEnabled && inDoNotDisturbNow()) {
                                android.util.Log.d("ScreenMonitorService", "处于免打扰时段，跳过显示悬浮窗")
                            } else if (isEnabled) {
                                // 延迟一小段时间确保屏幕完全亮起
                                kotlinx.coroutines.delay(300)
                                android.util.Log.d("ScreenMonitorService", "启动悬浮窗服务")
                                startFloatingWindow()
                            } else {
                                android.util.Log.d("ScreenMonitorService", "服务未启用，跳过显示悬浮窗")
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("ScreenMonitorService", "处理屏幕亮起事件失败", e)
                        }
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT) // 也监听用户解锁
        }
        try {
            registerReceiver(screenReceiver, filter)
            android.util.Log.d("ScreenMonitorService", "屏幕监听接收器注册成功")
        } catch (e: Exception) {
            android.util.Log.e("ScreenMonitorService", "注册屏幕监听失败", e)
        }
    }

    /**
     * 免打扰时段内不弹悬浮窗。时段可能跨午夜（默认 22:00-07:00），
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
            android.util.Log.d("ScreenMonitorService", "创建悬浮窗服务Intent")
            val intent = Intent(this, FloatingWindowService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(intent)
                android.util.Log.d("ScreenMonitorService", "已启动前台服务: FloatingWindowService")
            } else {
                startService(intent)
                android.util.Log.d("ScreenMonitorService", "已启动服务: FloatingWindowService")
            }
        } catch (e: Exception) {
            android.util.Log.e("ScreenMonitorService", "启动悬浮窗服务失败", e)
        }
    }

    private fun createNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("PhotosManager")
        .setContentText("屏幕监听服务运行中")
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setOngoing(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()

    override fun onDestroy() {
        super.onDestroy()
        screenReceiver?.let { unregisterReceiver(it) }
    }

    companion object {
        private const val CHANNEL_ID = "screen_monitor_channel"
        private const val NOTIFICATION_ID = 1001
    }
}

