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
import android.content.Context // Android Context
import androidx.compose.ui.platform.LocalContext // Jetpack Compose Local Context
import androidx.compose.ui.graphics.Color
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import java.text.SimpleDateFormat


@Composable
fun RegisterFood() {
    // 상태 변수들
    var productName by remember { mutableStateOf("") }
    var selectedDate1 by remember { mutableStateOf<Date?>(null) }
    var selectedDate2 by remember { mutableStateOf<Date?>(null) }
    var selectedCategory by remember { mutableStateOf("음식") }
    var quantity by remember { mutableStateOf(0) }
    var price by remember { mutableStateOf(0) }
    //var barcode by remember { mutableStateOf("") }
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var barcode by remember { mutableStateOf("1234567890") }  // 바코드 자동 설정

    // 앱 시작 시 자동으로 등록 날짜를 오늘 날짜로 설정
    selectedDate1 = remember { Calendar.getInstance().time }

    val categories = listOf("과일", "냉장", "미분류")


    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {

        // < 제품 추가 버튼
        Button(
            onClick = {
                // 전단계로 이동하는 기능은 나중에 구현
                // 버튼 클릭 시 아무 동작도 하지 않음
            },
            modifier = Modifier.wrapContentWidth()
        ) {
            Text("< 제품 추가")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 바코드 번호 자동 표시
        Text("바코드 번호: $barcode")

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
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.Yellow,  // 버튼 배경색을 노란색으로 설정
                contentColor = Color.White    // 버튼 텍스트 색상을 검은색으로 설정
            )
        ) {
            Text(
                text = "등록 날짜: ${
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate1)
                }"
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 유통기한 선택
        val context = LocalContext.current

        Button(
            onClick = {
                // DatePickerDialog 생성
                val datePicker = DatePickerDialog(
                    context, // LocalContext로 가져온 context 사용
                    { _: DatePicker, year: Int, month: Int, day: Int ->
                        calendar.set(year, month, day)
                        selectedDate2 = calendar.time
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                )
                datePicker.show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "유통기한: ${
                    selectedDate2?.let {
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it)
                    } ?: "선택 안 됨"
                }")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 카테고리 선택

        Box(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { isDropdownExpanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("카테고리: $selectedCategory")
            }

            DropdownMenu(
                expanded = isDropdownExpanded,
                onDismissRequest = { isDropdownExpanded = false }
            ) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        onClick = {
                            selectedCategory = category
                            isDropdownExpanded = false
                        }
                    ) {
                        Text(category)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // 수량 입력
        OutlinedTextField(
            value = productName, // 상태 변수 사용
            onValueChange = { productName = it }, // 입력값을 상태로 저장
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



@Preview(showBackground = true)
@Composable
fun PreviewRegisterFood() {
    RegisterFood()
}
