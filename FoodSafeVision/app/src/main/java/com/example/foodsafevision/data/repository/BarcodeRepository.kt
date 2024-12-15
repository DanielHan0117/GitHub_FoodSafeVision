package com.example.foodsafevision.data.repository

import android.content.Context
import android.util.Log
import com.example.foodsafevision.data.dao.BarcodeDao
import com.example.foodsafevision.data.entity.BarcodeEntity
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken
import com.google.gson.Gson

class BarcodeRepository(private val barcodeDao: BarcodeDao) {
    suspend fun loadBarcodeDataFromJson(context: Context) {
        try {
            val jsonString = context.assets.open("Barcode_data.json").bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<BarcodeEntity>>() {}.type
            val barcodeList: List<BarcodeEntity> = Gson().fromJson(jsonString, type)
            barcodeDao.insertAll(barcodeList)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getProductNameByBarcode(barcodeNumber: String): String? {
        return try {
            barcodeDao.getProductNameByBarcode(barcodeNumber)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getProductCount(): Int {
        return barcodeDao.getProductCount()
    }

    suspend fun getAllBarcodes(): List<BarcodeEntity> {
        return barcodeDao.getAllBarcodes()
    }
}