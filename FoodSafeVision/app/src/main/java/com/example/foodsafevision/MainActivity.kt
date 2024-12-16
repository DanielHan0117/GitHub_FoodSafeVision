package com.example.foodsafevision

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.foodsafevision.data.database.BarcodeDatabase
import com.example.foodsafevision.data.repository.BarcodeRepository
import com.example.foodsafevision.ui.theme.FoodSafeVisionTheme
import kotlinx.coroutines.launch
import android.Manifest
import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import androidx.room.Room
import com.example.foodsafevision.data.database.FoodDatabase
import com.example.foodsafevision.data.model.FoodEntity
import com.example.foodsafevision.data.repository.FoodRepository
import kotlinx.coroutines.Dispatchers
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodsafevision.data.database.TagDatabase
import com.example.foodsafevision.data.repository.TagRepository
import com.example.foodsafevision.util.NotificationHelper
import com.example.foodsafevision.viewmodel.FoodViewModel
import com.example.foodsafevision.viewmodel.FoodViewModelFactory
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class MainActivity : ComponentActivity() {
    private val PERMISSION_REQUEST_CODE = 100
    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.POST_NOTIFICATIONS
    )

    private lateinit var barcodeRepository: BarcodeRepository
    private lateinit var foodRepository: FoodRepository
    private lateinit var tagRepository: TagRepository
    private lateinit var notificationHelper: NotificationHelper

    private var dDayPeriod by mutableStateOf(7)
    private var isNotificationEnabled by mutableStateOf(true)
    private var notificationHour by mutableStateOf(7)
    private var notificationMinute by mutableStateOf(0)

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestPermissions()

        // 데이터베이스 초기화 및 JSON 데이터 로드
        val database = BarcodeDatabase.getDatabase(this)
        barcodeRepository = BarcodeRepository(database.barcodeDao())

        // JSON 데이터 로드 및 확인
        lifecycleScope.launch {
            barcodeRepository.loadBarcodeDataFromJson(this@MainActivity)
            val count = barcodeRepository.getProductCount()
            val allBarcodes = barcodeRepository.getAllBarcodes()
        }

        // 음식 데이터베이스 초기화 및 JSON 데이터 로드
        val foodDatabase = Room.databaseBuilder(
            this,
            FoodDatabase::class.java,
            "food_database"
        ).build()
        // Repository 초기화
        foodRepository = FoodRepository(foodDatabase.foodDao())

        // 태그 데이터베이스 초기화
        val tagDatabase = TagDatabase.getDatabase(this)
        tagRepository = TagRepository(tagDatabase.tagDao())

        // 알림 헬퍼 초기화
        notificationHelper = NotificationHelper(this)

        // 유통기한 체크 및 알림 설정
        lifecycleScope.launch {
            foodRepository.getAllFoods().collect { foodList ->
                val currentDate = LocalDate.now()
                foodList.forEach { food ->
                    val expirationDate = LocalDate.parse(food.expirationDate)
                    val daysUntilExpiry = ChronoUnit.DAYS.between(currentDate, expirationDate)

                    if (daysUntilExpiry in 0..dDayPeriod) {
                        notificationHelper.scheduleNotification(
                            food,
                            dDayPeriod,
                            isNotificationEnabled,
                            notificationHour,
                            notificationMinute
                        )
                    }
                }
            }
        }

        window.statusBarColor = androidx.compose.ui.graphics.Color.Black.toArgb()
        setContent {
            FoodSafeVisionTheme {
                val navController = rememberNavController()
                var showBarcodeDialog by remember { mutableStateOf(false) }
                var showAutoDialog by remember { mutableStateOf(false) }
                var scannedBarcode by remember { mutableStateOf<String?>(null) }
                var foodName by remember { mutableStateOf<String?>(null) }
                var expirationDate by remember { mutableStateOf<String?>(null) }
                var showDateDialog by remember { mutableStateOf(false) }

                NavHost(
                    navController = navController,
                    startDestination = "foodListScreen"
                ) {
                    composable("foodListScreen") {
                        FoodListScreen(
                            foodRepository = foodRepository,
                            tagRepository = tagRepository,
                            onAddFood = {
                                navController.navigate("foodScanner")
                            }
                        )
                    }
                    composable("foodScanner") {
                        FoodScanner(
                            barcodeRepository = barcodeRepository,
                            onBarcodeDetected = { barcode, productName ->
                                scannedBarcode = barcode.toString()
                                foodName = productName.toString()
                                showBarcodeDialog = true
                                navController.navigate("dateScanner")
                            },
                            onObjectDetected = { detectedLabel ->
                                foodName = detectedLabel
                                showAutoDialog = true
                                navController.navigate("dateScanner")
                            },
                            onTextInput = { inputText ->
                                foodName = inputText
                                showAutoDialog = true
                                navController.navigate("dateScanner")
                            },
                            onClickedDismiss = {
                                navController.navigate("foodListScreen") {
                                    popUpTo("foodScanner") { inclusive = true }
                                }
                            }
                        )

                        if (showBarcodeDialog) {
                            BarcodeResultDialog(
                                barcodeNumber = scannedBarcode ?: "",
                                productName = foodName,
                                onDismiss = { showBarcodeDialog = false }
                            )
                        }

                        if (showAutoDialog) {
                            ObjectDetectionDialog(
                                detectedLabel = foodName ?: "",
                                onDismiss = { showAutoDialog = false }
                            )
                        }
                    }
                    composable("dateScanner") {
                        val viewModel: FoodViewModel = viewModel(
                            factory = FoodViewModelFactory(foodRepository)
                        )

                        DateScanner(
                            onDateDetected = {
                                //navController.navigate("registerFood")
                            },
                            onDateSelected = { selectedDate ->
                                expirationDate = selectedDate
                                showDateDialog = true

                                // Food 데이터베이스에 새 항목 추가
                                viewModel.insertFood(
                                    FoodEntity(
                                        barcodeNumber = null.toString(),
                                        foodName = foodName ?: "",
                                        expirationDate = selectedDate,
                                        tag = "나의 냉장고",
                                        quantity = 1
                                    )
                                )

                                //navController.navigate("registerFood")
                            },
                            onClickedDismiss = {
                                navController.navigate("foodListScreen") {
                                    popUpTo("foodScanner") { inclusive = true }
                                }
                            }
                        )

                        if (showDateDialog) {
                            DateDetectionDialog(
                                detectedDate = expirationDate ?: "",
                                onDismiss = { showDateDialog = false }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }
    }
}

@Composable
fun BarcodeResultDialog(
    barcodeNumber: String,
    productName: String?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("바코드 스캔 결과") },
        text = {
            Column {
                Text("바코드 번호: $barcodeNumber")
                if (productName != null) {
                    Text("식품명: $productName")
                } else {
                    Text("데이터베이스에서 식품을 찾을 수 없습니다.")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("확인")
            }
        }
    )
}

@Composable
fun ObjectDetectionDialog(
    detectedLabel: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("객체 인식 결과") },
        text = {
            Column {
                Text("인식된 객체: $detectedLabel")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("확인")
            }
        }
    )
}

@Composable
fun DateDetectionDialog(
    detectedDate: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("유통기한 인식 결과") },
        text = {
            Column {
                Text("인식된 유통기한: $detectedDate")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("확인")
            }
        }
    )
}