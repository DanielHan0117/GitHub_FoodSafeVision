package com.example.foodsafevision

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.foodsafevision.data.database.BarcodeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun BarcodeRegistrationScreen(
    barcodeRepository: BarcodeRepository
) {
    var barcode by remember { mutableStateOf("") }
    var productName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        TextField(
            value = barcode,
            onValueChange = { barcode = it },
            label = { Text("바코드 번호") }
        )

        TextField(
            value = productName,
            onValueChange = { productName = it },
            label = { Text("식품명") }
        )

        Button(
            onClick = {
                CoroutineScope(Dispatchers.IO).launch {
                    barcodeRepository.addProduct(barcode, productName)
                }
            }
        ) {
            Text("등록")
        }
    }
}