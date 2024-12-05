package com.example.foodsafevision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.graphics.drawable.BitmapDrawable
import android.media.Image
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.Image
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.foodsafevision.data.database.BarcodeRepository
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean

enum class FoodMode {
    Barcode, Auto_Recognition
}

@OptIn(ExperimentalGetImage::class)
@Composable
fun FoodScanner(
    onBarcodeDetected: (String) -> Unit,  // 바코드 감지 콜백
    barcodeRepository: BarcodeRepository,
    onObjectDetected: (String) -> Unit = {}    // 객체 감지 콜백
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
                .setAssetFilePath("mobilenet_v3_small.tflite")
                .build()
        }

    // 객체 감지기 설정
    lateinit var nanoDetector: NanoDet
    val nanoDetInitialized = AtomicBoolean(false)

    // NanoDet 초기화 함수
    fun initNanoDet(context: Context) {
        if (!nanoDetInitialized.get()) {
            try {
                nanoDetector = NanoDet()
                val ret = nanoDetector.loadModel(context.assets, "nanodet.param", "nanodet.bin")
                if (ret == 0) {
                    nanoDetInitialized.set(true)
                }
            } catch (e: Exception) {
                Log.e("NanoDet", "모델 로딩 실패", e)
            }
        }
    }

        var showObjectDetectionDialog by remember { mutableStateOf(false) }
        var detectedObjectName by remember { mutableStateOf("") }
        var showBarcodeResult by remember { mutableStateOf(false) }
        var barcodeValue by remember { mutableStateOf("") }
        var productName by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(barcodeValue) {
            if (barcodeValue.isNotEmpty()) {
                productName = barcodeRepository.getProductName(barcodeValue)
            }
        }

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

                            var isProcessing = false

                            imageAnalysis.setAnalyzer(
                                ContextCompat.getMainExecutor(context)
                            ) { imageProxy ->
                                when (currentMode) {
                                    FoodMode.Barcode -> {
                                        if (!isProcessing) {
                                            val mediaImage = imageProxy.image
                                            if (mediaImage != null) {
                                                isProcessing = true
                                                val image = InputImage.fromMediaImage(
                                                    mediaImage,
                                                    imageProxy.imageInfo.rotationDegrees
                                                )

                                                scanner.process(image)
                                                    .addOnSuccessListener { barcodes ->
                                                        if (barcodes.isNotEmpty()) {
                                                            barcodeValue =
                                                                barcodes[0].rawValue ?: "알 수 없음"
                                                            showBarcodeResult = true

                                                            productName?.let { name ->
                                                                onBarcodeDetected(name)
                                                            }

                                                            Handler(Looper.getMainLooper()).postDelayed(
                                                                {
                                                                    isProcessing = false
                                                                },
                                                                3000
                                                            )
                                                        } else {
                                                            isProcessing = false
                                                        }
                                                    }
                                                    .addOnFailureListener {
                                                        isProcessing = false
                                                    }
                                                    .addOnCompleteListener {
                                                        imageProxy.close()
                                                    }
                                            } else {
                                                imageProxy.close()
                                            }
                                        } else {
                                            imageProxy.close()
                                        }
                                    }
                                    FoodMode.Auto_Recognition -> {
                                        if (shouldAnalyzeImage) {
                                            val mediaImage = imageProxy.image
                                            if (mediaImage != null) {
                                                try {
                                                    // 이미지를 Bitmap으로 변환
                                                    val bitmap = mediaImageToBitmap(mediaImage)

                                                    if (!nanoDetInitialized.get()) {
                                                        initNanoDet(context)
                                                    }

                                                    // NanoDet로 객체 감지 실행
                                                    val objects = nanoDetector.detect(bitmap, threshold = 0.4f)

                                                    if (objects.isNotEmpty()) {
                                                        // 가장 높은 신뢰도를 가진 객체 찾기
                                                        val highestConfidenceObject = objects.maxByOrNull { detectedObject ->
                                                            detectedObject.labels.maxOfOrNull { label -> label.confidence } ?: 0f
                                                        }

                                                        highestConfidenceObject?.labels?.firstOrNull()?.let { label ->
                                                            detectedObjectName = "${label.text} (${String.format("%.1f", label.confidence * 100)}%)"
                                                            showObjectDetectionDialog = true
                                                        }
                                                    } else {
                                                        detectedObjectName = "객체 인식 실패"
                                                        showObjectDetectionDialog = true
                                                    }
                                                } catch (e: Exception) {
                                                    Log.e("NanoDet", "객체 감지 실패", e)
                                                    detectedObjectName = "객체 인식 실패: ${e.localizedMessage}"
                                                    showObjectDetectionDialog = true
                                                }

                                                shouldAnalyzeImage = false
                                            }
                                            imageProxy.close()
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

// MediaImage를 Bitmap으로 변환하는 유틸리티 함수
private fun mediaImageToBitmap(mediaImage: Image): Bitmap {
    val planes = mediaImage.planes
    val yBuffer = planes[0].buffer
    val uBuffer = planes[1].buffer
    val vBuffer = planes[2].buffer

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)

    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)

    val yuvImage = YuvImage(nv21, ImageFormat.NV21, mediaImage.width, mediaImage.height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 100, out)
    val imageBytes = out.toByteArray()

    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}