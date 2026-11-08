package com.example.photosmanager.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.example.photosmanager.ui.floating.FloatingWindowManager

/**
 * 只在亮屏时被 BackgroundService 拉起来，挂上那一层整理界面。
 *
 * 它以前是前台服务，代价是多一条常驻通知（「照片管理界面」）。
 * 界面挂在 WindowManager 上，跟服务是不是前台服务没关系，只跟进程活着有没有人管有关系——
 * 进程由 BackgroundService 那条前台服务保着，所以这里降级成普通服务，通知就少一条。
 */
class FloatingWindowService : Service() {
    private var floatingWindowManager: FloatingWindowManager? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            if (floatingWindowManager == null) {
                floatingWindowManager = FloatingWindowManager(this)
            }
            // 每次调用都尝试显示（如果已经显示会先关闭再显示）
            floatingWindowManager?.show()
        } catch (e: Exception) {
            android.util.Log.e("FloatingWindowService", "启动悬浮窗失败", e)
            e.printStackTrace()
        }
        // 不 START_STICKY：这本来就是个用完即走的界面，重启后凭空再弹一次反而莫名其妙
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // 当任务被移除时，收起界面
        floatingWindowManager?.dismiss()
    }

    override fun onDestroy() {
        super.onDestroy()
        floatingWindowManager?.dismiss()
        floatingWindowManager = null
    }
}
