package com.example.foodsafevision.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "foods")
data class FoodEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val barcodeNumber: String,
    val foodName: String,
    val expirationDate: String,
    val tag: String,
    val quantity: Int
)