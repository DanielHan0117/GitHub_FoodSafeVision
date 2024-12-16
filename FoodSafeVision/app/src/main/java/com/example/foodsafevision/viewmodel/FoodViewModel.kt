package com.example.foodsafevision.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.foodsafevision.data.model.FoodEntity
import com.example.foodsafevision.data.repository.FoodRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate

class FoodViewModel(private val foodRepository: FoodRepository) : ViewModel() {
    private val _allFoods = MutableStateFlow<List<FoodEntity>>(emptyList())
    val allFoods: StateFlow<List<FoodEntity>> = _allFoods.asStateFlow()

    fun refreshFoods() {
        viewModelScope.launch {
            foodRepository.getAllFoods().collect { foods ->
                _allFoods.value = foods
            }
        }
    }

    init {
        viewModelScope.launch {
            foodRepository.getAllFoods().collect { foods ->
                _allFoods.value = foods
            }
        }
    }

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

    fun getFoodByBarcode(barcode: String) = foodRepository.getFoodByBarcode(barcode)

    fun getFoodsByExpirationDate(date: String) = foodRepository.getFoodsByExpirationDate(date)

    fun getFoodsByStorage(storage: String) = foodRepository.getFoodsByStorage(storage)

    fun updateFoodsTag(oldTag: String, newTag: String) {
        viewModelScope.launch {
            foodRepository.updateFoodsTag(oldTag, newTag)
        }
    }

    val sortedFoods = allFoods.map { foods ->
        foods.sortedBy { LocalDate.parse(it.expirationDate) }
    }
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