package com.example.foodsafevision.data.database

import javax.inject.Inject

class BarcodeRepository @Inject constructor(
    private val barcodeProductDao: BarcodeProductDao
) {
    suspend fun addProduct(barcode: String, name: String) {
        barcodeProductDao.insertProduct(
            BarcodeProduct(
                barcode = barcode,
                name = name
            )
        )
    }

    suspend fun getProductName(barcode: String): String? {
        return barcodeProductDao.getProductByBarcode(barcode)?.name
    }
}