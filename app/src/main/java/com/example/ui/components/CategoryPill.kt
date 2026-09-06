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

@Composable
fun CategoryPill(
    category: String,
    modifier: Modifier = Modifier
) {
    /*val (icon, bgColor, textColor, borderColor) = when (category.lowercase()) {
        "dairy" -> Quadruple(Icons.Outlined.Egg, Color(0xFFE8F5E9), Color(0xFF1B5E20), Color(0xFFC8E6C9))
        "fruits & veg", "fruits", "vegetables" -> Quadruple(Icons.Outlined.Eco, Color(0xFFF1F8E9), Color(0xFF33691E), Color(0xFFDCEDC8))
        "meat & fish", "meat", "fish" -> Quadruple(Icons.Outlined.SetMeal, Color(0xFFFFEBEE), Color(0xFFB71C1C), Color(0xFFFFCDD2))
        "bakery", "bread" -> Quadruple(Icons.Outlined.BakeryDining, Color(0xFFFFF3E0), Color(0xFFE65100), Color(0xFFFFE0B2))
        "beverages", "drinks" -> Quadruple(Icons.Outlined.LocalCafe, Color(0xFFE1F5FE), Color(0xFF01579B), Color(0xFFB3E5FC))
        "pantry" -> Quadruple(Icons.Outlined.Kitchen, Color(0xFFEDE7F6), Color(0xFF4A148C), Color(0xFFD1C4E9))
        "snacks" -> Quadruple(Icons.Outlined.Fastfood, Color(0xFFFFF8E1), Color(0xFFF57F17), Color(0xFFFFECB3))
        "household" -> Quadruple(Icons.Outlined.CleaningServices, Color(0xFFE0F2F1), Color(0xFF004D40), Color(0xFFB2DFDB))
        else -> Quadruple(Icons.Outlined.ShoppingBag, HighDensityPillBg, HighDensityTextSecondary, HighDensityBorder)
    }*/

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

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .border(0.75.dp, borderColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = String.format(java.util.Locale.US, "$%.2f %s", unitPrice, unitLabel.replace("$", "").trim()),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
            ),
            color = textColor
        )
    }
}

