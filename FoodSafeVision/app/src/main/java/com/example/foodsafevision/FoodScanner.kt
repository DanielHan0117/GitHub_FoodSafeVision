package com.example.foodsafevision

import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.core.graphics.drawable.toBitmap
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

enum class FoodMode {
    Barcode, Auto_Recognition
}

@Composable
fun FoodScanner() {
    var currentMode by remember { mutableStateOf(FoodMode.Barcode) }
    var showDialog by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    var showBarcodeResult by remember { mutableStateOf(false) } // 바코드 결과 팝업 상태
    var barcodeValue by remember { mutableStateOf("") } // 바코드 값 저장
    val context = LocalContext.current
    // 바코드 스캐너 초기화
    val options = remember {
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_ALL_FORMATS
            )
            .build()
    }
    val scanner = remember { BarcodeScanning.getClient(options) }
    // ML Kit 바코드 스캐너 설정

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
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )

                    LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        // 확인 버튼 로직
                        showDialog = false
                    },
                        enabled = inputText.isNotBlank()
                    ) {
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

        if (showBarcodeResult) {
            AlertDialog(
                onDismissRequest = { showBarcodeResult = false },
                title = { Text("바코드 스캔 결과") },
                text = { Text("일련번호: $barcodeValue") },
                confirmButton = {
                    TextButton(onClick = { showBarcodeResult = false }) {
                        Text("확인")
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
            Image(
                painter = painterResource(id = R.drawable.test),
                contentDescription = "Test Image",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        // 이미지를 비트맵으로 변환
                        val bitmap = (context.getDrawable(R.drawable.test) as BitmapDrawable).bitmap

                        // ML Kit 바코드 스캐너 초기화
                        val scanner = BarcodeScanning.getClient()

                        // 이미지 분석
                        val image = InputImage.fromBitmap(bitmap, 0)
                        scanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                if (barcodes.isNotEmpty()) {
                                    barcodeValue = barcodes[0].rawValue ?: "바코드를 찾을 수 없습니다"
                                    showBarcodeResult = true
                                } else {
                                    barcodeValue = "바코드를 찾을 수 없습니다"
                                    showBarcodeResult = true
                                }
                            }
                            .addOnFailureListener { e ->
                                barcodeValue = "스캔 실패: ${e.message}"
                                showBarcodeResult = true
                            }
                    }
            )
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
