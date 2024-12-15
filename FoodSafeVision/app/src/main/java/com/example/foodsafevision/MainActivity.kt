package com.example.foodsafevision

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
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


class MainActivity : ComponentActivity() {
    private val PERMISSION_REQUEST_CODE = 100
    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA
    )

    private lateinit var barcodeRepository: BarcodeRepository

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


        window.statusBarColor = androidx.compose.ui.graphics.Color.Black.toArgb()
        setContent {
            FoodSafeVisionTheme {
                val navController = rememberNavController()
                val foodList = remember { createSampleFoodList() }
                var showBarcodeDialog by remember { mutableStateOf(false) }
                var showAutoDialog by remember { mutableStateOf(false) }
                var scannedBarcode by remember { mutableStateOf<String?>(null) }
                var foodName by remember { mutableStateOf<String?>(null) }
                var expirationDate by remember { mutableStateOf<String?>(null) }
                var showDateDialog by remember { mutableStateOf(false) }

                NavHost(
                    navController = navController,
                    startDestination = "foodScanner"
                ) {
                    composable("foodListScreen") {
                        FoodListScreen(
                            foodList = foodList,
                            onCheckFood = {
                                navController.navigate("foodScanner")
                            },
                            onMenuClick = {
                                // 메뉴 열기 로직
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
                                showDateDialog = true
                                navController.navigate("dateScanner")
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

                        if (showDateDialog) {
                            DateDetectionDialog(
                                detectedDate = expirationDate ?: "",
                                onDismiss = { showDateDialog = false }
                            )
                        }
                    }
                    composable("dateScanner") {
                        DateScanner(
                            onDateDetected = {
                                navController.navigate("???")
                            },
                            onTextInput = {
                                navController.navigate("???")
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