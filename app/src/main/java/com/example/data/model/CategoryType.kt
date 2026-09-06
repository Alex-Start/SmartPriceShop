package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BakeryDining
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Egg
import androidx.compose.material.icons.outlined.Fastfood
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.SetMeal
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.*

enum class CategoryType(
    val displayName: String,
    val colorHex: String,
    val iconName: String,
    val icon: ImageVector,
    val bgColor: Color,
    val textColor: Color,
    val borderColor: Color
) {
    DAIRY(
        displayName = "Dairy",
        colorHex = "#2563EB",
        iconName = "egg",
        icon = iconFromName("egg"),
        bgColor = Color(0xFFE8F5E9),
        textColor = Color(0xFF1B5E20),
        borderColor = Color(0xFFC8E6C9)
    ),

    FRUITS_VEG(
        displayName = "Fruits & Veg",
        colorHex = "#16A34A",
        iconName = "nutrition",
        icon = iconFromName("nutrition"),
        bgColor = Color(0xFFF1F8E9),
        textColor = Color(0xFF33691E),
        borderColor = Color(0xFFDCEDC8)
    ),

    MEAT_FISH(
        displayName = "Meat & Fish",
        colorHex = "#DC2626",
        iconName = "restaurant",
        icon = iconFromName("restaurant"),
        bgColor = Color(0xFFFFEBEE),
        textColor = Color(0xFFB71C1C),
        borderColor = Color(0xFFFFCDD2)
    ),

    BAKERY(
        displayName = "Bakery",
        colorHex = "#D97706",
        iconName = "bakery_dining",
        icon = iconFromName("bakery_dining"),
        bgColor = Color(0xFFFFF3E0),
        textColor = Color(0xFFE65100),
        borderColor = Color(0xFFFFE0B2)
    ),

    BEVERAGES(
        displayName = "Beverages",
        colorHex = "#0284C7",
        iconName = "local_cafe",
        icon = iconFromName("local_cafe"),
        bgColor = Color(0xFFE1F5FE),
        textColor = Color(0xFF01579B),
        borderColor = Color(0xFFB3E5FC)
    ),

    PANTRY(
        displayName = "Pantry",
        colorHex = "#7C3AED",
        iconName = "kitchen",
        icon = iconFromName("kitchen"),
        bgColor = Color(0xFFEDE7F6),
        textColor = Color(0xFF4A148C),
        borderColor = Color(0xFFD1C4E9)
    ),

    SNACKS(
        displayName = "Snacks",
        colorHex = "#EA580C",
        iconName = "cookie",
        icon = iconFromName("cookie"),
        bgColor = Color(0xFFFFF8E1),
        textColor = Color(0xFFF57F17),
        borderColor = Color(0xFFFFECB3)
    ),

    HOUSEHOLD(
        displayName = "Household",
        colorHex = "#0D9488",
        iconName = "cleaning_services",
        icon = iconFromName("cleaning_services"),
        bgColor = Color(0xFFE0F2F1),
        textColor = Color(0xFF004D40),
        borderColor = Color(0xFFB2DFDB)
    ),

    OTHER(
        displayName = "Other",
        colorHex = "#64748B",
        iconName = "category",
        icon = iconFromName("category"),
        bgColor = HighDensityPillBg,
        textColor = HighDensityTextSecondary,
        borderColor = HighDensityBorder
    );

    companion object {
        fun fromName(name: String): CategoryType =
            entries.firstOrNull {
                it.displayName.equals(name, ignoreCase = true)
            } ?: OTHER
    }
}

fun iconFromName(iconName: String): ImageVector =
    when (iconName) {
        "egg" -> Icons.Outlined.Egg
        "nutrition" -> Icons.Outlined.Eco
        "restaurant" -> Icons.Outlined.SetMeal
        "bakery_dining" -> Icons.Outlined.BakeryDining
        "local_cafe" -> Icons.Outlined.LocalCafe
        "kitchen" -> Icons.Outlined.Kitchen
        "cookie" -> Icons.Outlined.Fastfood
        "cleaning_services" -> Icons.Outlined.CleaningServices
        else -> Icons.Outlined.ShoppingBag
    }