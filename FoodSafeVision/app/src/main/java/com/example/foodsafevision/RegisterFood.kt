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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.sharp.ArrowBack
import androidx.compose.material.icons.sharp.KeyboardArrowLeft
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
import com.example.foodsafevision.data.model.FoodEntity
import com.example.foodsafevision.data.repository.TagRepository
import com.example.foodsafevision.viewmodel.FoodViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

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

    val barcode by remember { mutableStateOf(initialBarcode ?: "") }
    var newFoodName by remember { mutableStateOf(initialFoodName) }
    var newExpiryDate by remember { mutableStateOf(initialExpirationDate) }
    var newTag by remember { mutableStateOf("나의 냉장고") }
    var newQuantity by remember { mutableStateOf("1") }

    var isExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { timestamp ->
                        val localDate = Instant.ofEpochMilli(timestamp)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        newExpiryDate = localDate.toString()
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
            }
        ) {
            DatePicker(
                state = datePickerState,
                title = {
                    Text(
                        "\n 유통기한 날짜 선택",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(
            onClick = { onNavigateBack() },
            modifier = Modifier.align(Alignment.Start)
        ) {
            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "다시 등록하기")
        }

        Spacer(modifier = Modifier.height(2.dp))

        TextField(
            value = barcode,
            onValueChange = { },
            enabled = false,
            readOnly = true,
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.barcode_icon),
                    contentDescription = "Barcode Icon",
                    modifier = Modifier.size(24.dp),
                    tint = Color.Black
                )
            },
            modifier = Modifier.fillMaxWidth(0.95f)
        )

        Spacer(modifier = Modifier.height(12.dp))

        TextField(
            value = newFoodName,
            onValueChange = { newFoodName = it },
            placeholder = { Text("음식명") },
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
            value = newExpiryDate,
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
                disabledTextColor = Color.Black,
                disabledLeadingIconColor = Color.Black,
                disabledIndicatorColor = Color.Gray,
                disabledContainerColor = Color.Unspecified
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
                }
            },
            placeholder = { Text("수량") },
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

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                val newFood = FoodEntity(
                    id = 0, // Room will auto-generate
                    barcodeNumber = barcode,
                    foodName = newFoodName,
                    expirationDate = newExpiryDate,
                    tag = newTag,
                    quantity = newQuantity.toIntOrNull() ?: 1
                )
                viewModel.addFood(newFood)
                onSaveComplete()
            },
            modifier = Modifier.fillMaxWidth(0.95f),
            enabled = newFoodName.isNotBlank() && newExpiryDate.isNotBlank()
        ) {
            Text("저장")
        }
    }
}