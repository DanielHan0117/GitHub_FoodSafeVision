package com.example.foodsafevision.data.dao

import androidx.room.*
import com.example.foodsafevision.data.model.FoodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFood(food: FoodEntity)

    @Update
    suspend fun updateFood(food: FoodEntity)

    @Delete
    suspend fun deleteFood(food: FoodEntity)

    @Query("SELECT * FROM foods")
    fun getAllFoods(): Flow<List<FoodEntity>>

    @Query("SELECT * FROM foods WHERE barcodeNumber = :barcode")
    fun getFoodByBarcode(barcode: String): Flow<FoodEntity?>

    @Query("SELECT * FROM foods WHERE expirationDate = :date")
    fun getFoodsByExpirationDate(date: String): Flow<List<FoodEntity>>

    @Query("SELECT * FROM foods WHERE tag = :storage")
    fun getFoodsByTag(storage: String): Flow<List<FoodEntity>>
}
