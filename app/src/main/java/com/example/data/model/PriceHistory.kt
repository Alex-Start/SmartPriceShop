package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(
    tableName = "price_history",
    indices = [
        Index(value = ["goodId"]),
        Index(value = ["shopId"]),
        Index(value = ["recordedAt"])
    ]
)
@JsonClass(generateAdapter = true)
data class PriceHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val goodId: Long,
    val shopId: Long,
    val regularPrice: Double,
    val discountPrice: Double? = null,
    val packageAmount: Double = 1.0,
    val packageUnit: String = "g",
    val pricePerUnit: Double = 0.0,
    val note: String? = null,
    val photoUri: String? = null,
    val recordedAt: Long = System.currentTimeMillis()
) {
    val effectivePrice: Double
        get() = if (discountPrice != null && discountPrice > 0.0 && discountPrice < regularPrice) discountPrice else regularPrice
}
