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
import com.example.photosmanager.data.repository.ReviewRepository
import com.example.photosmanager.data.session.SessionStore
import com.example.photosmanager.ui.settings.SettingsActivity
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
    val reviewRepository = remember { ReviewRepository(context) }
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
    // 所选相册已经全部判断完。不再自动开新一轮，改成明确的完成页让用户自己决定。
    var allReviewed by remember { mutableStateOf(false) }
    // 完成页要告诉用户"一共多少张都判断过了"
    var reviewedTotal by remember { mutableStateOf(0) }
    // 递增一次就重新抽一轮，用于完成页上的「重新整理一轮」
    var reloadTick by remember { mutableStateOf(0) }

    LaunchedEffect(reloadTick) {
        isLoading = true
        allReviewed = false
        try {
            val albums = preferencesRepository.enabledAlbums.first()
            val sessionCount = preferencesRepository.photosPerSession.first()
            val nearby = preferencesRepository.nearbyPhotosCount.first()

            enabledAlbums = albums
            photosPerSession = sessionCount
            nearbyCount = nearby

            // 先尝试续上未完成的一轮。悬浮窗每次亮屏都会重建，
            // 如果只认 remember，用户中途熄屏就会丢掉进度和待删标记。
            SessionStore.restoreIfNeeded(context)
            if (SessionStore.isActive) {
                photos = SessionStore.queue
                currentIndex = SessionStore.currentIndex
                isLoading = false
                return@LaunchedEffect
            }

            if (albums.isNotEmpty()) {
                val allPhotos = photoRepository.getAllPhotosFromAlbums(albums)
                // 去重：使用Set确保URI不重复
                val uniquePhotos = allPhotos.distinctBy { it.uri }
                // 已经判断过的照片不再重复推送
                val reviewed = reviewRepository.getReviewedUris()
                val eligible = uniquePhotos.filterNot { it.uri in reviewed }
                if (eligible.isEmpty() && uniquePhotos.isNotEmpty()) {
                    // 整个池子都判断完了。这里不能偷偷 startNewCycle 再开一轮，
                    // 用户需要知道"已经全部整理完了"，并自己决定是关服务、换相册，还是重新来一轮。
                    photos = emptyList()
                    allReviewed = true
                    reviewedTotal = uniquePhotos.size
                    isLoading = false
                    return@LaunchedEffect
                }
                // 如果可用照片数量不足，使用所有照片
                val photosToUse = if (eligible.size < photosPerSession) {
                    eligible
                } else {
                    eligible.shuffled().take(photosPerSession)
                }
                val selections = photosToUse.map { PhotoSelection(it) }
                SessionStore.startRound(context, selections)
                photos = selections
                currentIndex = 0
                isLoading = false
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
            // 悬浮窗是系统深色模式下用户看到的最大一块面，跟着配色走而不是写死米白
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (allReviewed) {
            SessionCompleteContent(
                totalPhotos = reviewedTotal,
                onDismiss = onDismiss,
                onOpenSettings = {
                    // 悬浮窗要先收起，否则它会盖在设置页上面
                    context.startActivity(
                        android.content.Intent(context, SettingsActivity::class.java)
                            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                    onDismiss()
                },
                onRestartCycle = {
                    scope.launch {
                        reviewRepository.startNewCycle()
                        reloadTick++
                    }
                }
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
                nearbyEnabled = !currentPhoto.fromNearby,
                onAction = { action ->
                    val position = currentIndex
                    when (action) {
                        PhotoAction.KEEP, PhotoAction.MARK_DELETE -> {
                            SessionStore.setAction(context, position, action)
                            // 记下这张已经判断过，后续轮次不再重复推送
                            reviewRepository.markReviewedAsync(photos[position].photoInfo.uri)
                            if (position < photos.size - 1) {
                                val next = position + 1
                                currentIndex = next
                                SessionStore.setIndex(context, next)
                            } else {
                                val toDelete = photos.filter { it.action == PhotoAction.MARK_DELETE }
                                    .map { it.photoInfo.uri }
                                // 本轮已经判完，会话收尾，下次亮屏重新抽一轮
                                SessionStore.finish(context)
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
                                    nearbyCount
                                )
                                // 去重：确保附近照片不与已有照片重复
                                val existingUris = photos.map { it.photoInfo.uri }.toSet()
                                // 过滤掉重复的照片，只保留不在当前队列中的照片。
                                // 标记 fromNearby：插进来的照片不能再点「附近」，
                                // 否则队列会被无限撑大。
                                val uniqueNearby = nearby
                                    .filter { it.uri !in existingUris }
                                    .map { PhotoSelection(it, fromNearby = true) }
                                if (uniqueNearby.isNotEmpty()) {
                                    // 插入到当前照片之后，进度位置不变
                                    SessionStore.insertAfterCurrent(context, uniqueNearby)
                                    photos = SessionStore.queue
                                }
                            }
                        }
                    }
                },
                onDismiss = onDismiss,
                onSwipeLeft = {
                    if (currentIndex < photos.size - 1) {
                        val next = currentIndex + 1
                        currentIndex = next
                        SessionStore.setIndex(context, next)
                    }
                },
                onSwipeRight = {
                    if (currentIndex > 0) {
                        val previous = currentIndex - 1
                        currentIndex = previous
                        SessionStore.setIndex(context, previous)
                    }
                },
                currentIndex = currentIndex,
                totalCount = photos.size
            )
        }
    }
}

/**
 * 所选相册全部判断完之后的完成页。
 * 目的：让用户知道"这一批整理完了"，而不是被无声地重新塞一轮老照片。
 */
@Composable
fun SessionCompleteContent(
    totalPhotos: Int,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
    onRestartCycle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "关闭")
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "这一批照片全部判断完了",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "所选相册里现有的 $totalPhotos 张都已经判断过了",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "可以去设置里关闭服务或更换相册",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onOpenSettings) {
                    Text("去设置")
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(onClick = onRestartCycle) {
                    Text("清空判断记录，重新整理一轮")
                }
            }
        }
    }
}

@Composable
fun PhotoViewerContent(
    photoSelection: PhotoSelection,
    albumName: String,
    swipeThreshold: Float,
    nearbyEnabled: Boolean,
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
            // 图片容器：Box + verticalScroll 让横向图占满可用宽度、纵向长图仍可滚动。
            // verticalScroll 会把最大高度放开为无限，容器高度取 max(可视高度, 图片高度)，
            // 因此比窗口矮的图片会被居中，比窗口高的图片从头开始滚动、顶部不会被裁掉。
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                contentAlignment = Alignment.Center
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
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Fit,
                        onError = {
                            android.util.Log.e("PhotoViewerContent", "图片加载失败: $imageUri")
                        }
                    )
                } else {
                    Text(
                        text = "无法加载图片",
                        modifier = Modifier.padding(16.dp),
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
                enabled = nearbyEnabled,
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

