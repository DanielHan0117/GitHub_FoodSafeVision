package com.example.foodsafevision.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "foods")
data class FoodEntity(
    @PrimaryKey
    val barcodeNumber: String,
    val foodName: String,
    val expirationDate: String,  // 유통기한
    val tag: String,
    val quantity: Int
)
