package com.example.photosmanager.ui.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalContext
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
                items(allAlbums) { album ->
                    // 整行可点，勾选框只是这行的状态显示：点哪儿都算选中，和参考仓库一致
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { toggle(album) }
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
                        Checkbox(
                            checked = enabledAlbums.contains(album),
                            onCheckedChange = { toggle(album) }
                        )
                    }
                }
            }
        }
    }
}
