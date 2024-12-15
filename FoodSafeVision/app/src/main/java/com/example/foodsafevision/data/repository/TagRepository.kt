package com.example.foodsafevision.data.repository

import com.example.foodsafevision.data.dao.TagDao
import com.example.foodsafevision.data.model.TagEntity

class TagRepository(private val tagDao: TagDao) {
    val allTags = tagDao.getAllTags()

    suspend fun insertTag(name: String) {
        tagDao.insertTag(TagEntity(name = name))
    }

    suspend fun updateTag(oldName: String, newName: String) {
        tagDao.getTagByName(oldName)?.let { tag ->
            tagDao.updateTag(tag.copy(name = newName))
        }
    }

    suspend fun deleteTag(name: String) {
        tagDao.getTagByName(name)?.let { tag ->
            tagDao.deleteTag(tag)
        }
    }

    suspend fun initializeDefaultTag() {
        if (!tagDao.hasDefaultTag()) {
            insertTag("나의 냉장고")
        }
    }
}