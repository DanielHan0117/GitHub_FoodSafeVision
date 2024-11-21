package com.example.foodsafevision.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "barcode_products")
data class BarcodeProduct(
    @PrimaryKey
    val barcode: String,
    val name: String,
    val manufacturer: String? = null,
    val category: String? = null
)