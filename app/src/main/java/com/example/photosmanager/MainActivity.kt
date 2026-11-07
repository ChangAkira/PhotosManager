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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.photosmanager.data.repository.PreferencesRepository
import com.example.photosmanager.service.BackgroundService
import com.example.photosmanager.service.ScreenMonitorService
import com.example.photosmanager.ui.components.PaletteCard
import com.example.photosmanager.ui.onboarding.OnboardingActivity
import com.example.photosmanager.ui.settings.SettingsActivity
import com.example.photosmanager.ui.theme.PHOTOSMANAGERTheme
import com.example.photosmanager.util.PermissionHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var preferencesRepository: PreferencesRepository

    /**
     * 权限是系统那边改的，从系统设置页回来只会走 onResume，不会重建本页。
     * 状态放在 Activity 身上、onResume 里刷一次就够了——之前挂的是 2 秒一次的轮询，
     * 页面看不见时也在跑，而且从授权页回来还要等最多两秒才更新。
     */
    private val permissionsGranted = mutableStateOf(false)

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
        preferencesRepository = PreferencesRepository(this)

        enableEdgeToEdge()
        setContent {
            PHOTOSMANAGERTheme {
                MainScreen(
                    hasAllPermissions = permissionsGranted.value,
                    onRequestPermissions = { requestPermissions() },
                    onOpenSettings = {
                        startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                    }
                )
            }
        }

        checkFirstLaunch()
        checkAndStartServices()
    }

    override fun onResume() {
        super.onResume()
        permissionsGranted.value = hasAllCorePermissions(this)
    }

    private fun checkFirstLaunch() {
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
        scope.launch {
            try {
                if (preferencesRepository.isFirstLaunch.first()) {
                    startActivity(Intent(this@MainActivity, OnboardingActivity::class.java))
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "checkFirstLaunch 失败", e)
            }
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

        permissionsGranted.value = true
        checkAndStartServices()
    }

    private fun checkAndStartServices() {
        if (!hasAllCorePermissions(this)) {
            android.util.Log.d("MainActivity", "权限未完全授予，跳过服务启动")
            return
        }
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
        scope.launch {
            try {
                if (preferencesRepository.isServiceEnabled.first()) {
                    startBackgroundService()
                    startScreenMonitorService()
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "检查服务状态失败", e)
            }
        }
    }

    private fun startBackgroundService() {
        try {
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

private fun hasAllCorePermissions(context: Context): Boolean =
    PermissionHelper.hasStoragePermission(context) &&
        PermissionHelper.hasNotificationPermission(context) &&
        PermissionHelper.hasOverlayPermission(context)

/**
 * 首页版式和 ScreenshotBye 的 activity_main.xml 一致：顶部不再叠一条 toolbar
 * （页面没有二级操作，标题就是那行大字），下面居中一列，24dp 内边距，
 * 标题 / 副标题 / 一行运行状态，然后两个整宽按钮，最后是电池优化卡（仅未放行时出现）
 * 与自启动卡（常驻、只给说明不放跳转按钮）。
 *
 * 权限授予状态全部由「请求权限」这一个按钮的启用与文案体现，不再逐行说明，
 * 也不放那条解释「附近」的长提示——那句写进使用引导和 README 更合适。
 */
@Composable
fun MainScreen(
    hasAllPermissions: Boolean,
    onRequestPermissions: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val preferencesRepository = remember { PreferencesRepository(context) }
    val isServiceEnabled by preferencesRepository.isServiceEnabled.collectAsState(initial = false)
    val photosPerSession by preferencesRepository.photosPerSession
        .collectAsState(initial = PreferencesRepository.DEFAULT_PHOTOS_PER_SESSION)

    val scroll = rememberScrollState()
    // 内容不满一屏时垂直居中（和参考仓库的 fillViewport + center_vertical 一样）；
    // 超过一屏就老实地从头排，免得标题被顶出可滚动范围。
    val centered = scroll.maxValue == 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            // targetSdk 36 之后系统强制内容顶到状态栏底下，本页又没有 Scaffold 帮忙留白，
            // 所以系统栏那圈内边距要自己加。加在 verticalScroll 之前，是让滚动视口跟着缩。
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(scroll)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = if (centered) Arrangement.Center else Arrangement.Top
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.home_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isServiceEnabled) stringResource(R.string.main_running, photosPerSession)
            else stringResource(R.string.main_stopped),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onRequestPermissions,
            modifier = Modifier.fillMaxWidth(),
            enabled = !hasAllPermissions
        ) {
            Text(
                stringResource(
                    if (hasAllPermissions) R.string.home_request_perms_done
                    else R.string.home_request_perms
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onOpenSettings,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.home_open_settings))
        }

        // 电池优化卡：只在系统没放行时出现，和参考仓库一致
        if (!isIgnoringBatteryOptimizations(context)) {
            Spacer(modifier = Modifier.height(16.dp))
            PaletteCard(contentPadding = PaddingValues(0.dp)) {
                WarningCardBody(
                    title = stringResource(R.string.home_battery_title),
                    body = stringResource(R.string.home_battery_body),
                    actionText = stringResource(R.string.home_battery_settings),
                    onAction = { openBatteryOptimizationSettings(context) }
                )
            }
        }

        // 自启动引导：常驻，只放说明文字，不放跳转按钮（各家 ROM 入口不同，跳过去也未必是这一页）
        Spacer(modifier = Modifier.height(16.dp))
        PaletteCard(contentPadding = PaddingValues(0.dp)) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.home_autostart_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.home_autostart_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WarningCardBody(
    title: String,
    body: String,
    actionText: String,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = onAction, modifier = Modifier.fillMaxWidth()) {
            Text(actionText)
        }
    }
}

private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        ?: return true
    return runCatching { powerManager.isIgnoringBatteryOptimizations(context.packageName) }
        .getOrDefault(false)
}

private fun openBatteryOptimizationSettings(context: Context) {
    try {
        context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    } catch (e: Exception) {
        android.util.Log.e("MainActivity", "打开电池优化设置失败", e)
    }
}
