package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AiScannedProductDto
import com.example.data.model.Good
import com.example.data.model.Shop
import com.example.data.model.ShopPriceDetail
import com.example.data.model.ShoppingListWithItems
import com.example.ui.components.AddToShoppingListDialog
import com.example.ui.components.BarcodeScannerScreen
import com.example.ui.components.PriceComparisonCard
import com.example.ui.components.parseColor
import com.example.ui.theme.*
import com.example.ui.viewmodel.PriceTrackerViewModel
import com.example.ui.viewmodel.SortOption
import com.example.util.AppCurrency
import com.example.util.AppLanguage
import com.example.util.AppStrings
import com.example.util.LocalAppCurrency
import com.example.util.LocalAppLanguage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: PriceTrackerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Navigation tab: 0 = Price Compare, 1 = Shopping Lists, 2 = Barcode Scanner
    var currentTab by remember { mutableIntStateOf(0) }

    // Language state
    val currentLang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val currentCurrency by viewModel.currentCurrency.collectAsStateWithLifecycle()

    // VM state
    val goodsWithPrices by viewModel.filteredGoodsWithPrices.collectAsStateWithLifecycle()
    val allShops by viewModel.allShops.collectAsStateWithLifecycle()
    val allGoods by viewModel.allGoods.collectAsStateWithLifecycle()
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedShopId by viewModel.selectedShopId.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val onlyPromos by viewModel.onlyPromotions.collectAsStateWithLifecycle()

    val selectedGoodItem by viewModel.selectedGoodWithPrices.collectAsStateWithLifecycle()
    val selectedGoodHistory by viewModel.selectedGoodHistory.collectAsStateWithLifecycle()

    val shoppingLists: List<ShoppingListWithItems> by viewModel.shoppingListsWithItems.collectAsStateWithLifecycle()
    val activeShoppingList: ShoppingListWithItems? by viewModel.selectedShoppingList.collectAsStateWithLifecycle()

    val isAiScanning by viewModel.isAiScanning.collectAsStateWithLifecycle()
    val aiScannedResult by viewModel.aiScannedResult.collectAsStateWithLifecycle()
    val aiScanError by viewModel.aiScanErrorMessage.collectAsStateWithLifecycle()
    val geminiApiKey by viewModel.geminiApiKey.collectAsStateWithLifecycle()
    val userMsg by viewModel.userMessage.collectAsStateWithLifecycle()

    // Dialog & Sheet States are persisted in ViewModel to survive rotations
    var showGeminiKeyDialog by remember { mutableStateOf(false) }
    var geminiKeyInput by remember(geminiApiKey) { mutableStateOf(geminiApiKey) }

    val showAddPriceDialog by viewModel.showAddPriceDialog.collectAsStateWithLifecycle()
    val addPriceDraft by viewModel.addPriceDraft.collectAsStateWithLifecycle() // DraftPriceForm

    var showAiScannerDialog by remember { mutableStateOf(false) }
    var showImportExportDialog by remember { mutableStateOf(false) }
    var showManageShopsDialog by remember { mutableStateOf(false) }
    var showManageCategoriesDialog by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showLanguageMenu by remember { mutableStateOf(false) }
    var showCurrencyMenu by remember { mutableStateOf(false) }
    var showTopToolsMenu by remember { mutableStateOf(false) }
    var showCurrencyRatesDialog by remember { mutableStateOf(false) }
    var goodForAddToList by remember { mutableStateOf<Good?>(null) }

    // Dynamic Categories including 'All'
    val displayCategories = remember(allCategories) {
        val list = mutableListOf(AppStrings.ALL)
        if (allCategories.isNotEmpty()) {
            list.addAll(allCategories.map { it.name })
        } else {
            list.addAll(AppStrings.defaultCategories)
        }
        list
    }

    // Show snackbar feedback
    LaunchedEffect(userMsg) {
        userMsg?.let {
            snackbarHostState.showSnackbar(it.message)
            viewModel.clearUserMessage()
        }
    }

    LaunchedEffect(geminiApiKey) {
        geminiKeyInput = geminiApiKey
    }

    CompositionLocalProvider(
        LocalAppLanguage provides currentLang,
        LocalAppCurrency provides currentCurrency
    ) {
        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .testTag("home_screen"),
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if (currentTab == 0) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        // Header Top Row: Title + Language Switcher + Currency Switcher + Redesigned Tools Menu Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Smart Price",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 22.sp,
                                    letterSpacing = (-0.5).sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Language Switcher Button
                                Box {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .height(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable { showLanguageMenu = true }
                                            .padding(horizontal = 8.dp)
                                            .testTag("language_selector_btn")
                                    ) {
                                        Text(
                                            text = "${currentLang.flag} ${currentLang.code.uppercase()}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Select Language",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = showLanguageMenu,
                                        onDismissRequest = { showLanguageMenu = false }
                                    ) {
                                        AppLanguage.entries.forEach { lang ->
                                            DropdownMenuItem(
                                                text = {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(text = lang.flag, fontSize = 16.sp)
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Column {
                                                            Text(
                                                                text = lang.nativeName,
                                                                fontWeight = if (lang == currentLang) FontWeight.Bold else FontWeight.Normal,
                                                                fontSize = 13.sp
                                                            )
                                                            Text(
                                                                text = lang.displayName,
                                                                fontSize = 11.sp,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                },
                                                trailingIcon = {
                                                    if (lang == currentLang) {
                                                        Icon(Icons.Filled.Check, contentDescription = null, tint = SapphireBrand, modifier = Modifier.size(16.dp))
                                                    }
                                                },
                                                onClick = {
                                                    viewModel.setLanguage(lang)
                                                    showLanguageMenu = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Currency Switcher Button
                                Box {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .height(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable { showCurrencyMenu = true }
                                            .padding(horizontal = 8.dp)
                                            .testTag("currency_selector_btn")
                                    ) {
                                        Text(
                                            text = "${currentCurrency.symbol} ${currentCurrency.code}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Select Currency",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = showCurrencyMenu,
                                        onDismissRequest = { showCurrencyMenu = false }
                                    ) {
                                        AppCurrency.entries.forEach { curr ->
                                            DropdownMenuItem(
                                                text = {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = curr.symbol,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 15.sp,
                                                            color = SapphireBrand,
                                                            modifier = Modifier.width(26.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Column {
                                                            Text(
                                                                text = "${curr.displayName} (${curr.code})",
                                                                fontWeight = if (curr == currentCurrency) FontWeight.Bold else FontWeight.Normal,
                                                                fontSize = 13.sp
                                                            )
                                                        }
                                                    }
                                                },
                                                trailingIcon = {
                                                    if (curr == currentCurrency) {
                                                        Icon(Icons.Filled.Check, contentDescription = null, tint = SapphireBrand, modifier = Modifier.size(16.dp))
                                                    }
                                                },
                                                onClick = {
                                                    viewModel.setCurrency(curr)
                                                    showCurrencyMenu = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Redesigned Tools Menu Button (Consolidating AI Scanner, Supermarkets, Categories, Backup)
                                Box {
                                    IconButton(
                                        onClick = { showTopToolsMenu = true },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .testTag("topbar_menu_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = AppStrings.moreActionsMenu(currentLang),
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = showTopToolsMenu,
                                        onDismissRequest = { showTopToolsMenu = false },
                                        modifier = Modifier
                                            .widthIn(min = 230.dp, max = 290.dp)
                                            .background(MaterialTheme.colorScheme.surface)
                                    ) {
                                        // 1. Gemini API Key Setup
                                        DropdownMenuItem(
                                            leadingIcon = {
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Key,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            },
                                            text = {
                                                Column {
                                                    Text(
                                                        text = "AI API key",
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = if (geminiApiKey.isBlank()) "Tap to add your Gemini key" else "Key saved on this device",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            },
                                            onClick = {
                                                showTopToolsMenu = false
                                                showGeminiKeyDialog = true
                                            },
                                            modifier = Modifier.testTag("menu_item_gemini_key")
                                        )

                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                        // 2. AI Price Tag Scanner
                                        DropdownMenuItem(
                                            leadingIcon = {
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(SapphireContainer),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.AutoAwesome,
                                                        contentDescription = null,
                                                        tint = SapphireBrand,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            },
                                            text = {
                                                Column {
                                                    Text(
                                                        text = AppStrings.aiCameraScanner(currentLang),
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = AppStrings.aiScannerMenuSubtitle(currentLang),
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            },
                                            onClick = {
                                                showTopToolsMenu = false
                                                showAiScannerDialog = true
                                            },
                                            modifier = Modifier.testTag("menu_item_ai_scanner")
                                        )

                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                        // 2. Manage Supermarkets
                                        DropdownMenuItem(
                                            leadingIcon = {
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Storefront,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            },
                                            text = {
                                                Column {
                                                    Text(
                                                        text = AppStrings.supermarkets(currentLang),
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = AppStrings.supermarketsSubtitle(currentLang),
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            },
                                            onClick = {
                                                showTopToolsMenu = false
                                                showManageShopsDialog = true
                                            },
                                            modifier = Modifier.testTag("menu_item_supermarkets")
                                        )

                                        // 3. Manage Categories
                                        DropdownMenuItem(
                                            leadingIcon = {
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Category,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            },
                                            text = {
                                                Column {
                                                    Text(
                                                        text = AppStrings.manageCategories(currentLang),
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = AppStrings.categoriesSubtitle(currentLang),
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            },
                                            onClick = {
                                                showTopToolsMenu = false
                                                showManageCategoriesDialog = true
                                            },
                                            modifier = Modifier.testTag("menu_item_categories")
                                        )

                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                        // 4. Backup & Restore Data
                                        DropdownMenuItem(
                                            leadingIcon = {
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.SwapVert,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            },
                                            text = {
                                                Column {
                                                    Text(
                                                        text = AppStrings.backupAndRestore(currentLang),
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = AppStrings.backupSubtitle(currentLang),
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            },
                                            onClick = {
                                                showTopToolsMenu = false
                                                showImportExportDialog = true
                                            },
                                            modifier = Modifier.testTag("menu_item_backup")
                                        )

                                        // 5. Currency Rates (manual)
                                        DropdownMenuItem(
                                            leadingIcon = {
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Sell,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            },
                                            text = {
                                                Column {
                                                    Text(
                                                        text = "Currency Rates",
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "Edit manual currency conversion rates",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            },
                                            onClick = {
                                                showTopToolsMenu = false
                                                showCurrencyRatesDialog = true
                                            },
                                            modifier = Modifier.testTag("menu_item_currency_rates")
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // High Density Pill Search Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                Box(modifier = Modifier.weight(1f)) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = AppStrings.searchPlaceholder(currentLang),
                                            style = TextStyle(fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                        )
                                    }
                                    BasicTextField(
                                        value = searchQuery,
                                        onValueChange = { viewModel.searchQuery.value = it },
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Normal
                                        ),
                                        cursorBrush = SolidColor(SapphireBrand),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("search_goods_input")
                                    )
                                }

                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { viewModel.searchQuery.value = "" },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear search",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.statusBarsPadding())
                }
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("main_bottom_nav")
                ) {
                    // Tab 0: Price Compare
                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == 0) Icons.Filled.Storefront else Icons.Outlined.Storefront,
                                contentDescription = AppStrings.tabCompare(currentLang)
                            )
                        },
                        label = { Text(AppStrings.tabCompare(currentLang), fontSize = 11.sp, fontWeight = if (currentTab == 0) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = SapphireBrand,
                            selectedTextColor = SapphireBrand,
                            indicatorColor = SapphireContainer
                        ),
                        modifier = Modifier.testTag("tab_compare")
                    )

                    // Tab 1: Shopping Lists
                    val totalUncheckedItems = shoppingLists.sumOf { listWithItems ->
                        listWithItems.items.count { !it.isChecked }
                    }

                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (totalUncheckedItems > 0) {
                                        Badge(
                                            containerColor = SapphireBrand,
                                            contentColor = Color.White
                                        ) {
                                            Text("$totalUncheckedItems", fontSize = 9.sp)
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (currentTab == 1) Icons.Filled.Checklist else Icons.Outlined.Checklist,
                                    contentDescription = AppStrings.tabShoppingLists(currentLang)
                                )
                            }
                        },
                        label = { Text(AppStrings.tabShoppingLists(currentLang), fontSize = 11.sp, fontWeight = if (currentTab == 1) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = SapphireBrand,
                            selectedTextColor = SapphireBrand,
                            indicatorColor = SapphireContainer
                        ),
                        modifier = Modifier.testTag("tab_shopping_lists")
                    )

                    // Tab 2: Barcode Scanner
                    NavigationBarItem(
                        selected = currentTab == 2,
                        onClick = { currentTab = 2 },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == 2) Icons.Filled.QrCodeScanner else Icons.Outlined.QrCodeScanner,
                                contentDescription = AppStrings.tabScanner(currentLang)
                            )
                        },
                        label = { Text(AppStrings.tabScanner(currentLang), fontSize = 11.sp, fontWeight = if (currentTab == 2) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = SapphireBrand,
                            selectedTextColor = SapphireBrand,
                            indicatorColor = SapphireContainer
                        ),
                        modifier = Modifier.testTag("tab_scanner")
                    )
                }
            },
            floatingActionButton = {
                if (currentTab == 0) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // High-Density Camera Quick Scan FAB
                        FloatingActionButton(
                            onClick = { showAiScannerDialog = true },
                            shape = RoundedCornerShape(16.dp),
                            containerColor = SapphireContainer,
                            contentColor = SapphireOnContainer,
                            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("fab_ai_scanner")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PhotoCamera,
                                contentDescription = AppStrings.aiCameraScanner(currentLang),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Add Price / Good Extended FAB
                        ExtendedFloatingActionButton(
                                                    onClick = { viewModel.openAddPrice() },
                            shape = RoundedCornerShape(16.dp),
                            containerColor = SapphireBrand,
                            contentColor = Color.White,
                            icon = { Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(20.dp)) },
                            text = { Text(AppStrings.logPrice(currentLang), fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                            modifier = Modifier.testTag("fab_add_price")
                        )
                    }
                }
            }
        ) { paddingValues ->
            when (currentTab) {
                0 -> {
                    // ================= TAB 0: PRICE COMPARISON FEED =================
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 1. Supermarket Filter Pills
                        item {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                item {
                                    val isAllSelected = selectedShopId == null
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(if (isAllSelected) SapphireBrand else HighDensityPillBg)
                                            .clickable { viewModel.selectedShopId.value = null }
                                            .padding(horizontal = 16.dp, vertical = 7.dp)
                                    ) {
                                        Text(
                                            text = AppStrings.allShops(currentLang),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (isAllSelected) Color.White else HighDensityTextSecondary
                                            )
                                        )
                                    }
                                }
                                items(allShops) { shop ->
                                    val isSelected = selectedShopId == shop.id
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(if (isSelected) SapphireBrand else HighDensityPillBg)
                                            .clickable {
                                                viewModel.selectedShopId.value = if (isSelected) null else shop.id
                                            }
                                            .padding(horizontal = 14.dp, vertical = 7.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) Color.White else parseColor(shop.colorHex))
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = shop.name,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (isSelected) Color.White else HighDensityTextSecondary
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // 2. Category Filter Pills
                        item {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(displayCategories) { cat ->
                                    val isSelected = selectedCategory == cat
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(if (isSelected) SapphireBrandDark else HighDensityInputBg)
                                            .clickable { viewModel.selectedCategory.value = cat }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = AppStrings.getCategoryName(cat, currentLang),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (isSelected) Color.White else HighDensityTextSecondary
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // 3. Quick Deals Toggle & Sort Controls
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Deals only toggle chip
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (onlyPromos) DealGreenBg else HighDensityInputBg)
                                        .border(1.dp, if (onlyPromos) DealGreenBorder else HighDensityBorder.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .clickable { viewModel.onlyPromotions.value = !onlyPromos }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.LocalOffer,
                                        contentDescription = null,
                                        tint = if (onlyPromos) DealGreen else HighDensityTextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = AppStrings.dealsOnly(currentLang),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (onlyPromos) DealGreen else HighDensityTextSecondary
                                        )
                                    )
                                }

                                // Sort dropdown (Supports Price Per Item vs Price Per Gram!)
                                Box {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(HighDensityInputBg)
                                            .border(1.dp, HighDensityBorder.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                            .clickable { showSortMenu = true }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                            .testTag("sort_dropdown_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.Sort,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = sortOption.getLabel(currentLang),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = showSortMenu,
                                        onDismissRequest = { showSortMenu = false }
                                    ) {
                                        SortOption.entries.forEach { option ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = option.getLabel(currentLang),
                                                        fontWeight = if (option == sortOption) FontWeight.Bold else FontWeight.Normal,
                                                        fontSize = 13.sp,
                                                        color = if (option == sortOption) SapphireBrand else MaterialTheme.colorScheme.onSurface
                                                    )
                                                },
                                                trailingIcon = {
                                                    if (option == sortOption) {
                                                        Icon(Icons.Filled.Check, contentDescription = null, tint = SapphireBrand, modifier = Modifier.size(16.dp))
                                                    }
                                                },
                                                colors = MenuDefaults.itemColors(
                                                    textColor = MaterialTheme.colorScheme.onSurface,
                                                    leadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    trailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                                ),
                                                onClick = {
                                                    viewModel.sortOption.value = option
                                                    showSortMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 4. Section Header: "CHEAPEST OPTIONS FOUND" & Count Badge
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp, bottom = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = AppStrings.cheapestOptionsFound(currentLang),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp,
                                        color = HighDensityTextSecondary.copy(alpha = 0.7f)
                                    )
                                )
                                Text(
                                    text = "${goodsWithPrices.size} ${AppStrings.itemsCountSuffix(currentLang)}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SapphireBrand
                                    )
                                )
                            }
                        }

                        // 5. High Density List of Products
                        if (goodsWithPrices.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.SearchOff,
                                            contentDescription = null,
                                            tint = HighDensityTextMuted.copy(alpha = 0.5f),
                                            modifier = Modifier.size(56.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = if (searchQuery.isNotBlank() || selectedCategory != AppStrings.ALL || selectedShopId != null) {
                                                AppStrings.noProductsFound(currentLang)
                                            } else {
                                                AppStrings.noGoodsRecorded(currentLang)
                                            },
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = HighDensityTextPrimary
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = AppStrings.addProductsHint(currentLang),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = HighDensityTextSecondary
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                                                                    onClick = { viewModel.openAddPrice() },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = SapphireBrand)
                                        ) {
                                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(AppStrings.addFirstProduct(currentLang))
                                        }
                                    }
                                }
                            }
                        } else {
                            items(goodsWithPrices, key = { it.good.id }) { item ->
                                PriceComparisonCard(
                                    item = item,
                                    onClick = { viewModel.selectGoodForDetail(item.good.id) },
                                    onAddPriceClick = {
                                                                            viewModel.openAddPrice(item.good, null, item.good.barcode)
                                    },
                                    onAddToListClick = {
                                        goodForAddToList = item.good
                                    },
                                    convertAmountForDisplay = { amt, fromCode -> viewModel.convertAmount(amt, fromCode) },
                                    formatAmountForDisplay = { amt, fromCode -> viewModel.formatAmountForDisplay(amt, fromCode) }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // ================= TAB 1: SHOPPING LISTS =================
                    ShoppingListsScreen(
                        shoppingListsWithItems = shoppingLists,
                        selectedList = activeShoppingList,
                        goodsWithPrices = goodsWithPrices,
                        allShops = allShops,
                        onSelectList = { listId -> viewModel.selectShoppingList(listId) },
                        onCreateList = { name, colorHex, shopId -> viewModel.createShoppingList(name, colorHex, shopId) },
                        onDeleteList = { listId -> viewModel.deleteShoppingList(listId) },
                        onAddGoodToShoppingList = { listId, goodId, qty -> viewModel.addGoodToShoppingList(listId, goodId, qty) },
                        onAddCustomItemToShoppingList = { listId, name, cat, qty, unit, weight, weightUnit, estPrice, notes ->
                            viewModel.addCustomItemToShoppingList(listId, name, cat, qty, unit, weight, weightUnit, estPrice, notes)
                        },
                        onToggleItemChecked = { item -> viewModel.toggleShoppingItemChecked(item) },
                        onUpdateQuantity = { itemId, qty -> viewModel.updateShoppingItemQuantity(itemId, qty) },
                        onDeleteItem = { itemId -> viewModel.deleteShoppingListItem(itemId) },
                        onClearCheckedItems = { listId -> viewModel.clearCheckedItemsForList(listId) },
                        onScanBarcodeForList = { currentTab = 2 },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
                2 -> {
                    // ================= TAB 2: BARCODE SCANNER =================
                    BarcodeScannerScreen(
                        goodsWithPrices = goodsWithPrices,
                        onFoundGoodSelected = { goodId ->
                            viewModel.selectGoodForDetail(goodId)
                            currentTab = 0
                        },
                        onAddNewProductWithBarcode = { barcode ->
                                                    viewModel.openAddPrice(null, null, barcode)
                        },
                        onOpenAiScanner = {
                            showAiScannerDialog = true
                        },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }

    // ==================== DIALOGS & SHEETS ====================

    // 0. Gemini API key dialog
    if (showGeminiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showGeminiKeyDialog = false },
            title = { Text("Gemini API key") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Paste your Google AI Studio key below. The app saves it on this phone, so you can use your own Gemini account without sharing a secret in the APK.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = geminiKeyInput,
                        onValueChange = { geminiKeyInput = it },
                        singleLine = true,
                        label = { Text("API key") },
                        placeholder = { Text("AIza...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {
                            }
                        }
                    ) {
                        Text("Get free key")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setGeminiApiKey(geminiKeyInput)
                        showGeminiKeyDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                Row {
                    if (geminiApiKey.isNotBlank()) {
                        TextButton(
                            onClick = {
                                viewModel.clearGeminiApiKey()
                                geminiKeyInput = ""
                                showGeminiKeyDialog = false
                            }
                        ) {
                            Text("Clear")
                        }
                    }
                    TextButton(onClick = { showGeminiKeyDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    // 1. Detailed Product & Comparison Sheet
    if (selectedGoodItem != null) {
        GoodDetailSheet(
            item = selectedGoodItem!!,
            historyList = selectedGoodHistory,
            onDismiss = { viewModel.selectGoodForDetail(null) },
            onAddOrUpdatePriceClick = {
                            viewModel.openAddPrice(selectedGoodItem!!.good, null, selectedGoodItem!!.good.barcode)
            },
            onEditPriceRecord = { shopPrice ->
                viewModel.openEditPrice(selectedGoodItem!!.good, shopPrice)
            },
            onAddToListClick = {
                goodForAddToList = selectedGoodItem!!.good
            },
            onDeletePriceRecord = { shopId ->
                viewModel.deletePriceRecord(selectedGoodItem!!.good.id, shopId)
            },
            onDeleteGood = {
                viewModel.deleteGood(selectedGoodItem!!.good.id)
            },
            onClearHistory = {
                viewModel.clearHistoryForGood(selectedGoodItem!!.good.id)
            }
        )
    }

    // 2. Add / Edit Price Dialog
    if (showAddPriceDialog) {
        AddEditPriceDialog(
            initialGood = addPriceDraft.good,
            initialShop = addPriceDraft.shop,
            initialBarcode = addPriceDraft.barcode,
            initialRegularPrice = addPriceDraft.regularPrice,
            initialDiscountPrice = addPriceDraft.discountPrice,
            initialPackageAmount = addPriceDraft.packageAmount,
            initialPackageUnit = addPriceDraft.packageUnit,
            initialIsPromotion = addPriceDraft.isPromotion,
            initialNote = addPriceDraft.note,
            initialPhotoUri = addPriceDraft.photoUri,
            initialProductImageUri = addPriceDraft.productImageUri,
            initialPricePhotoUri = addPriceDraft.pricePhotoUri,
            onProductImageSaved = { uri -> viewModel.updateDraftProductImageUri(uri) },
            onPricePhotoSaved = { uri -> viewModel.updateDraftPricePhotoUri(uri) },
        pendingPhotoTargetNameFromVm = viewModel.pendingPhotoTargetName.collectAsStateWithLifecycle().value,
        onSetPendingPhotoTargetName = { name -> viewModel.setPendingPhotoTargetName(name) },
        isEditingPrice = addPriceDraft.isEditing,
        allShops = allShops,
        allGoods = allGoods,
        allCategories = allCategories,
        onDismiss = {
            viewModel.closeAddPriceDialog()
        },
            onOpenCategoryManager = {
                showManageCategoriesDialog = true
            },

            onSave = { goodId, name, category, unitType, barcode, weight, weightUnit, imgUri, sName, sAddr, regPrice, discPrice, amount, unit, photoUri, isPromo, note ->
                viewModel.saveGoodAndPrice(
                    goodId = goodId,
                    goodName = name,
                    category = category,
                    unitType = unitType,
                    barcode = barcode,
                    weight = weight,
                    weightUnit = weightUnit,
                    goodImageUri = imgUri,
                    shopName = sName,
                    shopAddress = sAddr,
                    regularPrice = regPrice,
                    discountPrice = discPrice,
                    packageAmount = amount,
                    packageUnit = unit,
                    photoUri = photoUri,
                    isPromotion = isPromo,
                    note = note
                )
            },
            onOpenAiScanner = {
                viewModel.closeAddPriceDialog()
                showAiScannerDialog = true
            },
            onOpenBarcodeScanner = {
                viewModel.closeAddPriceDialog()
                currentTab = 2
            }
        )
    }

    // 3. Quick Add to Shopping List Dialog
    if (goodForAddToList != null) {
        AddToShoppingListDialog(
            good = goodForAddToList!!,
            shoppingLists = shoppingLists,
            onDismiss = { goodForAddToList = null },
            onAddToList = { listId, qty ->
                viewModel.addGoodToShoppingList(listId, goodForAddToList!!.id, qty)
                goodForAddToList = null
            },
            onCreateNewListAndAdd = { listName, qty ->
                viewModel.createShoppingList(listName, "#1E40AF", null)
                goodForAddToList = null
            }
        )
    }

    // 4. AI Vision Price Tag Scanner Dialog
    if (showAiScannerDialog) {
        AiPriceScannerDialog(
            isScanning = isAiScanning,
            scannedResult = aiScannedResult,
            errorMessage = aiScanError,
            onScanBitmap = { bitmap ->
                viewModel.analyzePriceTagImage(bitmap)
            },
            onApplyResult = { result: AiScannedProductDto, imagePath: String? ->
                val matchingGood = if (result.productName != null) {
                    allGoods.firstOrNull { it.name.equals(result.productName, ignoreCase = true) }
                } else null

                val chosenGood = matchingGood ?: Good(
                    name = result.productName ?: "Scanned Product",
                    category = result.category ?: AppStrings.defaultCategories.first(),
                    weight = result.packageAmount ?: 0.0,
                    weightUnit = result.packageUnit ?: "g",
                    barcode = result.barcode
                )

                val chosenShop = if (result.shopName != null) {
                    allShops.firstOrNull { it.name.equals(result.shopName, ignoreCase = true) }
                        ?: Shop(name = result.shopName, address = result.shopAddress ?: "")
                } else null

                val effectiveRegularPrice = when {
                    result.regularPrice != null && result.regularPrice > 0.0 -> result.regularPrice
                    result.discountPrice != null && result.discountPrice > 0.0 -> result.discountPrice
                    else -> null
                }
                val effectiveDiscountPrice = if (result.regularPrice != null && result.discountPrice != null && result.regularPrice > result.discountPrice) {
                    result.discountPrice
                } else null

                viewModel.openAddPrice(
                    good = chosenGood,
                    shop = chosenShop,
                    barcode = result.barcode ?: matchingGood?.barcode,
                    regularPrice = effectiveRegularPrice,
                    discountPrice = effectiveDiscountPrice,
                    packageAmount = result.packageAmount ?: matchingGood?.weight,
                    packageUnit = result.packageUnit ?: matchingGood?.weightUnit ?: "g",
                    isPromotion = effectiveDiscountPrice != null,
                    note = result.notes,
                    photoUri = imagePath,
                    productImageUri = matchingGood?.imageUri,
                    pricePhotoUri = imagePath,
                    isEditing = false
                )

                showAiScannerDialog = false
            },
            onDismiss = {
                viewModel.clearAiScanResult()
                showAiScannerDialog = false
            }
        )
    }

    // 5. Import & Export / Backup Dialog
    if (showImportExportDialog) {
        ImportExportDialog(
            onDismiss = { showImportExportDialog = false },
            onExportJson = { viewModel.getExportJsonString() },
            onImportJson = { json, overwrite, callback ->
                viewModel.importDataFromJson(json, overwrite, callback)
            },
            onExportZip = { viewModel.getExportZipBytes() },
            onImportZipUri = { uri, overwrite, callback -> viewModel.importZipFromUri(uri, overwrite, callback) },
            onClearHistory = { viewModel.clearAllPriceHistory() },
            onResetAll = { viewModel.resetAllData() },
            onReloadSampleData = { viewModel.reloadSampleData() }
        )
    }

    if (showCurrencyRatesDialog) {
        SettingsCurrencyRatesDialog(viewModel = viewModel, onDismiss = { showCurrencyRatesDialog = false })
    }

    // 6. Manage Supermarkets Dialog
    if (showManageShopsDialog) {
        ManageShopsDialog(
            shops = allShops,
            onDismiss = { showManageShopsDialog = false },
            onAddShop = { name, address ->
                viewModel.saveGoodAndPrice(
                    goodId = null,
                    goodName = "",
                    category = "Groceries",
                    unitType = "per item",
                    barcode = null,
                    weight = 0.0,
                    weightUnit = "g",
                    goodImageUri = null,
                    shopName = name,
                    shopAddress = address,
                    regularPrice = 0.0,
                    discountPrice = null,
                    packageAmount = 1.0,
                    packageUnit = "item"
                )
            },
            onDeleteShop = { shopId ->
                viewModel.deleteShop(shopId)
            }
        )
    }

    // 7. Manage Categories Dialog
    if (showManageCategoriesDialog) {
        ManageCategoriesDialog(
            categories = allCategories,
            onDismiss = { showManageCategoriesDialog = false },
            onAddCategory = { name, colorHex ->
                viewModel.addCategory(name, colorHex)
            },
            onUpdateCategory = { cat ->
                viewModel.updateCategory(cat)
            },
            onDeleteCategory = { catId ->
                viewModel.deleteCategory(catId)
            }
        )
    }
}
