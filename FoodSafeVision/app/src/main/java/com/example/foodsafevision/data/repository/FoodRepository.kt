package com.example.foodsafevision.data.repository

import com.example.foodsafevision.data.dao.FoodDao
import com.example.foodsafevision.data.model.FoodEntity
import kotlinx.coroutines.flow.Flow

class FoodRepository(private val foodDao: FoodDao) {

    suspend fun insertFood(food: FoodEntity) {
        foodDao.insertFood(food)
    }

    suspend fun updateFood(food: FoodEntity) {
        foodDao.updateFood(food)
    }

    suspend fun deleteFood(food: FoodEntity) {
        foodDao.deleteFood(food)
    }

    fun getAllFoods(): Flow<List<FoodEntity>> {
        return foodDao.getAllFoods()
    }

    fun getFoodByBarcode(barcode: String): Flow<FoodEntity?> {
        return foodDao.getFoodByBarcode(barcode)
    }

    fun getFoodsByExpirationDate(date: String): Flow<List<FoodEntity>> {
        return foodDao.getFoodsByExpirationDate(date)
    }

    fun getFoodsByStorage(storage: String): Flow<List<FoodEntity>> {
        return foodDao.getFoodsByTag(storage)
    }

    suspend fun hasFoodsWithTag(tag: String): Boolean {
        return foodDao.getFoodsCountByTag(tag) > 0
    }

    suspend fun updateFoodsTag(oldTag: String, newTag: String) {
        foodDao.updateFoodsTag(oldTag, newTag)
    }
}
