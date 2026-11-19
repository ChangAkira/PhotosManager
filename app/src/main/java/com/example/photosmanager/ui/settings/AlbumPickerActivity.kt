package com.example.photosmanager.ui.settings

import android.os.Build
import android.os.Bundle
import android.view.ViewConfiguration
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.photosmanager.R
import com.example.photosmanager.data.repository.PhotoRepository
import com.example.photosmanager.data.repository.PreferencesRepository
import com.example.photosmanager.ui.components.EmptyState
import com.example.photosmanager.ui.theme.PHOTOSMANAGERTheme
import kotlinx.coroutines.launch

/**
 * 相册选择从设置主页收进来的一级页面。
 * 设置页本来只有几项，把几十个相册平铺在上面会把开关、旋钮都挤出首屏。
 */
class AlbumPickerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PHOTOSMANAGERTheme {
                AlbumPickerScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumPickerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val photoRepository = remember { PhotoRepository(context) }
    val preferencesRepository = remember { PreferencesRepository(context) }
    val scope = rememberCoroutineScope()

    var allAlbums by remember { mutableStateOf<List<String>>(emptyList()) }
    val enabledAlbums by preferencesRepository.enabledAlbums.collectAsState(initial = emptySet())
    val flaggedAlbums by preferencesRepository.flaggedAlbums.collectAsState(initial = emptySet())

    LaunchedEffect(Unit) {
        allAlbums = try {
            photoRepository.getAllAlbumNames()
        } catch (e: Exception) {
            android.util.Log.e("AlbumPickerActivity", "读取相册失败", e)
            emptyList()
        }
    }

    fun toggle(album: String) {
        val next = if (enabledAlbums.contains(album)) enabledAlbums - album else enabledAlbums + album
        scope.launch { preferencesRepository.setEnabledAlbums(next) }
    }

    fun toggleFlag(album: String) {
        val next = if (flaggedAlbums.contains(album)) flaggedAlbums - album else flaggedAlbums + album
        scope.launch { preferencesRepository.setFlaggedAlbums(next) }
        // 插旗/取旗那一下"咔"，和旋钮切档用的是同一个反馈
        detentTick(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.album_title)) },
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
        if (allAlbums.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                EmptyState(stringResource(R.string.album_empty))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                item {
                    Text(
                        text = stringResource(R.string.album_hold_hint),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(allAlbums) { album ->
                    AlbumRow(
                        album = album,
                        checked = enabledAlbums.contains(album),
                        flagged = flaggedAlbums.contains(album),
                        onToggleCheck = { toggle(album) },
                        onToggleFlag = { toggleFlag(album) }
                    )
                }
            }
        }
    }
}

/**
 * 一行相册：点一下勾选／取消，长按到底则在选框前插一面小旗当记号。
 *
 * 长按不是"啪"一下就生效的：按住期间从行的左边缘往右铺一条灰色进度条，铺满的那一刻
 * 正好是系统判定长按成立的那一刻（两处读的是同一个时长，见 longPressTimeoutMillis）。
 * 中途松手进度条退回 0，什么也不会改——看得见还剩多少，用户就知道自己要不要再按一会儿，
 * 不至于手指多停了半秒、回来发现莫名其妙多出几面旗。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlbumRow(
    album: String,
    checked: Boolean,
    flagged: Boolean,
    onToggleCheck: () -> Unit,
    onToggleFlag: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    var pressing by remember { mutableStateOf(false) }
    val holdMillis = remember { longPressTimeoutMillis() }
    val fillColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
    val progress by animateFloatAsState(
        targetValue = if (pressing) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (pressing) holdMillis else 140,
            easing = LinearEasing
        ),
        label = "albumFlagHold"
    )

    LaunchedEffect(interaction) {
        interaction.interactions.collect { value ->
            pressing = value is PressInteraction.Press
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        if (progress > 0.001f) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawRect(color = fillColor, size = Size(size.width * progress, size.height))
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    interactionSource = interaction,
                    // 按压反馈交给那条灰色进度条，涟漪就关掉。这里必须是 indication = null 这种写法：
                    // release 变体链到的 foundation 是 1.7.0（BOM 2024.09.00），debug 变体被
                    // debugImplementation 的测试依赖顶到了 1.9.2，而带 interactionSource 的这个重载
                    // 在 1.7.0 里还挂着 @ExperimentalFoundationApi、到 1.9.2 才转正。
                    indication = null,
                    onLongClickLabel = stringResource(R.string.cd_album_flagged),
                    onClick = onToggleCheck,
                    onLongClick = onToggleFlag
                )
                .heightIn(min = 48.dp)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = album,
                modifier = Modifier.weight(1f),
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (flagged) {
                Icon(
                    painter = painterResource(R.drawable.ic_flag),
                    contentDescription = stringResource(R.string.cd_album_flagged),
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Checkbox(
                checked = checked,
                onCheckedChange = { onToggleCheck() }
            )
        }
    }
}

/**
 * 系统判定「长按成立」要按住多久，进度条就走多久——铺满和生效尽量是同一刻。
 *
 * 这个读数只有 API 29 起才有公开接口，再早就是框架里写死的 500ms，Compose 自己
 * 检测长按时也是这么取的，所以这里跟着它，不必另猜一个数。保底 250ms：某些定制
 * ROM 把这个值改得很小，太短的话进度条一闪就满，看不出"我正在长按"这件事。
 */
private fun longPressTimeoutMillis(): Int {
    val millis = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        @Suppress("DEPRECATION")
        ViewConfiguration.getLongPressTimeout()
    } else {
        500
    }
    return millis.coerceAtLeast(250)
}
