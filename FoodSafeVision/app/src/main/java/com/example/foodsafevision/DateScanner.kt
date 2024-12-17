package com.example.foodsafevision

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
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
import androidx.camera.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.TimeZone

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateScanner(
    onDateDetected: (String) -> Unit = {},
    onDateSelected: (String) -> Unit = {},
    onClickedDismiss: () -> Unit = {}
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = LocalDate.now()
            .atStartOfDay(ZoneId.of("Asia/Seoul"))
            .toInstant()
            .toEpochMilli()
            .plus(TimeZone.getDefault().rawOffset)
    )

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    var detectedDate by remember { mutableStateOf("") }
    var shouldAnalyze by remember { mutableStateOf(false) }

    var focusFrameColor by remember { mutableStateOf(Color.White) }
    val coroutineScope = rememberCoroutineScope()

    fun showSuccessAndProceed(action: () -> Unit) {
        focusFrameColor = Color.Green
        coroutineScope.launch {
            delay(500)
            focusFrameColor = Color.White
            action()
        }
    }

    fun analyzeImage(imageProxy: ImageProxy) {
        try {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val inputImage = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.imageInfo.rotationDegrees
                )

                textRecognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        Log.d("DateScanner", "인식된 텍스트: ${visionText.text}")

                        val processedText = visionText.text
                            .replace("O", "0")
                            .replace("o", "0")
                            .replace("Z", "2")
                            .replace(",", ".")
                            .replace("/", ".")
                            .replace("\\s+".toRegex(), "")
                            .replace("[^0-9.-]".toRegex(), "")

                        val datePatterns = listOf(
                            """(\d{4})(\d{2})(\d{2})""".toRegex(),  // YYYYMMDD
                            """(\d{4})[.\-](\d{1,2})[.\-]?(\d{1,2})""".toRegex(),  // YYYY.MM.DD
                            """(\d{1,2})[.\-](\d{1,2})[.\-](\d{4})""".toRegex()    // MM.DD.YYYY
                        )

                        val detected = datePatterns.firstNotNullOfOrNull { pattern ->
                            pattern.find(processedText)?.let { matchResult ->
                                try {
                                    val (first, second, third) = matchResult.destructured
                                    when (pattern) {
                                        datePatterns[0] -> { // YYYY.MM.DD
                                            val year = first.toInt()
                                            val month = second.toInt()
                                            val day = third.toInt()
                                            if (year in 2000..2100 && month in 1..12 && day in 1..31) {
                                                Triple(year.toString(), month.toString().padStart(2, '0'), day.toString().padStart(2, '0'))
                                            } else null
                                        }
                                        else -> { // MM.DD.YYYY
                                            val month = first.toInt()
                                            val day = second.toInt()
                                            val year = third.toInt()
                                            if (year in 2000..2100 && month in 1..12 && day in 1..31) {
                                                Triple(year.toString(), month.toString().padStart(2, '0'), day.toString().padStart(2, '0'))
                                            } else null
                                        }
                                    }
                                } catch (e: NumberFormatException) {
                                    null
                                }
                            }
                        }

                        if (detected != null) {
                            val (year, month, day) = detected
                            Log.d("DateScanner", "추출된 연도: $year")
                            Log.d("DateScanner", "추출된 월: $month")
                            Log.d("DateScanner", "추출된 일: $day")

                            val formattedDate = "$year-$month-$day"
                            detectedDate = formattedDate

                            showSuccessAndProceed {
                                onDateDetected(detectedDate)
                            }
                        } else {
                            detectedDate = "유통기한을 인식하지 못했습니다"
                        }
                    }
                    .addOnFailureListener {
                        detectedDate = "유통기한을 인식하지 못했습니다"
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                        shouldAnalyze = false
                    }
            }
        } catch (e: Exception) {
            Log.e("DateScanner", "이미지 분석 오류 발생", e)
            detectedDate = "유통기한을 인식하지 못했습니다"
            shouldAnalyze = false
            imageProxy.close()
        }
    }

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
                onClick = { showDatePicker = true },
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
            ) {
                Text("직접 선택")
            }
        }

        // 카메라 프리뷰 영역
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val previewView = remember { PreviewView(context) }
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize()) { view ->
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(view.surfaceProvider)
                    }
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                    imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                        if (shouldAnalyze) {
                            analyzeImage(imageProxy)
                            shouldAnalyze = false
                        } else {
                            imageProxy.close()
                        }
                    }
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                }, ContextCompat.getMainExecutor(context))
            }
            FocusFrame(frameColor = focusFrameColor)
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
                    .clickable {
                        shouldAnalyze = true
                    }
            )
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { timestamp ->
                            val localDate = Instant.ofEpochMilli(timestamp)
                                .atZone(ZoneId.of("Asia/Seoul"))
                                .toLocalDate()
                            val selectedDate = localDate.toString()
                            onDateSelected(selectedDate)
                        }
                        showDatePicker = false
                    },
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
                    onClick = { showDatePicker = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black,
                        disabledContainerColor = Color.White,
                        disabledContentColor = Color.DarkGray
                    )
                ) {
                    Text("취소")
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = Color.White
            )
        ) {
            DatePicker(
                state = datePickerState,
                title = {
                    Text(
                        "\n      유통기한 날짜 선택",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                colors = DatePickerDefaults.colors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black,
                    headlineContentColor = Color.Black,
                    weekdayContentColor = Color.Black,
                    subheadContentColor = Color.Black,
                    yearContentColor = Color.Black,
                    currentYearContentColor = Color.Black,
                    selectedYearContainerColor = Color.Black,
                    selectedYearContentColor = Color.White,
                    dayContentColor = Color.Black,
                    selectedDayContainerColor = Color.Black,
                    selectedDayContentColor = Color.White,
                    todayContentColor = Color.Black,
                    todayDateBorderColor = Color.Black
                )
            )
        }
    }
}

object BitmapUtils {
    fun preprocessImage(bitmap: Bitmap): Bitmap {
        // 1. 이미지 확대 (4배 확대)
        val scaledBitmap =
            Bitmap.createScaledBitmap(bitmap, bitmap.width * 4, bitmap.height * 4, true)

        // 2. 그레이스케일 변환
        val grayBitmap =
            Bitmap.createBitmap(scaledBitmap.width, scaledBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(grayBitmap)
        val paint = Paint()
        val colorMatrix = ColorMatrix().apply { setSaturation(0f) }
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(scaledBitmap, 0f, 0f, paint)

        // 3. 대비 및 Sharpening 적용
        val contrastBitmap = adjustContrast(grayBitmap, 2.5f)
        val sharpenedBitmap = applySharpening(contrastBitmap)

        // 4. Adaptive Thresholding 적용
        val thresholdBitmap = applyAdaptiveThreshold(sharpenedBitmap)

        // 5. Morphological Closing 연산으로 점선 연결
        return applyMorphologicalClosing(thresholdBitmap)
    }

    private fun adjustContrast(bitmap: Bitmap, contrast: Float): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
        val canvas = Canvas(output)
        val paint = Paint()
        val colorMatrix = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, 0f,
                0f, contrast, 0f, 0f, 0f,
                0f, 0f, contrast, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return output
    }

    private fun applySharpening(bitmap: Bitmap): Bitmap {
        val sharpenKernel = floatArrayOf(
            0f, -1f, 0f,
            -1f, 5f, -1f,
            0f, -1f, 0f
        )
        val kernel = ConvolveMatrix(3, sharpenKernel, 1f, 0f)
        return applyKernel(bitmap, kernel)
    }

    private fun applyAdaptiveThreshold(bitmap: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
        for (x in 0 until bitmap.width) {
            for (y in 0 until bitmap.height) {
                val pixel = bitmap.getPixel(x, y)
                val gray = android.graphics.Color.red(pixel)
                val threshold = 128 // Adaptive 값 조정 가능
                val binarizedColor =
                    if (gray > threshold) android.graphics.Color.WHITE else android.graphics.Color.BLACK
                output.setPixel(x, y, binarizedColor)
            }
        }
        return output
    }

    private fun applyMorphologicalClosing(bitmap: Bitmap): Bitmap {
        val outputBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
        for (x in 1 until bitmap.width - 1) {
            for (y in 1 until bitmap.height - 1) {
                val neighbors = mutableListOf<Int>()
                for (dx in -1..1) {
                    for (dy in -1..1) {
                        neighbors.add(bitmap.getPixel(x + dx, y + dy))
                    }
                }
                if (neighbors.any { it == android.graphics.Color.BLACK }) {
                    outputBitmap.setPixel(x, y, android.graphics.Color.BLACK)
                } else {
                    outputBitmap.setPixel(x, y, android.graphics.Color.WHITE)
                }
            }
        }
        return outputBitmap
    }

    private fun applyKernel(bitmap: Bitmap, kernel: ConvolveMatrix): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (y in 1 until bitmap.height - 1) {
            for (x in 1 until bitmap.width - 1) {
                var newPixel = 0f
                for (ky in 0 until kernel.size) {
                    for (kx in 0 until kernel.size) {
                        val pixel = pixels[(y + ky - 1) * bitmap.width + (x + kx - 1)]
                        newPixel += android.graphics.Color.red(pixel) * kernel.values[ky * kernel.size + kx]
                    }
                }
                val constrainedPixel = newPixel.coerceIn(0f, 255f).toInt()
                output.setPixel(
                    x,
                    y,
                    android.graphics.Color.rgb(constrainedPixel, constrainedPixel, constrainedPixel)
                )
            }
        }
        return output
    }

    class ConvolveMatrix(
        val size: Int,
        val values: FloatArray,
        val factor: Float,
        val offset: Float
    )
}