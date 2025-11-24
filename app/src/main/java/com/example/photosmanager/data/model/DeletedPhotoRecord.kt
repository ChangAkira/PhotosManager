package com.example.photosmanager.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "deleted_photo_records")
data class DeletedPhotoRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uri: String,
    val deletedAt: Long = System.currentTimeMillis()
)


