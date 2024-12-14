package com.example.foodsafevision.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.foodsafevision.data.dao.BarcodeDao
import com.example.foodsafevision.data.entity.BarcodeEntity

@Database(entities = [BarcodeEntity::class], version = 1)
abstract class BarcodeDatabase : RoomDatabase() {
    abstract fun barcodeDao(): BarcodeDao

    companion object {
        @Volatile
        private var INSTANCE: BarcodeDatabase? = null

        fun getDatabase(context: Context): BarcodeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BarcodeDatabase::class.java,
                    "barcode_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
