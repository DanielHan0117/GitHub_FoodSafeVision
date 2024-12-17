package com.example.foodsafevision

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
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
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.room.Room
import com.example.foodsafevision.data.database.FoodDatabase
import com.example.foodsafevision.data.repository.FoodRepository
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodsafevision.data.database.TagDatabase
import com.example.foodsafevision.data.repository.TagRepository
import com.example.foodsafevision.util.NotificationHelper
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
        val barcodeDatabase = BarcodeDatabase.getDatabase(this)
        barcodeRepository = BarcodeRepository(barcodeDatabase.barcodeDao())

        // JSON 데이터 로드 및 확인
        lifecycleScope.launch {
            barcodeRepository.loadBarcodeDataFromJson(this@MainActivity)
            val count = barcodeRepository.getProductCount()
            val allBarcodes = barcodeRepository.getAllBarcodes()
        }

        // 식품 데이터베이스 초기화 및 JSON 데이터 로드
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
                var scannedBarcode by remember { mutableStateOf<String?>(null) }
                var foodName by remember { mutableStateOf<String?>(null) }
                var expirationDate by remember { mutableStateOf<String?>(null) }

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
                        DisposableEffect(Unit) {
                            window.setFlags(
                                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                                WindowManager.LayoutParams.FLAG_FULLSCREEN
                            )
                            onDispose {
                                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                            }
                        }

                        FoodScanner(
                            onNavigateBack = {
                                navController.navigate("foodListScreen") {
                                    popUpTo("foodListScreen") { inclusive = true }
                                }
                            },
                            barcodeRepository = barcodeRepository,
                            onBarcodeDetected = { barcode, productName ->
                                scannedBarcode = barcode.toString()
                                foodName = productName.toString()
                                navController.navigate("dateScanner")
                            },
                            onFoodNameDetected = { detectedLabel ->
                                foodName = detectedLabel
                                navController.navigate("dateScanner")
                            },
                            onFoodNameInput = { inputFoodName ->
                                foodName = inputFoodName
                                navController.navigate("dateScanner")
                            },
                            onClickedDismiss = {
                                scannedBarcode = null
                                foodName = null
                                expirationDate = null
                                navController.navigate("foodListScreen") {
                                    popUpTo("foodListScreen") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("dateScanner") {
                        DisposableEffect(Unit) {
                            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
                                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            onDispose {
                                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                            }
                        }

                        DateScanner(
                            onDateDetected = { detectedDate ->
                                expirationDate = detectedDate
                                navController.navigate("registerFood")
                            },
                            onDateSelected = { selectedDate ->
                                expirationDate = selectedDate
                                navController.navigate("registerFood")
                            },
                            onClickedDismiss = {
                                scannedBarcode = null
                                foodName = null
                                expirationDate = null
                                navController.navigate("foodListScreen") {
                                    popUpTo("foodListScreen") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("registerFood") {
                        RegisterFood(
                            viewModel = viewModel(factory = FoodViewModelFactory(foodRepository)),
                            initialFoodName = foodName ?: "",
                            initialExpirationDate = expirationDate ?: "",
                            initialBarcode = scannedBarcode,
                            tagRepository = tagRepository,
                            onNavigateBack = {
                                scannedBarcode = null
                                foodName = null
                                expirationDate = null
                                navController.navigate("foodScanner") {
                                    popUpTo("foodScanner") { inclusive = true }
                                }
                            },
                            onSaveComplete = {
                                scannedBarcode = null
                                foodName = null
                                expirationDate = null
                                navController.navigate("foodListScreen") {
                                    popUpTo("foodListScreen") { inclusive = true }
                                }
                            }
                        )
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
fun FocusFrame(frameColor: Color = Color.White) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val lineThickness = 3.dp
        val lineLength = 30.dp
        Box(
            modifier = Modifier
                .width(300.dp)
                .height(100.dp)
        ) {
            listOf(
                Alignment.TopStart,
                Alignment.TopEnd,
                Alignment.BottomStart,
                Alignment.BottomEnd
            ).forEach { alignment ->
                Box(
                    modifier = Modifier
                        .size(lineLength, lineThickness)
                        .background(frameColor)
                        .align(alignment)
                )
                Box(
                    modifier = Modifier
                        .size(lineThickness, lineLength)
                        .background(frameColor)
                        .align(alignment)
                )
            }
        }
    }
}