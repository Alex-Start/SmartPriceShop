package com.example.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

enum class AppLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val flag: String
) {
    ENGLISH("en", "English", "English", "🇺🇸"),
    UKRAINIAN("uk", "Ukrainian", "Українська", "🇺🇦"),
    CZECH("cs", "Czech", "Čeština", "🇨🇿");

    companion object {
        fun fromCode(code: String): AppLanguage {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: ENGLISH
        }
    }
}

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.ENGLISH }

enum class AppCurrency(
    val code: String,
    val symbol: String,
    val displayName: String,
    val flag: String,
    val isSuffix: Boolean = false
) {
    USD("USD", "$", "USD ($)", "🇺🇸", isSuffix = false),
    EUR("EUR", "€", "EUR (€)", "🇪🇺", isSuffix = false),
    UAH("UAH", "₴", "UAH (₴)", "🇺🇦", isSuffix = true),
    CZK("CZK", "Kč", "CZK (Kč)", "🇨🇿", isSuffix = true);

    fun format(amount: Double): String {
        return if (isSuffix) {
            String.format(java.util.Locale.US, "%.2f %s", amount, symbol)
        } else {
            String.format(java.util.Locale.US, "%s%.2f", symbol, amount)
        }
    }

    companion object {
        fun fromCode(code: String): AppCurrency {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: USD
        }
    }
}

val LocalAppCurrency = staticCompositionLocalOf { AppCurrency.USD }

object AppStrings {

    fun getCategoryName(category: String, lang: AppLanguage): String {
        return when (category) {
            "All" -> when (lang) {
                AppLanguage.ENGLISH -> "All"
                AppLanguage.UKRAINIAN -> "Всі"
                AppLanguage.CZECH -> "Vše"
            }
            "Dairy" -> when (lang) {
                AppLanguage.ENGLISH -> "Dairy"
                AppLanguage.UKRAINIAN -> "Молочні продукти"
                AppLanguage.CZECH -> "Mléčné výrobky"
            }
            "Fruits & Veg" -> when (lang) {
                AppLanguage.ENGLISH -> "Fruits & Veg"
                AppLanguage.UKRAINIAN -> "Овочі та фрукти"
                AppLanguage.CZECH -> "Ovoce a zelenina"
            }
            "Meat & Fish" -> when (lang) {
                AppLanguage.ENGLISH -> "Meat & Fish"
                AppLanguage.UKRAINIAN -> "М'ясо та риба"
                AppLanguage.CZECH -> "Maso a ryby"
            }
            "Bakery" -> when (lang) {
                AppLanguage.ENGLISH -> "Bakery"
                AppLanguage.UKRAINIAN -> "Випічка"
                AppLanguage.CZECH -> "Pečivo"
            }
            "Beverages" -> when (lang) {
                AppLanguage.ENGLISH -> "Beverages"
                AppLanguage.UKRAINIAN -> "Напої"
                AppLanguage.CZECH -> "Nápoje"
            }
            "Pantry" -> when (lang) {
                AppLanguage.ENGLISH -> "Pantry"
                AppLanguage.UKRAINIAN -> "Бакалія"
                AppLanguage.CZECH -> "Trvanlivé potraviny"
            }
            "Snacks" -> when (lang) {
                AppLanguage.ENGLISH -> "Snacks"
                AppLanguage.UKRAINIAN -> "Снеки та солодощі"
                AppLanguage.CZECH -> "Pochutiny"
            }
            "Household" -> when (lang) {
                AppLanguage.ENGLISH -> "Household"
                AppLanguage.UKRAINIAN -> "Побутова хімія"
                AppLanguage.CZECH -> "Domácnost"
            }
            "Other" -> when (lang) {
                AppLanguage.ENGLISH -> "Other"
                AppLanguage.UKRAINIAN -> "Інше"
                AppLanguage.CZECH -> "Ostatní"
            }
            else -> category
        }
    }

    fun appName(lang: AppLanguage) = "Smart Price"

    fun tabCompare(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Compare"
        AppLanguage.UKRAINIAN -> "Порівняти"
        AppLanguage.CZECH -> "Porovnat"
    }

    fun tabShoppingLists(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Shopping Lists"
        AppLanguage.UKRAINIAN -> "Списки покупок"
        AppLanguage.CZECH -> "Nákupní seznamy"
    }

    fun tabScanner(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Scanner"
        AppLanguage.UKRAINIAN -> "Сканер"
        AppLanguage.CZECH -> "Čtečka"
    }

    fun searchPlaceholder(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Search goods, barcode or store..."
        AppLanguage.UKRAINIAN -> "Пошук товарів, штрихкоду чи магазину..."
        AppLanguage.CZECH -> "Hledat zboží, čárový kód nebo obchod..."
    }

    fun allShops(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "All Shops"
        AppLanguage.UKRAINIAN -> "Всі магазини"
        AppLanguage.CZECH -> "Všechny obchody"
    }

    fun dealsOnly(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Deals Only"
        AppLanguage.UKRAINIAN -> "Тільки знижки"
        AppLanguage.CZECH -> "Pouze akce"
    }

    fun cheapestOptionsFound(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "CHEAPEST OPTIONS FOUND"
        AppLanguage.UKRAINIAN -> "ЗНАЙДЕНО НАЙДЕШЕВШІ ВАРІАНТИ"
        AppLanguage.CZECH -> "NALEZENY NEJLEVNĚJŠÍ MOŽNOSTI"
    }

    fun itemsCountSuffix(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "ITEMS"
        AppLanguage.UKRAINIAN -> "ТОВАРІВ"
        AppLanguage.CZECH -> "POLOŽEK"
    }

    fun storesCountSuffix(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "STORES"
        AppLanguage.UKRAINIAN -> "МАГАЗИНІВ"
        AppLanguage.CZECH -> "OBCHODŮ"
    }

    fun noProductsFound(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "No products match your filters"
        AppLanguage.UKRAINIAN -> "Не знайдено товарів за вашим фільтром"
        AppLanguage.CZECH -> "Žádné produkty neodpovídají filtrům"
    }

    fun noGoodsRecorded(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "No goods or prices recorded yet"
        AppLanguage.UKRAINIAN -> "Ще немає записаних товарів чи цін"
        AppLanguage.CZECH -> "Zatím žádné zaznamenané produkty ani ceny"
    }

    fun addProductsHint(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Add products to compare prices across supermarkets"
        AppLanguage.UKRAINIAN -> "Додавайте товари для порівняння цін між супермаркетами"
        AppLanguage.CZECH -> "Přidejte produkty a porovnávejte ceny mezi supermarkety"
    }

    fun addFirstProduct(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Add First Product"
        AppLanguage.UKRAINIAN -> "Додати перший товар"
        AppLanguage.CZECH -> "Přidat první produkt"
    }

    fun logPrice(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Log Price"
        AppLanguage.UKRAINIAN -> "Записати ціну"
        AppLanguage.CZECH -> "Zapsat cenu"
    }

    fun supermarkets(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Supermarkets"
        AppLanguage.UKRAINIAN -> "Супермаркети"
        AppLanguage.CZECH -> "Supermarkety"
    }

    fun backupAndRestore(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Backup & Restore"
        AppLanguage.UKRAINIAN -> "Резервне копіювання"
        AppLanguage.CZECH -> "Záloha a obnovení"
    }

    fun selectLanguage(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Select Language"
        AppLanguage.UKRAINIAN -> "Вибір мови"
        AppLanguage.CZECH -> "Vybrat jazyk"
    }

    fun chooseLanguagePrompt(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Choose application language"
        AppLanguage.UKRAINIAN -> "Оберіть мову інтерфейсу застосунку"
        AppLanguage.CZECH -> "Zvolte jazyk rozhraní aplikace"
    }

    fun bestDeal(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Best Deal"
        AppLanguage.UKRAINIAN -> "Краща ціна"
        AppLanguage.CZECH -> "Nejlepší cena"
    }

    fun priceUp(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Price Up"
        AppLanguage.UKRAINIAN -> "Ціна зросла"
        AppLanguage.CZECH -> "Cena vzrostla"
    }

    fun prevPrefix(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Prev: "
        AppLanguage.UKRAINIAN -> "Попер: "
        AppLanguage.CZECH -> "Předch: "
    }

    fun addPrice(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Add Price"
        AppLanguage.UKRAINIAN -> "Додати ціну"
        AppLanguage.CZECH -> "Přidat cenu"
    }

    fun addToList(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Add to List"
        AppLanguage.UKRAINIAN -> "До списку"
        AppLanguage.CZECH -> "Do seznamu"
    }

    fun viewHistory(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "View History"
        AppLanguage.UKRAINIAN -> "Переглянути історію"
        AppLanguage.CZECH -> "Zobrazit historii"
    }

    fun savePrice(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Save Price"
        AppLanguage.UKRAINIAN -> "Зберегти ціну"
        AppLanguage.CZECH -> "Uložit cenu"
    }

    fun cancel(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Cancel"
        AppLanguage.UKRAINIAN -> "Скасувати"
        AppLanguage.CZECH -> "Zrušit"
    }

    fun close(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Close"
        AppLanguage.UKRAINIAN -> "Закрити"
        AppLanguage.CZECH -> "Zavřít"
    }

    fun delete(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Delete"
        AppLanguage.UKRAINIAN -> "Видалити"
        AppLanguage.CZECH -> "Smazat"
    }

    fun edit(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Edit"
        AppLanguage.UKRAINIAN -> "Редагувати"
        AppLanguage.CZECH -> "Upravit"
    }

    fun productName(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Product Name"
        AppLanguage.UKRAINIAN -> "Назва товару"
        AppLanguage.CZECH -> "Název produktu"
    }

    fun categoryLabel(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Category"
        AppLanguage.UKRAINIAN -> "Категорія"
        AppLanguage.CZECH -> "Kategorie"
    }

    fun weightOrVolume(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Weight / Volume"
        AppLanguage.UKRAINIAN -> "Вага / Об'єм"
        AppLanguage.CZECH -> "Hmotnost / Objem"
    }

    fun weightUnit(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Unit"
        AppLanguage.UKRAINIAN -> "Одиниця"
        AppLanguage.CZECH -> "Jednotka"
    }

    fun barcodeLabel(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Barcode (EAN/UPC)"
        AppLanguage.UKRAINIAN -> "Штрихкод (EAN/UPC)"
        AppLanguage.CZECH -> "Čárový kód (EAN/UPC)"
    }

    fun supermarketLabel(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Supermarket"
        AppLanguage.UKRAINIAN -> "Супермаркет"
        AppLanguage.CZECH -> "Supermarket"
    }

    fun addressLabel(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Address / Branch"
        AppLanguage.UKRAINIAN -> "Адреса / Філія"
        AppLanguage.CZECH -> "Adresa / Pobočka"
    }

    fun regularPriceLabel(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Regular Price"
        AppLanguage.UKRAINIAN -> "Звичайна ціна"
        AppLanguage.CZECH -> "Běžná cena"
    }

    fun discountPriceLabel(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Discount Price (Optional)"
        AppLanguage.UKRAINIAN -> "Акційна ціна (необов'язково)"
        AppLanguage.CZECH -> "Akční cena (volitelné)"
    }

    fun isPromoCheckbox(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Mark as Promotion / Special Offer"
        AppLanguage.UKRAINIAN -> "Позначити як акцію / спеціальну пропозицію"
        AppLanguage.CZECH -> "Označit jako akci / speciální nabídku"
    }

    fun notesLabel(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Notes (Optional)"
        AppLanguage.UKRAINIAN -> "Примітка (необов'язково)"
        AppLanguage.CZECH -> "Poznámka (volitelné)"
    }

    fun pricePerGramBadge(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Price / 100g"
        AppLanguage.UKRAINIAN -> "Ціна / 100г"
        AppLanguage.CZECH -> "Cena / 100g"
    }

    fun sortLabel(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Sort"
        AppLanguage.UKRAINIAN -> "Сортування"
        AppLanguage.CZECH -> "Řazení"
    }

    fun scanBarcode(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Scan Barcode"
        AppLanguage.UKRAINIAN -> "Сканувати штрихкод"
        AppLanguage.CZECH -> "Skenovat čárový kód"
    }

    fun aiCameraScanner(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "AI Price Tag Scanner"
        AppLanguage.UKRAINIAN -> "AI Сканер цінників"
        AppLanguage.CZECH -> "AI Skener cenovek"
    }

    fun manageStores(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Manage Supermarkets"
        AppLanguage.UKRAINIAN -> "Керування супермаркетами"
        AppLanguage.CZECH -> "Správa supermarketů"
    }

    fun addStore(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Add Supermarket"
        AppLanguage.UKRAINIAN -> "Додати супермаркет"
        AppLanguage.CZECH -> "Přidat supermarket"
    }

    fun storeName(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Store Name"
        AppLanguage.UKRAINIAN -> "Назва магазину"
        AppLanguage.CZECH -> "Název obchodu"
    }

    fun storeAddress(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Address / Location"
        AppLanguage.UKRAINIAN -> "Адреса / Розташування"
        AppLanguage.CZECH -> "Adresa / Umístění"
    }

    fun newList(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "New List"
        AppLanguage.UKRAINIAN -> "Новий список"
        AppLanguage.CZECH -> "Nový seznam"
    }

    fun createListTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Create Shopping List"
        AppLanguage.UKRAINIAN -> "Створити список покупок"
        AppLanguage.CZECH -> "Vytvořit nákupní seznam"
    }

    fun listNameLabel(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "List Name"
        AppLanguage.UKRAINIAN -> "Назва списку"
        AppLanguage.CZECH -> "Název seznamu"
    }

    fun targetStoreLabel(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Target Store (Optional)"
        AppLanguage.UKRAINIAN -> "Магазин (необов'язково)"
        AppLanguage.CZECH -> "Cílový obchod (volitelné)"
    }

    fun anyStoreBestPrices(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Any Store / Best Prices"
        AppLanguage.UKRAINIAN -> "Будь-який магазин / Кращі ціни"
        AppLanguage.CZECH -> "Jakýkoli obchod / Nejlepší ceny"
    }

    fun addItem(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Add Item"
        AppLanguage.UKRAINIAN -> "Додати товар"
        AppLanguage.CZECH -> "Přidat položku"
    }

    fun addFromCatalog(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Add from Catalog"
        AppLanguage.UKRAINIAN -> "Додати з каталогу"
        AppLanguage.CZECH -> "Přidat z katalogu"
    }

    fun addCustomItem(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Add Custom Item"
        AppLanguage.UKRAINIAN -> "Додати свій товар"
        AppLanguage.CZECH -> "Přidat vlastní položку"
    }

    fun estimatedTotal(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Estimated Total"
        AppLanguage.UKRAINIAN -> "Орієнтовна сума"
        AppLanguage.CZECH -> "Odhadovaný součet"
    }

    fun clearCompleted(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Clear Done"
        AppLanguage.UKRAINIAN -> "Очистити виконані"
        AppLanguage.CZECH -> "Vymazat hotové"
    }

    fun deleteListConfirm(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Delete this shopping list?"
        AppLanguage.UKRAINIAN -> "Видалити цей список покупок?"
        AppLanguage.CZECH -> "Smazat tento nákupní seznam?"
    }

    fun noItemsInList(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "No items in this list yet"
        AppLanguage.UKRAINIAN -> "У цьому списку ще немає товарів"
        AppLanguage.CZECH -> "V tomto seznamu zatím nejsou žádné položky"
    }

    fun addItemsHint(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Tap '+ Item' to add from your price database or create custom items"
        AppLanguage.UKRAINIAN -> "Натисніть '+ Товар', щоб додати з бази цін або створити власний"
        AppLanguage.CZECH -> "Klepněte na '+ Položka' pro přidání z databáze cen nebo vytvoření vlastní"
    }

    fun pointCameraAtBarcode(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Point camera at any barcode or enter manually"
        AppLanguage.UKRAINIAN -> "Наведіть камеру на штрихкод або введіть вручну"
        AppLanguage.CZECH -> "Namiřte fotoaparát na čárový kód nebo zadeйте ručně"
    }

    fun enterBarcodeManually(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Enter barcode..."
        AppLanguage.UKRAINIAN -> "Введіть штрихкод..."
        AppLanguage.CZECH -> "Zadejte čárový kód..."
    }

    fun checkBarcode(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Check"
        AppLanguage.UKRAINIAN -> "Перевірити"
        AppLanguage.CZECH -> "Zkontrolovat"
    }

    fun productFound(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Product Found!"
        AppLanguage.UKRAINIAN -> "Товар знайдено!"
        AppLanguage.CZECH -> "Produkt nalezen!"
    }

    fun barcodeNotFound(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Barcode not in database"
        AppLanguage.UKRAINIAN -> "Штрихкод відсутній у базі"
        AppLanguage.CZECH -> "Čárový kód není v databázi"
    }

    fun addNewProductWithBarcode(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Add New Product with this Barcode"
        AppLanguage.UKRAINIAN -> "Додати новий товар з цим штрихкодом"
        AppLanguage.CZECH -> "Přidat nový produkt s tímto čárovým kódem"
    }

    fun sampleBarcodesTest(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Sample Barcodes (Tap to Test)"
        AppLanguage.UKRAINIAN -> "Зразки штрихкодів (натисніть для перевірки)"
        AppLanguage.CZECH -> "Ukázkové čárové kódy (klepněte pro test)"
    }

    fun cameraPermissionRequired(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Camera Permission Required"
        AppLanguage.UKRAINIAN -> "Потрібен доступ до камери"
        AppLanguage.CZECH -> "Je vyžadován přístup k fotoaparátu"
    }

    fun grantPermission(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Grant Permission"
        AppLanguage.UKRAINIAN -> "Надати дозвіл"
        AppLanguage.CZECH -> "Udělit oprávnění"
    }

    fun exportJson(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Export Backup (JSON)"
        AppLanguage.UKRAINIAN -> "Експорт резервної копії (JSON)"
        AppLanguage.CZECH -> "Export zálohy (JSON)"
    }

    fun importJson(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Import Backup (JSON)"
        AppLanguage.UKRAINIAN -> "Імпорт резервної копії (JSON)"
        AppLanguage.CZECH -> "Import zálohy (JSON)"
    }

    fun resetAllData(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Reset All Data"
        AppLanguage.UKRAINIAN -> "Скинути всі дані"
        AppLanguage.CZECH -> "Resetovat všechna data"
    }

    fun reloadSampleData(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Reload Sample Data"
        AppLanguage.UKRAINIAN -> "Перезавантажити зразкові дані"
        AppLanguage.CZECH -> "Znovu načíst ukázková data"
    }

    fun copyToClipboard(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Copy JSON to Clipboard"
        AppLanguage.UKRAINIAN -> "Копіювати JSON в буфер"
        AppLanguage.CZECH -> "Kopírovat JSON do schránky"
    }

    fun copied(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Copied to clipboard!"
        AppLanguage.UKRAINIAN -> "Скопійовано в буфер!"
        AppLanguage.CZECH -> "Zkopírováno do schránky!"
    }

    fun priceHistoryTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Price History Chart"
        AppLanguage.UKRAINIAN -> "Графік історії цін"
        AppLanguage.CZECH -> "Graf historie cen"
    }

    fun priceComparisonTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Supermarket Price Comparison"
        AppLanguage.UKRAINIAN -> "Порівняння цін у супермаркетах"
        AppLanguage.CZECH -> "Porovnání cen v supermarketech"
    }

    // ==================== CATEGORY MANAGER ====================

    fun manageCategories(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Manage Categories"
        AppLanguage.UKRAINIAN -> "Керування категоріями"
        AppLanguage.CZECH -> "Správa kategorií"
    }

    fun addCategory(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Add Category"
        AppLanguage.UKRAINIAN -> "Додати категорію"
        AppLanguage.CZECH -> "Přidat kategorii"
    }

    fun editCategory(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Edit Category"
        AppLanguage.UKRAINIAN -> "Редагувати категорію"
        AppLanguage.CZECH -> "Upravit kategorii"
    }

    fun categoryName(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Category Name"
        AppLanguage.UKRAINIAN -> "Назва категорії"
        AppLanguage.CZECH -> "Název kategorie"
    }

    fun categoryNameHint(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "e.g. Frozen Foods, Pet Care, Organic"
        AppLanguage.UKRAINIAN -> "напр. Заморожені продукти, Зоотовари, Органіка"
        AppLanguage.CZECH -> "např. Mražené potraviny, Mazlíčci, Bio"
    }

    fun deleteCategoryConfirm(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Delete this category?"
        AppLanguage.UKRAINIAN -> "Видалити цю категорію?"
        AppLanguage.CZECH -> "Smazat tuto kategorii?"
    }

    fun defaultCategoryBadge(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Default"
        AppLanguage.UKRAINIAN -> "Стандартна"
        AppLanguage.CZECH -> "Výchozí"
    }

    fun customCategoryBadge(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Custom"
        AppLanguage.UKRAINIAN -> "Власна"
        AppLanguage.CZECH -> "Vlastní"
    }

    fun selectColor(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Select Color"
        AppLanguage.UKRAINIAN -> "Оберіть колір"
        AppLanguage.CZECH -> "Vyberte barvu"
    }

    fun manageCategoriesHint(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Create custom categories with personalized colors to organize your groceries and prices."
        AppLanguage.UKRAINIAN -> "Створюйте власні категорії з персоналізованими кольорами для впорядкування покупок і цін."
        AppLanguage.CZECH -> "Vytvářejte vlastní kategorie s barvami pro snadnou organizaci nákupů a cen."
    }

    fun manageCategoriesBtn(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Categories"
        AppLanguage.UKRAINIAN -> "Категорії"
        AppLanguage.CZECH -> "Kategorie"
    }

    // ==================== PARAMETER TOOLTIPS & HELPERS ====================

    fun editPriceTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Edit Price Item"
        AppLanguage.UKRAINIAN -> "Редагувати ціну товару"
        AppLanguage.CZECH -> "Upravit cenu položky"
    }

    fun editPriceItem(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Edit Price"
        AppLanguage.UKRAINIAN -> "Редагувати ціну"
        AppLanguage.CZECH -> "Upravit cenu"
    }

    fun saveChanges(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Save Changes"
        AppLanguage.UKRAINIAN -> "Зберегти зміни"
        AppLanguage.CZECH -> "Uložit změny"
    }

    fun addGoodsTitle(isEdit: Boolean, lang: AppLanguage) = if (isEdit) {
        when (lang) {
            AppLanguage.ENGLISH -> "Edit Product & Price"
            AppLanguage.UKRAINIAN -> "Редагувати товар і ціну"
            AppLanguage.CZECH -> "Upravit produkt a cenu"
        }
    } else {
        when (lang) {
            AppLanguage.ENGLISH -> "Add Product & Price"
            AppLanguage.UKRAINIAN -> "Додати товар і ціну"
            AppLanguage.CZECH -> "Přidat produkt a cenu"
        }
    }

    fun tooltipProductName(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Enter the clear brand or item name (e.g. Organic Whole Milk 1L, Barilla Spaghetti #5)."
        AppLanguage.UKRAINIAN -> "Введіть зрозумілу назву бренду або товару (напр. Молоко органічне 1л, Спагеті Barilla №5)."
        AppLanguage.CZECH -> "Zadejte přesný název značky nebo produktu (např. Plnotučné mléko 1l, Špagety Barilla č. 5)."
    }

    fun tooltipCategory(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Group items into categories for fast filtering, catalog organization, and grocery budgeting."
        AppLanguage.UKRAINIAN -> "Групуйте товари за категоріями для швидкої фільтрації, впорядкування каталогу та планування бюджету."
        AppLanguage.CZECH -> "Seskupujte položky do kategorií pro rychlé filtrování, organizaci a plánování rozpočtu."
    }

    fun tooltipWeight(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Net package weight or volume (e.g. 500g, 1.0kg, 1000ml). This enables automatic unit price calculation per 100g/1kg/1L for fair comparisons."
        AppLanguage.UKRAINIAN -> "Вага нетто або об'єм упаковки (напр. 500г, 1.0кг, 1000мл). Це дозволяє автоматично розраховувати ціну за 100г/1кг/1л для чесного порівняння."
        AppLanguage.CZECH -> "Čistá hmotnost nebo objem balení (např. 500g, 1.0kg, 1000ml). Umožňuje automatický výpočet jednotkové ceny za 100g/1kg/1l pro férové porovnání."
    }

    fun tooltipUnit(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Select the measurement unit: grams (g), kilograms (kg), milliliters (ml), liters (L), or pieces (pcs)."
        AppLanguage.UKRAINIAN -> "Оберіть одиницю вимірювання: грами (г), кілограми (кг), мілілітри (мл), літри (л) або штуки (шт)."
        AppLanguage.CZECH -> "Vyberte měrnou jednotku: gramy (g), kilogramy (kg), mililitry (ml), litry (l) nebo kusy (ks)."
    }

    fun tooltipBarcode(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Optional barcode (EAN-13, UPC, etc.) to quickly find and compare this product in-store using the camera scanner."
        AppLanguage.UKRAINIAN -> "Необов'язковий штрихкод (EAN-13, UPC тощо) для швидкого пошуку та порівняння товару в магазині за допомогою сканера."
        AppLanguage.CZECH -> "Volitelný čárový kód (EAN-13, UPC atd.) pro rychlé vyhledání a porovnání produktu v obchodě pomocí skeneru."
    }

    fun tooltipSupermarket(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Supermarket where this price was recorded (e.g. Lidl, Tesco, Aldi, Walmart, Carrefour)."
        AppLanguage.UKRAINIAN -> "Супермаркет, де зафіксовано ціну (напр. Сільпо, АТБ, Novus, Ашан, Фора)."
        AppLanguage.CZECH -> "Supermarket, kde byla cena zjištěna (např. Lidl, Tesco, Albert, Kaufland, Billa)."
    }

    fun tooltipAddress(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Optional store branch, address, or neighborhood to distinguish different store locations."
        AppLanguage.UKRAINIAN -> "Необов'язкова адреса чи філія магазину для розрізнення конкретних торгових точок."
        AppLanguage.CZECH -> "Volitelná pobočka nebo adresa prodejny pro rozlišení konkrétních míst."
    }

    fun tooltipRegularPrice(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Standard shelf retail price before any discounts, promotions, or loyalty card deductions."
        AppLanguage.UKRAINIAN -> "Звичайна роздрібна полицева ціна без знижок, акцій чи карт лояльності."
        AppLanguage.CZECH -> "Běžná maloobchodní cena na regálu bez slev, akcí nebo věrnostních slev."
    }

    fun tooltipDiscountPrice(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Discounted / Clubcard promotional price. If entered, the app calculates exact savings and marks the item with a discount badge."
        AppLanguage.UKRAINIAN -> "Акційна ціна або ціна за картою лояльності. Додаток автоматично розрахує відсоток економії та покаже бейдж знижки."
        AppLanguage.CZECH -> "Akční cena nebo cena s věrnostní kartou. Aplikace automaticky spočítá úsporu a označí položku slevovým štítkem."
    }

    fun tooltipIsPromotion(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Mark this item as an active promotional deal to easily find it in the 'Deals Only' filter tab."
        AppLanguage.UKRAINIAN -> "Позначте цей товар як акційну пропозицію, щоб швидко знаходити його у фільтрі 'Тільки знижки'."
        AppLanguage.CZECH -> "Označte tuto položku jako akční nabídku, abyste ji snadno našli ve filtru 'Pouze akce'."
    }

    fun tooltipNotes(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Helpful price tag details like 'Valid until Friday', 'Clubcard price', 'Buy 2 get 1 free', or brand variation."
        AppLanguage.UKRAINIAN -> "Корисні деталі з цінника, наприклад 'Діє до п'ятниці', 'Ціна з карткою', '1+1=3' або сорт."
        AppLanguage.CZECH -> "Užitečné poznámky z cenovky, např. 'Platí do pátku', 'Cena s aplikací', '2+1 zdarma' nebo varianta."
    }

    fun tooltipUnitPrice(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Standardized unit price calculated automatically (e.g. $ per 100g, per 1kg, per 1L) to reveal true value regardless of pack size."
        AppLanguage.UKRAINIAN -> "Стандартизована ціна за одиницю (напр. ₴ за 100г, за 1кг, за 1л), що дозволяє чесно бачити вигоду незалежно від розміру пачки."
        AppLanguage.CZECH -> "Standardizovaná jednotková cena vypočtená automaticky (např. Kč za 100g, za 1kg, za 1l) pro objektivní porovnání různých balení."
    }

    // ==================== AI SCANNER STRINGS ====================

    fun aiScannerSubtitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Snap a photo of any price tag or shelf label to automatically extract product name, price, discount, weight, and store"
        AppLanguage.UKRAINIAN -> "Сфотографуйте будь-який цінник на полиці, і AI автоматично розпізнає назву, ціну, знижку, вагу та магазин"
        AppLanguage.CZECH -> "Vyfoťte libovolnou cenovku a AI automaticky rozpozná název produktu, cenu, slevu, hmotnost a obchod"
    }

    fun takePhotoBtn(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Take Photo"
        AppLanguage.UKRAINIAN -> "Зробити фото"
        AppLanguage.CZECH -> "Vyfotit"
    }

    fun chooseGalleryBtn(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Gallery"
        AppLanguage.UKRAINIAN -> "Галерея"
        AppLanguage.CZECH -> "Galerie"
    }

    fun scanningProcessing(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Gemini AI is analyzing price tag..."
        AppLanguage.UKRAINIAN -> "Gemini AI аналізує цінник..."
        AppLanguage.CZECH -> "Gemini AI analyzuje cenovku..."
    }

    fun aiExtractionSuccess(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Price Tag Recognized!"
        AppLanguage.UKRAINIAN -> "Цінник розпізнано!"
        AppLanguage.CZECH -> "Cenovka rozpoznána!"
    }

    fun applyScannedData(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Fill In Product Form"
        AppLanguage.UKRAINIAN -> "Заповнити форму товару"
        AppLanguage.CZECH -> "Vyplnit formulář produktu"
    }

    fun photoCaptureError(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Could not capture photo. Please check camera permissions or select an image from gallery."
        AppLanguage.UKRAINIAN -> "Не вдалося отримати фото з камери. Перевірте дозволи або оберіть зображення з галереї."
        AppLanguage.CZECH -> "Nepodařilo se pořídit fotografii. Zkontrolujte oprávnění fotoaparátu nebo vyberte snímek z galerie."
    }

    fun scanAnotherPhoto(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Scan Another"
        AppLanguage.UKRAINIAN -> "Сканувати інше"
        AppLanguage.CZECH -> "Skenovat další"
    }

    // ==================== LOG PRICE & DETAILS LOCALIZATION ====================

    fun logPriceTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Log Price"
        AppLanguage.UKRAINIAN -> "Записати ціну"
        AppLanguage.CZECH -> "Zapsat cenu"
    }

    fun standardizedUnitPrice(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Standardized Unit Price:"
        AppLanguage.UKRAINIAN -> "Стандартизована ціна:"
        AppLanguage.CZECH -> "Standardizovaná cena:"
    }

    fun pricePerUnit(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Price per unit:"
        AppLanguage.UKRAINIAN -> "Ціна за одиницю:"
        AppLanguage.CZECH -> "Cena za jednotku:"
    }

    fun productNamePlaceholder(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "e.g. Organic Whole Milk 1L"
        AppLanguage.UKRAINIAN -> "напр. Молоко органічне 1л"
        AppLanguage.CZECH -> "např. Plnotučné mléko 1l"
    }

    fun barcodePlaceholder(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "e.g. 5000128741001"
        AppLanguage.UKRAINIAN -> "напр. 5000128741001"
        AppLanguage.CZECH -> "např. 5000128741001"
    }

    fun supermarketPlaceholder(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "e.g. Tesco, Lidl, Walmart, Aldi, Billa"
        AppLanguage.UKRAINIAN -> "напр. Сільпо, АТБ, Novus, Ашан, Фора"
        AppLanguage.CZECH -> "např. Tesco, Lidl, Albert, Kaufland, Billa"
    }

    fun addressPlaceholder(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "e.g. 10 High Street / Downtown"
        AppLanguage.UKRAINIAN -> "напр. вул. Хрещатик, 1 / Центр"
        AppLanguage.CZECH -> "např. Hlavní 10 / Centrum"
    }

    fun notesPlaceholder(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "e.g. Clubcard deal, Valid until Sunday"
        AppLanguage.UKRAINIAN -> "напр. Ціна за карткою, Акція до неділі"
        AppLanguage.CZECH -> "např. Akce s kartou, Platí do neděle"
    }

    fun photoLabel(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Photo"
        AppLanguage.UKRAINIAN -> "Фото"
        AppLanguage.CZECH -> "Foto"
    }

    fun ok(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "OK"
        AppLanguage.UKRAINIAN -> "Зрозуміло"
        AppLanguage.CZECH -> "Rozumím"
    }

    fun supermarketPrices(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "SUPERMARKET PRICES"
        AppLanguage.UKRAINIAN -> "ЦІНИ В СУПЕРМАРКЕТАХ"
        AppLanguage.CZECH -> "CENY V SUPERMARKETECH"
    }

    fun storesCount(count: Int, lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "$count ${if (count == 1) "STORE" else "STORES"}"
        AppLanguage.UKRAINIAN -> "$count ${if (count == 1) "МАГАЗИН" else if (count in 2..4) "МАГАЗИНИ" else "МАГАЗИНІВ"}"
        AppLanguage.CZECH -> "$count ${if (count == 1) "OBCHOD" else if (count in 2..4) "OBCHODY" else "OBCHODŮ"}"
    }

    fun noPricesRecordedYet(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "No prices recorded for this product yet. Tap 'Log Price' above."
        AppLanguage.UKRAINIAN -> "Для цього товару ще немає цін. Натисніть 'Записати ціну' вище."
        AppLanguage.CZECH -> "Pro tento produkt zatím nejsou ceny. Klepněte na 'Zapsat cenu' výše."
    }

    fun priceHistoryAndStats(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "PRICE HISTORY & STATS"
        AppLanguage.UKRAINIAN -> "ІСТОРІЯ ЦІН ТА СТАТИСТИКА"
        AppLanguage.CZECH -> "HISTORIE CEN A STATISTIKY"
    }

    fun clearHistory(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Clear History"
        AppLanguage.UKRAINIAN -> "Очистити історію"
        AppLanguage.CZECH -> "Vymazat historii"
    }

    fun deleteProductTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Delete Product?"
        AppLanguage.UKRAINIAN -> "Видалити товар?"
        AppLanguage.CZECH -> "Smazat produkt?"
    }

    fun deleteProductConfirmMsg(name: String, lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Are you sure you want to delete '$name' and all its recorded prices and history?"
        AppLanguage.UKRAINIAN -> "Ви впевнені, що хочете видалити '$name' та всі записані ціни й історію?"
        AppLanguage.CZECH -> "Opravdu chcete smazat '$name' a všechny jeho zaznamenané ceny i historii?"
    }

    fun clearHistoryTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Clear Price History?"
        AppLanguage.UKRAINIAN -> "Очистити історію цін?"
        AppLanguage.CZECH -> "Vymazat historii cen?"
    }

    fun clearHistoryMsg(name: String, lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "This will remove all historical price log entries for '$name', keeping only the current active prices."
        AppLanguage.UKRAINIAN -> "Це видалить усі історичні записи цін для '$name', залишивши лише поточні актуальні ціни."
        AppLanguage.CZECH -> "Tímto odstraníte všechny historické záznamy cen pro '$name' a ponecháte pouze aktuální aktivní ceny."
    }

    fun offBadge(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Off"
        AppLanguage.UKRAINIAN -> "Знижка"
        AppLanguage.CZECH -> "Sleva"
    }

    fun prevPrice(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Prev:"
        AppLanguage.UKRAINIAN -> "Попер:"
        AppLanguage.CZECH -> "Předch:"
    }

    fun noPricesRecorded(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "No prices recorded"
        AppLanguage.UKRAINIAN -> "Немає записів цін"
        AppLanguage.CZECH -> "Žádné záznamy cen"
    }

    fun clear(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Clear"
        AppLanguage.UKRAINIAN -> "Очистити"
        AppLanguage.CZECH -> "Vymazat"
    }

    fun cheapestBadge(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "CHEAPEST"
        AppLanguage.UKRAINIAN -> "НАЙДЕШЕВШЕ"
        AppLanguage.CZECH -> "NEJLEVNĚJŠÍ"
    }

    fun unitPrefix(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Unit:"
        AppLanguage.UKRAINIAN -> "Од:"
        AppLanguage.CZECH -> "Jedn:"
    }

    fun vsPrevious(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "vs previous"
        AppLanguage.UKRAINIAN -> "від попередньої"
        AppLanguage.CZECH -> "oproti předchozí"
    }

    fun updatedDate(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Updated"
        AppLanguage.UKRAINIAN -> "Оновлено"
        AppLanguage.CZECH -> "Aktualizováno"
    }

    fun noPricesYet(lang: AppLanguage) = noPricesRecordedYet(lang)

    fun priceHistoryStats(lang: AppLanguage) = priceHistoryAndStats(lang)

    fun deleteProductConfirm(lang: AppLanguage) = deleteProductTitle(lang)

    fun deleteProductMsg(name: String, lang: AppLanguage) = deleteProductConfirmMsg(name, lang)

    fun clearHistoryConfirm(lang: AppLanguage) = clearHistoryTitle(lang)

    fun moreActionsMenu(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "More options"
        AppLanguage.UKRAINIAN -> "Додаткові дії"
        AppLanguage.CZECH -> "Další možnosti"
    }

    fun language(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Language"
        AppLanguage.UKRAINIAN -> "Мова"
        AppLanguage.CZECH -> "Jazyk"
    }

    fun currency(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Currency"
        AppLanguage.UKRAINIAN -> "Валюта"
        AppLanguage.CZECH -> "Měna"
    }

    fun aiScannerMenuSubtitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Camera OCR & Price Detection"
        AppLanguage.UKRAINIAN -> "Камера розпізнавання цін"
        AppLanguage.CZECH -> "Fotoaparát pro čtení cenovek"
    }

    fun categoriesSubtitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Custom categories & color tags"
        AppLanguage.UKRAINIAN -> "Власні категорії та мітки"
        AppLanguage.CZECH -> "Vlastní kategorie a štítky"
    }

    fun supermarketsSubtitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Stores, branches & color badges"
        AppLanguage.UKRAINIAN -> "Магазини, адреси та мітки"
        AppLanguage.CZECH -> "Obchody, adresy a štítky"
    }

    fun backupSubtitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Export & import JSON data"
        AppLanguage.UKRAINIAN -> "Експорт та імпорт даних JSON"
        AppLanguage.CZECH -> "Export a import dat JSON"
    }
}
