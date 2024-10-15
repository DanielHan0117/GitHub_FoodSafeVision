package com.example.foodsafevision

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodListScreen(
    foodList: List<Food>,
    onAddFood: () -> Unit,
    onMenuClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onMenuClick) {
                            Icon(Icons.Default.Menu, contentDescription = "메뉴")
                        }
                        Text(
                            text = getCurrentDate(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onAddFood) {
                            Icon(Icons.Default.Edit, contentDescription = "편집")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            items(foodList) { food ->
                FoodItem(food)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun FoodItem(food: Food) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = food.name, style = MaterialTheme.typography.bodyLarge)
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "D-${food.daysUntilExpiry}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (food.daysUntilExpiry <= 5) Color.Red else Color.Unspecified
                )
                Text(
                    text = formatDate(food.expiryDate),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun formatDate(date: LocalDate): String {
    return "${date.monthValue}월 ${date.dayOfMonth}일 (${getKoreanDayOfWeek(date.dayOfWeek)})"
}

@RequiresApi(Build.VERSION_CODES.O)
fun getCurrentDate(): String {
    val currentDate = LocalDate.now()
    val formatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")
    val formattedDate = currentDate.format(formatter)
    val koreanDayOfWeek = getKoreanDayOfWeek(currentDate.dayOfWeek)
    return "$formattedDate ($koreanDayOfWeek)"
}

@RequiresApi(Build.VERSION_CODES.O)
fun getKoreanDayOfWeek(dayOfWeek: DayOfWeek): String {
    return when (dayOfWeek) {
        DayOfWeek.MONDAY -> "월"
        DayOfWeek.TUESDAY -> "화"
        DayOfWeek.WEDNESDAY -> "수"
        DayOfWeek.THURSDAY -> "목"
        DayOfWeek.FRIDAY -> "금"
        DayOfWeek.SATURDAY -> "토"
        DayOfWeek.SUNDAY -> "일"
    }
}

data class Food(
    val name: String,
    val expiryDate: LocalDate
) {
    val daysUntilExpiry: Long
        @RequiresApi(Build.VERSION_CODES.O)
        get() = ChronoUnit.DAYS.between(LocalDate.now(), expiryDate)
}

@RequiresApi(Build.VERSION_CODES.O)
fun createSampleFoodList(): List<Food> {
    return listOf(
        Food("사과", LocalDate.of(2024, 10, 18)),
        Food("바나나", LocalDate.of(2024, 10, 24)),
        Food("가나다라마바사", LocalDate.of(2024, 10, 26)),
        Food("가나다라마바사", LocalDate.of(2024, 10, 26)),
        Food("가나다라마바사", LocalDate.of(2024, 10, 26)),
        Food("가나다라마바사", LocalDate.of(2024, 10, 26)),
        Food("가나다라마바사", LocalDate.of(2024, 10, 26)),
        Food("가나다라마바사", LocalDate.of(2024, 10, 26)),
        Food("가나다라마바사", LocalDate.of(2024, 10, 26)),
        Food("가나다라마바사", LocalDate.of(2024, 10, 26)),
        Food("가나다라마바사", LocalDate.of(2024, 10, 26)),
        Food("가나다라마바사", LocalDate.of(2024, 10, 26)),
        Food("우유", LocalDate.of(2024, 10, 16)),
        Food("배", LocalDate.of(2024, 10, 20)),
        Food("포도", LocalDate.of(2024, 10, 21))
    ).sortedBy { it.expiryDate }
}