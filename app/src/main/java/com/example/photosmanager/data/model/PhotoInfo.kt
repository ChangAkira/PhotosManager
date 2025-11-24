package com.example.photosmanager.data.model

import android.net.Uri
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
    val displayName: String
) {
    fun toUri(): Uri = Uri.parse(uri)
}


