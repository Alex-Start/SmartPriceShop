package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.util.AppStrings
import com.squareup.moshi.JsonClass

@Entity(tableName = "shopping_lists")
@JsonClass(generateAdapter = true)
data class ShoppingList(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val colorHex: String = "#1E40AF",
    val targetShopId: Long? = null,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "shopping_list_items",
    indices = [
        Index(value = ["listId"]),
        Index(value = ["goodId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = ShoppingList::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
@JsonClass(generateAdapter = true)
data class ShoppingListItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val listId: Long,
    val goodId: Long? = null,
    val name: String,
    val category: String = AppStrings.defaultCategories.first(),
    val quantity: Double = 1.0,
    val unit: String = "pcs", // "pcs", "pack", "g", "kg", "ml", "L"
    val weight: Double = 0.0,
    val weightUnit: String = "g",
    val preferredShopId: Long? = null,
    val isChecked: Boolean = false,
    val estimatedUnitPrice: Double = 0.0,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    val totalEstimatedPrice: Double
        get() = quantity * estimatedUnitPrice
}

data class ShoppingListWithItems(
    val list: ShoppingList,
    val items: List<ShoppingListItem>,
    val targetShop: Shop? = null,
    val totalItemCount: Int = items.size,
    val completedItemCount: Int = items.count { it.isChecked },
    val totalEstimatedCost: Double = items.sumOf { it.totalEstimatedPrice }
)
