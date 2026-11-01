package com.example.photosmanager.ui.settings

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.photosmanager.data.repository.PreferencesRepository
import com.example.photosmanager.service.BackgroundService
import com.example.photosmanager.service.FloatingWindowService
import com.example.photosmanager.service.ScreenMonitorService
import com.example.photosmanager.ui.theme.PHOTOSMANAGERTheme
import com.example.photosmanager.ui.theme.Palette
import com.example.photosmanager.ui.theme.PaletteCatalog
import com.example.photosmanager.ui.theme.ThemeState
import kotlinx.coroutines.launch
import java.util.Locale

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PHOTOSMANAGERTheme {
                SettingsScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val preferencesRepository = remember { PreferencesRepository(context) }
    val scope = rememberCoroutineScope()

    // 全部直接订阅 DataStore：从相册次级页面返回、换主题之后，这一页要立刻反映最新值，
    // 之前手动记一份 var 的写法会留下过期状态。
    val photosPerSession by preferencesRepository.photosPerSession
        .collectAsState(initial = PreferencesRepository.DEFAULT_PHOTOS_PER_SESSION)
    val nearbyCount by preferencesRepository.nearbyPhotosCount
        .collectAsState(initial = PreferencesRepository.DEFAULT_NEARBY_PHOTOS_COUNT)
    val isServiceEnabled by preferencesRepository.isServiceEnabled
        .collectAsState(initial = false)
    val dndEnabled by preferencesRepository.dndEnabled
        .collectAsState(initial = false)
    val dndStart by preferencesRepository.dndStartMinute
        .collectAsState(initial = PreferencesRepository.DEFAULT_DND_START)
    val dndEnd by preferencesRepository.dndEndMinute
        .collectAsState(initial = PreferencesRepository.DEFAULT_DND_END)
    val enabledAlbums by preferencesRepository.enabledAlbums
        .collectAsState(initial = emptySet())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("启用服务")
                        Switch(
                            checked = isServiceEnabled,
                            onCheckedChange = { enabled ->
                                scope.launch {
                                    preferencesRepository.setServiceEnabled(enabled)
                                    // 根据开关状态启动或停止服务
                                    if (enabled) {
                                        // 启用服务：启动服务（需要检查权限，但这里只负责启动）
                                        val intentBackground = Intent(context, BackgroundService::class.java)
                                        val intentScreen = Intent(context, ScreenMonitorService::class.java)
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                            context.startForegroundService(intentBackground)
                                            context.startForegroundService(intentScreen)
                                        } else {
                                            context.startService(intentBackground)
                                            context.startService(intentScreen)
                                        }
                                    } else {
                                        // 禁用服务：停止所有服务
                                        val intentBackground = Intent(context, BackgroundService::class.java)
                                        val intentScreen = Intent(context, ScreenMonitorService::class.java)
                                        val intentFloating = Intent(context, FloatingWindowService::class.java)
                                        context.stopService(intentBackground)
                                        context.stopService(intentScreen)
                                        context.stopService(intentFloating)
                                    }
                                }
                            }
                        )
                    }
                }
            }

            item {
                Card {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        DetentDial(
                            label = "每次显示",
                            value = photosPerSession,
                            presets = SESSION_PRESETS,
                            modifier = Modifier.weight(1f),
                            onValueChange = { newValue ->
                                scope.launch { preferencesRepository.setPhotosPerSession(newValue) }
                            }
                        )
                        DetentDial(
                            label = "加入附近",
                            value = nearbyCount,
                            presets = NEARBY_PRESETS,
                            modifier = Modifier.weight(1f),
                            onValueChange = { newValue ->
                                scope.launch { preferencesRepository.setNearbyPhotosCount(newValue) }
                            }
                        )
                    }
                }
            }

            item {
                Card {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("免打扰时段")
                            Switch(
                                checked = dndEnabled,
                                onCheckedChange = { enabled ->
                                    scope.launch { preferencesRepository.setDndEnabled(enabled) }
                                }
                            )
                        }
                        if (dndEnabled) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                DndTimeField(
                                    caption = "开始",
                                    minute = dndStart,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        pickTime(context, dndStart) { hour, minute ->
                                            scope.launch {
                                                preferencesRepository.setDndStartMinute(hour * 60 + minute)
                                            }
                                        }
                                    }
                                )
                                Text(
                                    text = "—",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                                DndTimeField(
                                    caption = "结束",
                                    minute = dndEnd,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        pickTime(context, dndEnd) { hour, minute ->
                                            scope.launch {
                                                preferencesRepository.setDndEndMinute(hour * 60 + minute)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    onClick = {
                        context.startActivity(Intent(context, AlbumPickerActivity::class.java))
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("相册设置")
                        Text(
                            text = if (enabledAlbums.isEmpty()) "未选择" else "已选 ${enabledAlbums.size} 个",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            item {
                Card {
                    Column(modifier = Modifier.padding(vertical = 12.dp)) {
                        Text(
                            text = "主题颜色",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            PaletteCatalog.ordered.forEach { palette ->
                                ThemeSwatch(
                                    palette = palette,
                                    isSelected = palette.id == ThemeState.palette.id,
                                    onClick = {
                                        scope.launch { preferencesRepository.setThemePalette(palette.id) }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 一个色块代表一套配色，选中状态用同色描边的外圈表示，不额外写主题名 */
@Composable
private fun ThemeSwatch(
    palette: Palette,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val ring = if (isSelected) palette.swatch else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .border(2.dp, ring, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .semantics {
                role = Role.RadioButton
                selected = isSelected
                contentDescription = palette.displayName
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(palette.swatch)
        )
    }
}

@Composable
private fun DndTimeField(
    caption: String,
    minute: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clockText = formatMinute(minute)
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "$caption $clockText" }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = clockText,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = caption,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

private fun formatMinute(minute: Int): String =
    String.format(Locale.US, "%02d:%02d", minute / 60, minute % 60)

private fun pickTime(
    context: Context,
    initialMinute: Int,
    onPicked: (hour: Int, minute: Int) -> Unit
) {
    TimePickerDialog(
        context,
        { _, hour, minute -> onPicked(hour, minute) },
        initialMinute / 60,
        initialMinute % 60,
        android.text.format.DateFormat.is24HourFormat(context)
    ).show()
}
