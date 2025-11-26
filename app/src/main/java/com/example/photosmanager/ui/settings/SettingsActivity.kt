package com.example.photosmanager.ui.settings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.photosmanager.data.repository.PhotoRepository
import com.example.photosmanager.data.repository.PreferencesRepository
import com.example.photosmanager.service.BackgroundService
import com.example.photosmanager.service.FloatingWindowService
import com.example.photosmanager.service.ScreenMonitorService
import com.example.photosmanager.ui.theme.PHOTOSMANAGERTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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
    val context = androidx.compose.ui.platform.LocalContext.current
    val preferencesRepository = remember { PreferencesRepository(context) }
    val photoRepository = remember { PhotoRepository(context) }
    val scope = rememberCoroutineScope()

    var photosPerSession by remember { mutableIntStateOf(5) }
    var nearbyCount by remember { mutableIntStateOf(10) }
    var enabledAlbums by remember { mutableStateOf<Set<String>>(emptySet()) }
    var allAlbums by remember { mutableStateOf<List<String>>(emptyList()) }
    var isServiceEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        photosPerSession = preferencesRepository.photosPerSession.first()
        nearbyCount = preferencesRepository.nearbyPhotosCount.first()
        enabledAlbums = preferencesRepository.enabledAlbums.first()
        isServiceEnabled = preferencesRepository.isServiceEnabled.first()
        
        scope.launch {
            allAlbums = photoRepository.getAllAlbumNames()
        }
    }

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
                Text(
                    text = "基本设置",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("每次显示照片数量")
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = photosPerSession.toFloat(),
                            onValueChange = { newValue ->
                                val intValue = newValue.toInt()
                                photosPerSession = intValue
                                scope.launch {
                                    preferencesRepository.setPhotosPerSession(intValue)
                                }
                            },
                            valueRange = 1f..20f,
                            steps = 18
                        )
                        Text("${photosPerSession} 张")
                    }
                }
            }

            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("附近照片数量")
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = nearbyCount.toFloat(),
                            onValueChange = { newValue ->
                                val intValue = newValue.toInt()
                                nearbyCount = intValue
                                scope.launch {
                                    preferencesRepository.setNearbyPhotosCount(intValue)
                                }
                            },
                            valueRange = 5f..30f,
                            steps = 24
                        )
                        Text("${nearbyCount} 张")
                    }
                }
            }

            item {
                Text(
                    text = "服务设置",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            item {
                Card {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("启用服务")
                        Switch(
                            checked = isServiceEnabled,
                            onCheckedChange = { enabled ->
                                isServiceEnabled = enabled
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
                Text(
                    text = "相册设置",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            items(allAlbums) { album ->
                Card(
                    onClick = {
                        enabledAlbums = if (enabledAlbums.contains(album)) {
                            enabledAlbums - album
                        } else {
                            enabledAlbums + album
                        }
                        scope.launch {
                            preferencesRepository.setEnabledAlbums(enabledAlbums)
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(album)
                        Checkbox(
                            checked = enabledAlbums.contains(album),
                            onCheckedChange = {
                                enabledAlbums = if (enabledAlbums.contains(album)) {
                                    enabledAlbums - album
                                } else {
                                    enabledAlbums + album
                                }
                                scope.launch {
                                    preferencesRepository.setEnabledAlbums(enabledAlbums)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

