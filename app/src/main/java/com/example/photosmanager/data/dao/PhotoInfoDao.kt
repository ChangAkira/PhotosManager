package com.example.photosmanager.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.photosmanager.data.model.PhotoInfo

@Dao
interface PhotoInfoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photoInfo: PhotoInfo)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(photos: List<PhotoInfo>)

    @Query("SELECT * FROM photo_info WHERE albumName = :albumName")
    suspend fun getPhotosByAlbum(albumName: String): List<PhotoInfo>

    @Query("SELECT * FROM photo_info WHERE uri = :uri")
    suspend fun getPhotoByUri(uri: String): PhotoInfo?

    @Query("SELECT DISTINCT albumName FROM photo_info")
    suspend fun getAllAlbumNames(): List<String>
}


