package com.example.foodsafevision.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.foodsafevision.data.dao.TagDao
import com.example.foodsafevision.data.model.TagEntity

@Database(entities = [TagEntity::class], version = 1)
abstract class TagDatabase : RoomDatabase() {
    abstract fun tagDao(): TagDao

    companion object {
        @Volatile
        private var INSTANCE: TagDatabase? = null

        fun getDatabase(context: Context): TagDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TagDatabase::class.java,
                    "tag_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}