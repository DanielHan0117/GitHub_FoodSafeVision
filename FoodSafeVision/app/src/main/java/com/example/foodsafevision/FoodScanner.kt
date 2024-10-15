package com.example.foodsafevision

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

enum class FoodMode {
    Barcode, Auto_Recognition
}

@Composable
fun FoodScanner() {
    var currentMode by remember { mutableStateOf(FoodMode.Barcode) }
    var showDialog by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 상단 버튼 영역
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(
                onClick = { /* 취소 로직 */ },
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
            ) {
                Text("취소")
            }
            TextButton(
                onClick = { showDialog = true },
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
            ) {
                Text("직접 입력")
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = {
                    showDialog = false
                    inputText = ""
                },
                title = {
                    Text(
                        "  직접 입력",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                text = {
                    TextField(
                        value = inputText,
                        placeholder = { Text("식품명") },
                        onValueChange = { inputText = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        // 확인 버튼 로직
                        showDialog = false
                    }) {
                        Text("확인")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDialog = false
                        inputText = "" // 입력 초기화
                    }) {
                        Text("취소")
                    }
                }
            )
        }

        // 카메라 프리뷰 영역
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Green)
        ) {
            // 여기에 실제 카메라 프리뷰 구현
        }

        // 하단 버튼 영역
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .padding(vertical = 16.dp)
        ) {
            ModeButton(
                text = "바코드",
                isSelected = currentMode == FoodMode.Barcode,
                onClick = { currentMode = FoodMode.Barcode },
                modifier = Modifier.weight(1f)
            )
            ModeButton(
                text = "자동 인식",
                isSelected = currentMode == FoodMode.Auto_Recognition,
                onClick = { currentMode = FoodMode.Auto_Recognition },
                modifier = Modifier.weight(1f)
            )
        }

        // 카메라 셔터 버튼
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (currentMode == FoodMode.Auto_Recognition) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(Color.White, CircleShape)
                        .clickable { /* 카메라 셔터 로직 */ }
                )
            }
        }
    }
}

@Composable
fun ModeButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = text,
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        if (isSelected) {
            Box(
                modifier = Modifier
                    .height(2.dp)
                    .fillMaxWidth(0.4f)
                    .background(Color.White)
            )
        }
    }
}
