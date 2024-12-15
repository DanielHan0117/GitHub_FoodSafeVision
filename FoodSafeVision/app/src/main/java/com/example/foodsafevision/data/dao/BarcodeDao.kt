package com.example.foodsafevision.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.foodsafevision.data.entity.BarcodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BarcodeDao {
    @Query("SELECT productName FROM barcode_table WHERE barcodeNumber = :barcodeNumber LIMIT 1")
    suspend fun getProductNameByBarcode(barcodeNumber: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(barcodes: List<BarcodeEntity>)

    @Query("SELECT COUNT(*) FROM barcode_table")
    suspend fun getProductCount(): Int

    @Query("SELECT * FROM barcode_table")
    suspend fun getAllBarcodes(): List<BarcodeEntity>
}