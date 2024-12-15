package com.example.foodsafevision.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.foodsafevision.data.model.TagEntity
import com.example.foodsafevision.data.repository.TagRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TagViewModel(private val repository: TagRepository) : ViewModel() {
    val allTags: StateFlow<List<String>> = repository.allTags
        .map { tagEntities ->
            tagEntities.map { it.name }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

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
        }
    }

    fun deleteTag(name: String) {
        viewModelScope.launch {
            repository.deleteTag(name)
        }
    }
}

class TagViewModelFactory(
    private val repository: TagRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TagViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TagViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}