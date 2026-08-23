package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.GoodWithPrices
import com.example.data.model.Shop
import com.example.data.model.ShoppingList
import com.example.data.model.ShoppingListItem
import com.example.data.model.ShoppingListWithItems
import com.example.ui.theme.*
import com.example.util.AppCurrency
import com.example.util.LocalAppCurrency
import com.example.util.UnitPriceCalculator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListsScreen(
    shoppingListsWithItems: List<ShoppingListWithItems>,
    selectedList: ShoppingListWithItems?,
    goodsWithPrices: List<GoodWithPrices>,
    allShops: List<Shop>,
    onSelectList: (Long) -> Unit,
    onCreateList: (name: String, colorHex: String, targetShopId: Long?) -> Unit,
    onDeleteList: (Long) -> Unit,
    onAddGoodToShoppingList: (listId: Long, goodId: Long, quantity: Double) -> Unit,
    onAddCustomItemToShoppingList: (
        listId: Long,
        name: String,
        category: String,
        quantity: Double,
        unit: String,
        weight: Double,
        weightUnit: String,
        estimatedPrice: Double,
        notes: String
    ) -> Unit,
    onToggleItemChecked: (ShoppingListItem) -> Unit,
    onUpdateQuantity: (itemId: Long, quantity: Double) -> Unit,
    onDeleteItem: (itemId: Long) -> Unit,
    onClearCheckedItems: (listId: Long) -> Unit,
    onScanBarcodeForList: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentCurrency = LocalAppCurrency.current
    var showCreateListDialog by remember { mutableStateOf(false) }
    var showAddItemDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf<Long?>(null) }

    val activeList = selectedList ?: shoppingListsWithItems.firstOrNull()

    val uncheckedItems = remember(activeList) {
        activeList?.items?.filter { !it.isChecked } ?: emptyList()
    }
    val checkedItems = remember(activeList) {
        activeList?.items?.filter { it.isChecked } ?: emptyList()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HighDensityCanvas)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        // Top List Selector Bar & "New List" Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Shopping Lists",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = HighDensityTextPrimary
                )
            )

            Button(
                onClick = { showCreateListDialog = true },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SapphireBrand),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("create_shopping_list_button")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New List", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Horizontal List Selector Chips
        if (shoppingListsWithItems.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(shoppingListsWithItems) { item ->
                    val list = item.list
                    val isSelected = activeList?.list?.id == list.id
                    val listColor = try {
                        Color(android.graphics.Color.parseColor(list.colorHex))
                    } catch (e: Exception) {
                        SapphireBrand
                    }

                    Card(
                        modifier = Modifier
                            .clickable { onSelectList(list.id) }
                            .testTag("shopping_list_tab_${list.id}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) listColor.copy(alpha = 0.12f) else Color.White
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(
                                if (isSelected) listColor else HighDensityBorder
                            )
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(listColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = list.name,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) listColor else HighDensityTextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (isSelected) listColor else HighDensityInputBg)
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "${item.completedItemCount}/${item.totalItemCount}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else HighDensityTextSecondary
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (activeList == null) {
            // Empty state: no shopping lists
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ShoppingCart,
                        contentDescription = null,
                        tint = HighDensityTextSecondary,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No Shopping Lists Yet",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Create your first shopping list and add products from your price comparison database.",
                        style = MaterialTheme.typography.bodySmall.copy(color = HighDensityTextSecondary),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showCreateListDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SapphireBrand),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Create Shopping List")
                    }
                }
            }
        } else {
            // Active List Summary Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(HighDensityBorder)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeList.list.name,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = HighDensityTextPrimary
                                )
                            )
                            if (activeList.targetShop != null) {
                                Text(
                                    text = "Target Store: ${activeList.targetShop.name}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = SapphireBrand,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Share List
                            IconButton(
                                onClick = {
                                    val listText = buildString {
                                        appendLine("🛒 Shopping List: ${activeList.list.name}")
                                        appendLine("Estimated Cost: ${UnitPriceCalculator.formatCurrency(activeList.totalEstimatedCost, currentCurrency)}")
                                        appendLine()
                                        if (uncheckedItems.isNotEmpty()) {
                                            appendLine("To Buy:")
                                            uncheckedItems.forEach {
                                                appendLine("• ${it.name} x${it.quantity} ${it.unit} (${UnitPriceCalculator.formatCurrency(it.totalEstimatedPrice, currentCurrency)})")
                                            }
                                        }
                                        if (checkedItems.isNotEmpty()) {
                                            appendLine()
                                            appendLine("Purchased:")
                                            checkedItems.forEach {
                                                appendLine("✓ ${it.name} x${it.quantity} ${it.unit}")
                                            }
                                        }
                                    }
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Shopping List", listText))
                                    Toast.makeText(context, "Shopping list copied to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(imageVector = Icons.Outlined.Share, contentDescription = "Share", tint = HighDensityTextSecondary, modifier = Modifier.size(18.dp))
                            }

                            // Delete List
                            IconButton(
                                onClick = { showDeleteConfirmDialog = activeList.list.id },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(imageVector = Icons.Outlined.Delete, contentDescription = "Delete", tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Progress and Budget row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Estimated Total",
                                style = MaterialTheme.typography.labelSmall.copy(color = HighDensityTextSecondary)
                            )
                            Text(
                                text = UnitPriceCalculator.formatCurrency(activeList.totalEstimatedCost, currentCurrency),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = SapphireBrand
                                )
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${activeList.completedItemCount} of ${activeList.totalItemCount} items bought",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = HighDensityTextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val progress = if (activeList.totalItemCount > 0) activeList.completedItemCount.toFloat() / activeList.totalItemCount else 0f
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .width(130.dp)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = SapphireBrand,
                                trackColor = HighDensityInputBg
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Row: "+ Add Item" & "Scan Barcode"
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { showAddItemDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SapphireBrand),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("add_item_to_active_list_btn")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Item", fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = onScanBarcodeForList,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(0.9f)
                ) {
                    Icon(imageVector = Icons.Outlined.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Scan Item", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Items List
            if (activeList.items.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(HighDensityBorder)
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.PlaylistAdd,
                                contentDescription = null,
                                tint = HighDensityTextSecondary,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Your shopping list is empty",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap 'Add Item' to pick from your goods database",
                                style = MaterialTheme.typography.bodySmall.copy(color = HighDensityTextSecondary)
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Unchecked Items
                    if (uncheckedItems.isNotEmpty()) {
                        item {
                            Text(
                                text = "TO BUY (${uncheckedItems.size})",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = HighDensityTextSecondary
                                ),
                                modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 2.dp)
                            )
                        }

                        items(uncheckedItems, key = { it.id }) { item ->
                            ShoppingListItemRow(
                                item = item,
                                onToggleChecked = { onToggleItemChecked(item) },
                                onQuantityChange = { newQty -> onUpdateQuantity(item.id, newQty) },
                                onDelete = { onDeleteItem(item.id) }
                            )
                        }
                    }

                    // Checked / Completed Items
                    if (checkedItems.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 4.dp, top = 8.dp, bottom = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "PURCHASED (${checkedItems.size})",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = DealGreen
                                    )
                                )

                                Text(
                                    text = "Clear All",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFFDC2626),
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    modifier = Modifier
                                        .clickable { onClearCheckedItems(activeList.list.id) }
                                        .padding(4.dp)
                                )
                            }
                        }

                        items(checkedItems, key = { it.id }) { item ->
                            ShoppingListItemRow(
                                item = item,
                                onToggleChecked = { onToggleItemChecked(item) },
                                onQuantityChange = { newQty -> onUpdateQuantity(item.id, newQty) },
                                onDelete = { onDeleteItem(item.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Create New List Dialog
    if (showCreateListDialog) {
        CreateShoppingListDialog(
            allShops = allShops,
            onDismiss = { showCreateListDialog = false },
            onConfirm = { name, color, shopId ->
                onCreateList(name, color, shopId)
                showCreateListDialog = false
            }
        )
    }

    // Add Item Dialog
    if (showAddItemDialog && activeList != null) {
        AddShoppingListItemDialog(
            listId = activeList.list.id,
            goodsWithPrices = goodsWithPrices,
            allShops = allShops,
            onDismiss = { showAddItemDialog = false },
            onAddGood = { goodId, qty ->
                onAddGoodToShoppingList(activeList.list.id, goodId, qty)
            },
            onAddCustomItem = { name, cat, qty, unit, weight, weightUnit, estPrice, notes ->
                onAddCustomItemToShoppingList(
                    activeList.list.id,
                    name,
                    cat,
                    qty,
                    unit,
                    weight,
                    weightUnit,
                    estPrice,
                    notes
                )
            }
        )
    }

    // Delete List Confirmation
    if (showDeleteConfirmDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = { Text("Delete Shopping List?") },
            text = { Text("Are you sure you want to delete this shopping list and all its items?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteList(showDeleteConfirmDialog!!)
                        showDeleteConfirmDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ShoppingListItemRow(
    item: ShoppingListItem,
    onToggleChecked: () -> Unit,
    onQuantityChange: (Double) -> Unit,
    onDelete: () -> Unit
) {
    val currentCurrency = LocalAppCurrency.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("shopping_item_${item.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isChecked) Color(0xFFF9FAFB) else Color.White
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (item.isChecked) Color(0xFFE5E7EB) else HighDensityBorder
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Checkbox & Item Info
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = item.isChecked,
                    onCheckedChange = { onToggleChecked() },
                    colors = CheckboxDefaults.colors(checkedColor = SapphireBrand)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Column {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (item.isChecked) HighDensityTextSecondary else HighDensityTextPrimary,
                            textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None
                        )
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.category,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                color = SapphireBrand,
                                fontWeight = FontWeight.SemiBold
                            )
                        )

                        if (item.weight > 0.0) {
                            Text(
                                text = " • ${UnitPriceCalculator.formatWeight(item.weight, item.weightUnit)}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    color = HighDensityTextSecondary
                                )
                            )
                        }

                        if (item.estimatedUnitPrice > 0.0) {
                            Text(
                                text = " • ${UnitPriceCalculator.formatCurrency(item.totalEstimatedPrice, currentCurrency)}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (item.isChecked) DealGreen else SapphireBrand
                                )
                            )
                        }
                    }

                    if (item.notes.isNotBlank()) {
                        Text(
                            text = item.notes,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                color = HighDensityTextSecondary
                            )
                        )
                    }
                }
            }

            // Quantity adjusters & delete
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onQuantityChange(item.quantity - 1.0) },
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(HighDensityInputBg)
                ) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = "Minus", modifier = Modifier.size(12.dp))
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = if (item.quantity == item.quantity.toInt().toDouble()) "${item.quantity.toInt()}" else "${item.quantity}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = HighDensityTextPrimary
                    )
                )

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = { onQuantityChange(item.quantity + 1.0) },
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(HighDensityInputBg)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Plus", modifier = Modifier.size(12.dp))
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(imageVector = Icons.Outlined.Close, contentDescription = "Delete", tint = HighDensityTextSecondary, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@Composable
fun CreateShoppingListDialog(
    allShops: List<Shop>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, colorHex: String, targetShopId: Long?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#1E40AF") }
    var selectedShopId by remember { mutableStateOf<Long?>(null) }

    val colors = listOf("#1E40AF", "#16A34A", "#EA580C", "#9333EA", "#DC2626", "#0284C7", "#374151")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .testTag("create_shopping_list_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(HighDensityBorder)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Create Shopping List",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = HighDensityTextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("List Name *") },
                    placeholder = { Text("e.g. Weekly Groceries, Costco Run, BBQ") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF0F172A),
                        focusedBorderColor = SapphireBrand,
                        unfocusedBorderColor = HighDensityBorder,
                        focusedLabelColor = SapphireBrand,
                        unfocusedLabelColor = HighDensityTextSecondary,
                        cursorColor = SapphireBrand,
                        focusedPlaceholderColor = HighDensityTextMuted,
                        unfocusedPlaceholderColor = HighDensityTextMuted,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = Color(0xFF0F172A),
                        fontSize = 14.sp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_list_name_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Color Theme Selector
                Text(
                    text = "List Color Theme",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = HighDensityTextSecondary
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(colors) { hex ->
                        val isSelected = selectedColor == hex
                        val c = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) Color.Black else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Target Supermarket (Optional)
                if (allShops.isNotEmpty()) {
                    Text(
                        text = "Preferred Supermarket (Optional)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = HighDensityTextSecondary
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(allShops) { s ->
                            val isSelected = selectedShopId == s.id
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (isSelected) SapphireBrand else HighDensityInputBg)
                                    .border(1.dp, if (isSelected) SapphireBrand else HighDensityBorder, CircleShape)
                                    .clickable { selectedShopId = if (isSelected) null else s.id }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = s.name,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isSelected) Color.White else HighDensityTextSecondary
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onConfirm(name.trim(), selectedColor, selectedShopId)
                            }
                        },
                        enabled = name.isNotBlank(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SapphireBrand)
                    ) {
                        Text("Create List")
                    }
                }
            }
        }
    }
}
