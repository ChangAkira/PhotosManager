package com.example.photosmanager.ui.settings

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.photosmanager.R
import com.example.photosmanager.data.repository.PreferencesRepository
import com.example.photosmanager.service.BackgroundService
import com.example.photosmanager.service.FloatingWindowService
import com.example.photosmanager.service.ScreenMonitorService
import com.example.photosmanager.ui.about.AboutActivity
import com.example.photosmanager.ui.components.PaletteCard
import com.example.photosmanager.ui.components.SectionDivider
import com.example.photosmanager.ui.components.SectionTitle
import com.example.photosmanager.ui.components.SettingRow
import com.example.photosmanager.ui.onboarding.OnboardingActivity
import com.example.photosmanager.ui.theme.PHOTOSMANAGERTheme
import com.example.photosmanager.ui.theme.Palette
import com.example.photosmanager.ui.theme.PaletteCatalog
import com.example.photosmanager.ui.theme.ThemeState
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * 设置页四块，和孪生项目 ScreenshotBye 的分卡方式一致：
 * ① 启用服务（独立一卡，只有一个开关行）②旋钮 ③免打扰时段 ④主题颜色 + 次级入口。
 * 次级页面（相册、使用引导、关于）收进最后一张卡，免得几十本相册把开关和旋钮挤出首屏。
 */
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PHOTOSMANAGERTheme {
                SettingsScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
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
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ① 启用服务：独立一卡
            item {
                PaletteCard {
                    SettingRow(
                        label = stringResource(R.string.main_service_switch),
                        trailing = {
                            Switch(
                                checked = isServiceEnabled,
                                onCheckedChange = { enabled ->
                                    scope.launch {
                                        preferencesRepository.setServiceEnabled(enabled)
                                        if (enabled) startServices(context) else stopServices(context)
                                    }
                                }
                            )
                        }
                    )
                }
            }

            // ② 两个旋钮并排：可选值就那几档，摊成一圈数字比连续滑块好点
            item {
                PaletteCard(contentPadding = PaddingValues(horizontal = 2.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        DetentDial(
                            label = stringResource(R.string.dial_session),
                            value = photosPerSession,
                            presets = SESSION_PRESETS,
                            unitRes = R.string.unit_pics,
                            modifier = Modifier.weight(1f),
                            onValueChange = { newValue ->
                                scope.launch { preferencesRepository.setPhotosPerSession(newValue) }
                            }
                        )
                        DetentDial(
                            label = stringResource(R.string.dial_nearby),
                            value = nearbyCount,
                            presets = NEARBY_PRESETS,
                            unitRes = R.string.unit_pics,
                            modifier = Modifier.weight(1f),
                            onValueChange = { newValue ->
                                scope.launch { preferencesRepository.setNearbyPhotosCount(newValue) }
                            }
                        )
                    }
                }
            }

            // ③ 免打扰时段：只管"下一次亮屏要不要浮出来"
            item {
                PaletteCard {
                    SettingRow(
                        label = stringResource(R.string.dnd_title),
                        trailing = {
                            Switch(
                                checked = dndEnabled,
                                onCheckedChange = { enabled ->
                                    scope.launch { preferencesRepository.setDndEnabled(enabled) }
                                }
                            )
                        }
                    )
                    if (dndEnabled) {
                        SectionDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DndTimeField(
                                captionRes = R.string.dnd_start,
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
                                color = MaterialTheme.colorScheme.outline
                            )
                            DndTimeField(
                                captionRes = R.string.dnd_end,
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

            // ④ 主题颜色 + 次级入口
            item {
                PaletteCard(contentPadding = PaddingValues(vertical = 12.dp)) {
                    SectionTitle(stringResource(R.string.settings_theme))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
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

                    SectionDivider(verticalMargin = 8.dp)

                    SettingRow(
                        label = stringResource(R.string.main_open_guide),
                        onClick = {
                            context.startActivity(Intent(context, OnboardingActivity::class.java))
                        }
                    )
                    SettingRow(
                        label = stringResource(R.string.settings_album),
                        value = if (enabledAlbums.isEmpty()) stringResource(R.string.settings_album_none)
                        else stringResource(R.string.settings_album_count, enabledAlbums.size),
                        onClick = {
                            context.startActivity(Intent(context, AlbumPickerActivity::class.java))
                        }
                    )
                    SettingRow(
                        label = stringResource(R.string.settings_about),
                        onClick = {
                            context.startActivity(Intent(context, AboutActivity::class.java))
                        }
                    )
                }
            }
        }
    }
}

private fun startServices(context: Context) {
    val targets = listOf(BackgroundService::class.java, ScreenMonitorService::class.java)
    targets.forEach { service ->
        val intent = Intent(context, service)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}

private fun stopServices(context: Context) {
    listOf(
        BackgroundService::class.java,
        ScreenMonitorService::class.java,
        FloatingWindowService::class.java
    ).forEach { context.stopService(Intent(context, it)) }
}

/**
 * 一个色块代表一套配色：只有选中那个带同色粗环，其余是细细的 outline 环，
 * 和 ScreenshotBye 的 swatchView 一样。环比色块大 10dp，点起来是一整块 40dp。
 */
@Composable
private fun ThemeSwatch(
    palette: Palette,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val name = stringResource(palette.nameRes)
    val ringColor = if (isSelected) palette.swatch else MaterialTheme.colorScheme.outline
    val ringWidth = if (isSelected) 3.dp else 1.5.dp
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .border(ringWidth, ringColor, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .semantics {
                role = Role.RadioButton
                selected = isSelected
                contentDescription = name
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

/** 免打扰的起止时间：整块可点，点开是系统时间选择框 */
@Composable
private fun DndTimeField(
    @StringRes captionRes: Int,
    minute: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val caption = stringResource(captionRes)
    val clockText = formatMinute(minute)
    // semantics 里不能调 stringResource（不是组合作用域），先在这里取好
    val description = stringResource(R.string.dnd_time_desc, caption, clockText)
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description }
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
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** 时间戳读法："开始 22:00"，给读屏用 */
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
