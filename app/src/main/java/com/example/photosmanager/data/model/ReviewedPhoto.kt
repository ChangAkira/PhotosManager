package com.example.photosmanager.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 已经做出过判断的照片。抽取时排除掉，一轮池子全部整理完才重新开始，
 * 这样不会出现"某些照片反复露面、另一些永远见不到"的情况。
 */
@Entity(tableName = "reviewed_photos")
data class ReviewedPhoto(
    @PrimaryKey
    val uri: String,
    val decidedAt: Long = System.currentTimeMillis()
)
