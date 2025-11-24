package com.example.photosmanager.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.photosmanager.data.model.DeletedPhotoRecord

@Dao
interface DeletedPhotoDao {
    @Insert
    suspend fun insert(record: DeletedPhotoRecord)

    @Query("SELECT COUNT(*) FROM deleted_photo_records")
    suspend fun getTotalDeletedCount(): Long
}


