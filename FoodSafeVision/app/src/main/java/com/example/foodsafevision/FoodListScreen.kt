package com.example.foodsafevision

import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodsafevision.data.model.FoodEntity
import com.example.foodsafevision.data.repository.FoodRepository
import com.example.foodsafevision.data.repository.TagRepository
import com.example.foodsafevision.util.NotificationHelper
import com.example.foodsafevision.viewmodel.FoodViewModel
import com.example.foodsafevision.viewmodel.FoodViewModelFactory
import com.example.foodsafevision.viewmodel.TagViewModel
import com.example.foodsafevision.viewmodel.TagViewModelFactory
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.TimeZone
import kotlin.math.abs

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodListScreen(
    foodRepository: FoodRepository,
    tagRepository: TagRepository,
    onAddFood: () -> Unit
) {
    val foodViewModel: FoodViewModel = viewModel(
        factory = FoodViewModelFactory(foodRepository)
    )
    val tagViewModel: TagViewModel = viewModel(
        factory = TagViewModelFactory(tagRepository, foodRepository)
    )

    LaunchedEffect(Unit) {
        tagViewModel.refreshTags()
        foodViewModel.refreshFoods()
    }
    var tagSectionKey by remember { mutableStateOf(0) }

    val foodList by foodViewModel.allFoods.collectAsState(initial = emptyList())
    val tags by tagViewModel.allTags.collectAsState(initial = emptyList())
    var selectedTag by remember { mutableStateOf("나의 냉장고") }
    var showAddTagDialog by remember { mutableStateOf(false) }
    var showEditFoodDialog by remember { mutableStateOf(false) }
    var selectedFood by remember { mutableStateOf<FoodEntity?>(null) }
    var inputTag by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    var showSettingsDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    val notificationHelper = remember { NotificationHelper(context) }

    var dDayPeriod by remember {
        mutableStateOf(
            sharedPreferences.getInt("d_day_period", 7)
        )
    }

    var isNotificationEnabled by remember {
        mutableStateOf(
            sharedPreferences.getBoolean("notification_enabled", true)
        )
    }

    var notificationHour by remember {
        mutableStateOf(
            sharedPreferences.getInt("notification_hour", 7)
        )
    }

    var notificationMinute by remember {
        mutableStateOf(
            sharedPreferences.getInt("notification_minute", 0)
        )
    }

    val sortedFoodList by foodViewModel.sortedFoods.collectAsState(initial = emptyList())

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
                            IconButton(onClick = { showSettingsDialog = true }) {
                                Icon(
                                    Icons.Default.Menu,
                                    contentDescription = "메뉴",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Text(
                                text = getCurrentDate(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = onAddFood) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "식품 추가",
                                    modifier = Modifier.size(24.dp)
                                )
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
            key(tagSectionKey) {
                TagSection(
                    tags = tags.map { it },
                    selectedTag = selectedTag,
                    tagViewModel = tagViewModel,
                    foodViewModel = foodViewModel,
                    onTagSelected = { tag ->
                        selectedTag = tag
                        foodViewModel.refreshFoods()
                    },
                    onAddTag = { showAddTagDialog = true },
                    onEditTag = { oldTag, newTag ->
                        tagViewModel.updateTag(oldTag, newTag)
                        if (selectedTag == oldTag) {
                            selectedTag = newTag
                        }
                        foodViewModel.updateFoodsTag(oldTag, newTag)
                        tagSectionKey++
                    },
                    onDeleteTag = { tagToDelete ->
                        tagViewModel.deleteTag(tagToDelete)
                        if (selectedTag == tagToDelete) {
                            selectedTag = tags.firstOrNull() ?: ""
                        }
                        tagSectionKey++
                    }
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(sortedFoodList.filter { it.tag == selectedTag }) { food ->
                    FoodItem(
                        food = food,
                        onClick = {
                            selectedFood = food
                            showEditFoodDialog = true
                        },
                        dDayPeriod = dDayPeriod
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
            containerColor = Color.White,
            title = {
                Text("태그 추가", style = MaterialTheme.typography.titleLarge)
            },
            text = {
                TextField(
                    value = inputTag,
                    onValueChange = { inputTag = it },
                    placeholder = { Text("태그명") },
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color(0xFFF5F5F5),
                        unfocusedIndicatorColor = Color.Gray,
                        focusedContainerColor = Color(0xFFF5F5F5),
                        focusedIndicatorColor = Color.Gray,
                        cursorColor = Color.Black
                    ),
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black,
                        disabledContainerColor = Color.White,
                        disabledContentColor = Color.DarkGray
                    ),
                    enabled = inputTag.isNotBlank()
                ) {
                    Text("확인")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddTagDialog = false
                        inputTag = ""
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

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }

    SettingsDialog(
        showDialog = showSettingsDialog,
        onDismiss = { showSettingsDialog = false },
        currentDDayPeriod = dDayPeriod,
        onDDayPeriodChange = { newDDay ->
            dDayPeriod = newDDay
            sharedPreferences.edit()
                .putInt("d_day_period", newDDay)
                .apply()
            foodList.forEach { food ->
                notificationHelper.scheduleNotification(
                    food,
                    newDDay,
                    isNotificationEnabled,
                    notificationHour,
                    notificationMinute
                )
            }
        },
        isNotificationEnabled = isNotificationEnabled,
        onNotificationToggle = { enabled ->
            isNotificationEnabled = enabled
            sharedPreferences.edit()
                .putBoolean("notification_enabled", enabled)
                .apply()
        },
        currentHour = notificationHour,
        currentMinute = notificationMinute,
        onTimeChange = { hour, minute ->
            notificationHour = hour
            notificationMinute = minute
            sharedPreferences.edit()
                .putInt("notification_hour", hour)
                .putInt("notification_minute", minute)
                .apply()
            foodList.forEach { food ->
                notificationHelper.scheduleNotification(
                    food,
                    dDayPeriod,
                    isNotificationEnabled,
                    hour,
                    minute
                )
            }
        }
    )
}

@Composable
fun TagSection(
    tags: List<String>,
    selectedTag: String,
    tagViewModel: TagViewModel,
    foodViewModel: FoodViewModel,
    onTagSelected: (String) -> Unit,
    onAddTag: () -> Unit,
    onEditTag: (String, String) -> Unit,
    onDeleteTag: (String) -> Unit
) {
    var showEditTagDialog by remember { mutableStateOf(false) }
    var editingTag by remember { mutableStateOf("") }
    var editedTagName by remember { mutableStateOf("") }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

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
            containerColor = Color.White,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("태그 설정", style = MaterialTheme.typography.titleLarge)
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                when (tagViewModel.canDeleteTag(editingTag)) {
                                    is TagViewModel.DeleteTagResult.Success -> {
                                        if (selectedTag == editingTag) {
                                            val nextTag =
                                                tags.firstOrNull { it != editingTag } ?: ""
                                            onTagSelected(nextTag)
                                        }
                                        tagViewModel.deleteTag(editingTag)
                                        showEditTagDialog = false
                                    }

                                    is TagViewModel.DeleteTagResult.LastTag -> {
                                        Toast.makeText(
                                            context,
                                            "마지막 태그는 삭제할 수 없습니다",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }

                                    is TagViewModel.DeleteTagResult.HasFoods -> {
                                        Toast.makeText(
                                            context,
                                            "이 태그에 등록된 식품이 있어 삭제할 수 없습니다",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "태그 삭제"
                        )
                    }

                }
            },
            text = {
                TextField(
                    value = editedTagName,
                    onValueChange = { editedTagName = it },
                    placeholder = { Text(editingTag) },
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color(0xFFF5F5F5),
                        unfocusedIndicatorColor = Color.Gray,
                        focusedContainerColor = Color(0xFFF5F5F5),
                        focusedIndicatorColor = Color.Gray,
                        cursorColor = Color.Black
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            tagViewModel.updateTag(editingTag, editedTagName)
                            foodViewModel.updateFoodsTag(editingTag, editedTagName)
                            onEditTag(editingTag, editedTagName)
                            if (selectedTag == editingTag) {
                                onTagSelected(editedTagName)
                            }
                            tagViewModel.refreshTags()
                            showEditTagDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black,
                        disabledContainerColor = Color.White,
                        disabledContentColor = Color.DarkGray
                    ),
                    enabled = editedTagName.isNotBlank()
                ) {
                    Text("확인")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showEditTagDialog = false
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
fun FoodItem(food: FoodEntity, onClick: () -> Unit, dDayPeriod: Int) {
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
                        daysUntilExpiry <= dDayPeriod -> Color.Red
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
    var editedFoodName by remember { mutableStateOf(food.foodName) }
    var editedExpirationDate by remember { mutableStateOf(food.expirationDate) }
    var editedQuantity by remember { mutableStateOf(food.quantity.toString()) }
    var editedTag by remember { mutableStateOf(food.tag) }
    var expanded by remember { mutableStateOf(false) }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = LocalDate.parse(editedExpirationDate)
            .atStartOfDay(ZoneId.of("Asia/Seoul"))
            .toInstant()
            .toEpochMilli()
            .plus(TimeZone.getDefault().rawOffset)
    )

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
                            editedExpirationDate = localDate.toString()
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
                    onClick = {
                        showDatePicker = false
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

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("식품 수정", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "식품 삭제"
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(
                    value = food.barcodeNumber,
                    onValueChange = { },
                    enabled = false,
                    readOnly = true,
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color(0xFFF5F5F5),
                        focusedContainerColor = Color(0xFFF5F5F5),
                        unfocusedIndicatorColor = Color.Gray,
                        focusedIndicatorColor = Color.Gray,
                        disabledContainerColor = Color(0xFFF5F5F5)
                    ),
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.barcode_icon),
                            contentDescription = "Barcode Icon",
                            modifier = Modifier.size(24.dp),
                            tint = if (food.barcodeNumber.isBlank()) Color.Gray else Color.Black
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                TextField(
                    value = editedFoodName,
                    onValueChange = { editedFoodName = it },
                    placeholder = { Text(food.foodName) },
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color(0xFFF5F5F5),
                        unfocusedIndicatorColor = Color.Gray,
                        focusedContainerColor = Color(0xFFF5F5F5),
                        focusedIndicatorColor = Color.Gray,
                        cursorColor = Color.Black
                    ),
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.food_icon),
                            contentDescription = "식품명 수정",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextField(
                        value = editedExpirationDate,
                        onValueChange = { },
                        enabled = false,
                        readOnly = true,
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color(0xFFF5F5F5),
                            unfocusedIndicatorColor = Color.Gray,
                            focusedContainerColor = Color(0xFFF5F5F5),
                            focusedIndicatorColor = Color.Gray,
                            disabledContainerColor = Color(0xFFF5F5F5),
                            disabledTextColor = Color.Black
                        ),
                        leadingIcon = {
                            Icon(
                                Icons.Default.DateRange,
                                modifier = Modifier.size(24.dp),
                                contentDescription = "날짜 선택",
                                tint = Color.Black
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                showDatePicker = true
                            }
                    )
                }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    TextField(
                        value = editedTag,
                        onValueChange = { },
                        readOnly = true,
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color(0xFFF5F5F5),
                            unfocusedIndicatorColor = Color.Gray,
                            focusedContainerColor = Color(0xFFF5F5F5),
                            focusedIndicatorColor = Color.Gray,
                        ),
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.tag_icon),
                                contentDescription = "태그 수정",
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .background(Color.White)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        containerColor = Color.White
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

                TextField(
                    value = editedQuantity,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || (newValue.all { it.isDigit() } && newValue.toIntOrNull()
                                ?.let { it > 0 } == true)) {
                            editedQuantity = newValue
                            if (newValue.isNotEmpty()) {
                                newValue.toIntOrNull()?.let {
                                    if (it > 0) editedQuantity = it.toString()
                                }
                            }
                        }
                    },
                    placeholder = { Text(food.quantity.toString()) },
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color(0xFFF5F5F5),
                        unfocusedIndicatorColor = Color.Gray,
                        focusedContainerColor = Color(0xFFF5F5F5),
                        focusedIndicatorColor = Color.Gray,
                        cursorColor = Color.Black
                    ),
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.quantity_icon),
                            contentDescription = "수량 수정",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val editedFood = FoodEntity(
                        id = food.id,
                        barcodeNumber = food.barcodeNumber,
                        foodName = editedFoodName,
                        expirationDate = editedExpirationDate,
                        tag = editedTag,
                        quantity = editedQuantity.toInt()
                    )
                    onConfirm(editedFood)
                    onDismiss()
                },
                enabled = editedFoodName.isNotBlank() && editedQuantity.isNotBlank() && editedQuantity.toIntOrNull()
                    ?.let { it > 0 } == true,
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
                onClick = onDismiss,
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

@Composable
fun SettingsDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    currentDDayPeriod: Int,
    onDDayPeriodChange: (Int) -> Unit,
    isNotificationEnabled: Boolean,
    onNotificationToggle: (Boolean) -> Unit,
    currentHour: Int,
    currentMinute: Int,
    onTimeChange: (Int, Int) -> Unit
) {
    var selectedHour by remember { mutableStateOf(currentHour) }
    var selectedMinute by remember { mutableStateOf(currentMinute) }

    val hourListState = rememberLazyListState(
        initialFirstVisibleItemIndex = (Int.MAX_VALUE / 2) - ((Int.MAX_VALUE / 2) % 24) + currentHour
    )
    val minuteListState = rememberLazyListState(
        initialFirstVisibleItemIndex = (Int.MAX_VALUE / 2) - ((Int.MAX_VALUE / 2) % 60) + currentMinute
    )

    LaunchedEffect(hourListState) {
        hourListState.scrollToItem(hourListState.firstVisibleItemIndex, -23)
    }

    LaunchedEffect(minuteListState) {
        minuteListState.scrollToItem(minuteListState.firstVisibleItemIndex, -23)
    }

    LaunchedEffect(Unit) {
        hourListState.scrollToItem(
            (Int.MAX_VALUE / 2) - ((Int.MAX_VALUE / 2) % 24) + currentHour
        )
        minuteListState.scrollToItem(
            (Int.MAX_VALUE / 2) - ((Int.MAX_VALUE / 2) % 60) + currentMinute
        )
    }

    LaunchedEffect(hourListState.firstVisibleItemIndex) {
        val centerIndex = hourListState.firstVisibleItemIndex + 1
        val newHour = centerIndex % 24
        if (selectedHour != newHour) {
            selectedHour = newHour
            onTimeChange(newHour, selectedMinute)
        }
    }

    LaunchedEffect(minuteListState.firstVisibleItemIndex) {
        val centerIndex = minuteListState.firstVisibleItemIndex + 1
        val newMinute = centerIndex % 60
        if (selectedMinute != newMinute) {
            selectedMinute = newMinute
            onTimeChange(selectedHour, newMinute)
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = Color.White,
            title = { Text("설정", style = MaterialTheme.typography.headlineMedium) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // D-Day 설정
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("D-Day",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.Black
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            NumberPicker(
                                value = currentDDayPeriod,
                                onValueChange = onDDayPeriodChange,
                                range = 1..30
                            )
                        }
                    }

                    // 알림 설정
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "알림 설정",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.Black
                        )
                        Switch(
                            checked = isNotificationEnabled,
                            onCheckedChange = onNotificationToggle,
                            colors = SwitchDefaults.colors(
                                uncheckedBorderColor = Color.Gray,
                                uncheckedTrackColor = Color.White,
                                uncheckedThumbColor = Color.DarkGray,
                                checkedBorderColor = Color.Gray,
                                checkedTrackColor = Color.LightGray,
                                checkedThumbColor = Color.Black,
                            )
                        )
                    }

                    if (isNotificationEnabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("알림 시간",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.Black
                            )

                            Row(
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 시간 선택
                                Box(
                                    modifier = Modifier
                                        .height(46.dp)
                                        .width(50.dp)
                                ) {
                                    LazyColumn(
                                        state = hourListState,
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        userScrollEnabled = true
                                    ) {
                                        items(Int.MAX_VALUE) { index ->
                                            val hour = index % 24
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = String.format("%02d", hour),
                                                    style = MaterialTheme.typography.titleLarge,
                                                    color = if (hour == selectedHour) Color.Black else Color.LightGray
                                                )
                                            }
                                        }
                                    }
                                }

                                // 분 선택
                                Box(
                                    modifier = Modifier
                                        .height(46.dp)
                                        .width(50.dp)
                                ) {
                                    LazyColumn(
                                        state = minuteListState,
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        userScrollEnabled = true
                                    ) {
                                        items(Int.MAX_VALUE) { index ->
                                            val minute = index % 60
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = String.format("%02d", minute),
                                                    style = MaterialTheme.typography.titleLarge,
                                                    color = if (minute == selectedMinute) Color.Black else Color.LightGray
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onTimeChange(selectedHour, selectedMinute)
                        onDismiss()
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
            }
        )
    }
}

@Composable
fun NumberPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = {
                if (value > range.first) onValueChange(value - 1)
            }
        ) {
            Text("-", style = MaterialTheme.typography.headlineLarge)
        }
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge
        )
        IconButton(
            onClick = {
                if (value < range.last) onValueChange(value + 1)
            }
        ) {
            Text("+", style = MaterialTheme.typography.headlineMedium)
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