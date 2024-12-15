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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodsafevision.data.model.FoodEntity
import com.example.foodsafevision.data.model.TagEntity
import com.example.foodsafevision.data.repository.FoodRepository
import com.example.foodsafevision.data.repository.TagRepository
import com.example.foodsafevision.util.NotificationHelper
import com.example.foodsafevision.viewmodel.FoodViewModel
import com.example.foodsafevision.viewmodel.FoodViewModelFactory
import com.example.foodsafevision.viewmodel.TagViewModel
import com.example.foodsafevision.viewmodel.TagViewModelFactory
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodListScreen(
    foodRepository: FoodRepository,
    tagRepository: TagRepository,
    onAddFood: () -> Unit,
    onMenuClick: () -> Unit
) {
    val foodViewModel: FoodViewModel = viewModel(
        factory = FoodViewModelFactory(foodRepository)
    )
    val tagViewModel: TagViewModel = viewModel(
        factory = TagViewModelFactory(tagRepository)
    )

    val foodList by foodViewModel.getAllFoods().collectAsState(initial = emptyList())
    val tags by tagViewModel.allTags.collectAsState(initial = emptyList())
    var selectedTag by remember { mutableStateOf("나의 냉장고") }
    var showAddTagDialog by remember { mutableStateOf(false) }
    var showEditFoodDialog by remember { mutableStateOf(false) }
    var selectedFood by remember { mutableStateOf<FoodEntity?>(null) }
    var inputTag by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    val context = LocalContext.current
    val notificationHelper = remember { NotificationHelper(context) }

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
                            IconButton(onClick = onAddFood) {
                                Icon(Icons.Default.Add, contentDescription = "추가")
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
                tags = tags.map { it }, // TagEntity의 name만 추출
                selectedTag = selectedTag,
                tagViewModel = tagViewModel,
                onTagSelected = { tag -> selectedTag = tag },
                onAddTag = { showAddTagDialog = true },
                onEditTag = { oldTag, newTag ->
                    tagViewModel.updateTag(TagEntity(name = oldTag).toString(), newTag)
                    if (selectedTag == oldTag) selectedTag = newTag
                },
                onDeleteTag = { tagToDelete ->
                    tagViewModel.deleteTag(tagToDelete)
                    if (selectedTag == tagToDelete) {
                        selectedTag = tags.firstOrNull() ?: ""
                    }
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
                        foodViewModel.updateFood(editedFood)
                        showEditFoodDialog = false
                    },
                    onDelete = {
                        foodViewModel.deleteFood(selectedFood!!)
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
                Text("태그 추가", style = MaterialTheme.typography.titleMedium)
            },
            text = {
                TextField(
                    value = inputTag,
                    onValueChange = { inputTag = it },
                    placeholder = { Text("태그명") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (inputTag.isNotBlank()) {
                            tagViewModel.addTag(inputTag)
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
    tagViewModel: TagViewModel,
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
                    Text("태그 설정", style = MaterialTheme.typography.titleLarge)
                    IconButton(
                        onClick = {
                            tagViewModel.deleteTag(editingTag)
                            if (selectedTag == editingTag) {
                                onTagSelected(tags.firstOrNull() ?: "")
                            }
                            showEditTagDialog = false
                        }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "삭제")
                    }
                }
            },
            text = {
                TextField(
                    value = editedTagName,
                    onValueChange = { editedTagName = it },
                    placeholder = { Text(editingTag) },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        tagViewModel.updateTag(editingTag, editedTagName)
                        if (selectedTag == editingTag) {
                            onTagSelected(editedTagName)
                        }
                        showEditTagDialog = false
                    }
                ) {
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
fun FoodItem(food: FoodEntity, onClick: () -> Unit) {
    val currentDate = LocalDate.now()
    val expirationDate = LocalDate.parse(food.expirationDate)
    val daysUntilExpiry = ChronoUnit.DAYS.between(currentDate, expirationDate)

    val expiryStatus = when {
        daysUntilExpiry < 0 -> "D + ${abs(daysUntilExpiry)}"
        daysUntilExpiry == 0L -> "D - 0"
        else -> "D - $daysUntilExpiry"
    }

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
                    text = food.foodName,
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
                    text = expiryStatus,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        daysUntilExpiry <= 7 -> Color.Red
                        else -> Color.Black
                    }
                )
                Text(
                    text = formatDate(LocalDate.parse(food.expirationDate)),
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
    food: FoodEntity,
    tags: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (FoodEntity) -> Unit,
    onDelete: () -> Unit
) {
    var editedName by remember { mutableStateOf(food.foodName) }
    var editedExpiryDate by remember { mutableStateOf(food.expirationDate) }
    var editedQuantity by remember { mutableStateOf(food.quantity) }
    var editedTag by remember { mutableStateOf(food.tag) }
    var expanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { timestamp ->
                            val localDate = Instant.ofEpochMilli(timestamp)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            editedExpiryDate = localDate.toString()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("확인")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("취소")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

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
                    leadingIcon = {
                        Icon(Icons.Default.Edit, contentDescription = "음식명")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                TextField(
                    value = editedExpiryDate,
                    onValueChange = { },
                    readOnly = true,
                    leadingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "날짜 선택")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                TextField(
                    value = editedQuantity.toString(),
                    onValueChange = { newValue ->
                        newValue.toIntOrNull()?.let {
                            if (it > 0) editedQuantity = it
                        }
                    },
                    placeholder = { Text("수량") },
                    leadingIcon = {
                        Icon(Icons.Default.Check, contentDescription = "수량")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                ) {
                    TextField(
                        value = editedTag,
                        onValueChange = { },
                        readOnly = true,
                        leadingIcon = {
                            Icon(Icons.Default.List, contentDescription = "태그")
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        tags.forEach { tag ->
                            DropdownMenuItem(
                                text = { Text(tag) },
                                onClick = {
                                    editedTag = tag
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val editedFood = FoodEntity(
                        id = food.id,
                        barcodeNumber = food.barcodeNumber,
                        foodName = editedName,
                        expirationDate = editedExpiryDate,
                        tag = editedTag,
                        quantity = editedQuantity
                    )
                    onConfirm(editedFood)
                    onDismiss()
                }
            ) {
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