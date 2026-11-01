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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import com.example.photosmanager.data.repository.PhotoRepository
import com.example.photosmanager.data.repository.PreferencesRepository
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
                title = { Text("相册设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
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
                Text(
                    text = "没有读取到相册",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(allAlbums) { album ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { toggle(album) }
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(album)
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
