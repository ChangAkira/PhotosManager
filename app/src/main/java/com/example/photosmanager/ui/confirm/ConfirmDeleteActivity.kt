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
import com.example.photosmanager.ui.theme.PHOTOSMANAGERTheme
import kotlinx.coroutines.launch

class ConfirmDeleteActivity : ComponentActivity() {
    private val deleteRequestCode = 1001
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val photosToDelete = intent.getStringArrayListExtra("photos_to_delete") ?: arrayListOf()
        
        setContent {
            PHOTOSMANAGERTheme {
                ConfirmDeleteScreen(
                    photosToDelete = photosToDelete,
                    onConfirm = { selectedPhotos ->
                        deletePhotos(selectedPhotos)
                    },
                    onCancel = {
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
                // 用户授权删除，执行实际删除
                val photosToDelete = intent.getStringArrayListExtra("photos_to_delete") ?: arrayListOf()
                performActualDelete(photosToDelete)
            } else {
                // 用户拒绝删除
                android.util.Log.w("ConfirmDeleteActivity", "用户拒绝删除权限")
                finish()
            }
        }
    }

    private fun deletePhotos(photos: List<String>) {
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
            photos.forEach { uri ->
                if (photoRepository.deletePhoto(uri)) {
                    deletedCount++
                }
            }
            
            // 关闭所有Activity，包括MainActivity，但不关闭后台服务
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                // 关闭当前Activity和所有相关Activity
                finishAffinity()
                // 返回到桌面
                android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                    addCategory(android.content.Intent.CATEGORY_HOME)
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(this)
                }
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

