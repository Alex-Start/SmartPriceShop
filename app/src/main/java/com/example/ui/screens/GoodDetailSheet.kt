package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GoodWithPrices
import com.example.data.model.PriceHistoryWithShop
import com.example.data.model.PriceTrend
import com.example.data.model.ShopPriceDetail
import com.example.ui.components.CategoryPill
import com.example.ui.components.PriceHistoryChart
import com.example.ui.components.ProductPhotoGallery
import com.example.ui.components.parseColor
import com.example.ui.theme.*
import com.example.util.AppStrings
import com.example.util.LocalAppCurrency
import com.example.util.LocalAppLanguage
import com.example.util.UnitPriceCalculator
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoodDetailSheet(
    item: GoodWithPrices,
    historyList: List<PriceHistoryWithShop>,
    onDismiss: () -> Unit,
    onAddOrUpdatePriceClick: () -> Unit,
    onEditPriceRecord: (ShopPriceDetail) -> Unit = {},
    onAddToListClick: () -> Unit,
    onDeletePriceRecord: (shopId: Long) -> Unit,
    onDeleteGood: () -> Unit,
    onClearHistory: () -> Unit
) {
    val lang = LocalAppLanguage.current
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showClearHistoryConfirmDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = HighDensityCanvas,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.testTag("good_detail_sheet")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Header with Photo, Name, Category, Weight, and Barcode
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProductPhotoGallery(
                        productImageUri = item.good.imageUri,
                        priceImageUri = item.shopPrices.firstOrNull()?.priceRecord?.photoUri,
                        productLabel = item.good.name,
                        priceLabel = "${item.good.name} price photo",
                        modifier = Modifier,
                        thumbSize = 56.dp,
                        spacing = 8.dp
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.good.name,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = HighDensityTextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CategoryPill(category = item.good.category)

                            if (item.good.weight > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(SapphireContainer)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = UnitPriceCalculator.formatWeight(item.good.weight, item.good.weightUnit),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SapphireOnContainer
                                        )
                                    )
                                }
                            }

                            if (item.good.barcode != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "EAN: ${item.good.barcode}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        color = HighDensityTextSecondary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 2. Action Buttons (Add/Update Price, Add to Shopping List, Delete)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onAddOrUpdatePriceClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("detail_add_price_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = SapphireBrand),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(AppStrings.logPrice(lang), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = onAddToListClick,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("detail_add_to_list_btn")
                    ) {
                        Icon(imageVector = Icons.Outlined.AddShoppingCart, contentDescription = null, tint = SapphireBrand, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(AppStrings.addToList(lang), fontSize = 12.sp, color = SapphireBrand)
                    }

                    OutlinedButton(
                        onClick = { showDeleteConfirmDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PriceUpAmber),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("detail_delete_good_button")
                    ) {
                        Icon(imageVector = Icons.Outlined.Delete, contentDescription = "Delete Product", modifier = Modifier.size(18.dp))
                    }
                }
            }

            // 3. Supermarket Comparison Matrix Section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = AppStrings.supermarketPrices(lang),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 0.8.sp,
                            color = HighDensityTextSecondary
                        )
                    )
                    Text(
                        text = AppStrings.storesCount(item.shopPrices.size, lang),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = SapphireBrand
                        )
                    )
                }
            }

            if (item.shopPrices.isEmpty()) {
                item {
                    Text(
                        text = AppStrings.noPricesYet(lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = HighDensityTextSecondary
                    )
                }
            } else {
                items(item.shopPrices) { shopPrice ->
                    DetailShopPriceCard(
                        shopPrice = shopPrice,
                        goodWeight = item.good.weight,
                        goodWeightUnit = item.good.weightUnit,
                        onEdit = { onEditPriceRecord(shopPrice) },
                        onDelete = { onDeletePriceRecord(shopPrice.shop.id) }
                    )
                }
            }

            // 4. Price History Graph & Timeline Section
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = AppStrings.priceHistoryStats(lang),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 0.8.sp,
                            color = HighDensityTextSecondary
                        )
                    )

                    if (historyList.isNotEmpty()) {
                        TextButton(
                            onClick = { showClearHistoryConfirmDialog = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = PriceUpAmber),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(imageVector = Icons.Outlined.CleaningServices, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(AppStrings.clearHistory(lang), style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp))
                        }
                    }
                }
            }

            item {
                PriceHistoryChart(historyList = historyList)
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(AppStrings.deleteProductConfirm(lang), color = HighDensityTextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text(AppStrings.deleteProductMsg(item.good.name, lang), color = HighDensityTextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteGood()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PriceUpAmber)
                ) {
                    Text(AppStrings.delete(lang), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(AppStrings.cancel(lang), color = HighDensityTextSecondary)
                }
            }
        )
    }

    // Clear History Confirmation Dialog
    if (showClearHistoryConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryConfirmDialog = false },
            title = { Text(AppStrings.clearHistoryConfirm(lang), color = HighDensityTextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text(AppStrings.clearHistoryMsg(item.good.name, lang), color = HighDensityTextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        showClearHistoryConfirmDialog = false
                        onClearHistory()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PriceUpAmber)
                ) {
                    Text(AppStrings.clear(lang), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryConfirmDialog = false }) {
                    Text(AppStrings.cancel(lang), color = HighDensityTextSecondary)
                }
            }
        )
    }
}

@Composable
fun DetailShopPriceCard(
    shopPrice: ShopPriceDetail,
    goodWeight: Double = 0.0,
    goodWeightUnit: String = "g",
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val currentCurrency = LocalAppCurrency.current
    val isCheapest = shopPrice.isCheapest
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    val pricePerGramStr = if (goodWeight > 0) {
        val converted = com.example.util.CurrencyRates.convert(shopPrice.priceRecord.effectivePrice, shopPrice.priceRecord.currencyCode, currentCurrency.code)
        UnitPriceCalculator.formatPricePerGram(converted, goodWeight, goodWeightUnit, includeDetailedGram = true, currency = currentCurrency)
    } else null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isCheapest) DealGreenBorder else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            )
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shop info
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(parseColor(shopPrice.shop.colorHex))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = shopPrice.shop.name,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            if (isCheapest) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(DealGreenBg)
                                        .border(1.dp, DealGreenBorder, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = AppStrings.cheapestBadge(lang),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = DealGreen
                                    )
                                }
                            }
                        }
                        if (shopPrice.shop.address.isNotBlank()) {
                            Text(
                                text = shopPrice.shop.address,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }

                // Prices
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = UnitPriceCalculator.formatCurrencyWithConversion(shopPrice.priceRecord.effectivePrice, shopPrice.priceRecord.currencyCode, currentCurrency),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isCheapest) DealGreen else MaterialTheme.colorScheme.onSurface
                        )
                    )
                    if (shopPrice.priceRecord.discountPrice != null && shopPrice.priceRecord.regularPrice > shopPrice.priceRecord.discountPrice) {
                        Text(
                            text = UnitPriceCalculator.formatCurrencyWithConversion(shopPrice.priceRecord.regularPrice, shopPrice.priceRecord.currencyCode, currentCurrency),
                            style = MaterialTheme.typography.labelSmall.copy(
                                textDecoration = TextDecoration.LineThrough,
                                fontSize = 11.sp
                            ),
                            color = HighDensityTextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = HighDensityBorder.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            // Unit Price, Price per Gram & Trend, Edit and delete button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    if (pricePerGramStr != null) {
                        Text(
                            text = "${AppStrings.unitPrefix(lang)} $pricePerGramStr",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = SapphireBrand,
                                fontSize = 11.sp
                            )
                        )
                    } else if (shopPrice.priceRecord.pricePerUnit > 0) {
                        Text(
                            text = "${AppStrings.unitPrefix(lang)} ${UnitPriceCalculator.formatUnitPrice(shopPrice.priceRecord.pricePerUnit, shopPrice.priceRecord.unitMeasureLabel, currentCurrency)}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = SapphireBrand,
                                fontSize = 11.sp
                            )
                        )
                    }

                    when (shopPrice.priceTrend) {
                        PriceTrend.DOWN -> {
                            Text(
                                text = String.format(Locale.US, "📉 -%.0f%% ${AppStrings.vsPrevious(lang)}", shopPrice.priceDiffPercent),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = DealGreen
                            )
                        }
                        PriceTrend.UP -> {
                            Text(
                                text = String.format(Locale.US, "📈 +%.0f%% ${AppStrings.vsPrevious(lang)}", shopPrice.priceDiffPercent),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = PriceUpAmber
                            )
                        }
                        else -> {}
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${AppStrings.updatedDate(lang)} ${dateFormat.format(Date(shopPrice.priceRecord.updatedAt))}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            color = HighDensityTextMuted
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                            .testTag("edit_price_btn_${shopPrice.shop.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = AppStrings.editPriceItem(lang),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(HighDensityPillBg)
                            .testTag("delete_price_btn_${shopPrice.shop.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Remove shop price",
                            tint = HighDensityTextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
