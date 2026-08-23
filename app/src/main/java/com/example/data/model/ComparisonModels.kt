package com.example.data.model

import com.squareup.moshi.JsonClass

enum class PriceTrend {
    UP, DOWN, STABLE, NEW
}

@JsonClass(generateAdapter = true)
data class ShopPriceDetail(
    val shop: Shop,
    val priceRecord: PriceRecord,
    val isCheapest: Boolean = false,
    val priceTrend: PriceTrend = PriceTrend.NEW,
    val previousEffectivePrice: Double? = null,
    val priceDiffPercent: Double = 0.0
)

data class GoodWithPrices(
    val good: Good,
    val shopPrices: List<ShopPriceDetail>,
    val cheapestShopDetail: ShopPriceDetail? = null,
    val priceRangeText: String = "",
    val totalShopsRecorded: Int = 0
)

data class PriceHistoryWithShop(
    val history: PriceHistory,
    val shopName: String,
    val shopAddress: String,
    val shopColor: String
)

@JsonClass(generateAdapter = true)
data class BackupDataDto(
    val version: Int = 3,
    val exportedAt: Long = System.currentTimeMillis(),
    val goods: List<Good> = emptyList(),
    val shops: List<Shop> = emptyList(),
    val priceRecords: List<PriceRecord> = emptyList(),
    val priceHistory: List<PriceHistory> = emptyList(),
    val shoppingLists: List<ShoppingList> = emptyList(),
    val shoppingListItems: List<ShoppingListItem> = emptyList(),
    val categories: List<Category> = emptyList()
)

@JsonClass(generateAdapter = true)
data class AiScannedProductDto(
    val productName: String? = null,
    val category: String? = null,
    val shopName: String? = null,
    val shopAddress: String? = null,
    val regularPrice: Double? = null,
    val discountPrice: Double? = null,
    val packageAmount: Double? = null,
    val packageUnit: String? = null,
    val barcode: String? = null,
    val notes: String? = null
)
