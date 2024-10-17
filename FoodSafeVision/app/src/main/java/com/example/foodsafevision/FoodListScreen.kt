package com.example.foodsafevision

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
    onCheckFood: () -> Unit,
    onMenuClick: () -> Unit
) {
    var selectedTag by remember { mutableStateOf("나의 냉장고") }
    var tags by remember { mutableStateOf(listOf("나의 냉장고", "편의점")) }
    var showAddTagDialog by remember { mutableStateOf(false) }
    var showEditTagDialog by remember { mutableStateOf(false) }
    var showEditFoodDialog by remember { mutableStateOf(false) }
    var selectedFood by remember { mutableStateOf<Food?>(null) }
    var inputTag by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            Column {
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
                            IconButton(onClick = onCheckFood) {
                                Icon(Icons.Default.Check, contentDescription = "선택")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = Color.Black,
                        navigationIconContentColor = Color.Black,
                        actionIconContentColor = Color.Black
                    )
                )
                HorizontalDivider(thickness = 1.dp, color = Color.LightGray)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TagSection(
                tags = tags,
                selectedTag = selectedTag,
                onTagSelected = { tag -> selectedTag = tag },
                onAddTag = { showAddTagDialog = true },
                onEditTag = { oldTag, newTag ->
                    tags = tags.map { if (it == oldTag) newTag else it }
                    if (selectedTag == oldTag) selectedTag = newTag
                },
                onDeleteTag = { tagToDelete ->
                    tags = tags.filter { it != tagToDelete }
                    if (selectedTag == tagToDelete) selectedTag = tags.firstOrNull() ?: ""
                }
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(foodList.filter { it.tag == selectedTag }) { food ->
                    FoodItem(
                        food = food,
                        onClick = {
                            selectedFood = food
                            showEditFoodDialog = true
                        }
                    )
                }
            }

            if (showEditFoodDialog && selectedFood != null) {
                EditFoodDialog(
                    food = selectedFood!!,
                    tags = tags,
                    onDismiss = { showEditFoodDialog = false },
                    onConfirm = { editedFood ->
                        // TODO: 여기서 수정된 음식 정보를 처리하는 로직 추가
                        showEditFoodDialog = false
                    },
                    onDelete = {
                        // TODO: 여기서 음식 삭제 로직 추가
                        showEditFoodDialog = false
                    }
                )
            }
        }
    }

    if (showAddTagDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddTagDialog = false
                inputTag = ""
            },
            title = {
                Text(
                    "태그 추가",
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                TextField(
                    value = inputTag,
                    placeholder = { Text("태그명") },
                    onValueChange = { inputTag = it },
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
                        if (inputTag.isNotBlank()) {
                            tags = tags + inputTag
                            showAddTagDialog = false
                            inputTag = ""
                        }
                    },
                    enabled = inputTag.isNotBlank()
                ) {
                    Text("확인")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddTagDialog = false
                    inputTag = ""
                }) {
                    Text("취소")
                }
            }
        )
    }
}

@Composable
fun TagSection(
    tags: List<String>,
    selectedTag: String,
    onTagSelected: (String) -> Unit,
    onAddTag: () -> Unit,
    onEditTag: (String, String) -> Unit,
    onDeleteTag: (String) -> Unit
) {
    var showEditTagDialog by remember { mutableStateOf(false) }
    var editingTag by remember { mutableStateOf("") }
    var editedTagName by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState())
        ) {
            tags.forEach { tag ->
                Tag(
                    text = tag,
                    isSelected = tag == selectedTag,
                    onClick = { onTagSelected(tag) },
                    onLongClick = {
                        editingTag = tag
                        editedTagName = tag
                        showEditTagDialog = true
                    }
                )
            }
        }
        Box(
            modifier = Modifier
                .padding(start = 8.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                .clickable(onClick = onAddTag)
                .padding(4.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "태그 추가",
                tint = Color.Black,
                modifier = Modifier.size(24.dp)
            )
        }
    }

    if (showEditTagDialog) {
        AlertDialog(
            onDismissRequest = { showEditTagDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "태그 설정",
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(onClick = {
                        onDeleteTag(editingTag)
                        showEditTagDialog = false
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "삭제")
                    }
                }
            },
            text = {
                Column {
                    TextField(
                        value = editedTagName,
                        placeholder = { Text(editingTag) },
                        onValueChange = { editedTagName = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onEditTag(editingTag, editedTagName)
                    showEditTagDialog = false
                }) {
                    Text("확인")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditTagDialog = false }) {
                    Text("취소")
                }
            }
        )
    }
}

@Composable
fun Tag(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(end = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFFDCDCDC) else Color.White)
            .border(
                width = 1.dp,
                color = Color.LightGray,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onLongClick() },
                    onTap = { onClick() }
                )
            }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = Color.Black,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun FoodItem(food: Food, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = food.name,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (food.quantity > 1) {
                    Text(
                        text = " (${food.quantity})",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = food.expiryStatus,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (food.daysUntilExpiry <= 5) Color.Red else Color.Black
                )
                Text(
                    text = formatDate(food.expiryDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditFoodDialog(
    food: Food,
    tags: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (Food) -> Unit,
    onDelete: () -> Unit
) {
    var editedName by remember { mutableStateOf(food.name) }
    var editedExpiryDate by remember { mutableStateOf(food.expiryDate.toString()) }
    var editedQuantity by remember { mutableStateOf(food.quantity.toString()) }
    var editedTag by remember { mutableStateOf(food.tag) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("음식 수정", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "삭제")
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    placeholder = { Text(food.name) },
                    leadingIcon = {
                        Icon(Icons.Default.Edit, contentDescription = "음식명")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                TextField(
                    value = editedExpiryDate,
                    onValueChange = { editedExpiryDate = it },
                    placeholder = { Text(food.expiryDate.toString()) },
                    leadingIcon = {
                        Icon(Icons.Default.DateRange, contentDescription = "유통기한")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                TextField(
                    value = editedQuantity,
                    onValueChange = { editedQuantity = it },
                    placeholder = { Text(food.quantity.toString()) },
                    leadingIcon = {
                        Icon(Icons.Default.Check, contentDescription = "수량")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(
                    expanded = false,
                    onExpandedChange = { },
                ) {
                    TextField(
                        value = editedTag,
                        onValueChange = { },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = false) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenuBox(
                        expanded = false,
                        onExpandedChange = { },
                    ) {
                        TextField(
                            value = editedTag,
                            onValueChange = { },
                            readOnly = true,
                            leadingIcon = {
                                Icon(Icons.Default.List, contentDescription = "태그")
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = false) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = false,
                            onDismissRequest = { },
                        ) {
                            tags.forEach { tag ->
                                DropdownMenuItem(
                                    text = { Text(tag) },
                                    onClick = { editedTag = tag }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val editedFood = Food(
                    name = editedName,
                    expiryDate = LocalDate.parse(editedExpiryDate),
                    tag = editedTag,
                    quantity = editedQuantity.toIntOrNull() ?: 1
                )
                onConfirm(editedFood)
            }) {
                Text("확인")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
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
    val expiryDate: LocalDate,
    val tag: String,
    val quantity: Int = 1
) {
    val daysUntilExpiry: Long
        @RequiresApi(Build.VERSION_CODES.O)
        get() = ChronoUnit.DAYS.between(LocalDate.now(), expiryDate)

    @get:RequiresApi(Build.VERSION_CODES.O)
    val expiryStatus: String
        get() = when {
            daysUntilExpiry > 0 -> "D-${daysUntilExpiry}"
            daysUntilExpiry == 0L -> "D-0"
            else -> "D+${-daysUntilExpiry}"
        }
}

@RequiresApi(Build.VERSION_CODES.O)
fun createSampleFoodList(): List<Food> {
    return listOf(
        Food("사과", LocalDate.of(2024, 10, 18), "나의 냉장고", 1),
        Food("바나나", LocalDate.of(2024, 10, 24), "나의 냉장고", 2),
        Food("가나다라마바사", LocalDate.of(2024, 10, 26), "편의점", 99),
        Food("가나다라마바사", LocalDate.of(2024, 10, 26), "편의점"),
        Food("가나다라마바사", LocalDate.of(2024, 10, 26), "편의점"),
        Food("가나다라마바사", LocalDate.of(2024, 10, 26), "편의점"),
        Food("가나다라마바사", LocalDate.of(2024, 10, 26), "편의점"),
        Food("가나다라마바사", LocalDate.of(2024, 10, 26), "편의점"),
        Food("가나다라마바사", LocalDate.of(2024, 10, 26), "편의점"),
        Food("가나다라마바사", LocalDate.of(2024, 10, 26), "편의점"),
        Food("가나다라마바사", LocalDate.of(2024, 10, 26), "편의점"),
        Food("가나다라마바사", LocalDate.of(2024, 10, 26), "편의점"),
        Food("우유", LocalDate.of(2024, 10, 16), "나의 냉장고", 3),
        Food("배", LocalDate.of(2024, 10, 20), "나의 냉장고", 4),
        Food("포도", LocalDate.of(2024, 10, 21), "나의 냉장고", 5)
    ).sortedBy { it.expiryDate }
}
