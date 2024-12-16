package com.example.foodsafevision

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateScanner(
    onDateDetected: (String) -> Unit = {},
    onDateSelected: (String) -> Unit = {},
    onClickedDismiss: () -> Unit = {}
) {
    var showDialog by remember { mutableStateOf(false) }
    val currentDate = remember { Calendar.getInstance() }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = LocalDate.now()
            .atStartOfDay(ZoneId.of("Asia/Seoul"))
            .toInstant()
            .toEpochMilli()
            .plus(TimeZone.getDefault().rawOffset)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    )
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

        // 카메라 프리뷰 영역
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val context = LocalContext.current
            val lifecycleOwner = LocalLifecycleOwner.current
            val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
            val previewView = remember { PreviewView(context) }

            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            ) { view ->
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder()
                        .build()
                        .also {
                            it.setSurfaceProvider(view.surfaceProvider)
                        }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (exc: Exception) {
                        Log.e("DateScanner", "카메라 바인딩 실패", exc)
                    }
                }, ContextCompat.getMainExecutor(context))
            }

            // 포커스 프레임 추가
            Box(
                modifier = Modifier
                    .width(300.dp)
                    .height(100.dp)
                    .align(Alignment.Center)
            ) {
                // 왼쪽 상단 모서리
                Box(
                    modifier = Modifier
                        .size(30.dp, 3.dp)
                        .background(Color.White)
                        .align(Alignment.TopStart)
                )
                Box(
                    modifier = Modifier
                        .size(3.dp, 30.dp)
                        .background(Color.White)
                        .align(Alignment.TopStart)
                )

                // 오른쪽 상단 모서리
                Box(
                    modifier = Modifier
                        .size(30.dp, 3.dp)
                        .background(Color.White)
                        .align(Alignment.TopEnd)
                )
                Box(
                    modifier = Modifier
                        .size(3.dp, 30.dp)
                        .background(Color.White)
                        .align(Alignment.TopEnd)
                )

                // 왼쪽 하단 모서리
                Box(
                    modifier = Modifier
                        .size(30.dp, 3.dp)
                        .background(Color.White)
                        .align(Alignment.BottomStart)
                )
                Box(
                    modifier = Modifier
                        .size(3.dp, 30.dp)
                        .background(Color.White)
                        .align(Alignment.BottomStart)
                )

                // 오른쪽 하단 모서리
                Box(
                    modifier = Modifier
                        .size(30.dp, 3.dp)
                        .background(Color.White)
                        .align(Alignment.BottomEnd)
                )
                Box(
                    modifier = Modifier
                        .size(3.dp, 30.dp)
                        .background(Color.White)
                        .align(Alignment.BottomEnd)
                )
            }
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

    if (showDialog) {
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { timestamp ->
                            val localDate = Instant.ofEpochMilli(timestamp)
                                .atZone(ZoneId.of("Asia/Seoul"))
                                .toLocalDate()
                            val currentDate = localDate.toString()
                            onDateSelected(currentDate)
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
            DatePicker(
                state = datePickerState,
                title = {
                    Text(
                        "\n      유통기한 날짜 선택",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            )
        }
    }
}