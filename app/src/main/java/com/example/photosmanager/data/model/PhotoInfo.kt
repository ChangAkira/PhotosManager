package com.example.photosmanager.data.model

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "photo_info")
data class PhotoInfo(
    @PrimaryKey
    val uri: String,
    val albumName: String,
    val dateAdded: Long,
    val dateModified: Long,
    val size: Long,
    val width: Int,
    val height: Int,
    val displayName: String,
    /**
     * 所在目录的 BUCKET_ID。相册显示名可以重复，这个才是目录的唯一身份，
     * 「附近」必须按它取邻居，否则同名目录的照片会被混在一起。
     */
    @ColumnInfo(defaultValue = "-1")
    val bucketId: Long = -1L
) {
    fun toUri(): Uri = Uri.parse(uri)
}
