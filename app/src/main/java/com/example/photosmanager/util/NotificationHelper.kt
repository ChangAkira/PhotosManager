package com.example.photosmanager.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.photosmanager.R

object NotificationHelper {
    private const val CHANNEL_ID = "photos_manager_channel"

    /** 喜报用固定 id：下一次破百直接覆盖上一条，通知栏不会攒出一串 */
    private const val MILESTONE_NOTIFICATION_ID = 2001

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.ch_service),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.ch_service_desc)
            }
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 累计删除数破整百时的那条喜报。走「照片整理结果」这条渠道，
     * 和常驻的「后台服务运行中」不是一条，用户可以在系统设置里单独关掉它。
     */
    fun showDeletedMilestone(context: Context, milestone: Int) {
        createNotificationChannel(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.notif_milestone_body, milestone))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(MILESTONE_NOTIFICATION_ID, notification)
    }
}


