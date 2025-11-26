package com.example.photosmanager.ui.floating

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.photosmanager.data.model.PhotoAction
import com.example.photosmanager.data.model.PhotoInfo
import com.example.photosmanager.data.model.PhotoSelection
import com.example.photosmanager.data.repository.PhotoRepository
import com.example.photosmanager.data.repository.PreferencesRepository
import com.example.photosmanager.ui.theme.BeigeBackground
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun FloatingWindowContent(
    onDismiss: () -> Unit,
    onComplete: (List<String>) -> Unit
) {
    val context = LocalContext.current
    val photoRepository = remember { PhotoRepository(context) }
    val preferencesRepository = remember { PreferencesRepository(context) }
    val scope = rememberCoroutineScope()
    
    // 获取屏幕宽度的像素值
    val density = LocalDensity.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val screenWidthPx = remember(screenWidthDp) {
        with(density) { screenWidthDp.dp.toPx() }
    }
    val swipeThreshold = remember(screenWidthPx) { screenWidthPx * 0.1f }

    var photos by remember { mutableStateOf<List<PhotoSelection>>(emptyList()) }
    var currentIndex by remember { mutableStateOf(0) }
    var enabledAlbums by remember { mutableStateOf<Set<String>>(emptySet()) }
    var photosPerSession by remember { mutableStateOf(5) }
    var nearbyCount by remember { mutableStateOf(10) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val albums = preferencesRepository.enabledAlbums.first()
            val sessionCount = preferencesRepository.photosPerSession.first()
            val nearby = preferencesRepository.nearbyPhotosCount.first()
            
            enabledAlbums = albums
            photosPerSession = sessionCount
            nearbyCount = nearby
            
            if (albums.isNotEmpty()) {
                scope.launch {
                    try {
                        val allPhotos = photoRepository.getAllPhotosFromAlbums(albums)
                        // 去重：使用Set确保URI不重复
                        val uniquePhotos = allPhotos.distinctBy { it.uri }
                        // 如果去重后照片数量不足，使用所有照片
                        val photosToUse = if (uniquePhotos.size < sessionCount) {
                            uniquePhotos
                        } else {
                            uniquePhotos.shuffled().take(sessionCount)
                        }
                        photos = photosToUse.map { PhotoSelection(it) }
                        isLoading = false
                    } catch (e: Exception) {
                        android.util.Log.e("FloatingWindowContent", "加载照片失败", e)
                        isLoading = false
                    }
                }
            } else {
                isLoading = false
            }
        } catch (e: Exception) {
            android.util.Log.e("FloatingWindowContent", "初始化失败", e)
            isLoading = false
        }
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BeigeBackground)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (photos.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // 顶部栏：关闭按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                }
                // 居中显示提示信息
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "没有可用的照片",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else if (currentIndex < photos.size) {
            val currentPhoto = photos[currentIndex]
            PhotoViewerContent(
                photoSelection = currentPhoto,
                albumName = currentPhoto.photoInfo.albumName,
                swipeThreshold = swipeThreshold,
                onAction = { action ->
                    photos[currentIndex].action = action
                    when (action) {
                        PhotoAction.KEEP -> {
                            if (currentIndex < photos.size - 1) {
                                currentIndex++
                            } else {
                                val toDelete = photos.filter { it.action == PhotoAction.MARK_DELETE }
                                    .map { it.photoInfo.uri }
                                // 如果没有照片需要删除，直接关闭悬浮窗
                                if (toDelete.isEmpty()) {
                                    onDismiss()
                                } else {
                                    onComplete(toDelete)
                                }
                            }
                        }
                        PhotoAction.MARK_DELETE -> {
                            if (currentIndex < photos.size - 1) {
                                currentIndex++
                            } else {
                                val toDelete = photos.filter { it.action == PhotoAction.MARK_DELETE }
                                    .map { it.photoInfo.uri }
                                // 如果没有照片需要删除，直接关闭悬浮窗
                                if (toDelete.isEmpty()) {
                                    onDismiss()
                                } else {
                                    onComplete(toDelete)
                                }
                            }
                        }
                        PhotoAction.VIEW_NEARBY -> {
                            scope.launch {
                                val nearby = photoRepository.getNearbyPhotos(
                                    currentPhoto.photoInfo,
                                    currentPhoto.photoInfo.albumName,
                                    nearbyCount
                                )
                                // 去重：确保附近照片不与已有照片重复
                                val existingUris = photos.map { it.photoInfo.uri }.toSet()
                                // 过滤掉重复的照片，只保留不在当前队列中的照片
                                val uniqueNearby = nearby.filter { it.uri !in existingUris }
                                if (uniqueNearby.isNotEmpty()) {
                                    val nearbySelections = uniqueNearby.map { PhotoSelection(it) }
                                    // 将去重后的附近照片插入到当前照片之后，保留所有原有照片
                                    val before = photos.take(currentIndex + 1)
                                    val after = photos.drop(currentIndex + 1)
                                    photos = before + nearbySelections + after
                                }
                            }
                        }
                    }
                },
                onDismiss = onDismiss,
                onSwipeLeft = {
                    if (currentIndex < photos.size - 1) {
                        currentIndex++
                    }
                },
                onSwipeRight = {
                    if (currentIndex > 0) {
                        currentIndex--
                    }
                },
                currentIndex = currentIndex,
                totalCount = photos.size
            )
        }
    }
}

@Composable
fun PhotoViewerContent(
    photoSelection: PhotoSelection,
    albumName: String,
    swipeThreshold: Float,
    onAction: (PhotoAction) -> Unit,
    onDismiss: () -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    currentIndex: Int,
    totalCount: Int
) {
    val scrollState = rememberScrollState()
    var dragOffset by remember { mutableStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 顶部栏：相册名称和关闭按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "来自于 $albumName",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "关闭")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 照片显示区域
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .pointerInput(swipeThreshold) {
                    var totalDragX = 0f
                    var hasTriggeredSwipe = false // 防止重复触发
                    val threshold = swipeThreshold // 从参数获取阈值
                    
                    detectDragGestures(
                        onDragEnd = {
                            // 只在拖拽结束时检查：水平拖拽超过屏幕宽度的60%则切换照片
                            if (kotlin.math.abs(totalDragX) > threshold && !hasTriggeredSwipe) {
                                if (totalDragX > 0) {
                                    // 向右滑动，显示上一张
                                    onSwipeRight()
                                } else {
                                    // 向左滑动，显示下一张
                                    onSwipeLeft()
                                }
                            }
                            // 重置
                            totalDragX = 0f
                            hasTriggeredSwipe = false
                        }
                    ) { change: PointerInputChange, dragAmount: Offset ->
                        totalDragX += dragAmount.x
                        // 不在拖拽过程中触发，只在结束时触发
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                // 使用 Coil 加载图片，添加错误处理
                val imageUri = try {
                    Uri.parse(photoSelection.photoInfo.uri)
                } catch (e: Exception) {
                    android.util.Log.e("PhotoViewerContent", "URI解析失败: ${photoSelection.photoInfo.uri}", e)
                    null
                }
                
                if (imageUri != null) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .align(Alignment.CenterHorizontally),
                        contentScale = ContentScale.Fit,
                        onError = {
                            android.util.Log.e("PhotoViewerContent", "图片加载失败: $imageUri")
                        }
                    )
                } else {
                    Text(
                        text = "无法加载图片",
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.CenterHorizontally)
                            .padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // 高度条 - 使用简单的指示器
            if (scrollState.maxValue > 0) {
                val scrollProgress = scrollState.value.toFloat() / scrollState.maxValue.toFloat()
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(4.dp)
                        .padding(vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .background(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp)
                            )
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(scrollProgress.coerceIn(0f, 1f))
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp)
                            )
                            .align(Alignment.TopCenter)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 操作按钮 - 改为文字按钮，使用主题色
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { onAction(PhotoAction.KEEP) },
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
            ) {
                Text("保留")
            }

            Button(
                onClick = { onAction(PhotoAction.MARK_DELETE) },
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
            ) {
                Text("删除")
            }

            Button(
                onClick = { onAction(PhotoAction.VIEW_NEARBY) },
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
            ) {
                Text("附近")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 进度指示
        Text(
            text = "${currentIndex + 1} / $totalCount",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

