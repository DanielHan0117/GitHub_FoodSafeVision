@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.foodsafevision

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import com.example.foodsafevision.data.model.FoodEntity
import com.example.foodsafevision.data.repository.TagRepository
import com.example.foodsafevision.viewmodel.FoodViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterFood(
    viewModel: FoodViewModel,
    initialFoodName: String,
    initialExpirationDate: String,
    initialBarcode: String?,
    tagRepository: TagRepository,
    onNavigateBack: () -> Unit,
    onSaveComplete: () -> Unit
) {
    val tags by tagRepository.allTags.collectAsState(initial = emptyList())

    val barcodeNumber by remember { mutableStateOf(initialBarcode ?: "") }
    var newFoodName by remember { mutableStateOf(initialFoodName) }
    var newExpirationDate by remember { mutableStateOf(initialExpirationDate) }
    var newTag by remember { mutableStateOf("나의 냉장고") }
    var newQuantity by remember { mutableStateOf("1") }

    var isExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = LocalDate.now()
            .atStartOfDay(ZoneId.of("Asia/Seoul"))
            .toInstant()
            .toEpochMilli()
            .plus(TimeZone.getDefault().rawOffset)
    )

    val focusManager = LocalFocusManager.current

    Scaffold(
        containerColor = Color.White,
        topBar = {
            Column {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.Default.KeyboardArrowLeft,
                                contentDescription = "다시 등록",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    },
                    title = {
                        Text(
                            "음식 등록",
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White,
                        navigationIconContentColor = Color.Black
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
                .padding(horizontal = 16.dp)
                .background(Color.White)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    focusManager.clearFocus()
                },
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            TextField(
                value = barcodeNumber,
                onValueChange = { },
                enabled = false,
                readOnly = true,
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color(0xFFF5F5F5),
                    focusedContainerColor = Color(0xFFF5F5F5),
                    disabledIndicatorColor = Color.DarkGray,
                    disabledContainerColor = Color(0xFFF5F5F5),
                    errorContainerColor = Color(0xFFF5F5F5)
                ),
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.barcode_icon),
                        contentDescription = "Barcode Icon",
                        modifier = Modifier.size(24.dp),
                        tint = if (barcodeNumber.isBlank()) Color.Gray else Color.Black
                    )
                },
                modifier = Modifier.fillMaxWidth(0.95f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            TextField(
                value = newFoodName,
                onValueChange = { newFoodName = it },
                placeholder = { Text("음식명") },
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
                        contentDescription = "Food Icon",
                        modifier = Modifier.size(24.dp),
                        tint = Color.Black
                    )
                },
                modifier = Modifier.fillMaxWidth(0.95f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            TextField(
                value = newExpirationDate,
                onValueChange = { },
                enabled = false,
                readOnly = true,
                leadingIcon = {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = "Date Icon",
                        tint = Color.Black
                    )
                },
                placeholder = { Text("유통기한") },
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color(0xFFF5F5F5),
                    focusedContainerColor = Color(0xFFF5F5F5),
                    disabledIndicatorColor = Color.DarkGray,
                    disabledContainerColor = Color(0xFFF5F5F5),
                    disabledTextColor = Color.Black,
                    errorContainerColor = Color(0xFFF5F5F5)
                ),
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        showDatePicker = true
                    }
            )

            Spacer(modifier = Modifier.height(12.dp))

            ExposedDropdownMenuBox(
                expanded = isExpanded,
                onExpandedChange = { isExpanded = !isExpanded }
            ) {
                TextField(
                    value = newTag,
                    onValueChange = { },
                    readOnly = true,
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color(0xFFF5F5F5),
                        unfocusedIndicatorColor = Color.Gray,
                        focusedContainerColor = Color(0xFFF5F5F5),
                        focusedIndicatorColor = Color.Gray
                    ),
                    leadingIcon = {
                        Image(
                            painter = painterResource(id = R.drawable.tag_icon),
                            contentDescription = "Tag Icon",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(0.95f)
                )

                ExposedDropdownMenu(
                    expanded = isExpanded,
                    onDismissRequest = { isExpanded = false }
                ) {
                    tags.forEach { tag ->
                        DropdownMenuItem(
                            text = { Text(tag.name) },
                            onClick = {
                                newTag = tag.name
                                isExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextField(
                value = newQuantity,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() || (newValue.all { it.isDigit() } && newValue.toIntOrNull()
                            ?.let { it > 0 } == true)) {
                        newQuantity = newValue
                        if (newValue.isNotEmpty()) {
                            newValue.toIntOrNull()?.let {
                                if (it > 0) newQuantity = it.toString()
                            }
                        }
                    }
                },
                placeholder = { Text("수량") },
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color(0xFFF5F5F5),
                    unfocusedIndicatorColor = Color.Gray,
                    focusedContainerColor = Color(0xFFF5F5F5),
                    focusedIndicatorColor = Color.Gray,
                    cursorColor = Color.Black
                ),
                leadingIcon = {
                    Image(
                        painter = painterResource(id = R.drawable.quantity_icon),
                        contentDescription = "Quantity Icon",
                        modifier = Modifier.size(24.dp)
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(0.95f)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val newFood = FoodEntity(
                        id = 0,
                        barcodeNumber = barcodeNumber,
                        foodName = newFoodName,
                        expirationDate = newExpirationDate,
                        tag = newTag,
                        quantity = newQuantity.toInt()
                    )
                    viewModel.addFood(newFood)
                    onSaveComplete()
                },
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .height(56.dp),
                enabled = newFoodName.isNotBlank() && newQuantity.isNotBlank() && newQuantity.toIntOrNull()
                    ?.let { it > 0 } == true,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black,
                    contentColor = Color.White,
                    disabledContainerColor = Color.LightGray,
                    disabledContentColor = Color.DarkGray
                )
            ) {
                Text("저장")
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { timestamp ->
                        val localDate = Instant.ofEpochMilli(timestamp)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        newExpirationDate = localDate.toString()
                    }
                    showDatePicker = false
                }) {
                    Text("확인")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
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
                        "\n 유통기한 날짜 선택",
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