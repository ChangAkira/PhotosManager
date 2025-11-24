package com.example.photosmanager.service

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.photosmanager.R
import com.example.photosmanager.ui.floating.FloatingWindowManager

class FloatingWindowService : Service() {
    private var floatingWindowManager: FloatingWindowManager? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        try {
            startForeground(NOTIFICATION_ID, createNotification())
        } catch (e: Exception) {
            android.util.Log.e("FloatingWindowService", "启动前台服务失败", e)
        }
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                CHANNEL_ID,
                "悬浮窗服务",
                android.app.NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "悬浮窗服务通知"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(android.app.NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            android.util.Log.d("FloatingWindowService", "onStartCommand 被调用")
            if (floatingWindowManager == null) {
                android.util.Log.d("FloatingWindowService", "创建 FloatingWindowManager")
                floatingWindowManager = FloatingWindowManager(this)
            }
            // 每次调用都尝试显示（如果已经显示会先关闭再显示）
            android.util.Log.d("FloatingWindowService", "调用 show() 显示悬浮窗")
            floatingWindowManager?.show()
        } catch (e: Exception) {
            android.util.Log.e("FloatingWindowService", "启动悬浮窗失败", e)
            e.printStackTrace()
        }
        // 使用 START_STICKY 确保服务被系统杀死后能重启
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // 当任务被移除时，关闭悬浮窗但不停止服务
        android.util.Log.d("FloatingWindowService", "任务被移除，关闭悬浮窗")
        floatingWindowManager?.dismiss()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        android.util.Log.d("FloatingWindowService", "onDestroy 被调用")
        floatingWindowManager?.dismiss()
        floatingWindowManager = null
        // 注意：这里不停止服务，让服务继续运行以便下次显示
    }

    private fun createNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("PhotosManager")
        .setContentText("照片管理界面")
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setOngoing(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()

    companion object {
        private const val CHANNEL_ID = "floating_window_channel"
        private const val NOTIFICATION_ID = 1002
    }
}

