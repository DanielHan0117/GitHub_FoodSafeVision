package com.example.foodsafevision

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.FocusRequester
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateScanner(
    onDateDetected: (String) -> Unit = {},
    onDateSelected: (String) -> Unit = {},
    onClickedDismiss: () -> Unit = {}
) {
    var showDialog by remember { mutableStateOf(false) }
    val currentDate = remember { Calendar.getInstance() }
    val datePickerState = rememberDatePickerState( initialSelectedDateMillis = currentDate.timeInMillis )

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
                onClick = { onClickedDismiss() },
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
            DatePickerDialog(
                onDismissRequest = { showDialog = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { timestamp ->
                                val date = SimpleDateFormat(
                                    "yyyy-MM-dd",
                                    Locale.getDefault()
                                ).format(Date(timestamp))
                                onDateSelected(date)
                            }
                            showDialog = false
                        }
                    ) {
                        Text("확인")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDialog = false }
                    ) {
                        Text("취소")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
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

        // 카메라 셔터 버튼
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(154.dp)
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(Color.White, CircleShape)
                    .clickable { /* 카메라 셔터 로직 */ }
            )
        }
    }
}