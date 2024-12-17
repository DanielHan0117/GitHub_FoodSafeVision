package com.example.foodsafevision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
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
import com.example.foodsafevision.data.repository.BarcodeRepository
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import org.pytorch.Module
import org.pytorch.IValue
import org.pytorch.Tensor


enum class FoodMode {
    Barcode, Auto_Recognition
}


private fun assetFilePath(context: Context, assetName: String): String {
    val file = File(context.filesDir, assetName)
    if (!file.exists()) {
        context.assets.open(assetName).use { inputStream ->
            FileOutputStream(file).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }
    return file.absolutePath
}

private fun Image.toBitmap(): Bitmap {
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

    val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, out)
    val imageBytes = out.toByteArray()
    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}

// 레이블 로딩 함수
private fun loadLabels(context: Context): Map<Int, String> {
    val foodLabels = mapOf(
        //924 to "과카몰리",
        //925 to "콘소메",
        //926 to "핫팟",
        //927 to "트라이플",
        //928 to "아이스크림",
        //929 to "아이스 롤리",
        //930 to "프렌치 로프",
        //931 to "베이글",
        //932 to "프레첼",
        //933 to "치즈버거",
        //934 to "핫도그",
        //935 to "으깬 감자",
        //936 to "양배추",
        //937 to "브로콜리",
        //938 to "콜리플라워",
        //939 to "주키니",
        //940 to "스파게티 스쿼시",
        //941 to "도토리 호박",
        //942 to "버터넛 스쿼시",
        //943 to "오이",
        //944 to "아티초크",
        //945 to "피망",
        //946 to "카르둔",
        //947 to "버섯",
        //948 to "그래니 스미스 사과",
        //949 to "딸기",
        //950 to "오렌지",
        //951 to "레몬",
        //952 to "무화과",
        953 to "파인애플",
        //954 to "바나나",
        //955 to "잭프루트",
        //956 to "커스터드 애플",
        //957 to "석류",
        //959 to "카르보나라",
        //960 to "초콜릿 소스",
        //961 to "도우",
        //962 to "미트로프",
        963 to "피자",
        //964 to "팟파이",
        //965 to "부리토"
    )

    return foodLabels
}

@OptIn(ExperimentalGetImage::class)
@Composable
fun FoodScanner(
    onNavigateBack: () -> Unit,
    barcodeRepository: BarcodeRepository,
    onBarcodeDetected: (Any?, Any?) -> Unit,
    onFoodNameDetected: (String) -> Unit = {},
    onFoodNameInput: (String) -> Unit = {},
    onClickedDismiss: () -> Unit = {}
) {
    BackHandler {
        onNavigateBack()
    }

    var foodName by remember { mutableStateOf("") }
    var currentMode by remember { mutableStateOf(FoodMode.Barcode) }
    var showFoodNameInput by remember { mutableStateOf(false) }
    var inputFoodName by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    var focusFrameColor by remember { mutableStateOf(Color.White) }

    fun showSuccessAndProceed(action: () -> Unit) {
        focusFrameColor = Color.Green
        coroutineScope.launch {
            delay(500)
            focusFrameColor = Color.White
            action()
        }
    }

    // 바코드 스캐너 초기화
    val options = remember {
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_ALL_FORMATS
            )
            .build()
    }
    val scanner = remember { BarcodeScanning.getClient(options) }
    var showBarcodeResult by remember { mutableStateOf(false) }
    var barcodeValue by remember { mutableStateOf("") }

    var shouldAnalyze by remember { mutableStateOf(false) }

    // PyTorch 모듈 초기화
    val context = LocalContext.current
    var labels by remember { mutableStateOf(emptyMap<Int, String>()) }
    val module = try {
        val path = assetFilePath(context, "mobilenet_v3_small.pt")
        Log.d("FoodScanner", "모델 파일 경로: $path")
        val exists = File(path).exists()
        Log.d("FoodScanner", "파일 존재 여부: $exists")
        Module.load(path)
    } catch (e: Exception) {
        Log.e("FoodScanner", "모델 로딩 실패: ${e.message}", e)
        null
    }

    // 이미지 분석 로직
    fun analyzeImage(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (module == null) Log.e("FoodScanner", "모듈이 null입니다")
        if (mediaImage != null && module != null) {
            try {
                val bitmap = mediaImage.toBitmap()
                val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)

                // 이미지를 Float 배열로 변환
                val floatArray = FloatArray(3 * 224 * 224)
                var index = 0
                for (y in 0 until 224) {
                    for (x in 0 until 224) {
                        val pixel = resizedBitmap.getPixel(x, y)
                        val r = android.graphics.Color.red(pixel)
                        val g = android.graphics.Color.green(pixel)
                        val b = android.graphics.Color.blue(pixel)

                        floatArray[index++] = (r / 255f - 0.485f) / 0.229f
                        floatArray[index++] = (g / 255f - 0.456f) / 0.224f
                        floatArray[index++] = (b / 255f - 0.406f) / 0.225f
                    }
                }

                // 텐서 생성
                val inputTensor = Tensor.fromBlob(
                    floatArray,
                    longArrayOf(1, 3, 224, 224)
                )

                // 추론 실행
                val outputTensor = module.forward(IValue.from(inputTensor)).toTensor()
                val scores = outputTensor.dataAsFloatArray

                // 최대값 찾기
                var maxScore = Float.NEGATIVE_INFINITY
                var maxScoreIdx = -1
                scores.forEachIndexed { index: Int, score: Float ->
                    // 원하는 클래스 인덱스만 확인
                    if (index in labels.keys && score > maxScore) {
                        maxScore = score
                        maxScoreIdx = index
                    }
                }

                // 레이블 이름으로 결과 전달
                val detectedLabel = labels[maxScoreIdx] ?: ""
                showSuccessAndProceed {
                    onFoodNameDetected(detectedLabel)
                }

            } catch (e: Exception) {
                Log.e("FoodScanner", "이미지 분석 실패", e)
            } finally {
                imageProxy.close()
            }
        } else {
            Log.e("FoodScanner", "이미지 또는 모듈이 null입니다")
            imageProxy.close()
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
                onClick = { onClickedDismiss() },
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
            ) {
                Text("취소")
            }
            TextButton(
                onClick = { showFoodNameInput = true },
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
            ) {
                Text("직접 입력")
            }
        }

        if (showFoodNameInput) {
            AlertDialog(
                onDismissRequest = {
                    showFoodNameInput = false
                    inputFoodName = ""
                },
                containerColor = Color.White,
                title = {
                    Text(
                        "  직접 입력",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                text = {
                    TextField(
                        value = inputFoodName,
                        placeholder = { Text("음식명") },
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color(0xFFF5F5F5),
                            unfocusedIndicatorColor = Color.Gray,
                            focusedContainerColor = Color(0xFFF5F5F5),
                            focusedIndicatorColor = Color.Gray,
                            cursorColor = Color.Black
                        ),
                        onValueChange = { inputFoodName = it },
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
                    TextButton(
                        onClick = {
                            if (inputFoodName.isNotBlank()) {
                                foodName = inputFoodName
                                showFoodNameInput = false
                                onFoodNameInput(inputFoodName)
                            }
                        },
                        enabled = inputFoodName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black,
                            disabledContainerColor = Color.White,
                            disabledContentColor = Color.DarkGray
                        )
                    ) {
                        Text("확인")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showFoodNameInput = false
                            inputFoodName = "" // 입력 초기화
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black,
                            disabledContainerColor = Color.White,
                            disabledContentColor = Color.DarkGray
                        )
                    ) {
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
                                                val scannedBarcode =
                                                    barcodes[0].rawValue?.takeIf { it.isNotBlank() }
                                                        ?: ""
                                                barcodeValue = scannedBarcode
                                                showBarcodeResult = true

                                                showSuccessAndProceed {
                                                    coroutineScope.launch {
                                                        try {
                                                            val productName =
                                                                barcodeRepository.getProductNameByBarcode(
                                                                    scannedBarcode
                                                                )
                                                            onBarcodeDetected(
                                                                scannedBarcode,
                                                                productName ?: ""
                                                            )
                                                        } catch (e: Exception) {
                                                            Log.e("BarcodeScanner", "바코드 조회 실패", e)
                                                            onBarcodeDetected(scannedBarcode, "")
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        .addOnCompleteListener {
                                            imageProxy.close()
                                        }
                                }
                            }

                            FoodMode.Auto_Recognition -> {
                                if (shouldAnalyze) {
                                    analyzeImage(imageProxy)
                                    shouldAnalyze = false
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

            // 포커스 프레임 추가
            Box(
                modifier = when (currentMode) {
                    FoodMode.Barcode -> Modifier
                        .width(300.dp)
                        .height(140.dp)
                        .align(Alignment.Center)

                    FoodMode.Auto_Recognition -> Modifier
                        .width(300.dp)
                        .height(300.dp)
                        .align(Alignment.Center)
                },
                contentAlignment = Alignment.Center
            ) {
                // 왼쪽 상단 모서리
                Box(
                    modifier = Modifier
                        .size(30.dp, 3.dp)
                        .background(focusFrameColor)
                        .align(Alignment.TopStart)
                )
                Box(
                    modifier = Modifier
                        .size(3.dp, 30.dp)
                        .background(focusFrameColor)
                        .align(Alignment.TopStart)
                )

                // 오른쪽 상단 모서리
                Box(
                    modifier = Modifier
                        .size(30.dp, 3.dp)
                        .background(focusFrameColor)
                        .align(Alignment.TopEnd)
                )
                Box(
                    modifier = Modifier
                        .size(3.dp, 30.dp)
                        .background(focusFrameColor)
                        .align(Alignment.TopEnd)
                )

                // 왼쪽 하단 모서리
                Box(
                    modifier = Modifier
                        .size(30.dp, 3.dp)
                        .background(focusFrameColor)
                        .align(Alignment.BottomStart)
                )
                Box(
                    modifier = Modifier
                        .size(3.dp, 30.dp)
                        .background(focusFrameColor)
                        .align(Alignment.BottomStart)
                )

                // 오른쪽 하단 모서리
                Box(
                    modifier = Modifier
                        .size(30.dp, 3.dp)
                        .background(focusFrameColor)
                        .align(Alignment.BottomEnd)
                )
                Box(
                    modifier = Modifier
                        .size(3.dp, 30.dp)
                        .background(focusFrameColor)
                        .align(Alignment.BottomEnd)
                )
            }
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
                onClick = {
                    if (currentMode != FoodMode.Auto_Recognition) {
                        currentMode = FoodMode.Auto_Recognition
                        if (labels.isEmpty()) {
                            labels = loadLabels(context)
                        }
                    }
                },
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
                            shouldAnalyze = true  // 셔터 버튼을 눌렀을 때 분석 플래그 설정
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