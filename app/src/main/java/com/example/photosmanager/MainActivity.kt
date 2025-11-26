package com.example.photosmanager

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.photosmanager.data.repository.PreferencesRepository
import com.example.photosmanager.service.BackgroundService
import com.example.photosmanager.service.ScreenMonitorService
import com.example.photosmanager.ui.onboarding.OnboardingActivity
import com.example.photosmanager.ui.settings.SettingsActivity
import com.example.photosmanager.ui.theme.PHOTOSMANAGERTheme
import com.example.photosmanager.util.PermissionHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var preferencesRepository: PreferencesRepository

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // 继续请求其他权限
            requestPermissions()
        }
    }
    
    private val requestMultiplePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // 检查所有权限是否已授予
        requestPermissions()
    }

    private val requestOverlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        checkAndStartServices()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        android.util.Log.d("MainActivity", "onCreate 开始")
        
        try {
            android.util.Log.d("MainActivity", "初始化 PreferencesRepository")
            preferencesRepository = PreferencesRepository(this)
            
            android.util.Log.d("MainActivity", "设置 Compose 内容")
            enableEdgeToEdge()
            setContent {
                PHOTOSMANAGERTheme {
                    MainScreen(
                        onRequestPermissions = { 
                            try {
                                requestPermissions()
                            } catch (e: Exception) {
                                android.util.Log.e("MainActivity", "请求权限失败", e)
                            }
                        },
                        onOpenSettings = { 
                            try {
                                startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                            } catch (e: Exception) {
                                android.util.Log.e("MainActivity", "打开设置失败", e)
                            }
                        }
                    )
                }
            }

            android.util.Log.d("MainActivity", "检查首次启动")
            // 检查是否需要显示引导
            checkFirstLaunch()
            
            android.util.Log.d("MainActivity", "检查并启动服务")
            // 检查权限并启动服务
            checkAndStartServices()
            
            android.util.Log.d("MainActivity", "onCreate 完成")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "onCreate 失败", e)
            e.printStackTrace()
            // 不直接 finish，而是显示错误信息
            try {
                setContent {
                    androidx.compose.material3.Text(
                        "应用启动失败: ${e.message}\n请查看日志获取详细信息",
                        modifier = androidx.compose.ui.Modifier.padding(16.dp)
                    )
                }
            } catch (e2: Exception) {
                android.util.Log.e("MainActivity", "显示错误信息也失败", e2)
                finish()
            }
        }
    }

    private fun checkFirstLaunch() {
        try {
            val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
            scope.launch {
                try {
                    val isFirstLaunch = preferencesRepository.isFirstLaunch.first()
                    if (isFirstLaunch) {
                        startActivity(Intent(this@MainActivity, OnboardingActivity::class.java))
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "checkFirstLaunch 失败", e)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "checkFirstLaunch 初始化失败", e)
        }
    }

    private fun requestPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        
        // 检查并添加存储权限
        if (!PermissionHelper.hasStoragePermission(this)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                // Android 10以下还需要写权限
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
        }

        // 检查并添加通知权限
        if (!PermissionHelper.hasNotificationPermission(this)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // 如果有需要请求的权限，批量请求
        if (permissionsToRequest.isNotEmpty()) {
            requestMultiplePermissionsLauncher.launch(permissionsToRequest.toTypedArray())
            return
        }

        // 请求悬浮窗权限（需要跳转到设置）
        if (!PermissionHelper.hasOverlayPermission(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            requestOverlayPermissionLauncher.launch(intent)
            return
        }

        checkAndStartServices()
    }

    private fun checkAndStartServices() {
        try {
            if (!PermissionHelper.hasStoragePermission(this) ||
                !PermissionHelper.hasNotificationPermission(this) ||
                !PermissionHelper.hasOverlayPermission(this)) {
                android.util.Log.d("MainActivity", "权限未完全授予，跳过服务启动")
                return
            }

            android.util.Log.d("MainActivity", "所有权限已授予，检查服务状态")
            val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
            scope.launch {
                try {
                    val isServiceEnabled = preferencesRepository.isServiceEnabled.first()
                    android.util.Log.d("MainActivity", "服务启用状态: $isServiceEnabled")
                    if (isServiceEnabled) {
                        startBackgroundService()
                        startScreenMonitorService()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "检查服务状态失败", e)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "checkAndStartServices 失败", e)
        }
    }

    private fun startBackgroundService() {
        try {
            android.util.Log.d("MainActivity", "启动后台服务")
            val intent = Intent(this, BackgroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "启动后台服务失败", e)
        }
    }

    private fun startScreenMonitorService() {
        try {
            android.util.Log.d("MainActivity", "启动屏幕监听服务")
            val intent = Intent(this, ScreenMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "启动屏幕监听服务失败", e)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onRequestPermissions: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var hasAllPermissions by remember { mutableStateOf(false) }
    
    // 实时检查权限状态
    LaunchedEffect(Unit) {
        hasAllPermissions = PermissionHelper.hasStoragePermission(context) &&
                PermissionHelper.hasNotificationPermission(context) &&
                PermissionHelper.hasOverlayPermission(context)
    }
    
    // 当权限状态可能改变时重新检查
    DisposableEffect(Unit) {
        val checkPermissions = {
            hasAllPermissions = PermissionHelper.hasStoragePermission(context) &&
                    PermissionHelper.hasNotificationPermission(context) &&
                    PermissionHelper.hasOverlayPermission(context)
        }
        
        // 定期检查权限状态
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                try {
                    checkPermissions()
                    handler.postDelayed(this, 2000) // 改为2秒检查一次
                } catch (e: Exception) {
                    // 忽略错误
                }
            }
        }
        handler.post(runnable)
        
        onDispose {
            try {
                handler.removeCallbacks(runnable)
            } catch (e: Exception) {
                // 忽略错误
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PhotosManager") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "PhotosManager",
                style = MaterialTheme.typography.headlineLarge
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "帮助您在碎片时间里管理手机相册",
                style = MaterialTheme.typography.bodyLarge
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "tip: 点击附近可将当前照片的附近几张照片添加进考察队列，因为无用照片的附近很可能也是无用照片~",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = onRequestPermissions,
                modifier = Modifier.fillMaxWidth(),
                enabled = !hasAllPermissions
            ) {
                Text(if (hasAllPermissions) "权限已授予" else "请求权限")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("设置")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 电池优化引导
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                val isIgnoringBatteryOptimizations = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    powerManager.isIgnoringBatteryOptimizations(context.packageName)
                } else {
                    true
                }
                
                if (!isIgnoringBatteryOptimizations) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "为了确保应用正常运行",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "建议将应用加入白名单或忽略电池优化，以防止被系统清理",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Button(
                                onClick = {
                                    try {
                                        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        android.util.Log.e("MainActivity", "打开电池优化设置失败", e)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("前往设置")
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 应用启动管理引导（针对华为/荣耀等厂商）
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "为保障稳定运行",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "请关闭系统对应用的自动管理（设置搜索应用启动管理），并打开\"允许自启动\"\"允许后台活动\"选项",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}