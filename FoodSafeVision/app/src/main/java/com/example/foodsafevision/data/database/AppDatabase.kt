package com.example.foodsafevision.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [BarcodeProduct::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun barcodeProductDao(): BarcodeProductDao
}