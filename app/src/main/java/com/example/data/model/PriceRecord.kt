package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.util.AppCurrency
import com.squareup.moshi.JsonClass

@Entity(
    tableName = "price_records",
    indices = [
        Index(value = ["goodId", "shopId"], unique = true),
        Index(value = ["goodId"]),
        Index(value = ["shopId"])
    ]
)
@JsonClass(generateAdapter = true)
data class PriceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val goodId: Long,
    val shopId: Long,
    val regularPrice: Double,
    val discountPrice: Double? = null,
    val packageAmount: Double = 1.0, // e.g. 500
    val packageUnit: String = "g", // "g", "kg", "ml", "L", "item", "pcs", "pack"
    val pricePerUnit: Double = 0.0, // standardized calculated unit price (e.g. price per 100g or per 1kg or per item)
    val unitMeasureLabel: String = "$/100g",
    val isPromotion: Boolean = false,
    val promoEndDate: Long? = null,
    val photoUri: String? = null,
    val currencyCode: String = AppCurrency.CZK.code,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val effectivePrice: Double
        get() = if (discountPrice != null && discountPrice > 0.0 && discountPrice < regularPrice) discountPrice else regularPrice

    val discountPercentage: Int
        get() = if (discountPrice != null && discountPrice > 0.0 && regularPrice > discountPrice) {
            (((regularPrice - discountPrice) / regularPrice) * 100).toInt()
        } else 0
}
