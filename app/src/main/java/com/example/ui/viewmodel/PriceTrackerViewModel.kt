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
import com.example.util.AppStrings
import com.example.util.UnitPriceCalculator
import com.example.util.CurrencyRates
import java.io.File
import java.io.FileOutputStream

enum class SortOption {
    PRICE_PER_ITEM,
    PRICE_PER_GRAM,
    BIGGEST_DISCOUNT,
    NAME_AZ,
    NAME_ZA,
    NOTE_AZ,
    NOTE_ZA,
    RECENTLY_UPDATED,
    MOST_SHOPS;

    fun getLabel(lang: AppLanguage = AppLanguage.ENGLISH): String = when (this) {
        PRICE_PER_ITEM -> when (lang) {
            AppLanguage.ENGLISH -> "Price: per item"
            AppLanguage.UKRAINIAN -> "Ціна: за штуку"
            AppLanguage.CZECH -> "Cena: za kus"
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
        NAME_ZA -> when (lang) {
            AppLanguage.ENGLISH -> "Name (Z-A)"
            AppLanguage.UKRAINIAN -> "Назва (Я-А)"
            AppLanguage.CZECH -> "Název (Z-A)"
        }
        NOTE_AZ -> when (lang) {
            AppLanguage.ENGLISH -> "Note (A-Z)"
            AppLanguage.UKRAINIAN -> "Примітка (А-Я)"
            AppLanguage.CZECH -> "Poznámka (A-Z)"
        }
        NOTE_ZA -> when (lang) {
            AppLanguage.ENGLISH -> "Note (Z-A)"
            AppLanguage.UKRAINIAN -> "Примітка (Я-А)"
            AppLanguage.CZECH -> "Poznámka (Z-A)"
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

    // Draft form stored to survive configuration changes
    data class DraftPriceForm(
        val good: Good? = null,
        val shop: Shop? = null,
        val barcode: String? = null,
        val regularPrice: Double? = null,
        val discountPrice: Double? = null,
        val packageAmount: Double? = null,
        val packageUnit: String? = null,
        val isPromotion: Boolean = false,
        val note: String? = null,
        val photoUri: String? = null,
        val productImageUri: String? = null,
        val pricePhotoUri: String? = null,
        val isEditing: Boolean = false
    )

    private val _addPriceDraft = MutableStateFlow(DraftPriceForm())
    val addPriceDraft: StateFlow<DraftPriceForm> = _addPriceDraft

    private val _showAddPriceDialog = MutableStateFlow(false)
    val showAddPriceDialog: StateFlow<Boolean> = _showAddPriceDialog

    fun openAddPrice(
        good: Good? = null,
        shop: Shop? = null,
        barcode: String? = null,
        regularPrice: Double? = null,
        discountPrice: Double? = null,
        packageAmount: Double? = null,
        packageUnit: String? = null,
        isPromotion: Boolean = false,
        note: String? = null,
        photoUri: String? = null,
        productImageUri: String? = null,
        pricePhotoUri: String? = null,
        isEditing: Boolean = false
    ) {
        _addPriceDraft.value = DraftPriceForm(
            good = good,
            shop = shop,
            barcode = barcode ?: good?.barcode,
            regularPrice = regularPrice,
            discountPrice = discountPrice,
            packageAmount = packageAmount ?: good?.weight,
            packageUnit = packageUnit ?: good?.weightUnit ?: "g",
            isPromotion = isPromotion,
            note = note ?: good?.notes,
            photoUri = photoUri,
            productImageUri = productImageUri ?: good?.imageUri,
            pricePhotoUri = pricePhotoUri,
            isEditing = isEditing
        )
        _showAddPriceDialog.value = true
    }

    fun openEditPrice(good: Good, shopPrice: ShopPriceDetail) {
        _addPriceDraft.value = DraftPriceForm(
            good = good,
            shop = shopPrice.shop,
            barcode = good.barcode,
            regularPrice = shopPrice.priceRecord.regularPrice,
            discountPrice = shopPrice.priceRecord.discountPrice,
            packageAmount = shopPrice.priceRecord.packageAmount,
            packageUnit = shopPrice.priceRecord.packageUnit,
            isPromotion = shopPrice.priceRecord.isPromotion,
            note = good.notes,
            photoUri = shopPrice.priceRecord.photoUri,
            productImageUri = good.imageUri,
            pricePhotoUri = shopPrice.priceRecord.photoUri,
            isEditing = true
        )
        _showAddPriceDialog.value = true
    }

    fun closeAddPriceDialog() {
        _showAddPriceDialog.value = false
        _addPriceDraft.value = DraftPriceForm()
    }

    // Incremental draft updates so UI actions (camera/gallery) persist across rotation
    fun updateDraftProductImageUri(uri: String?) {
        _addPriceDraft.value = _addPriceDraft.value.copy(productImageUri = uri)
    }

    fun updateDraftPricePhotoUri(uri: String?) {
        _addPriceDraft.value = _addPriceDraft.value.copy(pricePhotoUri = uri)
    }

    // Language state
    val currentLanguage = MutableStateFlow(
        AppLanguage.fromCode(prefs.getString("selected_language", AppLanguage.ENGLISH.code) ?: "en")
    )

    // Pending photo target name preserved across camera launches and rotations (string enum name)
    private val _pendingPhotoTargetName = MutableStateFlow<String?>(null)
    val pendingPhotoTargetName: StateFlow<String?> = _pendingPhotoTargetName

    fun setPendingPhotoTargetName(name: String?) {
        _pendingPhotoTargetName.value = name
    }

    fun setLanguage(lang: AppLanguage) {
        currentLanguage.value = lang
        prefs.edit().putString("selected_language", lang.code).apply()
    }

    // Currency state
    val currentCurrency = MutableStateFlow(
        AppCurrency.fromCode(prefs.getString("selected_currency", AppCurrency.CZK.code) ?: AppCurrency.CZK.code)
    )

    fun setCurrency(currency: AppCurrency) {
        currentCurrency.value = currency
        prefs.edit().putString("selected_currency", currency.code).apply()
    }

    init {
        val db = AppDatabase.getDatabase(application)
        repository = PriceRepository(db)
        // load saved currency rates (or defaults)
        CurrencyRates.loadFromPrefs(application)
        viewModelScope.launch {
            repository.seedSampleDataIfEmpty()
        }
    }

    // Currency rates management (per-device manual rates)
    fun setCurrencyRate(currencyCode: String, ratePer1USD: Double) {
        CurrencyRates.setRate(currencyCode, ratePer1USD)
        CurrencyRates.saveToPrefs(getApplication())
    }

    fun getCurrencyRates(): Map<String, Double> = CurrencyRates.getRates()

    /**
     * Convert an amount stored in sourceCurrency (if null, default CZK) to app selected currency.
     */
    fun convertAmount(amount: Double, sourceCurrency: String?): Double {
        val targetCode = currentCurrency.value.code
        return CurrencyRates.convert(amount, sourceCurrency ?: AppCurrency.CZK.code, targetCode)
    }

    fun formatAmountForDisplay(amount: Double, sourceCurrency: String?): String {
        val converted = convertAmount(amount, sourceCurrency)
        val targetCurr = AppCurrency.fromCode(currentCurrency.value.code)
        return UnitPriceCalculator.formatCurrency(converted, targetCurr)
    }

    // Filter and Search states
    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow(AppStrings.ALL)
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
        if (params.category != AppStrings.ALL) {
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
            SortOption.NAME_ZA -> list.sortedByDescending { it.good.name.lowercase() }
            SortOption.NOTE_AZ -> list.sortedBy {
                it.good.notes.ifBlank { "\uFFFF" }.lowercase()
            }
            SortOption.NOTE_ZA -> list.sortedByDescending {
                it.good.notes.ifBlank { "" }.lowercase()
            }
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
    val geminiApiKey = MutableStateFlow(prefs.getString("gemini_api_key", "") ?: "")

    fun setGeminiApiKey(key: String) {
        val trimmed = key.trim()
        prefs.edit().putString("gemini_api_key", trimmed).apply()
        geminiApiKey.value = trimmed
    }

    fun clearGeminiApiKey() {
        prefs.edit().remove("gemini_api_key").apply()
        geminiApiKey.value = ""
    }

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
                    note = note,
                    currencyCode = currentCurrency.value.code
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

            val result = aiScannerService.scanPriceTagImage(bitmap, geminiApiKey.value)
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

    suspend fun getExportZipBytes(): ByteArray {
        return repository.exportDataToZipBytes(getApplication())
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

    fun importZipFromUri(uri: android.net.Uri, overwrite: Boolean = false, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val result = repository.importDataFromZip(stream, overwrite, context)
                    result.onSuccess { count ->
                        userMessage.value = UiSnackbarMessage("Imported $count items from ZIP")
                        onComplete(true, "Imported $count items from ZIP")
                    }.onFailure { e ->
                        userMessage.value = UiSnackbarMessage("ZIP import failed: ${e.localizedMessage}", isError = true)
                        onComplete(false, e.localizedMessage ?: "ZIP import failed")
                    }
                } ?: run {
                    onComplete(false, "Failed to open selected file")
                }
            } catch (e: Exception) {
                userMessage.value = UiSnackbarMessage("ZIP import error: ${e.localizedMessage}", isError = true)
                onComplete(false, e.localizedMessage ?: "ZIP import failed")
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
