package com.example.foodsafevision

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.ui.platform.LocalContext
import java.util.*

@Composable
fun RegisterFood() {
    // 상태 변수들
    var productName by remember { mutableStateOf("") }
    var selectedDate1 by remember { mutableStateOf(Calendar.getInstance().time) }
    var selectedDate2 by remember { mutableStateOf(Calendar.getInstance().time) }
    var selectedCategory by remember { mutableStateOf("음식") }
    var quantity by remember { mutableStateOf(0) }
    var price by remember { mutableStateOf(0) }
    var barcode by remember { mutableStateOf("") }

    val categories = listOf("음식", "음료", "약품", "기타")
    val calendar = Calendar.getInstance()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // 바코드 번호 입력 (버튼으로 대체 가능)
        Button(
            onClick = {
                // 바코드 스캔 로직 추가
                barcode = "1234567890"  // 예시로 바코드 번호 설정
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("바코드 번호: $barcode")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 제품 이름 입력
        OutlinedTextField(
            value = productName,
            onValueChange = { productName = it },
            label = { Text("제품 이름") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 등록 날짜 선택
        Button(
            onClick = {
                val newDate = Calendar.getInstance() // 날짜 선택 로직
                selectedDate1 = newDate.time
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("등록 날짜: ${selectedDate1.toLocaleString().split(" ")[0]}")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 유통기한 선택
        Button(
            onClick = {
                val newDate = Calendar.getInstance() // 날짜 선택 로직
                selectedDate2 = newDate.time
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("유통기한: ${selectedDate2.toLocaleString().split(" ")[0]}")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 카테고리 선택
        DropdownMenu(
            expanded = true,
            onDismissRequest = { /* 처리 로직 */ }
        ) {
            categories.forEach { category ->
                DropdownMenuItem(onClick = { selectedCategory = category }) {
                    Text(category)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 수량 입력
        OutlinedTextField(
            value = quantity.toString(),
            onValueChange = { quantity = it.toIntOrNull() ?: 0 },
            label = { Text("수량") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))


        // 저장 버튼
        Button(
            onClick = {
                // 저장 로직
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("저장")
        }
    }
}




@Composable
fun DropdownMenuItem(onClick: () -> Unit, content: @Composable () -> Unit) {

}

@Composable
fun DropdownMenu(expanded: Boolean, onDismissRequest: () -> Unit, content: @Composable () -> Unit) {

}

@Composable
fun OutlinedTextField(value: TextFieldValue, onValueChange: () -> Unit, label: () -> Unit, modifier: Modifier) {

}

@Composable
fun Text(s: String, style: Any) {

}

@Composable
fun Button(onClick: () -> Unit, modifier: Modifier, content: @Composable () -> Unit) {

}

@Preview(showBackground = true)
@Composable
fun PreviewRegisterFood() {
    RegisterFood()
}
