package com.example.photosmanager.ui.confirm

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.photosmanager.data.repository.PhotoRepository
import com.example.photosmanager.data.repository.ReviewRepository
import com.example.photosmanager.ui.theme.PHOTOSMANAGERTheme
import kotlinx.coroutines.launch

class ConfirmDeleteActivity : ComponentActivity() {
    private val deleteRequestCode = 1001

    /** 本轮在悬浮窗里被标记为待删的全部照片 */
    private val requestedPhotos: ArrayList<String> by lazy {
        intent.getStringArrayListExtra("photos_to_delete") ?: arrayListOf()
    }

    /** 用户在确认页真正勾选、并且通过系统授权要删除的照片 */
    private var selectedForDelete: List<String> = emptyList()

    private val reviewRepository by lazy { ReviewRepository(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PHOTOSMANAGERTheme {
                ConfirmDeleteScreen(
                    photosToDelete = requestedPhotos,
                    onConfirm = { selectedPhotos ->
                        deletePhotos(selectedPhotos)
                    },
                    onCancel = {
                        // 整批作废：这些照片其实没删掉，退回池子，之后还会再推
                        reviewRepository.releaseAsync(requestedPhotos)
                        finish()
                    }
                )
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == deleteRequestCode) {
            if (resultCode == RESULT_OK) {
                // 用户授权删除，执行实际删除。
                // 注意只能删 selectedForDelete：以前这里直接取 Intent 里的完整列表，
                // 用户在确认页取消勾选的照片照样会被删掉。
                performActualDelete(selectedForDelete)
            } else {
                // 用户拒绝删除，整批都没删成
                android.util.Log.w("ConfirmDeleteActivity", "用户拒绝删除权限")
                reviewRepository.releaseAsync(requestedPhotos)
                finish()
            }
        }
    }

    private fun deletePhotos(photos: List<String>) {
        selectedForDelete = photos
        // 没被勾选的照片不在删除请求里，先退回池子
        val unchecked = requestedPhotos - photos.toSet()
        reviewRepository.releaseAsync(unchecked)

        if (photos.isEmpty()) {
            finish()
            return
        }

        // Android 11+需要使用MediaStore.createDeleteRequest
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            try {
                val photoUris = photos.map { android.net.Uri.parse(it) }
                val deleteRequest = android.provider.MediaStore.createDeleteRequest(
                    contentResolver,
                    photoUris
                )
                startIntentSenderForResult(
                    deleteRequest.intentSender,
                    deleteRequestCode,
                    null,
                    0,
                    0,
                    0,
                    null
                )
            } catch (e: Exception) {
                android.util.Log.e("ConfirmDeleteActivity", "创建删除请求失败", e)
                // 如果创建请求失败，尝试直接删除
                performActualDelete(photos)
            }
        } else {
            // Android 10及以下，直接删除
            performActualDelete(photos)
        }
    }

    private fun performActualDelete(photos: List<String>) {
        val photoRepository = PhotoRepository(this)
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)

        scope.launch {
            var deletedCount = 0L
            val failed = mutableListOf<String>()
            photos.forEach { uri ->
                if (photoRepository.deletePhoto(uri)) {
                    deletedCount++
                } else {
                    failed.add(uri)
                }
            }
            android.util.Log.d("ConfirmDeleteActivity", "删除成功 $deletedCount 张，失败 ${failed.size} 张")
            // 删除失败的退回池子，否则用户永远等不到它
            if (failed.isNotEmpty()) {
                reviewRepository.release(failed)
            }

            // 只关掉确认页自己。
            // 以前这里是 finishAffinity() 再主动发一个 CATEGORY_HOME 的 Intent，
            // 等于删完强行把用户丢回桌面。但用户是从悬浮窗进来的，
            // 背后还停在他刚才在看的那个应用上，finish() 之后系统会自然回到那个界面。
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                finish()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmDeleteScreen(
    photosToDelete: List<String>,
    onConfirm: (List<String>) -> Unit,
    onCancel: () -> Unit
) {
    var selectedPhotos by remember { mutableStateOf(photosToDelete.toSet()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("确认删除") }
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = onCancel) {
                        Text("取消")
                    }
                    Button(
                        onClick = { onConfirm(selectedPhotos.toList()) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("确认删除 (${selectedPhotos.size})")
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(photosToDelete) { uri ->
                PhotoDeleteItem(
                    uri = uri,
                    isSelected = selectedPhotos.contains(uri),
                    onToggle = {
                        selectedPhotos = if (selectedPhotos.contains(uri)) {
                            selectedPhotos - uri
                        } else {
                            selectedPhotos + uri
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun PhotoDeleteItem(
    uri: String,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        onClick = onToggle
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() }
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Image(
                painter = rememberAsyncImagePainter(Uri.parse(uri)),
                contentDescription = null,
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        }
    }
}

