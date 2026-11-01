package com.example.photosmanager.data.repository

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.example.photosmanager.data.database.AppDatabase
import com.example.photosmanager.data.model.PhotoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PhotoRepository(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)

    private val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.DATE_ADDED,
        MediaStore.Images.Media.DATE_MODIFIED,
        MediaStore.Images.Media.SIZE,
        MediaStore.Images.Media.WIDTH,
        MediaStore.Images.Media.HEIGHT,
        MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
        MediaStore.Images.Media.BUCKET_ID
    )

    suspend fun getAllPhotosFromAlbums(albumNames: Set<String>): List<PhotoInfo> = withContext(Dispatchers.IO) {
        val selection = if (albumNames.isNotEmpty()) {
            "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} IN (${albumNames.joinToString(",") { "?" }})"
        } else {
            null
        }
        queryPhotos(selection, albumNames.toTypedArray())
    }

    /**
     * 取同一目录内、按添加时间排序的邻居照片。
     *
     * 这里必须按 BUCKET_ID 而不是相册显示名来查：显示名可以重复
     * （DCIM/Camera 和 Pictures/Camera 都叫 "Camera"），按名字查会把两个目录
     * 的照片混在一起排序，给出的"附近"和用户正在看的这张毫无关系。
     */
    suspend fun getNearbyPhotos(
        currentPhoto: PhotoInfo,
        count: Int
    ): List<PhotoInfo> = withContext(Dispatchers.IO) {
        val (selection, selectionArgs) = if (currentPhoto.bucketId > 0L) {
            "${MediaStore.Images.Media.BUCKET_ID} = ?" to arrayOf(currentPhoto.bucketId.toString())
        } else {
            // 旧版本会话里存的 PhotoInfo 没有 bucketId，退回按显示名查
            "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} = ?" to arrayOf(currentPhoto.albumName)
        }

        val allPhotos = queryPhotos(selection, selectionArgs)
        val currentIndex = allPhotos.indexOfFirst { it.uri == currentPhoto.uri }

        if (currentIndex == -1) return@withContext emptyList()

        val startIndex = maxOf(0, currentIndex - count / 2)
        val endIndex = minOf(allPhotos.size, startIndex + count)

        allPhotos.subList(startIndex, endIndex)
    }

    private fun queryPhotos(selection: String?, selectionArgs: Array<String>?): List<PhotoInfo> {
        val photos = mutableListOf<PhotoInfo>()
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val bucketColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                ).toString()

                photos.add(
                    PhotoInfo(
                        uri = uri,
                        albumName = cursor.getString(bucketColumn) ?: "未知相册",
                        dateAdded = cursor.getLong(dateAddedColumn) * 1000,
                        dateModified = cursor.getLong(dateModifiedColumn) * 1000,
                        size = cursor.getLong(sizeColumn),
                        width = cursor.getInt(widthColumn),
                        height = cursor.getInt(heightColumn),
                        displayName = cursor.getString(nameColumn) ?: "",
                        bucketId = cursor.getLong(bucketIdColumn)
                    )
                )
            }
        }

        return photos
    }

    suspend fun getAllAlbumNames(): List<String> = withContext(Dispatchers.IO) {
        val albums = mutableSetOf<String>()
        
        val projection = arrayOf(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            val bucketColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            while (cursor.moveToNext()) {
                cursor.getString(bucketColumn)?.let { albums.add(it) }
            }
        }
        
        albums.toList().sorted()
    }

    suspend fun deletePhoto(uri: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val photoUri = android.net.Uri.parse(uri)
            android.util.Log.d("PhotoRepository", "开始删除照片: $uri")
            
            // 先尝试直接使用URI删除
            try {
                val deleted = context.contentResolver.delete(photoUri, null, null)
                android.util.Log.d("PhotoRepository", "直接删除返回: $deleted")
                if (deleted > 0) {
                    database.deletedPhotoDao().insert(
                        com.example.photosmanager.data.model.DeletedPhotoRecord(uri = uri)
                    )
                    android.util.Log.d("PhotoRepository", "照片删除成功 (直接删除): $uri")
                    return@withContext true
                }
            } catch (e: SecurityException) {
                android.util.Log.e("PhotoRepository", "直接删除权限异常: ${e.message}", e)
            } catch (e: Exception) {
                android.util.Log.e("PhotoRepository", "直接删除异常: ${e.message}", e)
            }
            
            // 如果直接删除失败，尝试使用MediaStore删除（Android 10+）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    val id = ContentUris.parseId(photoUri)
                    android.util.Log.d("PhotoRepository", "尝试使用MediaStore删除，ID: $id")
                    
                    // 使用MediaStore URI删除
                    val deleteUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    
                    val deleted = context.contentResolver.delete(deleteUri, null, null)
                    android.util.Log.d("PhotoRepository", "MediaStore删除返回: $deleted")
                    
                    if (deleted > 0) {
                        database.deletedPhotoDao().insert(
                            com.example.photosmanager.data.model.DeletedPhotoRecord(uri = uri)
                        )
                        android.util.Log.d("PhotoRepository", "照片删除成功 (MediaStore): $uri")
                        return@withContext true
                    } else {
                        // 尝试使用selection方式删除
                        val selection = "${MediaStore.Images.Media._ID} = ?"
                        val selectionArgs = arrayOf(id.toString())
                        val deleted2 = context.contentResolver.delete(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            selection,
                            selectionArgs
                        )
                        android.util.Log.d("PhotoRepository", "MediaStore selection删除返回: $deleted2")
                        
                        if (deleted2 > 0) {
                            database.deletedPhotoDao().insert(
                                com.example.photosmanager.data.model.DeletedPhotoRecord(uri = uri)
                            )
                            android.util.Log.d("PhotoRepository", "照片删除成功 (MediaStore selection): $uri")
                            return@withContext true
                        } else {
                            android.util.Log.w("PhotoRepository", "MediaStore删除返回0，URI: $deleteUri, 可能没有删除权限或文件不存在")
                            // 在Android 11+上，可能需要使用createDeleteRequest
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                android.util.Log.w("PhotoRepository", "Android 11+可能需要使用MediaStore.createDeleteRequest()")
                            }
                        }
                    }
                } catch (e: SecurityException) {
                    android.util.Log.e("PhotoRepository", "MediaStore删除权限异常: ${e.message}", e)
                } catch (e: Exception) {
                    android.util.Log.e("PhotoRepository", "MediaStore删除异常: ${e.message}", e)
                    e.printStackTrace()
                }
            }
            
            android.util.Log.w("PhotoRepository", "照片删除失败: $uri")
            false
        } catch (e: Exception) {
            android.util.Log.e("PhotoRepository", "删除照片异常: ${e.message}", e)
            e.printStackTrace()
            false
        }
    }
    

    suspend fun getTotalDeletedCount(): Long {
        return database.deletedPhotoDao().getTotalDeletedCount()
    }
}

