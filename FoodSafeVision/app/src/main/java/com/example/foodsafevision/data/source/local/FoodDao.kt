package com.example.foodsafevision.data.source.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.foodsafevision.data.model.FoodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Query("SELECT * FROM foods")
    fun getAllFoods(): Flow<List<FoodEntity>>

    @Query("SELECT * FROM foods WHERE barcodeNumber = :barcodeNumber")
    suspend fun getFoodByBarcode(barcodeNumber: String): FoodEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFood(food: FoodEntity)

    @Delete
    suspend fun deleteFood(food: FoodEntity)

    @Query("UPDATE foods SET quantity = :quantity WHERE barcodeNumber = :barcodeNumber")
    suspend fun updateQuantity(barcodeNumber: String, quantity: Int)
}
