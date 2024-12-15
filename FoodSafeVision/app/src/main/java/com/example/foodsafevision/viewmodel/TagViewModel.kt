package com.example.foodsafevision.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.foodsafevision.data.repository.FoodRepository
import com.example.foodsafevision.data.repository.TagRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TagViewModel(
    private val repository: TagRepository,
    private val foodRepository: FoodRepository
) : ViewModel() {
    private val _allTags = repository.allTags
        .map { tagEntities ->
            tagEntities.map { it.name }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val allTags: StateFlow<List<String>> = _allTags

    init {
        viewModelScope.launch {
            repository.initializeDefaultTag()
        }
    }

    fun addTag(name: String) {
        viewModelScope.launch {
            repository.insertTag(name)
        }
    }

    fun updateTag(oldName: String, newName: String) {
        viewModelScope.launch {
            repository.updateTag(oldName, newName)
            refreshTags()
        }
    }

    fun deleteTag(name: String) {
        viewModelScope.launch {
            repository.deleteTag(name)
            refreshTags()
        }
    }

    fun refreshTags() {
        viewModelScope.launch {
            repository.allTags.collect { tags ->
                _allTags
            }
        }
    }

    sealed class DeleteTagResult {
        object Success : DeleteTagResult()
        object LastTag : DeleteTagResult()
        object HasFoods : DeleteTagResult()
    }

    suspend fun canDeleteTag(tag: String): DeleteTagResult {
        return when {
            allTags.value.size <= 1 -> DeleteTagResult.LastTag
            foodRepository.hasFoodsWithTag(tag) -> DeleteTagResult.HasFoods
            else -> DeleteTagResult.Success
        }
    }
}

class TagViewModelFactory(
    private val repository: TagRepository,
    private val foodRepository: FoodRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TagViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TagViewModel(repository, foodRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}