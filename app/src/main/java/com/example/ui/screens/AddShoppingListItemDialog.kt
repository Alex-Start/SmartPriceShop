package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.GoodWithPrices
import com.example.data.model.Shop
import com.example.ui.theme.*
import com.example.util.AppStrings
import com.example.util.LocalAppCurrency
import com.example.util.UnitPriceCalculator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddShoppingListItemDialog(
    listId: Long,
    goodsWithPrices: List<GoodWithPrices>,
    allShops: List<Shop>,
    onDismiss: () -> Unit,
    onAddGood: (goodId: Long, quantity: Double) -> Unit,
    onAddCustomItem: (
        name: String,
        category: String,
        quantity: Double,
        unit: String,
        weight: Double,
        weightUnit: String,
        estimatedPrice: Double,
        notes: String
    ) -> Unit
) {
    val currentCurrency = LocalAppCurrency.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = From Database, 1 = Custom Item

    // Database Search Tab State
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(AppStrings.ALL) }

    // Custom Item Tab State
    var customName by remember { mutableStateOf("") }
    var customCategory by remember { mutableStateOf(AppStrings.defaultCategories.first()) }
    var customQuantityText by remember { mutableStateOf("1") }
    var customUnit by remember { mutableStateOf("pcs") }
    var customWeightText by remember { mutableStateOf("500") }
    var customWeightUnit by remember { mutableStateOf("g") }
    var customEstimatedPriceText by remember { mutableStateOf("") }
    var customNotes by remember { mutableStateOf("") }

    val categories = listOf(AppStrings.ALL) + AppStrings.defaultCategories
    val units = listOf("pcs", "pack", "item", "g", "kg", "ml", "L")
    val weightUnits = listOf("g", "kg", "ml", "L", "oz", "lb")

    // Filter goods from database
    val filteredGoods = remember(goodsWithPrices, searchQuery, selectedCategory) {
        var list = goodsWithPrices
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            list = list.filter {
                it.good.name.lowercase().contains(q) ||
                it.good.category.lowercase().contains(q) ||
                (it.good.barcode?.contains(q) == true)
            }
        }
        if (selectedCategory != AppStrings.ALL) {
            list = list.filter { it.good.category.equals(selectedCategory, ignoreCase = true) }
        }
        list
    }

    val inputTextColor = Color(0xFF0F172A)
    val inputColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = inputTextColor,
        unfocusedTextColor = inputTextColor,
        focusedBorderColor = SapphireBrand,
        unfocusedBorderColor = HighDensityBorder,
        focusedLabelColor = SapphireBrand,
        unfocusedLabelColor = HighDensityTextSecondary,
        cursorColor = SapphireBrand,
        focusedPlaceholderColor = HighDensityTextMuted,
        unfocusedPlaceholderColor = HighDensityTextMuted,
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White
    )
    val inputTextStyle = TextStyle(
        color = inputTextColor,
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.9f)
                .testTag("add_shopping_list_item_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(HighDensityBorder)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SapphireContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PlaylistAdd,
                                contentDescription = null,
                                tint = SapphireBrand,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Add Item to Shopping List",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = HighDensityTextPrimary
                            )
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = HighDensityTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Segmented Tabs: "From Database" vs "Custom Item"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(HighDensityInputBg)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedTab == 0) SapphireBrand else Color.Transparent)
                            .clickable { selectedTab = 0 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Inventory2,
                                contentDescription = null,
                                tint = if (selectedTab == 0) Color.White else HighDensityTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "From Database",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (selectedTab == 0) Color.White else HighDensityTextSecondary
                                )
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedTab == 1) SapphireBrand else Color.Transparent)
                            .clickable { selectedTab = 1 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = null,
                                tint = if (selectedTab == 1) Color.White else HighDensityTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Custom Item",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (selectedTab == 1) Color.White else HighDensityTextSecondary
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Content
                if (selectedTab == 0) {
                    // ================= TAB 0: FROM GOODS DATABASE =================
                    Column(modifier = Modifier.weight(1f)) {
                        // Search bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search your catalog of goods...") },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = HighDensityTextSecondary, modifier = Modifier.size(18.dp))
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = inputColors,
                            textStyle = inputTextStyle,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_goods_for_list_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Category chips
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(categories) { cat ->
                                val isSelected = selectedCategory == cat
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (isSelected) SapphireBrand else HighDensityInputBg)
                                        .border(1.dp, if (isSelected) SapphireBrand else HighDensityBorder, CircleShape)
                                        .clickable { selectedCategory = cat }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = cat,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isSelected) Color.White else HighDensityTextSecondary
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // List of goods
                        if (filteredGoods.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No matching products found.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = HighDensityTextSecondary)
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredGoods) { item ->
                                    val good = item.good
                                    val cheapest = item.cheapestShopDetail
                                    var qty by remember { mutableDoubleStateOf(1.0) }

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = HighDensityInputBg),
                                        border = CardDefaults.outlinedCardBorder().copy(
                                            brush = androidx.compose.ui.graphics.SolidColor(HighDensityBorder)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = good.name,
                                                    style = MaterialTheme.typography.titleSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = HighDensityTextPrimary
                                                    )
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = good.category,
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontSize = 10.sp,
                                                            color = SapphireBrand,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    )
                                                    if (good.weight > 0) {
                                                        Text(
                                                            text = " • ${UnitPriceCalculator.formatWeight(good.weight, good.weightUnit)}",
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                fontSize = 10.sp,
                                                                color = HighDensityTextSecondary
                                                            )
                                                        )
                                                    }
                                                    if (cheapest != null) {
                                                        Text(
                                                            text = " • ${cheapest.shop.name}: ${UnitPriceCalculator.formatCurrencyWithConversion(cheapest.priceRecord.effectivePrice, cheapest.priceRecord.currencyCode, currentCurrency)}",
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                fontSize = 10.sp,
                                                                color = DealGreen,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        )
                                                    }
                                                }
                                            }

                                            // Add button
                                            Button(
                                                onClick = {
                                                    onAddGood(good.id, qty)
                                                    onDismiss()
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = SapphireBrand),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Add", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // ================= TAB 1: CUSTOM ITEM =================
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        OutlinedTextField(
                            value = customName,
                            onValueChange = { customName = it },
                            label = { Text("Item Name *") },
                            placeholder = { Text("e.g. Avocado 2-pack") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = inputColors,
                            textStyle = inputTextStyle,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_item_name_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Category
                        Text(
                            text = "Category",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = HighDensityTextSecondary
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(categories.filter { it != AppStrings.ALL }) { cat ->
                                val isSelected = customCategory == cat
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (isSelected) SapphireBrand else HighDensityInputBg)
                                        .border(1.dp, if (isSelected) SapphireBrand else HighDensityBorder, CircleShape)
                                        .clickable { customCategory = cat }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = cat,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isSelected) Color.White else HighDensityTextSecondary
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quantity & Unit
                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = customQuantityText,
                                onValueChange = { customQuantityText = it },
                                label = { Text("Quantity") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = inputColors,
                                textStyle = inputTextStyle,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedTextField(
                                value = customUnit,
                                onValueChange = { customUnit = it },
                                label = { Text("Unit") },
                                placeholder = { Text("pcs") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = inputColors,
                                textStyle = inputTextStyle,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Weight & Weight Unit (Optional for price per gram)
                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = customWeightText,
                                onValueChange = { customWeightText = it },
                                label = { Text("Weight / Size") },
                                placeholder = { Text("500") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = inputColors,
                                textStyle = inputTextStyle,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedTextField(
                                value = customWeightUnit,
                                onValueChange = { customWeightUnit = it },
                                label = { Text("Weight Unit") },
                                placeholder = { Text("g") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = inputColors,
                                textStyle = inputTextStyle,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = customEstimatedPriceText,
                            onValueChange = { customEstimatedPriceText = it },
                            label = { Text("Estimated Price (${currentCurrency.symbol}) Optional") },
                            placeholder = { Text("2.50") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = inputColors,
                            textStyle = inputTextStyle,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = customNotes,
                            onValueChange = { customNotes = it },
                            label = { Text("Notes / Brand Preference") },
                            placeholder = { Text("e.g. buy organic if available") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = inputColors,
                            textStyle = inputTextStyle,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Custom item save button
                    Button(
                        onClick = {
                            if (customName.isNotBlank()) {
                                onAddCustomItem(
                                    customName.trim(),
                                    customCategory,
                                    customQuantityText.toDoubleOrNull() ?: 1.0,
                                    customUnit.ifBlank { "pcs" },
                                    customWeightText.toDoubleOrNull() ?: 0.0,
                                    customWeightUnit.ifBlank { "g" },
                                    customEstimatedPriceText.toDoubleOrNull() ?: 0.0,
                                    customNotes.trim()
                                )
                                onDismiss()
                            }
                        },
                        enabled = customName.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_custom_shopping_item_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SapphireBrand)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Custom Item to List")
                    }
                }
            }
        }
    }
}
