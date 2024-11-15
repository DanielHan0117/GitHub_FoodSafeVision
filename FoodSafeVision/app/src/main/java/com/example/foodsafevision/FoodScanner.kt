package com.example.foodsafevision

import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions

enum class FoodMode {
    Barcode, Auto_Recognition
}

@OptIn(ExperimentalGetImage::class)
@Composable
fun FoodScanner(
    onBarcodeDetected: () -> Unit = {},  // 바코드 감지 콜백
    onObjectDetected: () -> Unit = {}    // 객체 감지 콜백
) {
    var currentMode by remember { mutableStateOf(FoodMode.Barcode) }
    var showDialog by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

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

    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val previewView = remember { PreviewView(context) }
    var shouldAnalyzeImage by remember { mutableStateOf(false) }

    // MobileNet V3 모델 설정
    val localModel = remember {
        LocalModel.Builder()
            .setAssetFilePath("mobilenet_v3_1.0_224_float.tflite")
            .build()
    }

    // 객체 감지기 설정
    val customObjectDetector = remember {
        val options = CustomObjectDetectorOptions.Builder(localModel)
            .setDetectorMode(CustomObjectDetectorOptions.STREAM_MODE)
            .enableClassification()
            .setClassificationConfidenceThreshold(0.5f)
            .setMaxPerObjectLabelCount(3)
            .build()
        ObjectDetection.getClient(options)
    }

    var showObjectDetectionDialog by remember { mutableStateOf(false) }
    var detectedObjectName by remember { mutableStateOf("") }
    var showBarcodeResult by remember { mutableStateOf(false) }
    var barcodeValue by remember { mutableStateOf("") }

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

        // 카메라 프리뷰 영역
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Green)
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

                    imageAnalysis.setAnalyzer(
                        ContextCompat.getMainExecutor(context)
                    ) { imageProxy ->
                        when (currentMode) {
                            FoodMode.Barcode -> {
                                val mediaImage = imageProxy.image
                                if (mediaImage != null) {
                                    val image = InputImage.fromMediaImage(
                                        mediaImage,
                                        imageProxy.imageInfo.rotationDegrees
                                    )

                                    scanner.process(image)
                                    .addOnSuccessListener { barcodes ->
                                        if (barcodes.isNotEmpty()) {
                                            // 바코드 값 저장 및 다이얼로그 표시
                                            barcodeValue = barcodes[0].rawValue ?: "알 수 없음"
                                            showBarcodeResult = true
                                            onBarcodeDetected()
                                        }
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                                }
                            }
                            FoodMode.Auto_Recognition -> {
                                if (shouldAnalyzeImage) {
                                    // 객체 인식 기능 구현
                                } else {
                                    imageProxy.close()
                                }
                            }
                        }
                    }

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
                        Log.e("CameraPreview", "바인딩 실패", exc)
                    }
                }, ContextCompat.getMainExecutor(context))
            }
        }

        if (showBarcodeResult) {
            AlertDialog(
                onDismissRequest = { showBarcodeResult = false },
                title = { Text("바코드 스캔 결과") },
                text = { Text("바코드 번호: $barcodeValue") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showBarcodeResult = false
                            onBarcodeDetected()  // DateScanner로 이동
                        }
                    ) {
                        Text("확인")
                    }
                }
            )
        }

        if (showObjectDetectionDialog) {
            AlertDialog(
                onDismissRequest = {
                    showObjectDetectionDialog = false
                },
                title = { Text("객체 감지 결과") },
                text = { Text("감지된 객체: $detectedObjectName") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showObjectDetectionDialog = false
                        }
                    ) {
                        Text("확인")
                    }
                }
            )
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
                        .clickable {
                            shouldAnalyzeImage = true  // 셔터 버튼을 눌렀을 때 분석 플래그 설정
                        }
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