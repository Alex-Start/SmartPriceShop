package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.GeminiPriceScannerService
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.PriceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.util.AppCurrency
import com.example.util.AppLanguage
import com.example.util.UnitPriceCalculator
import java.io.File
import java.io.FileOutputStream

enum class SortOption {
    PRICE_PER_ITEM,
    PRICE_PER_GRAM,
    BIGGEST_DISCOUNT,
    NAME_AZ,
    RECENTLY_UPDATED,
    MOST_SHOPS;

    fun getLabel(lang: AppLanguage = AppLanguage.ENGLISH): String = when (this) {
        PRICE_PER_ITEM -> when (lang) {
            AppLanguage.ENGLISH -> "Price: Lowest Per Item"
            AppLanguage.UKRAINIAN -> "Ціна: Від найдешевшого (за шт)"
            AppLanguage.CZECH -> "Cena: Od nejlevnějšího (za kus)"
        }
        PRICE_PER_GRAM -> when (lang) {
            AppLanguage.ENGLISH -> "Price: Lowest Per Gram / 100g"
            AppLanguage.UKRAINIAN -> "Ціна: За грам / 100г (цукор, морква...)"
            AppLanguage.CZECH -> "Cena: Za gram / 100g (cukr, mrkev...)"
        }
        BIGGEST_DISCOUNT -> when (lang) {
            AppLanguage.ENGLISH -> "Biggest Discount %"
            AppLanguage.UKRAINIAN -> "Найбільша знижка %"
            AppLanguage.CZECH -> "Největší sleva %"
        }
        NAME_AZ -> when (lang) {
            AppLanguage.ENGLISH -> "Name (A-Z)"
            AppLanguage.UKRAINIAN -> "Назва (А-Я)"
            AppLanguage.CZECH -> "Název (A-Z)"
        }
        RECENTLY_UPDATED -> when (lang) {
            AppLanguage.ENGLISH -> "Recently Updated"
            AppLanguage.UKRAINIAN -> "Нещодавно оновлені"
            AppLanguage.CZECH -> "Nedávno aktualizované"
        }
        MOST_SHOPS -> when (lang) {
            AppLanguage.ENGLISH -> "Most Compared Stores"
            AppLanguage.UKRAINIAN -> "Найбільше магазинів"
            AppLanguage.CZECH -> "Nejvíce obchodů"
        }
    }

    val label: String
        get() = getLabel(AppLanguage.ENGLISH)
}

data class UiSnackbarMessage(
    val message: String,
    val isError: Boolean = false
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PriceTrackerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PriceRepository
    private val aiScannerService = GeminiPriceScannerService()
    private val prefs = application.getSharedPreferences("price_tracker_prefs", android.content.Context.MODE_PRIVATE)

    // Language state
    val currentLanguage = MutableStateFlow(
        AppLanguage.fromCode(prefs.getString("selected_language", AppLanguage.ENGLISH.code) ?: "en")
    )

    fun setLanguage(lang: AppLanguage) {
        currentLanguage.value = lang
        prefs.edit().putString("selected_language", lang.code).apply()
    }

    // Currency state
    val currentCurrency = MutableStateFlow(
        AppCurrency.fromCode(prefs.getString("selected_currency", AppCurrency.USD.code) ?: "USD")
    )

    fun setCurrency(currency: AppCurrency) {
        currentCurrency.value = currency
        prefs.edit().putString("selected_currency", currency.code).apply()
    }

    init {
        val db = AppDatabase.getDatabase(application)
        repository = PriceRepository(db)
        viewModelScope.launch {
            repository.seedSampleDataIfEmpty()
        }
    }

    // Filter and Search states
    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow("All")
    val selectedShopId = MutableStateFlow<Long?>(null)
    val sortOption = MutableStateFlow(SortOption.PRICE_PER_ITEM)
    val onlyPromotions = MutableStateFlow(false)

    // Repository flows
    val allShops: StateFlow<List<Shop>> = repository.allShops
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allGoods: StateFlow<List<Good>> = repository.allGoods
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<Category>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    data class FilterParams(
        val query: String,
        val category: String,
        val shopId: Long?,
        val sort: SortOption,
        val promosOnly: Boolean
    )

    private val filterParamsFlow: Flow<FilterParams> = combine(
        searchQuery,
        selectedCategory,
        selectedShopId,
        sortOption,
        onlyPromotions
    ) { query, category, shopId, sort, promosOnly ->
        FilterParams(query, category, shopId, sort, promosOnly)
    }

    // Filtered list of goods with comparison details
    val filteredGoodsWithPrices: StateFlow<List<GoodWithPrices>> = combine(
        repository.goodsWithPrices,
        filterParamsFlow
    ) { allItems, params ->
        var list = allItems

        // Search query filter
        if (params.query.isNotBlank()) {
            val q = params.query.trim().lowercase()
            list = list.filter { item ->
                item.good.name.lowercase().contains(q) ||
                item.good.category.lowercase().contains(q) ||
                (item.good.barcode?.contains(q) == true) ||
                item.shopPrices.any { it.shop.name.lowercase().contains(q) }
            }
        }

        // Category filter
        if (params.category != "All") {
            list = list.filter { it.good.category.equals(params.category, ignoreCase = true) }
        }

        // Specific Shop filter
        if (params.shopId != null) {
            list = list.filter { item -> item.shopPrices.any { it.shop.id == params.shopId } }
        }

        // Promotions Only filter
        if (params.promosOnly) {
            list = list.filter { item ->
                item.shopPrices.any { it.priceRecord.isPromotion || ((it.priceRecord.discountPrice ?: 0.0) > 0.0) }
            }
        }

        // Sorting
        when (params.sort) {
            SortOption.PRICE_PER_ITEM -> list.sortedWith(
                compareBy<GoodWithPrices> { item ->
                    item.cheapestShopDetail?.priceRecord?.effectivePrice ?: Double.MAX_VALUE
                }.thenBy { it.good.name.lowercase() }
            )
            SortOption.PRICE_PER_GRAM -> list.sortedWith(
                compareBy<GoodWithPrices> { item ->
                    val cheapestPrice = item.cheapestShopDetail?.priceRecord?.effectivePrice
                    if (cheapestPrice != null && item.good.weight > 0) {
                        val pricePerGram = UnitPriceCalculator.calculatePricePerGram(
                            cheapestPrice,
                            item.good.weight,
                            item.good.weightUnit
                        )
                        pricePerGram ?: (1_000_000.0 + cheapestPrice)
                    } else if (cheapestPrice != null) {
                        1_000_000.0 + cheapestPrice
                    } else {
                        Double.MAX_VALUE
                    }
                }.thenBy { it.good.name.lowercase() }
            )
            SortOption.BIGGEST_DISCOUNT -> list.sortedByDescending { item ->
                item.shopPrices.maxOfOrNull { it.priceRecord.discountPercentage } ?: 0
            }
            SortOption.NAME_AZ -> list.sortedBy { it.good.name.lowercase() }
            SortOption.RECENTLY_UPDATED -> list.sortedByDescending { item ->
                item.shopPrices.maxOfOrNull { it.priceRecord.updatedAt } ?: item.good.createdAt
            }
            SortOption.MOST_SHOPS -> list.sortedByDescending { it.shopPrices.size }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected Good for Details & History
    val selectedGoodId = MutableStateFlow<Long?>(null)
    
    val selectedGoodWithPrices: StateFlow<GoodWithPrices?> = combine(
        repository.goodsWithPrices,
        selectedGoodId
    ) { items, goodId ->
        if (goodId == null) null else items.firstOrNull { it.good.id == goodId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedGoodHistory: StateFlow<List<PriceHistoryWithShop>> = selectedGoodId
        .flatMapLatest { goodId ->
            if (goodId == null) flowOf(emptyList())
            else repository.getPriceHistoryForGood(goodId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ==================== SHOPPING LISTS STATE ====================

    val shoppingListsWithItems: StateFlow<List<ShoppingListWithItems>> = repository.shoppingListsWithItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedShoppingListId = MutableStateFlow<Long?>(null)

    val selectedShoppingList: StateFlow<ShoppingListWithItems?> = combine(
        repository.shoppingListsWithItems,
        selectedShoppingListId
    ) { lists, listId ->
        if (listId == null) lists.firstOrNull()
        else lists.firstOrNull { it.list.id == listId } ?: lists.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // UI Feedback Message
    val userMessage = MutableStateFlow<UiSnackbarMessage?>(null)

    // AI Scanner State
    val isAiScanning = MutableStateFlow(false)
    val aiScannedResult = MutableStateFlow<AiScannedProductDto?>(null)
    val aiScanErrorMessage = MutableStateFlow<String?>(null)

    fun clearUserMessage() {
        userMessage.value = null
    }

    fun selectGoodForDetail(goodId: Long?) {
        selectedGoodId.value = goodId
    }

    fun selectShoppingList(listId: Long?) {
        selectedShoppingListId.value = listId
    }

    // ==================== ACTIONS ====================

    fun saveGoodAndPrice(
        goodId: Long?,
        goodName: String,
        category: String,
        unitType: String,
        barcode: String?,
        weight: Double,
        weightUnit: String,
        goodImageUri: String?,
        shopName: String,
        shopAddress: String,
        regularPrice: Double,
        discountPrice: Double?,
        packageAmount: Double,
        packageUnit: String,
        photoUri: String? = null,
        isPromotion: Boolean = false,
        note: String? = null
    ) {
        viewModelScope.launch {
            try {
                // 1. Save or Update Good
                val targetGood = if (goodId != null && goodId > 0) {
                    val existing = repository.getGoodById(goodId)
                    existing?.copy(
                        name = goodName.trim(),
                        category = category,
                        unitType = unitType,
                        barcode = barcode?.trim()?.ifBlank { null } ?: existing.barcode,
                        weight = if (weight > 0.0) weight else existing.weight,
                        weightUnit = weightUnit.ifBlank { existing.weightUnit },
                        imageUri = goodImageUri ?: existing.imageUri
                    ) ?: Good(
                        name = goodName.trim(),
                        category = category,
                        unitType = unitType,
                        barcode = barcode?.trim()?.ifBlank { null },
                        weight = weight,
                        weightUnit = weightUnit,
                        imageUri = goodImageUri
                    )
                } else {
                    Good(
                        name = goodName.trim(),
                        category = category,
                        unitType = unitType,
                        barcode = barcode?.trim()?.ifBlank { null },
                        weight = weight,
                        weightUnit = weightUnit,
                        imageUri = goodImageUri
                    )
                }
                val savedGoodId = repository.saveGood(targetGood)
                val effectiveGoodId = if (goodId != null && goodId > 0) goodId else savedGoodId

                // 2. Get or Create Shop
                val shop = repository.getOrCreateShop(shopName, shopAddress)

                // 3. Save Price Record & History
                repository.savePriceRecord(
                    goodId = effectiveGoodId,
                    shopId = shop.id,
                    regularPrice = regularPrice,
                    discountPrice = discountPrice,
                    packageAmount = packageAmount,
                    packageUnit = packageUnit,
                    photoUri = photoUri,
                    isPromotion = isPromotion,
                    note = note
                )

                userMessage.value = UiSnackbarMessage("Price for '$goodName' at ${shop.name} saved successfully!")
            } catch (e: Exception) {
                userMessage.value = UiSnackbarMessage("Error saving price: ${e.localizedMessage}", isError = true)
            }
        }
    }

    fun findGoodByBarcode(barcode: String, onFound: (Good?) -> Unit) {
        viewModelScope.launch {
            val good = repository.findGoodByBarcode(barcode)
            onFound(good)
        }
    }

    fun deleteGood(goodId: Long) {
        viewModelScope.launch {
            try {
                repository.deleteGood(goodId)
                if (selectedGoodId.value == goodId) {
                    selectedGoodId.value = null
                }
                userMessage.value = UiSnackbarMessage("Product and prices removed")
            } catch (e: Exception) {
                userMessage.value = UiSnackbarMessage("Failed to delete product: ${e.localizedMessage}", isError = true)
            }
        }
    }

    fun deletePriceRecord(goodId: Long, shopId: Long) {
        viewModelScope.launch {
            try {
                repository.deletePriceRecord(goodId, shopId)
                userMessage.value = UiSnackbarMessage("Shop price record removed")
            } catch (e: Exception) {
                userMessage.value = UiSnackbarMessage("Failed to delete price record", isError = true)
            }
        }
    }

    fun deleteShop(shopId: Long) {
        viewModelScope.launch {
            try {
                repository.deleteShop(shopId)
                userMessage.value = UiSnackbarMessage("Supermarket removed")
            } catch (e: Exception) {
                userMessage.value = UiSnackbarMessage("Failed to delete shop", isError = true)
            }
        }
    }

    // ==================== CATEGORY ACTIONS ====================

    fun addCategory(name: String, colorHex: String = "#1E40AF") {
        viewModelScope.launch {
            try {
                if (name.isBlank()) return@launch
                repository.saveCategory(name.trim(), colorHex)
                userMessage.value = UiSnackbarMessage("Category '$name' added!")
            } catch (e: Exception) {
                userMessage.value = UiSnackbarMessage("Failed to add category: ${e.localizedMessage}", isError = true)
            }
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            try {
                repository.updateCategory(category)
                userMessage.value = UiSnackbarMessage("Category updated")
            } catch (e: Exception) {
                userMessage.value = UiSnackbarMessage("Failed to update category", isError = true)
            }
        }
    }

    fun deleteCategory(categoryId: Long) {
        viewModelScope.launch {
            try {
                repository.deleteCategory(categoryId)
                userMessage.value = UiSnackbarMessage("Category deleted")
            } catch (e: Exception) {
                userMessage.value = UiSnackbarMessage("Failed to delete category", isError = true)
            }
        }
    }

    fun deleteHistoryEntry(historyId: Long) {
        viewModelScope.launch {
            try {
                repository.deleteHistoryEntry(historyId)
                userMessage.value = UiSnackbarMessage("History entry removed")
            } catch (e: Exception) {
                userMessage.value = UiSnackbarMessage("Failed to delete history entry", isError = true)
            }
        }
    }

    fun clearAllPriceHistory() {
        viewModelScope.launch {
            try {
                repository.clearPriceHistory()
                userMessage.value = UiSnackbarMessage("All price history logs cleared")
            } catch (e: Exception) {
                userMessage.value = UiSnackbarMessage("Failed to clear price history", isError = true)
            }
        }
    }

    fun clearHistoryForGood(goodId: Long) {
        viewModelScope.launch {
            try {
                repository.clearPriceHistoryForGood(goodId)
                userMessage.value = UiSnackbarMessage("History for this item cleared")
            } catch (e: Exception) {
                userMessage.value = UiSnackbarMessage("Failed to clear item history", isError = true)
            }
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            try {
                repository.resetAllData()
                selectedGoodId.value = null
                selectedShoppingListId.value = null
                userMessage.value = UiSnackbarMessage("All app data reset successfully")
            } catch (e: Exception) {
                userMessage.value = UiSnackbarMessage("Failed to reset data: ${e.localizedMessage}", isError = true)
            }
        }
    }

    fun reloadSampleData() {
        viewModelScope.launch {
            try {
                repository.seedSampleDataIfEmpty()
                userMessage.value = UiSnackbarMessage("Sample supermarket dataset loaded")
            } catch (e: Exception) {
                userMessage.value = UiSnackbarMessage("Failed to load sample data", isError = true)
            }
        }
    }

    // ==================== SHOPPING LIST ACTIONS ====================

    fun createShoppingList(name: String, colorHex: String = "#1E40AF", targetShopId: Long? = null) {
        viewModelScope.launch {
            try {
                val newId = repository.createShoppingList(name, colorHex, targetShopId)
                selectedShoppingListId.value = newId
                userMessage.value = UiSnackbarMessage("Created shopping list '$name'")
            } catch (e: Exception) {
                userMessage.value = UiSnackbarMessage("Failed to create shopping list: ${e.localizedMessage}", isError = true)
            }
        }
    }

    fun updateShoppingList(list: ShoppingList) {
        viewModelScope.launch {
            try {
                repository.updateShoppingList(list)
                userMessage.value = UiSnackbarMessage("Shopping list updated")
            } catch (e: Exception) {
                userMessage.value = UiSnackbarMessage("Failed to update list", isError = true)
            }
        }
    }

    fun deleteShoppingList(listId: Long) {
        viewModelScope.launch {
            try {
                repository.deleteShoppingList(listId)
                if (selectedShoppingListId.value == listId) {
                    selectedShoppingListId.value = null
                }
                userMessage.value = UiSnackbarMessage("Shopping list deleted")
            } catch (e: Exception) {
                userMessage.value = UiSnackbarMessage("Failed to delete list", isError = true)
            }
        }
    }

    fun addGoodToShoppingList(listId: Long, goodId: Long, quantity: Double = 1.0, preferredShopId: Long? = null) {
        viewModelScope.launch {
            try {
                repository.addGoodToShoppingList(listId, goodId, quantity, preferredShopId)
                userMessage.value = UiSnackbarMessage("Added item to shopping list!")
            } catch (e: Exception) {
                userMessage.value = UiSnackbarMessage("Failed to add item: ${e.localizedMessage}", isError = true)
            }
        }
    }

    fun addCustomItemToShoppingList(
        listId: Long,
        name: String,
        category: String,
        quantity: Double,
        unit: String,
        weight: Double = 0.0,
        weightUnit: String = "g",
        estimatedPrice: Double = 0.0,
        notes: String = "",
        preferredShopId: Long? = null
    ) {
        viewModelScope.launch {
            try {
                repository.addCustomItemToShoppingList(
                    listId = listId,
                    name = name,
                    category = category,
                    quantity = quantity,
                    unit = unit,
                    weight = weight,
                    weightUnit = weightUnit,
                    estimatedPrice = estimatedPrice,
                    notes = notes,
                    preferredShopId = preferredShopId
                )
                userMessage.value = UiSnackbarMessage("Added '$name' to list!")
            } catch (e: Exception) {
                userMessage.value = UiSnackbarMessage("Failed to add item", isError = true)
            }
        }
    }

    fun toggleShoppingItemChecked(item: ShoppingListItem) {
        viewModelScope.launch {
            try {
                repository.toggleItemChecked(item)
            } catch (e: Exception) {
                userMessage.value = UiSnackbarMessage("Failed to update item", isError = true)
            }
        }
    }

    fun updateShoppingItemQuantity(itemId: Long, quantity: Double) {
        viewModelScope.launch {
            try {
                repository.updateItemQuantity(itemId, quantity)
            } catch (e: Exception) {
                userMessage.value = UiSnackbarMessage("Failed to update quantity", isError = true)
            }
        }
    }

    fun deleteShoppingListItem(itemId: Long) {
        viewModelScope.launch {
            try {
                repository.deleteShoppingListItem(itemId)
                userMessage.value = UiSnackbarMessage("Item removed from list")
            } catch (e: Exception) {
                userMessage.value = UiSnackbarMessage("Failed to remove item", isError = true)
            }
        }
    }

    fun clearCheckedItemsForList(listId: Long) {
        viewModelScope.launch {
            try {
                repository.deleteCheckedItemsForList(listId)
                userMessage.value = UiSnackbarMessage("Completed items cleared")
            } catch (e: Exception) {
                userMessage.value = UiSnackbarMessage("Failed to clear items", isError = true)
            }
        }
    }

    // ==================== AI VISION SCANNING ====================

    fun analyzePriceTagImage(bitmap: Bitmap) {
        viewModelScope.launch {
            isAiScanning.value = true
            aiScanErrorMessage.value = null
            aiScannedResult.value = null

            val result = aiScannerService.scanPriceTagImage(bitmap)
            isAiScanning.value = false

            result.onSuccess { dto ->
                aiScannedResult.value = dto
            }.onFailure { error ->
                aiScanErrorMessage.value = error.localizedMessage ?: "Failed to scan price tag"
            }
        }
    }

    fun clearAiScanResult() {
        aiScannedResult.value = null
        aiScanErrorMessage.value = null
        isAiScanning.value = false
    }

    // ==================== IMPORT / EXPORT ====================

    suspend fun getExportJsonString(): String {
        return repository.exportDataToJson()
    }

    fun importDataFromJson(jsonString: String, overwrite: Boolean = false, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = repository.importDataFromJson(jsonString, overwrite)
            result.onSuccess { count ->
                userMessage.value = UiSnackbarMessage("Successfully imported $count items!")
                onComplete(true, "Successfully imported $count items!")
            }.onFailure { err ->
                userMessage.value = UiSnackbarMessage("Import failed: ${err.localizedMessage}", isError = true)
                onComplete(false, err.localizedMessage ?: "Invalid JSON format")
            }
        }
    }

    suspend fun saveImageToInternalStorage(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val filename = "goods_img_${System.currentTimeMillis()}.jpg"
        val file = File(getApplication<Application>().filesDir, filename)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        file.absolutePath
    }
}
