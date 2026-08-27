package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.GoodWithPrices
import com.example.data.model.PriceTrend
import com.example.data.model.ShopPriceDetail
import com.example.ui.theme.*
import com.example.util.AppCurrency
import com.example.util.AppStrings
import com.example.util.LocalAppCurrency
import com.example.util.LocalAppLanguage
import com.example.util.UnitPriceCalculator
import java.io.File

@Composable
fun ProductPhotoGallery(
    productImageUri: String?,
    priceImageUri: String?,
    productLabel: String,
    priceLabel: String,
    modifier: Modifier = Modifier,
    thumbSize: Dp = 44.dp,
    spacing: Dp = 6.dp
) {
    var expandedImage by remember { mutableStateOf<String?>(null) }
    val visibleItems = listOfNotNull(
        productImageUri?.let { Pair(productLabel, it) },
        priceImageUri?.let { Pair(priceLabel, it) }
    ).take(2)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (visibleItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .size(thumbSize)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(thumbSize * 0.5f)
                )
            }
            return@Row
        }

        visibleItems.forEach { (label, uri) ->
            Box(
                modifier = Modifier
                    .size(thumbSize)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { expandedImage = uri }
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = File(uri),
                    contentDescription = label,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }

    expandedImage?.let { uri ->
        Dialog(onDismissRequest = { expandedImage = null }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth(0.96f)
                    .aspectRatio(1f)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = File(uri),
                        contentDescription = "Expanded image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                    IconButton(
                        onClick = { expandedImage = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

@Composable
fun PriceComparisonCard(
    item: GoodWithPrices,
    onClick: () -> Unit,
    onAddPriceClick: () -> Unit,
    onAddToListClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    val currentCurrency = LocalAppCurrency.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("good_card_${item.good.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Main Product Row (High-Density Layout)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Product and price photos (side-by-side)
                ProductPhotoGallery(
                    productImageUri = item.good.imageUri,
                    priceImageUri = item.cheapestShopDetail?.priceRecord?.photoUri,
                    productLabel = item.good.name,
                    priceLabel = "${item.good.name} price photo",
                    modifier = Modifier,
                    thumbSize = 28.dp,
                    spacing = 6.dp
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Product Name, Shop details, Unit price & Price
                Column(modifier = Modifier.weight(1f)) {
                    // Top line: Name + Effective Price
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = item.good.name,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        if (item.cheapestShopDetail != null) {
                            val cheapest = item.cheapestShopDetail
                            val isDiscount = cheapest.priceRecord.discountPrice != null && cheapest.priceRecord.discountPrice > 0
                            Text(
                                text = UnitPriceCalculator.formatCurrency(cheapest.priceRecord.effectivePrice, currentCurrency),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDiscount) DealGreen else MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Subtitle: Store name • Unit price / Price per gram & Weight
                    if (item.cheapestShopDetail != null) {
                        val cheapest = item.cheapestShopDetail
                        val weightStr = if (item.good.weight > 0) " (${UnitPriceCalculator.formatWeight(item.good.weight, item.good.weightUnit)})" else ""
                        val pricePerGramStr = if (item.good.weight > 0) {
                            " • ${UnitPriceCalculator.formatPricePerGram(cheapest.priceRecord.effectivePrice, item.good.weight, item.good.weightUnit, currency = currentCurrency)}"
                        } else if (cheapest.priceRecord.pricePerUnit > 0) {
                            " • ${UnitPriceCalculator.formatUnitPrice(cheapest.priceRecord.pricePerUnit, cheapest.priceRecord.unitMeasureLabel, currentCurrency)}"
                        } else ""

                        Text(
                            text = "${cheapest.shop.name}$weightStr$pricePerGramStr",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        val localizedCat = AppStrings.getCategoryName(item.good.category, lang)
                        val weightStr = if (item.good.weight > 0) " • ${UnitPriceCalculator.formatWeight(item.good.weight, item.good.weightUnit)}" else ""
                        Text(
                            text = "$localizedCat$weightStr",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Badges row: Best Deal / Price Up / Prev Price / Discount
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (item.cheapestShopDetail != null) {
                            val cheapest = item.cheapestShopDetail
                            val isDiscount = cheapest.priceRecord.discountPrice != null && cheapest.priceRecord.regularPrice > cheapest.priceRecord.discountPrice

                            if (isDiscount) {
                                val discountPercent = (((cheapest.priceRecord.regularPrice - cheapest.priceRecord.discountPrice!!) / cheapest.priceRecord.regularPrice) * 100).toInt()
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(DealGreenBg)
                                        .border(1.dp, DealGreenBorder, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (discountPercent > 0) "-$discountPercent% ${AppStrings.offBadge(lang)}" else AppStrings.bestDeal(lang),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DealGreen
                                        )
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(DealGreenBg)
                                        .border(1.dp, DealGreenBorder, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = AppStrings.bestDeal(lang),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DealGreen
                                        )
                                    )
                                }
                            }

                            if (cheapest.priceTrend == PriceTrend.UP) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(PriceUpAmberBg)
                                        .border(1.dp, PriceUpAmberBorder, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = AppStrings.priceUp(lang),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PriceUpAmber
                                        )
                                    )
                                }
                            }

                            if (isDiscount) {
                                Text(
                                    text = "${AppStrings.prevPrice(lang)} ${UnitPriceCalculator.formatCurrency(cheapest.priceRecord.regularPrice, currentCurrency)}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        color = HighDensityTextMuted,
                                        textDecoration = TextDecoration.LineThrough
                                    )
                                )
                            }
                        } else {
                            Text(
                                text = AppStrings.noPricesRecorded(lang),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    color = HighDensityTextMuted
                                )
                            )
                        }
                    }
                }

                // Action buttons (Add to list + Log Price)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onAddToListClick,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(HighDensityPillBg)
                            .testTag("add_to_list_btn_${item.good.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AddShoppingCart,
                            contentDescription = AppStrings.addToList(lang),
                            tint = SapphireBrand,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = onAddPriceClick,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(HighDensityPillBg)
                            .testTag("add_price_button_${item.good.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = AppStrings.logPrice(lang),
                            tint = HighDensityTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Shop Price Pills Row (Cross-supermarket density comparison)
            if (item.shopPrices.size > 1) {
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(item.shopPrices) { shopPrice ->
                        ShopPriceChip(shopPrice = shopPrice)
                    }
                }
            }
        }
    }
}

@Composable
fun ShopPriceChip(
    shopPrice: ShopPriceDetail,
    modifier: Modifier = Modifier
) {
    val currentCurrency = LocalAppCurrency.current
    val isCheapest = shopPrice.isCheapest
    val isDiscount = shopPrice.priceRecord.discountPrice != null && shopPrice.priceRecord.discountPrice > 0.0

    val chipBg = if (isCheapest) DealGreenBg else HighDensityPillBg
    val chipBorder = if (isCheapest) DealGreenBorder else HighDensityBorder.copy(alpha = 0.5f)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(chipBg)
            .border(1.dp, chipBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Shop color indicator dot
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(parseColor(shopPrice.shop.colorHex))
        )
        Spacer(modifier = Modifier.width(5.dp))

        // Shop Name
        Text(
            text = shopPrice.shop.name,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = HighDensityTextSecondary
            ),
            maxLines = 1
        )
        Spacer(modifier = Modifier.width(5.dp))

        // Price
        Text(
            text = UnitPriceCalculator.formatCurrency(shopPrice.priceRecord.effectivePrice, currentCurrency),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCheapest) DealGreen else if (isDiscount) AccentRed else HighDensityTextPrimary
            )
        )

        // Trend Icon
        when (shopPrice.priceTrend) {
            PriceTrend.DOWN -> {
                Spacer(modifier = Modifier.width(3.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                    contentDescription = "Price dropped",
                    tint = DealGreen,
                    modifier = Modifier.size(12.dp)
                )
            }
            PriceTrend.UP -> {
                Spacer(modifier = Modifier.width(3.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = "Price increased",
                    tint = PriceUpAmber,
                    modifier = Modifier.size(12.dp)
                )
            }
            else -> {}
        }
    }
}
