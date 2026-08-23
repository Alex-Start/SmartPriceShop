# Smart Price — Supermarket Price Comparison & Tracker

> **Status:** Draft Beta (v1.0.0-beta.1) — Ready for Testing & Next AI Iteration  
> **Platform:** Android (API 24+ / Android 7.0 to Android 15)  
> **Tech Stack:** Kotlin, Jetpack Compose, Material Design 3, Room Database, Gemini 2.5 Flash API, CameraX  
> **Application ID:** `com.aistudio.supermarketprices.kpmxqy`

---

## 1. App Description

**Smart Price** is an offline-first Android utility designed to help shoppers track, compare, and optimize grocery purchases across multiple supermarkets (e.g., Tesco, Lidl, Billa, Albert, Kaufland, Walmart). 

It automatically calculates and normalizes standard unit prices (cost per 100g, 1kg, 100ml, 1L, or per unit) to uncover genuine discounts versus misleading packaging. It also features a camera-based **AI Price Tag & Product Scanner** powered by Google Gemini 2.5 Flash to automatically detect product names, categories, stores, regular prices, club/promotional discounts, and package weights from physical shelf tags.

---

## 2. Requirements & Feature Matrix

| ID | Module / Feature | Description | Status | Verification Criteria |
|:---|:---|:---|:---:|:---|
| **REQ-01** | **Multi-Store Price Comparison** | Compare identical or similar goods across different supermarkets with unit price normalization. | ✅ Implemented | Highlights cheapest store with green deal badge; shows difference in % and currency. |
| **REQ-02** | **Normalized Unit Price Engine** | Computes price per standard unit (per 100g, 1kg, 100ml, 1L, or piece) regardless of package size discrepancies. | ✅ Implemented | Mathematically converts net weights and fluid volumes reliably. |
| **REQ-03** | **Product & Price Catalog** | View goods grouped by category, filter by search query, supermarket, or sort by lowest price, name, or recency. | ✅ Implemented | Interactive search bar, category chips, and filter sorting drawer. |
| **REQ-04** | **Add & Edit Price Records** | Dialog to record or update regular price, promotional price, supermarket, store branch, package amount, unit, and notes. | ✅ Implemented | Edit button on price cards reopens dialog pre-filled with existing data; updates Room DB. |
| **REQ-05** | **AI Price Tag Scanner** | Capture shelf price tags via camera or gallery to extract product name, supermarket, regular price, sale price, and package amount via Gemini Vision. | ✅ Implemented | Maps current shelf price directly to Regular Price; populates Add/Edit dialog seamlessly. |
| **REQ-06** | **Custom API Key Support** | Allow users to use either the pre-configured project key or supply their own free Google AI Studio key. | ✅ Implemented | Key input with persistence in SharedPreferences; fallback mechanism. |
| **REQ-07** | **Barcode Scanner** | CameraX barcode scanning to look up or link barcodes to goods for instant retrieval. | ✅ Implemented | Camera view overlay with manual and automatic barcode detection. |
| **REQ-08** | **Price History & Trends** | Record date-stamped price entries and visualize historical fluctuations per supermarket. | ✅ Implemented | Visual trend indicators (arrow up/down/flat) and history list in product detail sheet. |
| **REQ-09** | **Shopping Lists with Store Optimization** | Create shopping lists with smart price estimates and supermarket routing. | ✅ Implemented | Computes total estimated basket cost at the cheapest overall store. |
| **REQ-10** | **Supermarket Manager** | Add, edit, delete supermarkets with custom badge colors and address details. | ✅ Implemented | Full CRUD dialog with color palette selector. |
| **REQ-11** | **Category Manager** | Custom product categories with color tagging. | ✅ Implemented | Full CRUD dialog with predefined and user-created categories. |
| **REQ-12** | **Backup & Restore (JSON)** | Export database to JSON file and import/merge data on another device. | ✅ Implemented | JSON serialization with schema validation and conflict resolution. |
| **REQ-13** | **Localization (i18n)** | Full support for English, Ukrainian (Українська), and Czech (Čeština). | ✅ Implemented | Instant language switcher in top bar; localized strings for all UI elements. |
| **REQ-14** | **Multi-Currency** | Support for USD ($), EUR (€), UAH (₴), CZK (Kč), and GBP (£). | ✅ Implemented | Instant currency switcher with formatted values throughout the app. |
| **REQ-15** | **Dark Mode & Contrast** | Theme-aware layout with high-contrast text rendering on black/dark surfaces. | ✅ Implemented | M3 `onSurface` contrast compliance; distinct muted tones for tooltips/placeholders. |

---

## 3. Architecture & Technical Design

### 3.1 Architecture Overview
The application follows modern Android **MVVM (Model-View-ViewModel)** and **Clean Architecture** patterns:

```
app/src/main/java/com/example/
├── data/
│   ├── ai/
│   │   └── GeminiPriceScannerService.kt   # Gemini 2.5 Flash REST client & structured prompt
│   ├── local/
│   │   ├── AppDatabase.kt                 # Room Database definition & type converters
│   │   └── Dao.kt                         # GoodDao, ShopDao, PriceRecordDao, ShoppingListDao
│   ├── model/
│   │   ├── Good.kt                        # Good entity (name, category, barcode, image)
│   │   ├── Shop.kt                        # Shop entity (name, address, color, icon)
│   │   ├── PriceRecord.kt                 # PriceRecord entity (shopId, goodId, regular, discount)
│   │   ├── PriceHistory.kt                # PriceHistory log entity
│   │   ├── Category.kt                    # Category entity (name, color, icon)
│   │   ├── ShoppingListModels.kt          # ShoppingList and ShoppingListItem entities
│   │   └── ComparisonModels.kt            # Aggregated domain DTOs (GoodWithPrices, ShopPriceDetail)
│   └── repository/
│       └── SupermarketRepository.kt       # Single source of truth for DB operations
├── ui/
│   ├── components/
│   │   ├── PriceComparisonCard.kt         # Product card with price breakdown & best deal tag
│   │   ├── PriceHistoryChart.kt           # Canvas-based price trend visualizer
│   │   ├── BarcodeScannerView.kt          # CameraX barcode preview overlay
│   │   ├── CategoryPill.kt                # Category selection chip
│   │   └── AddToShoppingListDialog.kt     # Quick add to shopping list modal
│   ├── screens/
│   │   ├── HomeScreen.kt                  # Main screen with search, categories, list, FAB, top bar
│   │   ├── GoodDetailSheet.kt             # Bottom sheet for product details, store prices & history
│   │   ├── AddEditPriceDialog.kt          # Comprehensive add/edit product & price dialog
│   │   ├── AiPriceScannerDialog.kt        # AI camera scanner with live preview and OCR parsing
│   │   ├── ManageShopsDialog.kt           # Supermarket manager CRUD dialog
│   │   ├── ManageCategoriesDialog.kt      # Category manager CRUD dialog
│   │   ├── ShoppingListsScreen.kt         # Shopping list manager with store estimation
│   │   └── ImportExportDialog.kt          # JSON backup and restore dialog
│   └── theme/
│       ├── Color.kt                       # Material 3 color tokens & custom semantic colors
│       ├── Theme.kt                       # Dynamic/Static Light & Dark Color Schemes
│       └── Type.kt                        # Typography definitions
├── util/
│   ├── Localization.kt                    # AppLanguage & AppStrings repository (EN, UK, CS)
│   └── UnitPriceCalculator.kt             # Unit price calculation & currency formatting utilities
├── MainActivity.kt                        # Single activity entry point with edge-to-edge
└── SupermarketApplication.kt              # Application class initializing Room DB
```

---

## 4. UI & UX Design System

### 4.1 Visual Hierarchy & Styling
- **Design Standard:** Material Design 3 (M3) with dynamic color adaptability and manual dark/light theme support.
- **Color Palette:**
  - **Sapphire Primary:** `#1A73E8` (Actions, Primary FABs, Key Accents)
  - **Deal Green:** `#0D904F` / Background `#E6F4EA` (Best price badges, savings indicators)
  - **Promo Orange / Coral:** `#E37400` / `#D93025` (Discount badges, price drops)
  - **Dark Surface:** `#121316` / `#1E1F23` (True dark mode with high contrast)
  - **Text Colors:** Dynamic `MaterialTheme.colorScheme.onSurface` (White `#E2E2E6` in dark mode, `#1A1C1E` in light mode)
  - **Secondary / Tooltip Text:** `MaterialTheme.colorScheme.onSurfaceVariant` (`#8E9199`)
- **Touch Targets:** All interactive elements (icon buttons, menu items, pills) adhere to minimum `48dp x 48dp` accessibility guidelines.
- **Test Tags:** Standardized `snake_case` test tags (e.g., `topbar_menu_btn`, `edit_price_btn_<id>`, `add_price_fab`) for automated testing.

---

## 5. Gemini AI Integration Details

### 5.1 Model & Configuration
- **Model:** `gemini-2.5-flash`
- **Endpoint:** Direct REST API via `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent`
- **Output Mode:** Structured JSON (`responseMimeType: "application/json"`)
- **System Prompt Instructions:**
  - Instructs Gemini to inspect the supermarket shelf price tag.
  - Extracts main shelf price as `regularPrice`.
  - Extracts promotional/clubcard price as `discountPrice`.
  - Normalizes package weight (e.g., converts `500g` to `500.0` with unit `"g"`).
  - Identifies store brand names (Tesco, Lidl, Billa, Albert, etc.) from tag logos or text.

### 5.2 Token & Account Model
- **Built-in Key:** Sourced from `BuildConfig.GEMINI_API_KEY` (configured via AI Studio Secrets).
- **Personal Key Override:** Any user can enter their own free key from [Google AI Studio](https://aistudio.google.com/app/apikey) in the scanner settings.
- **Cost:** Free Tier (15 RPM / 1,500 RPD), no credit card required.

---

## 6. Database Schema (Room SQLite)

```sql
-- Goods table
CREATE TABLE goods (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    name TEXT NOT NULL,
    category TEXT NOT NULL,
    barcode TEXT,
    imageUri TEXT,
    weight REAL NOT NULL DEFAULT 0.0,
    weightUnit TEXT NOT NULL DEFAULT 'g',
    notes TEXT,
    createdAt INTEGER NOT NULL
);

-- Shops / Supermarkets table
CREATE TABLE shops (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    name TEXT NOT NULL,
    address TEXT NOT NULL,
    colorHex TEXT NOT NULL,
    iconName TEXT NOT NULL
);

-- Price Records table (Current active prices per shop)
CREATE TABLE price_records (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    goodId INTEGER NOT NULL,
    shopId INTEGER NOT NULL,
    regularPrice REAL NOT NULL,
    discountPrice REAL,
    packageAmount REAL NOT NULL DEFAULT 1.0,
    packageUnit TEXT NOT NULL DEFAULT 'pcs',
    isPromotion INTEGER NOT NULL DEFAULT 0,
    recordedAt INTEGER NOT NULL,
    photoUri TEXT,
    FOREIGN KEY(goodId) REFERENCES goods(id) ON DELETE CASCADE,
    FOREIGN KEY(shopId) REFERENCES shops(id) ON DELETE CASCADE
);

-- Price History table (Audit log of all price changes)
CREATE TABLE price_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    goodId INTEGER NOT NULL,
    shopId INTEGER NOT NULL,
    price REAL NOT NULL,
    isDiscount INTEGER NOT NULL DEFAULT 0,
    timestamp INTEGER NOT NULL,
    FOREIGN KEY(goodId) REFERENCES goods(id) ON DELETE CASCADE,
    FOREIGN KEY(shopId) REFERENCES shops(id) ON DELETE CASCADE
);
```

---

## 7. Setup & Build Instructions

### 7.1 Prerequisites
- Android Studio Ladybug / Meerkat or Google AI Studio Build Environment
- JDK 17+ (or JDK 11 with Gradle 8+)
- Android SDK 36 (minSdk 24)

### 7.2 Building the Project
```bash
# Compile and verify Kotlin sources
gradle :app:compileDebugKotlin

# Run JVM Unit & Robolectric Tests
gradle :app:testDebugUnitTest

# Assemble Debug APK
gradle :app:assembleDebug
```
Output APK location: `app/build/outputs/apk/debug/app-debug.apk`

---

## 8. Roadmap for Next AI Iteration

The following items are prioritized for subsequent development sessions:

1. **Receipt OCR Multi-Item Ingestion:** Expand the Gemini Scanner to parse full supermarket paper receipts into multiple price records in a single batch.
2. **Offline ML Kit Fallback:** Add on-device Google ML Kit Text Recognition fallback when internet access is unavailable.
3. **Geo-Location Store Detection:** Automatically suggest the nearest supermarket based on GPS coordinates.
4. **Inflation & Price Fluctuation Alerts:** Highlight products whose price has increased by more than X% over the last 30/90 days.
5. **Interactive Price History Graph:** Enhance the canvas chart with pinch-to-zoom and multi-store overlay lines.
