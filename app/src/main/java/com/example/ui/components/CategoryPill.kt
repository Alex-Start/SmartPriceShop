package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryType
import com.example.ui.theme.*
import com.example.util.LocalAppCurrency
import com.example.util.UnitPriceCalculator

@Composable
fun CategoryPill(
    category: String,
    modifier: Modifier = Modifier
) {

    val categoryType = CategoryType.fromName(category)
    val icon = categoryType.icon
    val bgColor = categoryType.bgColor
    val textColor = categoryType.textColor
    val borderColor = categoryType.borderColor

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(0.75.dp, borderColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(11.dp)
        )
        Text(
            text = " $category",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
            ),
            color = textColor
        )
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun UnitPriceBadge(
    unitPrice: Double,
    unitLabel: String,
    modifier: Modifier = Modifier,
    isCheapest: Boolean = false
) {
    val bgColor = if (isCheapest) DealGreenBg else HighDensityInputBg
    val textColor = if (isCheapest) DealGreen else HighDensityTextSecondary
    val borderColor = if (isCheapest) DealGreenBorder else HighDensityBorder
    val currentCurrency = LocalAppCurrency.current

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .border(0.75.dp, borderColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            //text = String.format(java.util.Locale.US, "$%.2f %s", unitPrice, unitLabel.replace("$", "").trim()),
            text = UnitPriceCalculator.formatUnitPrice(unitPrice, unitLabel, currentCurrency),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
            ),
            color = textColor
        )
    }
}

