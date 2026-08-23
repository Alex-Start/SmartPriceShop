package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.Category
import com.example.data.model.Good
import com.example.data.model.Shop
import com.example.ui.theme.*
import com.example.util.AppStrings
import com.example.util.LocalAppCurrency
import com.example.util.LocalAppLanguage
import com.example.util.UnitPriceCalculator
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPriceDialog(
    initialGood: Good? = null,
    initialShop: Shop? = null,
    initialBarcode: String? = null,
    initialRegularPrice: Double? = null,
    initialDiscountPrice: Double? = null,
    initialPackageAmount: Double? = null,
    initialPackageUnit: String? = null,
    initialIsPromotion: Boolean = false,
    initialNote: String? = null,
    initialPhotoUri: String? = null,
    isEditingPrice: Boolean = false,
    allShops: List<Shop>,
    allGoods: List<Good>,
    allCategories: List<Category> = emptyList(),
    onDismiss: () -> Unit,
    onOpenCategoryManager: () -> Unit = {},
    onSave: (
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
        photoUri: String?,
        isPromotion: Boolean,
        note: String?
    ) -> Unit,
    onOpenAiScanner: () -> Unit,
    onOpenBarcodeScanner: () -> Unit
) {
    val context = LocalContext.current
    val lang = LocalAppLanguage.current
    val currentCurrency = LocalAppCurrency.current

    var goodName by remember { mutableStateOf(initialGood?.name ?: "") }
    var selectedCategory by remember { mutableStateOf(initialGood?.category ?: "Dairy") }
    var barcodeText by remember { mutableStateOf(initialGood?.barcode ?: initialBarcode ?: "") }
    var weightText by remember {
        mutableStateOf(
            if (initialGood != null && initialGood.weight > 0) {
                if (initialGood.weight == initialGood.weight.toInt().toDouble()) "${initialGood.weight.toInt()}" else "${initialGood.weight}"
            } else if (initialPackageAmount != null && initialPackageAmount > 0) {
                if (initialPackageAmount == initialPackageAmount.toInt().toDouble()) "${initialPackageAmount.toInt()}" else "$initialPackageAmount"
            } else "500"
        )
    }
    var weightUnit by remember { mutableStateOf(initialGood?.weightUnit ?: initialPackageUnit ?: "g") }

    var shopName by remember { mutableStateOf(initialShop?.name ?: (allShops.firstOrNull()?.name ?: "Tesco")) }
    var shopAddress by remember { mutableStateOf(initialShop?.address ?: "") }
    var regularPriceText by remember {
        mutableStateOf(
            if (initialRegularPrice != null && initialRegularPrice > 0) {
                if (initialRegularPrice == initialRegularPrice.toInt().toDouble()) "${initialRegularPrice.toInt()}" else "$initialRegularPrice"
            } else ""
        )
    }
    var discountPriceText by remember {
        mutableStateOf(
            if (initialDiscountPrice != null && initialDiscountPrice > 0) {
                if (initialDiscountPrice == initialDiscountPrice.toInt().toDouble()) "${initialDiscountPrice.toInt()}" else "$initialDiscountPrice"
            } else ""
        )
    }
    var packageAmountText by remember {
        mutableStateOf(
            if (initialPackageAmount != null && initialPackageAmount > 0) {
                if (initialPackageAmount == initialPackageAmount.toInt().toDouble()) "${initialPackageAmount.toInt()}" else "$initialPackageAmount"
            } else if (initialGood != null && initialGood.weight > 0) {
                if (initialGood.weight == initialGood.weight.toInt().toDouble()) "${initialGood.weight.toInt()}" else "${initialGood.weight}"
            } else "500"
        )
    }
    var packageUnit by remember { mutableStateOf(initialPackageUnit ?: initialGood?.weightUnit ?: "g") }
    var noteText by remember { mutableStateOf(initialNote ?: "") }
    var isPromotion by remember { mutableStateOf(initialIsPromotion || (initialDiscountPrice != null && initialDiscountPrice > 0)) }
    var imagePath by remember { mutableStateOf(initialPhotoUri ?: initialGood?.imageUri) }

    // Active tooltip dialog state
    var activeTooltipTitle by remember { mutableStateOf<String?>(null) }
    var activeTooltipDescription by remember { mutableStateOf<String?>(null) }

    // Categories list from DB or fallback
    val categoryNames = remember(allCategories) {
        if (allCategories.isNotEmpty()) {
            allCategories.map { it.name }
        } else {
            listOf("Dairy", "Fruits & Veg", "Meat & Fish", "Bakery", "Beverages", "Pantry", "Snacks", "Household", "Other")
        }
    }

    val units = listOf("g", "kg", "ml", "L", "item", "pcs", "pack")

    // Sync weight and package amount
    LaunchedEffect(weightText) {
        if (weightText.isNotBlank()) {
            packageAmountText = weightText
        }
    }
    LaunchedEffect(weightUnit) {
        packageUnit = weightUnit
    }

    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                } else {
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                val file = File(context.filesDir, "good_${System.currentTimeMillis()}.jpg")
                file.outputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                imagePath = file.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Live Unit Price & Price per Gram Calculation
    val regPrice = regularPriceText.toDoubleOrNull() ?: 0.0
    val discPrice = discountPriceText.toDoubleOrNull()
    val effectivePrice = if (discPrice != null && discPrice > 0.0 && discPrice < regPrice) discPrice else regPrice
    val pkgAmount = packageAmountText.toDoubleOrNull() ?: 1.0

    val (unitPrice, unitLabel) = UnitPriceCalculator.calculateUnitPrice(effectivePrice, pkgAmount, packageUnit, currency = currentCurrency)
    val pricePerGramDetailed = UnitPriceCalculator.formatPricePerGram(effectivePrice, pkgAmount, packageUnit, includeDetailedGram = true, currency = currentCurrency)

    // Standardized Input Styling - Dynamically adapts to Light & Dark theme
    // Real entered text is onSurface (Crisp White in Dark mode, Crisp Charcoal in Light mode)
    // Placeholders, field labels, and tooltips are distinctly muted grey (onSurfaceVariant)
    val enteredTextColor = MaterialTheme.colorScheme.onSurface
    val greyPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    val greyLabelColor = MaterialTheme.colorScheme.onSurfaceVariant

    val inputColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = enteredTextColor,
        unfocusedTextColor = enteredTextColor,
        focusedPlaceholderColor = greyPlaceholderColor,
        unfocusedPlaceholderColor = greyPlaceholderColor,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = greyLabelColor,
        cursorColor = MaterialTheme.colorScheme.primary,
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface
    )
    val inputTextStyle = TextStyle(
        color = enteredTextColor,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.94f)
                .testTag("add_edit_price_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AddShoppingCart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        val dialogTitle = when {
                            isEditingPrice -> AppStrings.editPriceTitle(lang)
                            initialGood != null -> AppStrings.addGoodsTitle(true, lang)
                            else -> AppStrings.addGoodsTitle(false, lang)
                        }
                        Text(
                            text = dialogTitle,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = AppStrings.close(lang),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Content Scroll
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // AI Quick-Scan Banner Button
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onOpenAiScanner() }
                            .testTag("ai_scanner_quick_btn"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Outlined.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = AppStrings.aiCameraScanner(lang),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = AppStrings.aiScannerSubtitle(lang),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                        maxLines = 1
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Outlined.CameraAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 1. PRODUCT NAME PARAMETER (With Tooltip)
                    FieldHeaderWithTooltip(
                        label = "${AppStrings.productName(lang)} *",
                        tooltipTitle = AppStrings.productName(lang),
                        tooltipDesc = AppStrings.tooltipProductName(lang),
                        onShowTooltip = { t, d ->
                            activeTooltipTitle = t
                            activeTooltipDescription = d
                        }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                .clickable { imagePickerLauncher.launch("image/*") }
                                .testTag("pick_good_image_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (imagePath != null) {
                                AsyncImage(
                                    model = File(imagePath!!),
                                    contentDescription = "Goods photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Outlined.AddPhotoAlternate,
                                        contentDescription = "Pick image",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = AppStrings.photoLabel(lang),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = greyLabelColor
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        OutlinedTextField(
                            value = goodName,
                            onValueChange = { goodName = it },
                            placeholder = { Text(AppStrings.productNamePlaceholder(lang), color = greyPlaceholderColor) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = inputColors,
                            textStyle = inputTextStyle,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_good_name")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. CATEGORY PARAMETER (With Tooltip & Category Manager Button)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FieldHeaderWithTooltip(
                            label = "${AppStrings.categoryLabel(lang)} *",
                            tooltipTitle = AppStrings.categoryLabel(lang),
                            tooltipDesc = AppStrings.tooltipCategory(lang),
                            onShowTooltip = { t, d ->
                                activeTooltipTitle = t
                                activeTooltipDescription = d
                            }
                        )

                        TextButton(
                            onClick = onOpenCategoryManager,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                AppStrings.manageCategoriesBtn(lang),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(categoryNames) { cat ->
                            val isSelected = selectedCategory.equals(cat, ignoreCase = true)
                            val localizedCatName = AppStrings.getCategoryName(cat, lang)
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { selectedCategory = cat }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = localizedCatName,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. BARCODE PARAMETER (With Tooltip)
                    FieldHeaderWithTooltip(
                        label = AppStrings.barcodeLabel(lang),
                        tooltipTitle = AppStrings.barcodeLabel(lang),
                        tooltipDesc = AppStrings.tooltipBarcode(lang),
                        onShowTooltip = { t, d ->
                            activeTooltipTitle = t
                            activeTooltipDescription = d
                        }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = barcodeText,
                        onValueChange = { barcodeText = it },
                        placeholder = { Text(AppStrings.barcodePlaceholder(lang), color = greyPlaceholderColor) },
                        singleLine = true,
                        colors = inputColors,
                        textStyle = inputTextStyle,
                        trailingIcon = {
                            IconButton(onClick = onOpenBarcodeScanner) {
                                Icon(
                                    imageVector = Icons.Outlined.QrCodeScanner,
                                    contentDescription = AppStrings.scanBarcode(lang),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_barcode")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 4. WEIGHT / VOLUME & UNIT PARAMETERS (With Tooltips)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1.1f)) {
                            FieldHeaderWithTooltip(
                                label = "${AppStrings.weightOrVolume(lang)} *",
                                tooltipTitle = AppStrings.weightOrVolume(lang),
                                tooltipDesc = AppStrings.tooltipWeight(lang),
                                onShowTooltip = { t, d ->
                                    activeTooltipTitle = t
                                    activeTooltipDescription = d
                                }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = weightText,
                                onValueChange = { weightText = it },
                                placeholder = { Text("500", color = greyPlaceholderColor) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = inputColors,
                                textStyle = inputTextStyle,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_weight_amount")
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            FieldHeaderWithTooltip(
                                label = "${AppStrings.weightUnit(lang)} *",
                                tooltipTitle = AppStrings.weightUnit(lang),
                                tooltipDesc = AppStrings.tooltipUnit(lang),
                                onShowTooltip = { t, d ->
                                    activeTooltipTitle = t
                                    activeTooltipDescription = d
                                }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                items(units) { u ->
                                    val isSelected = weightUnit.equals(u, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable {
                                                weightUnit = u
                                                packageUnit = u
                                            }
                                            .padding(horizontal = 8.dp, vertical = 7.dp)
                                    ) {
                                        Text(
                                            text = u,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 5. SUPERMARKET & ADDRESS PARAMETERS (With Tooltips)
                    FieldHeaderWithTooltip(
                        label = "${AppStrings.supermarketLabel(lang)} *",
                        tooltipTitle = AppStrings.supermarketLabel(lang),
                        tooltipDesc = AppStrings.tooltipSupermarket(lang),
                        onShowTooltip = { t, d ->
                            activeTooltipTitle = t
                            activeTooltipDescription = d
                        }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (allShops.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(allShops) { s ->
                                val isSelected = shopName.equals(s.name, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        shopName = s.name
                                        shopAddress = s.address
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = s.name,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                OutlinedTextField(
                    value = shopName,
                    onValueChange = { shopName = it },
                    placeholder = { Text(AppStrings.supermarketPlaceholder(lang), color = greyPlaceholderColor) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = inputColors,
                    textStyle = inputTextStyle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_shop_name")
                )

                Spacer(modifier = Modifier.height(10.dp))

                FieldHeaderWithTooltip(
                    label = AppStrings.addressLabel(lang),
                    tooltipTitle = AppStrings.addressLabel(lang),
                    tooltipDesc = AppStrings.tooltipAddress(lang),
                    onShowTooltip = { t, d ->
                        activeTooltipTitle = t
                        activeTooltipDescription = d
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = shopAddress,
                    onValueChange = { shopAddress = it },
                    placeholder = { Text(AppStrings.addressPlaceholder(lang), color = greyPlaceholderColor) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = inputColors,
                    textStyle = inputTextStyle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_shop_address")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 6. REGULAR & DISCOUNT PRICES (With Tooltips)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        FieldHeaderWithTooltip(
                            label = "${AppStrings.regularPriceLabel(lang)} (${currentCurrency.symbol}) *",
                            tooltipTitle = AppStrings.regularPriceLabel(lang),
                            tooltipDesc = AppStrings.tooltipRegularPrice(lang),
                            onShowTooltip = { t, d ->
                                activeTooltipTitle = t
                                activeTooltipDescription = d
                            }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = regularPriceText,
                            onValueChange = { regularPriceText = it },
                            placeholder = { Text("2.49", color = greyPlaceholderColor) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = inputColors,
                            textStyle = inputTextStyle,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_regular_price")
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        FieldHeaderWithTooltip(
                            label = "${AppStrings.discountPriceLabel(lang)} (${currentCurrency.symbol})",
                            tooltipTitle = AppStrings.discountPriceLabel(lang),
                            tooltipDesc = AppStrings.tooltipDiscountPrice(lang),
                            onShowTooltip = { t, d ->
                                activeTooltipTitle = t
                                activeTooltipDescription = d
                            }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = discountPriceText,
                            onValueChange = {
                                discountPriceText = it
                                isPromotion = it.isNotBlank()
                            },
                            placeholder = { Text("1.99", color = greyPlaceholderColor) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = inputColors,
                            textStyle = inputTextStyle,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_discount_price")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 7. Calculated Unit Price Live Indicator (With Tooltip)
                if (regPrice > 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = AppStrings.standardizedUnitPrice(lang),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    )
                                    IconButton(
                                        onClick = {
                                            activeTooltipTitle = AppStrings.standardizedUnitPrice(lang)
                                            activeTooltipDescription = AppStrings.tooltipUnitPrice(lang)
                                        },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Info,
                                            contentDescription = "Info",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = UnitPriceCalculator.formatUnitPrice(unitPrice, unitLabel, currentCurrency),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }

                            if (pricePerGramDetailed != null) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${AppStrings.pricePerUnit(lang)} $pricePerGramDetailed",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                                    )
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // 8. PROMOTION CHECKBOX & NOTES (With Tooltip)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isPromotion,
                        onCheckedChange = { isPromotion = it },
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                    )
                    Text(
                        text = AppStrings.isPromoCheckbox(lang),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            activeTooltipTitle = AppStrings.isPromoCheckbox(lang)
                            activeTooltipDescription = AppStrings.tooltipIsPromotion(lang)
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = "Info",
                            tint = greyLabelColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                FieldHeaderWithTooltip(
                    label = AppStrings.notesLabel(lang),
                    tooltipTitle = AppStrings.notesLabel(lang),
                    tooltipDesc = AppStrings.tooltipNotes(lang),
                    onShowTooltip = { t, d ->
                        activeTooltipTitle = t
                        activeTooltipDescription = d
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    placeholder = { Text(AppStrings.notesPlaceholder(lang), color = greyPlaceholderColor) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = inputColors,
                    textStyle = inputTextStyle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_deal_note")
                )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(AppStrings.cancel(lang))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (goodName.isNotBlank() && shopName.isNotBlank() && regPrice > 0.0) {
                                onSave(
                                    initialGood?.id,
                                    goodName.trim(),
                                    selectedCategory.trim(),
                                    "per $weightUnit",
                                    barcodeText.ifBlank { null }?.trim(),
                                    weightText.toDoubleOrNull() ?: 0.0,
                                    weightUnit,
                                    imagePath,
                                    shopName.trim(),
                                    shopAddress.trim(),
                                    regPrice,
                                    discPrice,
                                    pkgAmount,
                                    packageUnit,
                                    imagePath,
                                    isPromotion,
                                    noteText.ifBlank { null }?.trim()
                                )
                                onDismiss()
                            }
                        },
                        enabled = goodName.isNotBlank() && shopName.isNotBlank() && regPrice > 0.0,
                        modifier = Modifier.testTag("save_price_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isEditingPrice) AppStrings.saveChanges(lang) else AppStrings.savePrice(lang), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    // Parameter Tooltip Dialog
    if (activeTooltipTitle != null && activeTooltipDescription != null) {
        AlertDialog(
            onDismissRequest = {
                activeTooltipTitle = null
                activeTooltipDescription = null
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = activeTooltipTitle!!,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            text = {
                Text(
                    text = activeTooltipDescription!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        activeTooltipTitle = null
                        activeTooltipDescription = null
                    }
                ) {
                    Text(AppStrings.ok(lang), fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun FieldHeaderWithTooltip(
    label: String,
    tooltipTitle: String,
    tooltipDesc: String,
    onShowTooltip: (String, String) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { onShowTooltip(tooltipTitle, tooltipDesc) }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = "Help for $tooltipTitle",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp)
        )
    }
}
