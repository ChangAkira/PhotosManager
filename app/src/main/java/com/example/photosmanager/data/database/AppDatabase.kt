package com.example.photosmanager.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.photosmanager.data.dao.PhotoInfoDao
import com.example.photosmanager.data.dao.DeletedPhotoDao
import com.example.photosmanager.data.dao.ReviewedPhotoDao
import com.example.photosmanager.data.model.PhotoInfo
import com.example.photosmanager.data.model.DeletedPhotoRecord
import com.example.photosmanager.data.model.ReviewedPhoto

@Database(
    entities = [PhotoInfo::class, DeletedPhotoRecord::class, ReviewedPhoto::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun photoInfoDao(): PhotoInfoDao
    abstract fun deletedPhotoDao(): DeletedPhotoDao
    abstract fun reviewedPhotoDao(): ReviewedPhotoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "photos_manager_database"
                )
                    // v1 -> v2 新增 reviewed_photos 表、photo_info 增加 bucketId 列。
                    // 这两张表此前只写不读，重建不会丢用户数据。
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
