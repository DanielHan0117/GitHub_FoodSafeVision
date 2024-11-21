package com.example.foodsafevision.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BarcodeProductDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: BarcodeProduct)

    @Query("SELECT * FROM barcode_products WHERE barcode = :barcode")
    suspend fun getProductByBarcode(barcode: String): BarcodeProduct?

    @Query("SELECT * FROM barcode_products")
    suspend fun getAllProducts(): List<BarcodeProduct>
}