package com.example.photosmanager.ui.floating

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import com.example.photosmanager.util.PermissionHelper
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.photosmanager.ui.confirm.ConfirmDeleteActivity

class FloatingWindowManager(private val context: Context) {
    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null
    private var lifecycleOwner: LifecycleOwner? = null

    fun show() {
        try {
            Log.d("FloatingWindowManager", "开始显示悬浮窗")
            // 检查悬浮窗权限
            if (!PermissionHelper.hasOverlayPermission(context)) {
                Log.e("FloatingWindowManager", "没有悬浮窗权限")
                return
            }
            Log.d("FloatingWindowManager", "悬浮窗权限检查通过")
            
            // 如果已经显示，先关闭
            if (composeView != null) {
                Log.d("FloatingWindowManager", "检测到已有悬浮窗，先关闭")
                dismiss()
                // 等待一下确保完全关闭
                Thread.sleep(100)
            }
            
            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            if (windowManager == null) {
                Log.e("FloatingWindowManager", "无法获取 WindowManager")
                return
            }
            Log.d("FloatingWindowManager", "已获取 WindowManager")
            
            // 使用 Application Context 创建 ComposeView
            val appContext = if (context is android.app.Service) {
                context.applicationContext
            } else {
                context
            }
            
            Log.d("FloatingWindowManager", "创建 LifecycleOwner 和 SavedStateRegistryOwner")
            // 创建 LifecycleOwner 和 SavedStateRegistryOwner 用于 ComposeView
            val owner = object : LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
                private val lifecycleRegistry = LifecycleRegistry(this)
                private val savedStateRegistryController = SavedStateRegistryController.create(this)
                private val _viewModelStore = ViewModelStore()
                
                init {
                    // 在 init 中立即调用 performRestore（这是初始化阶段）
                    // 使用空的 Bundle，因为悬浮窗不需要恢复状态
                    try {
                        savedStateRegistryController.performRestore(android.os.Bundle())
                        Log.d("FloatingWindowManager", "performRestore 在 init 中成功")
                    } catch (e: Exception) {
                        Log.e("FloatingWindowManager", "performRestore 在 init 中失败", e)
                        // 如果失败，继续执行，但 Compose 可能会出错
                    }
                    // 设置 Lifecycle 状态为 CREATED（performRestore 之后）
                    lifecycleRegistry.currentState = Lifecycle.State.CREATED
                }
                
                override val lifecycle: Lifecycle
                    get() = lifecycleRegistry
                
                override val savedStateRegistry: SavedStateRegistry
                    get() = savedStateRegistryController.savedStateRegistry
                
                override val viewModelStore: ViewModelStore
                    get() = _viewModelStore
            }
            lifecycleOwner = owner
            
            Log.d("FloatingWindowManager", "创建 ComposeView")
            composeView = ComposeView(appContext).apply {
                // 设置所有必需的 ViewTree 属性 - 必须在添加到 WindowManager 之前设置
                setViewTreeLifecycleOwner(owner)
                setViewTreeSavedStateRegistryOwner(owner)
                setViewTreeViewModelStoreOwner(owner)
            }
            
            // 在设置 LifecycleOwner 后再设置内容
            composeView?.setContent {
                com.example.photosmanager.ui.theme.PHOTOSMANAGERTheme {
                    FloatingWindowContent(
                        onDismiss = { 
                            Log.d("FloatingWindowManager", "用户关闭悬浮窗")
                            dismiss() 
                        },
                        onComplete = { photosToDelete ->
                            Log.d("FloatingWindowManager", "照片检查完成，准备删除 ${photosToDelete.size} 张照片")
                            dismiss()
                            // 只有当有照片需要删除时才显示确认删除界面
                            if (photosToDelete.isNotEmpty()) {
                                showConfirmDeleteActivity(photosToDelete)
                            }
                        }
                    )
                }
            }
            
            // 在添加到 WindowManager 之前，确保 Lifecycle 状态正确
            (lifecycleOwner?.lifecycle as? LifecycleRegistry)?.currentState = Lifecycle.State.STARTED
            (lifecycleOwner?.lifecycle as? LifecycleRegistry)?.currentState = Lifecycle.State.RESUMED

            // 获取屏幕尺寸
            val displayMetrics = android.content.res.Resources.getSystem().displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels
            val windowHeight = (screenHeight * 0.75).toInt()
            
            Log.d("FloatingWindowManager", "屏幕尺寸: ${screenWidth}x${screenHeight}, 悬浮窗高度: $windowHeight")

            val params = WindowManager.LayoutParams().apply {
                type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                }
                format = PixelFormat.TRANSLUCENT
                // FLAG_NOT_FOCUSABLE: 不获取焦点，但可以接收触摸事件
                // FLAG_LAYOUT_IN_SCREEN: 允许窗口延伸到屏幕外
                // FLAG_HARDWARE_ACCELERATED: 硬件加速
                // FLAG_NOT_TOUCH_MODAL: 允许触摸事件传递给后面的窗口（当触摸在窗口外时）
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = windowHeight
                gravity = Gravity.BOTTOM
                x = 0
                y = 0
            }

            Log.d("FloatingWindowManager", "添加悬浮窗到 WindowManager")
            windowManager?.addView(composeView, params)
            Log.d("FloatingWindowManager", "悬浮窗已成功显示")
        } catch (e: Exception) {
            Log.e("FloatingWindowManager", "显示悬浮窗失败", e)
            e.printStackTrace()
        }
    }

    fun dismiss() {
        try {
            // 先停止 Lifecycle 和清理 ViewModelStore
            lifecycleOwner?.let { owner ->
                val lifecycle = owner.lifecycle
                if (lifecycle is LifecycleRegistry) {
                    lifecycle.currentState = Lifecycle.State.DESTROYED
                }
                // 清理 ViewModelStore
                if (owner is ViewModelStoreOwner) {
                    owner.viewModelStore.clear()
                }
            }
            
            composeView?.let { view ->
                windowManager?.removeView(view)
            }
        } catch (e: Exception) {
            Log.e("FloatingWindowManager", "关闭悬浮窗失败", e)
        } finally {
            composeView = null
            windowManager = null
            lifecycleOwner = null
        }
    }

    private fun showConfirmDeleteActivity(photosToDelete: List<String>) {
        val intent = Intent(context, ConfirmDeleteActivity::class.java).apply {
            putStringArrayListExtra("photos_to_delete", ArrayList(photosToDelete))
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

