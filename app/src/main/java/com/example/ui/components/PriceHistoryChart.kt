package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PriceHistoryWithShop
import com.example.ui.theme.*
import com.example.util.AppCurrency
import com.example.util.LocalAppCurrency
import com.example.util.UnitPriceCalculator
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PriceHistoryChart(
    historyList: List<PriceHistoryWithShop>,
    modifier: Modifier = Modifier
) {
    val currentCurrency = LocalAppCurrency.current

    if (historyList.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(HighDensityInputBg),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.TrendingUp,
                    contentDescription = null,
                    tint = HighDensityTextMuted,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "No historical price logs recorded yet",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = HighDensityTextSecondary
                )
            }
        }
        return
    }

    // Sort chronologically for charting
    val sorted = historyList.sortedBy { it.history.recordedAt }
    val textMeasurer = rememberTextMeasurer()
    val minPrice = sorted.minOf { it.history.effectivePrice }
    val maxPrice = sorted.maxOf { it.history.effectivePrice }
    // Convert min/max to currently selected currency for labeling
    val minPriceConv = sorted.minOf { com.example.util.CurrencyRates.convert(it.history.effectivePrice, it.history.currencyCode, currentCurrency.code) }
    val maxPriceConv = sorted.maxOf { com.example.util.CurrencyRates.convert(it.history.effectivePrice, it.history.currencyCode, currentCurrency.code) }
    val priceSpan = (maxPrice - minPrice).coerceAtLeast(0.5)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("price_history_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Timeline,
                        contentDescription = null,
                        tint = SapphireBrand,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Price Trend Over Time",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = HighDensityTextPrimary
                        )
                    )
                }

                val latest = sorted.lastOrNull()?.history?.effectivePrice ?: 0.0
                val earliest = sorted.firstOrNull()?.history?.effectivePrice ?: 0.0
                if (sorted.size > 1) {
                    val latestRaw = sorted.lastOrNull()?.history?.effectivePrice ?: 0.0
                    val earliestRaw = sorted.firstOrNull()?.history?.effectivePrice ?: 0.0
                    val latestConv = sorted.lastOrNull()?.let { com.example.util.CurrencyRates.convert(it.history.effectivePrice, it.history.currencyCode, currentCurrency.code) } ?: latestRaw
                    val earliestConv = sorted.firstOrNull()?.let { com.example.util.CurrencyRates.convert(it.history.effectivePrice, it.history.currencyCode, currentCurrency.code) } ?: earliestRaw
                    val isCheaper = latestConv < earliestConv
                    val diff = Math.abs(latestConv - earliestConv)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isCheaper) DealGreenBg else PriceUpAmberBg)
                            .border(1.dp, if (isCheaper) DealGreenBorder else PriceUpAmberBorder, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isCheaper) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                            contentDescription = null,
                            tint = if (isCheaper) DealGreen else PriceUpAmber,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = " ${currentCurrency.format(diff)}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = if (isCheaper) DealGreen else PriceUpAmber
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Canvas Chart
            val lineColor = SapphireBrand
            val gridColor = HighDensityBorder.copy(alpha = 0.5f)
            val labelStyle = TextStyle(color = HighDensityTextMuted, fontSize = 10.sp)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                val w = size.width
                val h = size.height
                val padBottom = 18f
                val padTop = 12f
                val padLeft = 45f
                val chartH = h - padBottom - padTop
                val chartW = w - padLeft - 10f

                // Draw Y-axis guide lines (min and max)
                drawLine(
                    color = gridColor,
                    start = Offset(padLeft, padTop),
                    end = Offset(w, padTop),
                    strokeWidth = 1f
                )
                drawText(
                    textMeasurer = textMeasurer,
                text = currentCurrency.format(maxPriceConv),
                    topLeft = Offset(0f, padTop - 10f),
                    style = labelStyle
                )

                drawLine(
                    color = gridColor,
                    start = Offset(padLeft, padTop + chartH),
                    end = Offset(w, padTop + chartH),
                    strokeWidth = 1f
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = currentCurrency.format(minPriceConv),
                    topLeft = Offset(0f, padTop + chartH - 10f),
                    style = labelStyle
                )

                if (sorted.size == 1) {
                    // Single point
                    val x = padLeft + chartW / 2f
                    val y = padTop + chartH / 2f
                    drawCircle(color = lineColor, radius = 5f, center = Offset(x, y))
                    drawCircle(color = Color.White, radius = 2.5f, center = Offset(x, y))
                } else {
                    val points = sorted.mapIndexed { index, item ->
                        val x = padLeft + (index.toFloat() / (sorted.size - 1)) * chartW
                        val normalized = (item.history.effectivePrice - minPrice) / priceSpan
                        val y = padTop + (1.0 - normalized).toFloat() * chartH
                        Offset(x, y)
                    }

                    // Draw path
                    val path = Path()
                    points.forEachIndexed { i, pt ->
                        if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
                    }
                    drawPath(
                        path = path,
                        color = lineColor,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw dots and date labels
                    val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
                    points.forEachIndexed { i, pt ->
                        drawCircle(color = lineColor, radius = 4f, center = pt)
                        drawCircle(color = Color.White, radius = 2f, center = pt)

                        // Date label on last and first point
                        if (i == 0 || i == points.size - 1) {
                            val dateStr = dateFormat.format(Date(sorted[i].history.recordedAt))
                            val dateOffset = if (i == 0) pt.x else pt.x - 25f
                            drawText(
                                textMeasurer = textMeasurer,
                                text = dateStr,
                                topLeft = Offset(dateOffset.coerceAtLeast(padLeft), h - 14f),
                                style = labelStyle
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = HighDensityBorder.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            // Recent Log Records
            Text(
                text = "LOG RECORDS (${historyList.size})",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 0.5.sp,
                    color = HighDensityTextSecondary
                )
            )
            Spacer(modifier = Modifier.height(4.dp))

            val dateFormatFull = SimpleDateFormat("MMM dd · HH:mm", Locale.getDefault())
            historyList.take(4).forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(parseColor(item.shopColor))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = item.shopName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = HighDensityTextPrimary,
                                fontSize = 11.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = dateFormatFull.format(Date(item.history.recordedAt)),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                color = HighDensityTextMuted
                            )
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = UnitPriceCalculator.formatCurrencyWithConversion(item.history.effectivePrice, item.history.currencyCode, currentCurrency),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = if (item.history.discountPrice != null) DealGreen else HighDensityTextPrimary
                            )
                        )
                    }
                }
            }
        }
    }
}

fun parseColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        SapphireBrand
    }
}
