package com.example.foodsafevision.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.foodsafevision.data.model.FoodEntity
import com.example.foodsafevision.data.repository.FoodRepository
import kotlinx.coroutines.launch

class FoodViewModel(private val foodRepository: FoodRepository) : ViewModel() {

    fun insertFood(food: FoodEntity) {
        viewModelScope.launch {
            foodRepository.insertFood(food)
        }
    }

    fun updateFood(food: FoodEntity) {
        viewModelScope.launch {
            foodRepository.updateFood(food)
        }
    }

    fun deleteFood(food: FoodEntity) {
        viewModelScope.launch {
            foodRepository.deleteFood(food)
        }
    }

    fun getAllFoods() = foodRepository.getAllFoods()

    fun getFoodByBarcode(barcode: String) = foodRepository.getFoodByBarcode(barcode)

    fun getFoodsByExpirationDate(date: String) = foodRepository.getFoodsByExpirationDate(date)

    fun getFoodsByStorage(storage: String) = foodRepository.getFoodsByStorage(storage)
}

class FoodViewModelFactory(
    private val foodRepository: FoodRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FoodViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FoodViewModel(foodRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
