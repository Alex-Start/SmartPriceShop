package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.util.UnitPriceCalculator
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import android.content.Context
import com.example.util.AppCurrency
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import java.io.File
import java.io.FileOutputStream

class PriceRepository(private val database: AppDatabase) {

    private val goodDao = database.goodDao()
    private val shopDao = database.shopDao()
    private val priceRecordDao = database.priceRecordDao()
    private val priceHistoryDao = database.priceHistoryDao()
    private val shoppingListDao = database.shoppingListDao()
    private val categoryDao = database.categoryDao()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val backupAdapter = moshi.adapter(BackupDataDto::class.java).indent("  ")

    val allGoods: Flow<List<Good>> = goodDao.getAllGoods()
    val allShops: Flow<List<Shop>> = shopDao.getAllShops()
    val allPriceRecords: Flow<List<PriceRecord>> = priceRecordDao.getAllPriceRecords()
    val allPriceHistory: Flow<List<PriceHistory>> = priceHistoryDao.getAllPriceHistory()
    val allShoppingLists: Flow<List<ShoppingList>> = shoppingListDao.getAllShoppingLists()
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()

    /**
     * Combines goods, shops, price records, and price history into rich comparison items.
     */
    val goodsWithPrices: Flow<List<GoodWithPrices>> = combine(
        goodDao.getAllGoods(),
        shopDao.getAllShops(),
        priceRecordDao.getAllPriceRecords(),
        priceHistoryDao.getAllPriceHistory()
    ) { goods, shops, priceRecords, priceHistory ->
        val shopsMap = shops.associateBy { it.id }
        val pricesByGood = priceRecords.groupBy { it.goodId }
        val historyByGoodAndShop = priceHistory.groupBy { "${it.goodId}_${it.shopId}" }

        goods.map { good ->
            val records = pricesByGood[good.id] ?: emptyList()
            
            // Build shop price details
            val shopPriceDetails = records.mapNotNull { record ->
                val shop = shopsMap[record.shopId] ?: return@mapNotNull null
                val historyKey = "${record.goodId}_${record.shopId}"
                val histories = (historyByGoodAndShop[historyKey] ?: emptyList())
                    .sortedByDescending { it.recordedAt }
                
                // Find previous historical price before the current update if any
                val prevHistory = histories.firstOrNull { it.recordedAt < record.updatedAt }
                val prevEffective = prevHistory?.effectivePrice
                val currentEffective = record.effectivePrice

                val (trend, diffPercent) = when {
                    prevEffective == null -> Pair(PriceTrend.NEW, 0.0)
                    currentEffective < prevEffective - 0.009 -> {
                        val diff = ((prevEffective - currentEffective) / prevEffective) * 100.0
                        Pair(PriceTrend.DOWN, diff)
                    }
                    currentEffective > prevEffective + 0.009 -> {
                        val diff = ((currentEffective - prevEffective) / prevEffective) * 100.0
                        Pair(PriceTrend.UP, diff)
                    }
                    else -> Pair(PriceTrend.STABLE, 0.0)
                }

                ShopPriceDetail(
                    shop = shop,
                    priceRecord = record,
                    priceTrend = trend,
                    previousEffectivePrice = prevEffective,
                    priceDiffPercent = diffPercent
                )
            }

            // Find cheapest shop for this good based on unit price or effective price
            val cheapestRecord = shopPriceDetails.minByOrNull { 
                if (it.priceRecord.pricePerUnit > 0.0) it.priceRecord.pricePerUnit else it.priceRecord.effectivePrice 
            }
            val cheapestShopDetail = cheapestRecord?.copy(isCheapest = true)

            val markedDetails = shopPriceDetails.map { detail ->
                if (cheapestRecord != null && detail.shop.id == cheapestRecord.shop.id) {
                    detail.copy(isCheapest = true)
                } else {
                    detail.copy(isCheapest = false)
                }
            }.sortedBy { it.priceRecord.effectivePrice }

                // compute numeric min/max (note: currencies might differ across shops; min/max are raw effectivePrice values as stored)
                val priceMinVal = if (markedDetails.isEmpty()) null else markedDetails.minOf { it.priceRecord.effectivePrice }
                val priceMaxVal = if (markedDetails.isEmpty()) null else markedDetails.maxOf { it.priceRecord.effectivePrice }

                val priceRange = if (markedDetails.isEmpty()) {
                    "No price recorded yet"
                } else if (markedDetails.size == 1) {
                    val r = markedDetails.first().priceRecord
                    UnitPriceCalculator.formatCurrencyWithConversion(r.effectivePrice, r.currencyCode, AppCurrency.CZK)
                } else {
                    val min = priceMinVal ?: 0.0
                    val max = priceMaxVal ?: 0.0
                    val minRecord = markedDetails.minByOrNull { it.priceRecord.effectivePrice }?.priceRecord
                    val maxRecord = markedDetails.maxByOrNull { it.priceRecord.effectivePrice }?.priceRecord
                    val minStr = if (minRecord != null) UnitPriceCalculator.formatCurrencyWithConversion(min, minRecord.currencyCode, AppCurrency.CZK) else UnitPriceCalculator.formatCurrencyWithConversion(min)
                    val maxStr = if (maxRecord != null) UnitPriceCalculator.formatCurrencyWithConversion(max, maxRecord.currencyCode, AppCurrency.CZK) else UnitPriceCalculator.formatCurrencyWithConversion(max)
                    "${minStr} - ${maxStr}"
                }

                GoodWithPrices(
                    good = good,
                    shopPrices = markedDetails,
                    cheapestShopDetail = cheapestShopDetail,
                    priceRangeText = "",
                    priceMin = priceMinVal,
                    priceMinCurrencyCode = markedDetails.minByOrNull { it.priceRecord.effectivePrice }?.priceRecord?.currencyCode,
                    priceMax = priceMaxVal,
                    priceMaxCurrencyCode = markedDetails.maxByOrNull { it.priceRecord.effectivePrice }?.priceRecord?.currencyCode,
                    totalShopsRecorded = markedDetails.size
                )
            }
    }

    /**
     * Rich flow of all shopping lists with items, target shop, and cost totals.
     */
    val shoppingListsWithItems: Flow<List<ShoppingListWithItems>> = combine(
        shoppingListDao.getAllShoppingLists(),
        shoppingListDao.getAllItems(),
        shopDao.getAllShops()
    ) { lists, allItems, shops ->
        val shopsMap = shops.associateBy { it.id }
        val itemsByList = allItems.groupBy { it.listId }

        lists.map { list ->
            val items = itemsByList[list.id] ?: emptyList()
            val targetShop = list.targetShopId?.let { shopsMap[it] }
            ShoppingListWithItems(
                list = list,
                items = items,
                targetShop = targetShop,
                totalItemCount = items.size,
                completedItemCount = items.count { it.isChecked },
                totalEstimatedCost = items.sumOf { it.totalEstimatedPrice }
            )
        }
    }

    suspend fun getGoodById(id: Long): Good? = goodDao.getGoodById(id)

    suspend fun findGoodByBarcode(barcode: String): Good? = withContext(Dispatchers.IO) {
        val cleanBarcode = barcode.trim()
        if (cleanBarcode.isBlank()) return@withContext null
        goodDao.getGoodByBarcode(cleanBarcode)
    }

    suspend fun getShopById(id: Long): Shop? = shopDao.getShopById(id)

    suspend fun saveGood(good: Good): Long = withContext(Dispatchers.IO) {
        goodDao.insertGood(good)
    }

    suspend fun saveShop(shop: Shop): Long = withContext(Dispatchers.IO) {
        shopDao.insertShop(shop)
    }

    suspend fun getOrCreateShop(name: String, address: String = ""): Shop = withContext(Dispatchers.IO) {
        val trimmed = name.trim()
        val existing = shopDao.getShopByName(trimmed)
        if (existing != null) {
            existing
        } else {
            val colors = listOf("#1E40AF", "#006C4C", "#8E4E00", "#7B2CBF", "#D90429", "#007F5F", "#2B2D42")
            val chosenColor = colors[Math.abs(trimmed.hashCode()) % colors.size]
            val newShop = Shop(
                name = trimmed,
                address = address.trim(),
                colorHex = chosenColor
            )
            val newId = shopDao.insertShop(newShop)
            newShop.copy(id = newId)
        }
    }

    /**
     * Saves a price record for a Good at a Shop.
     * Automatically calculates standardized unit price and appends an entry to PriceHistory!
     */
    suspend fun savePriceRecord(
        goodId: Long,
        shopId: Long,
        regularPrice: Double,
        discountPrice: Double? = null,
        packageAmount: Double = 1.0,
        packageUnit: String = "g",
        photoUri: String? = null,
        isPromotion: Boolean = false,
        note: String? = null,
        currencyCode: String = AppCurrency.CZK.code
    ): Long = withContext(Dispatchers.IO) {
        val effectivePrice = if (discountPrice != null && discountPrice > 0 && discountPrice < regularPrice) discountPrice else regularPrice
        val (unitPrice, unitLabel) = UnitPriceCalculator.calculateUnitPrice(effectivePrice, packageAmount, packageUnit)

        val existing = priceRecordDao.getPriceRecord(goodId, shopId)
        val now = System.currentTimeMillis()

        // 1. Log to history
        val historyEntry = PriceHistory(
            goodId = goodId,
            shopId = shopId,
            regularPrice = regularPrice,
            discountPrice = discountPrice,
            packageAmount = packageAmount,
            packageUnit = packageUnit,
            pricePerUnit = unitPrice,
            note = note,
            photoUri = photoUri ?: existing?.photoUri,
            currencyCode = currencyCode,
            recordedAt = now
        )
        priceHistoryDao.insertHistory(historyEntry)

        // 2. Update current PriceRecord
        val newRecord = PriceRecord(
            id = existing?.id ?: 0,
            goodId = goodId,
            shopId = shopId,
            regularPrice = regularPrice,
            discountPrice = discountPrice,
            packageAmount = packageAmount,
            packageUnit = packageUnit,
            pricePerUnit = unitPrice,
            unitMeasureLabel = unitLabel,
            isPromotion = isPromotion || (discountPrice != null && discountPrice > 0.0),
            photoUri = photoUri ?: existing?.photoUri,
            currencyCode = currencyCode,
            updatedAt = now
        )
        priceRecordDao.insertPriceRecord(newRecord)
    }

    suspend fun getPriceHistoryForGood(goodId: Long): Flow<List<PriceHistoryWithShop>> {
        return combine(
            priceHistoryDao.getHistoryForGood(goodId),
            shopDao.getAllShops()
        ) { historyList, shops ->
            val shopsMap = shops.associateBy { it.id }
            historyList.map { history ->
                val shop = shopsMap[history.shopId]
                PriceHistoryWithShop(
                    history = history,
                    shopName = shop?.name ?: "Unknown Shop",
                    shopAddress = shop?.address ?: "",
                    shopColor = shop?.colorHex ?: "#1E40AF"
                )
            }
        }
    }

    suspend fun deleteGood(goodId: Long) = withContext(Dispatchers.IO) {
        goodDao.deleteGoodById(goodId)
        priceRecordDao.deletePricesForGood(goodId)
        priceHistoryDao.deleteHistoryForGood(goodId)
    }

    suspend fun deleteShop(shopId: Long) = withContext(Dispatchers.IO) {
        shopDao.deleteShopById(shopId)
        priceRecordDao.deletePricesForShop(shopId)
        priceHistoryDao.deleteHistoryForShop(shopId)
    }

    suspend fun deletePriceRecord(goodId: Long, shopId: Long) = withContext(Dispatchers.IO) {
        priceRecordDao.deletePriceRecord(goodId, shopId)
    }

    suspend fun deleteHistoryEntry(historyId: Long) = withContext(Dispatchers.IO) {
        priceHistoryDao.deleteHistoryById(historyId)
    }

    suspend fun clearPriceHistory() = withContext(Dispatchers.IO) {
        priceHistoryDao.deleteAllPriceHistory()
    }

    suspend fun clearPriceHistoryForGood(goodId: Long) = withContext(Dispatchers.IO) {
        priceHistoryDao.deleteHistoryForGood(goodId)
    }

    suspend fun saveCategory(name: String, colorHex: String = "#1E40AF"): Long = withContext(Dispatchers.IO) {
        val trimmed = name.trim()
        val existing = categoryDao.getCategoryByName(trimmed)
        if (existing != null) {
            existing.id
        } else {
            val cat = Category(name = trimmed, colorHex = colorHex, isDefault = false)
            categoryDao.insertCategory(cat)
        }
    }

    suspend fun updateCategory(category: Category) = withContext(Dispatchers.IO) {
        categoryDao.updateCategory(category)
    }

    suspend fun deleteCategory(categoryId: Long) = withContext(Dispatchers.IO) {
        categoryDao.deleteCategoryById(categoryId)
    }

    /*suspend fun seedDefaultCategoriesIfEmpty() = withContext(Dispatchers.IO) {
        val list = categoryDao.getAllCategoriesList()
        if (list.isEmpty()) {
            val defaults = listOf(
                Category(name = "Dairy", colorHex = "#2563EB", isDefault = true, iconName = "egg"),
                Category(name = "Fruits & Veg", colorHex = "#16A34A", isDefault = true, iconName = "nutrition"),
                Category(name = "Meat & Fish", colorHex = "#DC2626", isDefault = true, iconName = "restaurant"),
                Category(name = "Bakery", colorHex = "#D97706", isDefault = true, iconName = "bakery_dining"),
                Category(name = "Beverages", colorHex = "#0284C7", isDefault = true, iconName = "local_cafe"),
                Category(name = "Pantry", colorHex = "#7C3AED", isDefault = true, iconName = "kitchen"),
                Category(name = "Snacks", colorHex = "#EA580C", isDefault = true, iconName = "cookie"),
                Category(name = "Household", colorHex = "#0D9488", isDefault = true, iconName = "cleaning_services"),
                Category(name = "Other", colorHex = "#64748B", isDefault = true, iconName = "category")
            )
            categoryDao.insertCategories(defaults)
        }
    }*/

    suspend fun seedDefaultCategoriesIfEmpty() =  withContext(Dispatchers.IO) {
            val categories = categoryDao.getAllCategoriesList()
            if (categories.isEmpty()) {
                val defaults = CategoryType.entries.map {
                        Category(
                            name = it.displayName,
                            colorHex = it.colorHex,
                            iconName = it.iconName,
                            isDefault = true
                        )
                    }

                categoryDao.insertCategories(defaults)
            }
        }

    suspend fun resetAllData() = withContext(Dispatchers.IO) {
        shoppingListDao.deleteAllItems()
        shoppingListDao.deleteAllShoppingLists()
        priceHistoryDao.deleteAllPriceHistory()
        priceRecordDao.deleteAllPriceRecords()
        goodDao.deleteAllGoods()
        shopDao.deleteAllShops()
        categoryDao.deleteAllCategories()
        seedDefaultCategoriesIfEmpty()
    }

    // ==================== SHOPPING LISTS ====================

    fun getItemsForShoppingList(listId: Long): Flow<List<ShoppingListItem>> {
        return shoppingListDao.getItemsForList(listId)
    }

    suspend fun createShoppingList(name: String, colorHex: String = "#1E40AF", targetShopId: Long? = null): Long = withContext(Dispatchers.IO) {
        val list = ShoppingList(
            name = name.trim(),
            colorHex = colorHex,
            targetShopId = targetShopId
        )
        shoppingListDao.insertShoppingList(list)
    }

    suspend fun updateShoppingList(list: ShoppingList) = withContext(Dispatchers.IO) {
        shoppingListDao.updateShoppingList(list)
    }

    suspend fun deleteShoppingList(listId: Long) = withContext(Dispatchers.IO) {
        shoppingListDao.deleteAllItemsForList(listId)
        shoppingListDao.deleteShoppingListById(listId)
    }

    suspend fun addGoodToShoppingList(
        listId: Long,
        goodId: Long,
        quantity: Double = 1.0,
        preferredShopId: Long? = null
    ): Long = withContext(Dispatchers.IO) {
        val good = goodDao.getGoodById(goodId) ?: return@withContext -1L
        
        // Find best or target price for this good
        val priceRecords = priceRecordDao.getPricesForGoodList(goodId)
        val estimatedPrice = if (preferredShopId != null) {
            priceRecords.firstOrNull { it.shopId == preferredShopId }?.effectivePrice
                ?: priceRecords.minByOrNull { it.effectivePrice }?.effectivePrice ?: 0.0
        } else {
            priceRecords.minByOrNull { it.effectivePrice }?.effectivePrice ?: 0.0
        }

        val item = ShoppingListItem(
            listId = listId,
            goodId = good.id,
            name = good.name,
            category = good.category,
            quantity = quantity,
            unit = good.weightUnit.ifBlank { "pcs" },
            weight = good.weight,
            weightUnit = good.weightUnit,
            preferredShopId = preferredShopId,
            estimatedUnitPrice = estimatedPrice,
            notes = good.notes
        )
        shoppingListDao.insertItem(item)
    }

    suspend fun addCustomItemToShoppingList(
        listId: Long,
        name: String,
        category: String = "Groceries",
        quantity: Double = 1.0,
        unit: String = "pcs",
        weight: Double = 0.0,
        weightUnit: String = "g",
        estimatedPrice: Double = 0.0,
        notes: String = "",
        preferredShopId: Long? = null
    ): Long = withContext(Dispatchers.IO) {
        val item = ShoppingListItem(
            listId = listId,
            goodId = null,
            name = name.trim(),
            category = category,
            quantity = quantity,
            unit = unit,
            weight = weight,
            weightUnit = weightUnit,
            preferredShopId = preferredShopId,
            estimatedUnitPrice = estimatedPrice,
            notes = notes.trim()
        )
        shoppingListDao.insertItem(item)
    }

    suspend fun toggleItemChecked(item: ShoppingListItem) = withContext(Dispatchers.IO) {
        shoppingListDao.updateItem(item.copy(isChecked = !item.isChecked))
    }

    suspend fun updateShoppingListItem(item: ShoppingListItem) = withContext(Dispatchers.IO) {
        shoppingListDao.updateItem(item)
    }

    suspend fun updateItemQuantity(itemId: Long, newQuantity: Double) = withContext(Dispatchers.IO) {
        if (newQuantity <= 0.0) {
            shoppingListDao.deleteItemById(itemId)
        } else {
            val all = shoppingListDao.getAllItemsList()
            val existing = all.firstOrNull { it.id == itemId }
            if (existing != null) {
                shoppingListDao.updateItem(existing.copy(quantity = newQuantity))
            }
        }
    }

    suspend fun deleteShoppingListItem(itemId: Long) = withContext(Dispatchers.IO) {
        shoppingListDao.deleteItemById(itemId)
    }

    suspend fun deleteCheckedItemsForList(listId: Long) = withContext(Dispatchers.IO) {
        shoppingListDao.deleteCheckedItemsForList(listId)
    }

    // ==================== JSON IMPORT / EXPORT ====================

    suspend fun exportDataToJson(): String = withContext(Dispatchers.IO) {
        val goods = goodDao.getAllGoodsList()
        val shops = shopDao.getAllShopsList()
        val records = priceRecordDao.getAllPriceRecordsList()
        val history = priceHistoryDao.getAllPriceHistoryList()
        val lists = shoppingListDao.getAllShoppingListsList()
        val items = shoppingListDao.getAllItemsList()
        val categories = categoryDao.getAllCategoriesList()

        val backup = BackupDataDto(
            version = 3,
            exportedAt = System.currentTimeMillis(),
            goods = goods,
            shops = shops,
            priceRecords = records,
            priceHistory = history,
            shoppingLists = lists,
            shoppingListItems = items,
            categories = categories
        )
        backupAdapter.toJson(backup)
    }

    // ZIP export: produces a ZIP with manifest.json and images/ folder
    suspend fun exportDataToZipBytes(context: Context): ByteArray = withContext(Dispatchers.IO) {
        val goods = goodDao.getAllGoodsList()
        val shops = shopDao.getAllShopsList()
        val records = priceRecordDao.getAllPriceRecordsList()
        val history = priceHistoryDao.getAllPriceHistoryList()
        val lists = shoppingListDao.getAllShoppingListsList()
        val items = shoppingListDao.getAllItemsList()
        val categories = categoryDao.getAllCategoriesList()

        // Collect image files referenced by goods and price records
        val imageEntries = mutableListOf<Pair<File, String>>()
        var imageIndex = 0

        fun addImageIfExists(path: String?): String? {
            if (path == null) return null
            try {
                val f = File(path)
                if (f.exists()) {
                    val entryName = "images/img_${System.currentTimeMillis()}_${imageIndex}_${f.name}"
                    imageIndex += 1
                    imageEntries.add(Pair(f, entryName))
                    return entryName
                }
            } catch (_: Exception) {}
            return null
        }

        // Build copies that point to entry names instead of absolute paths
        val goodsForManifest = goods.map { g ->
            val newImgEntry = addImageIfExists(g.imageUri)
            g.copy(imageUri = newImgEntry)
        }
        val recordsForManifest = records.map { r ->
            val newPhotoEntry = addImageIfExists(r.photoUri)
            r.copy(photoUri = newPhotoEntry)
        }

        val backup = BackupDataDto(
            version = 3,
            exportedAt = System.currentTimeMillis(),
            goods = goodsForManifest,
            shops = shops,
            priceRecords = recordsForManifest,
            priceHistory = history,
            shoppingLists = lists,
            shoppingListItems = items,
            categories = categories
        )

        val manifestJson = backupAdapter.toJson(backup)

        val baos = ByteArrayOutputStream()
        ZipOutputStream(BufferedOutputStream(baos)).use { zos ->
            // manifest
            val manifestBytes = manifestJson.toByteArray(Charsets.UTF_8)
            val mEntry = ZipEntry("manifest.json")
            zos.putNextEntry(mEntry)
            zos.write(manifestBytes)
            zos.closeEntry()

            // images
            for ((file, entryName) in imageEntries) {
                try {
                    val entry = ZipEntry(entryName)
                    zos.putNextEntry(entry)
                    FileInputStream(file).use { fis ->
                        val buf = ByteArray(4096)
                        var read: Int
                        while (fis.read(buf).also { read = it } > 0) {
                            zos.write(buf, 0, read)
                        }
                    }
                    zos.closeEntry()
                } catch (_: Exception) {
                }
            }
            zos.finish()
        }
        baos.toByteArray()
    }

    // ZIP import: reads manifest.json and images, writes images into app filesDir and updates manifest references
    suspend fun importDataFromZip(inputStream: InputStream, overwrite: Boolean = false, context: Context): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val tempDir = File(context.filesDir, "imported_images")
            if (!tempDir.exists()) tempDir.mkdirs()

            val zis = ZipInputStream(BufferedInputStream(inputStream))
            var entry = zis.nextEntry
            var manifestText: String? = null
            val savedImageMap = mutableMapOf<String, String>() // entryName -> saved absolute path

            while (entry != null) {
                val name = entry.name
                if (entry.isDirectory) {
                    entry = zis.nextEntry
                    continue
                }
                if (name == "manifest.json") {
                    val sb = StringBuilder()
                    val reader = zis.bufferedReader(Charsets.UTF_8)
                    reader.use { r -> sb.append(r.readText()) }
                    manifestText = sb.toString()
                } else if (name.startsWith("images/")) {
                    val baseName = name.substringAfterLast('/')
                    val outFile = File(tempDir, "${System.currentTimeMillis()}_${baseName}")
                    FileOutputStream(outFile).use { fos ->
                        val buf = ByteArray(4096)
                        var read: Int
                        while (zis.read(buf).also { read = it } > 0) {
                            fos.write(buf, 0, read)
                        }
                    }
                    savedImageMap[name] = outFile.absolutePath
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
            zis.close()

            if (manifestText == null) return@withContext Result.failure(IllegalArgumentException("ZIP missing manifest.json"))

            val backup = backupAdapter.fromJson(manifestText)
                ?: return@withContext Result.failure(IllegalArgumentException("Invalid manifest JSON in ZIP"))

            if (overwrite) {
                resetAllData()
            }

            // Replace image entry names with actual saved paths
            val goodsResolved = backup.goods.map { g ->
                val img = g.imageUri
                if (img != null && img.startsWith("images/")) {
                    val saved = savedImageMap[img]
                    g.copy(imageUri = saved)
                } else g
            }
            val recordsResolved = backup.priceRecords.map { r ->
                val p = r.photoUri
                if (p != null && p.startsWith("images/")) {
                    val saved = savedImageMap[p]
                    r.copy(photoUri = saved)
                } else r
            }

            // Insert data similar to JSON import
            if (backup.categories.isNotEmpty()) categoryDao.insertCategories(backup.categories) else seedDefaultCategoriesIfEmpty()
            if (backup.shops.isNotEmpty()) shopDao.insertShops(backup.shops)
            if (goodsResolved.isNotEmpty()) goodDao.insertGoods(goodsResolved)
            if (recordsResolved.isNotEmpty()) priceRecordDao.insertPriceRecords(recordsResolved)
            if (backup.priceHistory.isNotEmpty()) priceHistoryDao.insertHistories(backup.priceHistory)
            if (backup.shoppingLists.isNotEmpty()) shoppingListDao.insertShoppingLists(backup.shoppingLists)
            if (backup.shoppingListItems.isNotEmpty()) shoppingListDao.insertItems(backup.shoppingListItems)

            val totalImported = goodsResolved.size + backup.shops.size + recordsResolved.size + backup.shoppingLists.size + backup.shoppingListItems.size + backup.categories.size
            Result.success(totalImported)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importDataFromJson(jsonString: String, overwrite: Boolean = false): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val backup = backupAdapter.fromJson(jsonString)
                ?: return@withContext Result.failure(IllegalArgumentException("Invalid JSON format"))

            if (overwrite) {
                resetAllData()
            }

            // Insert categories
            if (backup.categories.isNotEmpty()) {
                categoryDao.insertCategories(backup.categories)
            } else {
                seedDefaultCategoriesIfEmpty()
            }
            // Insert shops
            if (backup.shops.isNotEmpty()) {
                shopDao.insertShops(backup.shops)
            }
            // Insert goods
            if (backup.goods.isNotEmpty()) {
                goodDao.insertGoods(backup.goods)
            }
            // Insert price records
            if (backup.priceRecords.isNotEmpty()) {
                priceRecordDao.insertPriceRecords(backup.priceRecords)
            }
            // Insert history
            if (backup.priceHistory.isNotEmpty()) {
                priceHistoryDao.insertHistories(backup.priceHistory)
            }
            // Insert shopping lists
            if (backup.shoppingLists.isNotEmpty()) {
                shoppingListDao.insertShoppingLists(backup.shoppingLists)
            }
            // Insert items
            if (backup.shoppingListItems.isNotEmpty()) {
                shoppingListDao.insertItems(backup.shoppingListItems)
            }

            val totalImported = backup.goods.size + backup.shops.size + backup.priceRecords.size + backup.shoppingLists.size + backup.shoppingListItems.size + backup.categories.size
            Result.success(totalImported)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Seeds rich demo supermarket data with realistic barcodes, weights, prices, and sample shopping lists.
     */
    suspend fun seedSampleDataIfEmpty() = withContext(Dispatchers.IO) {
        seedDefaultCategoriesIfEmpty()
        val existingGoods = goodDao.getAllGoodsList()
        if (existingGoods.isNotEmpty()) return@withContext

        // 1. Create Supermarkets
        val tesco = Shop(id = 1, name = "Tesco Extra", address = "High Street Superstore", colorHex = "#00539F")
        val walmart = Shop(id = 2, name = "Walmart", address = "Westside Commerce Blvd", colorHex = "#0071DC")
        val lidl = Shop(id = 3, name = "Lidl", address = "Station Road Market", colorHex = "#0050AA")
        val aldi = Shop(id = 4, name = "Aldi", address = "Greenway Park Retail", colorHex = "#0A2240")
        val carrefour = Shop(id = 5, name = "Carrefour", address = "Boulevard Plaza", colorHex = "#004E98")

        shopDao.insertShops(listOf(tesco, walmart, lidl, aldi, carrefour))

        // 2. Create Goods with realistic barcodes and weights
        val goods = listOf(
            Good(
                id = 1,
                name = "Organic Whole Milk",
                category = "Dairy",
                barcode = "5000128741001",
                unitType = "per 1L",
                defaultQuantity = 1.0,
                weight = 1000.0,
                weightUnit = "ml",
                notes = "Fresh semi-skimmed or whole milk"
            ),
            Good(
                id = 2,
                name = "Free Range Eggs 12pk",
                category = "Dairy",
                barcode = "5000128741002",
                unitType = "per item",
                defaultQuantity = 12.0,
                weight = 680.0,
                weightUnit = "g",
                notes = "Large grade A free range eggs"
            ),
            Good(
                id = 3,
                name = "Italian Spaghetti",
                category = "Pantry",
                barcode = "5000128741003",
                unitType = "per 100g",
                defaultQuantity = 500.0,
                weight = 500.0,
                weightUnit = "g",
                notes = "100% durum wheat bronze cut"
            ),
            Good(
                id = 4,
                name = "Extra Virgin Olive Oil",
                category = "Pantry",
                barcode = "5000128741004",
                unitType = "per 100ml",
                defaultQuantity = 1000.0,
                weight = 1000.0,
                weightUnit = "ml",
                notes = "Cold pressed 1L bottle"
            ),
            Good(
                id = 5,
                name = "Arabica Coffee Beans",
                category = "Beverages",
                barcode = "5000128741005",
                unitType = "per 100g",
                defaultQuantity = 500.0,
                weight = 500.0,
                weightUnit = "g",
                notes = "Medium roast whole beans"
            ),
            Good(
                id = 6,
                name = "Fresh Cavendish Bananas",
                category = "Fruits & Veg",
                barcode = "5000128741006",
                unitType = "per 100g",
                defaultQuantity = 1000.0,
                weight = 1000.0,
                weightUnit = "g",
                notes = "Ripe yellow bananas"
            ),
            Good(
                id = 7,
                name = "Fresh Atlantic Salmon Fillet",
                category = "Meat & Fish",
                barcode = "5000128741007",
                unitType = "per 100g",
                defaultQuantity = 400.0,
                weight = 400.0,
                weightUnit = "g",
                notes = "Skin-on boneless fresh fillets"
            ),
            Good(
                id = 8,
                name = "Sourdough Bread Loaf",
                category = "Bakery",
                barcode = "5000128741008",
                unitType = "per 100g",
                defaultQuantity = 600.0,
                weight = 600.0,
                weightUnit = "g",
                notes = "Artisan crusty rustic sourdough"
            ),
            Good(
                id = 9,
                name = "White Granulated Sugar",
                category = "Pantry",
                barcode = "5000128741009",
                unitType = "per 100g",
                defaultQuantity = 1000.0,
                weight = 1000.0,
                weightUnit = "g",
                notes = "Pure fine granulated white sugar 1kg"
            ),
            Good(
                id = 10,
                name = "Fresh Crisp Carrots",
                category = "Fruits & Veg",
                barcode = "5000128741010",
                unitType = "per 100g",
                defaultQuantity = 1000.0,
                weight = 1000.0,
                weightUnit = "g",
                notes = "Crunchy washed carrots 1kg bag"
            ),
            Good(
                id = 11,
                name = "Cane Sugar (500g)",
                category = "Pantry",
                barcode = "5000128741011",
                unitType = "per 100g",
                defaultQuantity = 500.0,
                weight = 500.0,
                weightUnit = "g",
                notes = "Natural unrefined cane sugar 500g"
            )
        )
        goodDao.insertGoods(goods)

        val oneDayAgo = System.currentTimeMillis() - 86400000L * 3
        val fiveDaysAgo = System.currentTimeMillis() - 86400000L * 10
        val twoWeeksAgo = System.currentTimeMillis() - 86400000L * 20

        // 3. Price records & historical prices for Milk (goodId = 1)
        saveSamplePrice(1, 1, 18.5, null, 1000.0, "ml", twoWeeksAgo, 1.95)
        saveSamplePrice(1, 3, 15.5, 13.9, 1000.0, "ml", oneDayAgo, 1.55) // Lidl is cheapest!
        saveSamplePrice(1, 4, 14.9, null, 1000.0, "ml", fiveDaysAgo, 1.49)
        saveSamplePrice(1, 2, 19.9, 17.9, 1000.0, "ml", oneDayAgo, 2.09)

        // 4. Eggs 12pk (goodId = 2)
        saveSamplePrice(2, 4, 28.9, null, 12.0, "pcs", oneDayAgo, 3.10) // Aldi cheapest
        saveSamplePrice(2, 1, 34.0, 29.9, 12.0, "pcs", oneDayAgo, 3.40)
        saveSamplePrice(2, 3, 31.5, null, 12.0, "pcs", fiveDaysAgo, 2.99)
        saveSamplePrice(2, 5, 36.5, null, 12.0, "pcs", oneDayAgo, 3.65)

        // 5. Spaghetti 500g (goodId = 3)
        saveSamplePrice(3, 3, 9.5, 07.9, 500.0, "g", oneDayAgo, 0.95) // Lidl promo
        saveSamplePrice(3, 4, 8.5, null, 500.0, "g", fiveDaysAgo, 0.85)
        saveSamplePrice(3, 1, 12.5, 10.0, 500.0, "g", oneDayAgo, 1.25)
        saveSamplePrice(3, 2, 11.5, null, 500.0, "g", twoWeeksAgo, 1.15)

        // 6. Olive Oil 1L (goodId = 4)
        saveSamplePrice(4, 5, 89.9, 74.9, 1000.0, "ml", oneDayAgo, 9.49)
        saveSamplePrice(4, 3, 69.9, null, 1000.0, "ml", fiveDaysAgo, 6.49) // Lidl cheapest
        saveSamplePrice(4, 1, 85.0, null, 1000.0, "ml", twoWeeksAgo, 8.50)

        // 7. Coffee Beans 500g (goodId = 5)
        saveSamplePrice(5, 1, 65.0, 49.9, 500.0, "g", oneDayAgo, 6.50) // Tesco Clubcard deal
        saveSamplePrice(5, 2, 58.0, null, 500.0, "g", fiveDaysAgo, 5.80)
        saveSamplePrice(5, 4, 54.9, null, 500.0, "g", twoWeeksAgo, 5.29)

        // 8. Bananas 1kg (goodId = 6)
        saveSamplePrice(6, 4, 11.9, null, 1000.0, "g", oneDayAgo, 1.29) // Aldi
        saveSamplePrice(6, 3, 12.5, 10.9, 1000.0, "g", oneDayAgo, 1.25) // Lidl
        saveSamplePrice(6, 1, 14.5, null, 1000.0, "g", fiveDaysAgo, 1.45)

        // 9. White Granulated Sugar 1kg (goodId = 9)
        saveSamplePrice(9, 3, 11.9, null, 1000.0, "g", oneDayAgo, 1.25) // Lidl $1.19/1kg = $0.119/100g = $0.00119/g
        saveSamplePrice(9, 4, 12.5, null, 1000.0, "g", fiveDaysAgo, 1.25)
        saveSamplePrice(9, 1, 13.9, null, 1000.0, "g", twoWeeksAgo, 1.39)

        // 10. Fresh Crisp Carrots 1kg (goodId = 10)
        saveSamplePrice(10, 3, 8.9, 7.9, 1000.0, "g", oneDayAgo, 0.89) // Lidl $0.79/1kg = $0.079/100g = $0.00079/g
        saveSamplePrice(10, 4, 8.9, null, 1000.0, "g", fiveDaysAgo, 0.89)
        saveSamplePrice(10, 1, 10.5, null, 1000.0, "g", twoWeeksAgo, 1.05)

        // 11. Cane Sugar 500g (goodId = 11)
        saveSamplePrice(11, 3, 9.9, null, 500.0, "g", oneDayAgo, 1.09) // Lidl $0.99 for 500g = $0.198/100g = $0.00198/g
        saveSamplePrice(11, 2, 11.5, null, 500.0, "g", fiveDaysAgo, 1.15)
        saveSamplePrice(11, 1, 12.0, null, 500.0, "g", twoWeeksAgo, 1.20)

        // 9. Seed Sample Shopping Lists
        val weeklyListId = shoppingListDao.insertShoppingList(
            ShoppingList(
                id = 1,
                name = "Weekly Family Groceries",
                colorHex = "#1E40AF",
                targetShopId = 3 // Lidl
            )
        )
        val weekendBbqListId = shoppingListDao.insertShoppingList(
            ShoppingList(
                id = 2,
                name = "Weekend BBQ & Dinner",
                colorHex = "#16A34A",
                targetShopId = null
            )
        )

        shoppingListDao.insertItems(
            listOf(
                ShoppingListItem(listId = weeklyListId, goodId = 1, name = "Organic Whole Milk", category = "Dairy", quantity = 2.0, unit = "ml", weight = 1000.0, weightUnit = "ml", isChecked = true, estimatedUnitPrice = 1.39),
                ShoppingListItem(listId = weeklyListId, goodId = 2, name = "Free Range Eggs 12pk", category = "Dairy", quantity = 1.0, unit = "pcs", weight = 680.0, weightUnit = "g", isChecked = false, estimatedUnitPrice = 2.89),
                ShoppingListItem(listId = weeklyListId, goodId = 3, name = "Italian Spaghetti", category = "Pantry", quantity = 2.0, unit = "g", weight = 500.0, weightUnit = "g", isChecked = false, estimatedUnitPrice = 0.79),
                ShoppingListItem(listId = weeklyListId, goodId = 6, name = "Fresh Cavendish Bananas", category = "Fruits & Veg", quantity = 1.0, unit = "g", weight = 1000.0, weightUnit = "g", isChecked = false, estimatedUnitPrice = 1.09),
                ShoppingListItem(listId = weekendBbqListId, goodId = 7, name = "Fresh Atlantic Salmon Fillet", category = "Meat & Fish", quantity = 3.0, unit = "g", weight = 400.0, weightUnit = "g", isChecked = false, estimatedUnitPrice = 4.99),
                ShoppingListItem(listId = weekendBbqListId, goodId = 8, name = "Sourdough Bread Loaf", category = "Bakery", quantity = 1.0, unit = "g", weight = 600.0, weightUnit = "g", isChecked = true, estimatedUnitPrice = 1.65)
            )
        )
    }

    private suspend fun saveSamplePrice(
        goodId: Long,
        shopId: Long,
        currentRegular: Double,
        currentDiscount: Double?,
        amount: Double,
        unit: String,
        currentTimestamp: Long,
        pastPrice: Double
    ) {
        val (pastUnit, _) = UnitPriceCalculator.calculateUnitPrice(pastPrice, amount, unit)
        val pastHistory = PriceHistory(
            goodId = goodId,
            shopId = shopId,
            regularPrice = pastPrice,
            discountPrice = null,
            packageAmount = amount,
            packageUnit = unit,
            pricePerUnit = pastUnit,
            note = "Previous recorded price",
            currencyCode = AppCurrency.CZK.code,
            recordedAt = currentTimestamp - 86400000L * 7
        )
        priceHistoryDao.insertHistory(pastHistory)

        val currentEffective = currentDiscount ?: currentRegular
        val (unitPrice, unitLabel) = UnitPriceCalculator.calculateUnitPrice(currentEffective, amount, unit)

        val currentHistory = PriceHistory(
            goodId = goodId,
            shopId = shopId,
            regularPrice = currentRegular,
            discountPrice = currentDiscount,
            packageAmount = amount,
            packageUnit = unit,
            pricePerUnit = unitPrice,
            note = if (currentDiscount != null) "Promotional price offer" else "Regular shelf price",
            currencyCode = AppCurrency.CZK.code,
            recordedAt = currentTimestamp
        )
        priceHistoryDao.insertHistory(currentHistory)

        val record = PriceRecord(
            goodId = goodId,
            shopId = shopId,
            regularPrice = currentRegular,
            discountPrice = currentDiscount,
            packageAmount = amount,
            packageUnit = unit,
            pricePerUnit = unitPrice,
            unitMeasureLabel = unitLabel,
            isPromotion = currentDiscount != null,
            currencyCode = AppCurrency.CZK.code,
            updatedAt = currentTimestamp
        )
        priceRecordDao.insertPriceRecord(record)
    }
}
