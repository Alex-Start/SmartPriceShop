package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.util.AppStrings
import com.squareup.moshi.JsonClass

@Entity(tableName = "goods")
@JsonClass(generateAdapter = true)
data class Good(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: String = AppStrings.defaultCategories.first(),
    val barcode: String? = null,
    val unitType: String = "per 100g", // "per item", "per 100g", "per 1kg", "per 100ml", "per 1L", "per pack"
    val defaultQuantity: Double = 1.0,
    val weight: Double = 0.0, // Weight or package size (e.g. 500.0, 1.0)
    val weightUnit: String = "g", // "g", "kg", "ml", "L", "oz", "lb", "pcs"
    val imageUri: String? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
