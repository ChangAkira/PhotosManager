package com.example.photosmanager

import android.app.Application
import com.example.photosmanager.data.repository.PreferencesRepository
import com.example.photosmanager.ui.theme.ThemeState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class PhotosManagerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val repository = PreferencesRepository(applicationContext)
        // 配色要在整个进程里跟着 DataStore 走：悬浮窗服务、设置页都不是同一个 Activity，
        // 只在某个页面里读一次的话，冷启动弹出的悬浮窗会退回默认米白。
        // 这个 scope 和应用进程同生命周期，不需要取消。
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            repository.themePaletteId
                .catch { emit(PreferencesRepository.DEFAULT_THEME_PALETTE) }
                .collect { ThemeState.select(it) }
        }
    }
}
