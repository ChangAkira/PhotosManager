package com.example.photosmanager.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ReviewedPhotoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: com.example.photosmanager.data.model.ReviewedPhoto)

    @Query("SELECT uri FROM reviewed_photos")
    suspend fun getReviewedUris(): List<String>

    @Query("DELETE FROM reviewed_photos WHERE uri IN (:uris)")
    suspend fun delete(uris: List<String>)

    @Query("DELETE FROM reviewed_photos")
    suspend fun deleteAll()
}
