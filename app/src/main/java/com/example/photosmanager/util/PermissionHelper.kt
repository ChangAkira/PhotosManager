package com.example.photosmanager.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object PermissionHelper {
    fun hasStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun hasOverlayPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.provider.Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    /**
     * 「无感删除」= Android 11+ 的「所有文件访问」特殊权限。授了它，删除当场生效、不再弹系统确认框。
     *
     * 只认系统里的实测结果，应用里不再存一个自己的开关——用户在系统页面反手关掉之后，
     * 应用里的开关会骗人（孪生项目 ScreenshotBye 就是这么改的）。
     * Android 11 以下没有这一项，返回 false；那一版本来也没有删除确认弹窗，一直是直删。
     * 系统方法在个别 ROM 上会抛，兜成 false——顶少了无感删除，不该把设置页带崩。
     */
    fun canDeleteWithoutConfirm(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            runCatching { android.os.Environment.isExternalStorageManager() }.getOrDefault(false)

    /**
     * 跳「所有文件访问」的系统授权页。特殊访问只能用户自己在这里手动打开，应用侧不能弹窗申请。
     * 带包名那条 action 在部分 ROM 上不存在，失败就退回总列表页；两条都不行就什么都不做，
     * 至少行尾的实测状态还是对的。
     */
    fun openAllFilesAccess(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        val direct = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            .setData(android.net.Uri.fromParts("package", context.packageName, null))
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(direct) }.onFailure {
            runCatching {
                context.startActivity(
                    Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }
}


