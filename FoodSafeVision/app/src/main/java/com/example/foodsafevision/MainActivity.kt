package com.example.foodsafevision

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import com.example.foodsafevision.ui.theme.FoodSafeVisionTheme

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = androidx.compose.ui.graphics.Color.Black.toArgb()
        setContent {
            FoodSafeVisionTheme {
                val foodList = remember { createSampleFoodList() }

                //FoodScanner()

                //FoodList()

                FoodListScreen(
                    foodList = foodList,
                    onAddFood = {
                        // FoodScanner를 사용하여 새 음식 추가 로직
                    },
                    onMenuClick = {
                        // 메뉴 열기 로직
                    }
                )

            }
        }
    }
}