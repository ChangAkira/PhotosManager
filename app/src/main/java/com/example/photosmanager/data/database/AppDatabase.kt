package com.example.photosmanager.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.photosmanager.data.dao.PhotoInfoDao
import com.example.photosmanager.data.dao.DeletedPhotoDao
import com.example.photosmanager.data.model.PhotoInfo
import com.example.photosmanager.data.model.DeletedPhotoRecord

@Database(
    entities = [PhotoInfo::class, DeletedPhotoRecord::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun photoInfoDao(): PhotoInfoDao
    abstract fun deletedPhotoDao(): DeletedPhotoDao

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
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}


