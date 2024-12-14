package com.example.foodsafevision.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "barcode_table")
data class BarcodeEntity(
    @PrimaryKey
    val barcodeNumber: String,
    val productName: String
)
