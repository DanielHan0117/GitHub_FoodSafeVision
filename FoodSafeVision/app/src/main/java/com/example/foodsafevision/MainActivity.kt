package com.example.foodsafevision

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.example.foodsafevision.data.database.AppDatabase
import com.example.foodsafevision.data.database.BarcodeRepository
import com.example.foodsafevision.ui.theme.FoodSafeVisionTheme
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = androidx.compose.ui.graphics.Color.Black.toArgb()
        setContent {
            FoodSafeVisionTheme {
                val navController = rememberNavController()
                var foodList = remember { createSampleFoodList() }

                val database = Room.databaseBuilder(
                    applicationContext,
                    AppDatabase::class.java,
                    "barcode-database"
                ).build()

                val barcodeRepository = BarcodeRepository(database.barcodeProductDao())

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
                                navController.navigate("barcodeRegistration")
                            }
                        )
                    }
                    composable("barcodeRegistration") {
                        BarcodeRegistrationScreen(
                            barcodeRepository = barcodeRepository
                        )
                    }
                    composable("foodScanner") {
                        FoodScanner(
                            barcodeRepository = barcodeRepository,
                            onBarcodeDetected = { productName ->
                                val newFood = Food(
                                    name = productName,
                                    expiryDate = LocalDate.now().plusDays(7),
                                    tag = "나의 냉장고"
                                )
                                foodList.toMutableList().apply {
                                    add(newFood)
                                }.also {
                                    foodList = it
                                }
                                navController.navigate("foodListScreen")
                            },
                            onObjectDetected = {
                                navController.navigate("dateScanner")
                            }
                        )
                    }
                    composable("dateScanner") {
                        DateScanner()
                    }
                }
            }
        }
    }
}
