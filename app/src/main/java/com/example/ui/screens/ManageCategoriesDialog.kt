package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Category
import com.example.util.AppStrings
import com.example.util.LocalAppLanguage

val CATEGORY_COLORS = listOf(
    "#2563EB", // Blue
    "#16A34A", // Green
    "#DC2626", // Red
    "#D97706", // Amber
    "#7C3AED", // Purple
    "#0284C7", // Sky
    "#EA580C", // Orange
    "#0D9488", // Teal
    "#DB2777", // Pink
    "#4B5563"  // Gray
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCategoriesDialog(
    categories: List<Category>,
    onAddCategory: (String, String) -> Unit,
    onUpdateCategory: (Category) -> Unit,
    onDeleteCategory: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val lang = LocalAppLanguage.current
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var categoryNameInput by remember { mutableStateOf("") }
    var selectedColorHex by remember { mutableStateOf(CATEGORY_COLORS[0]) }
    var showDeleteConfirmId by remember { mutableStateOf<Long?>(null) }
    var isAddingNew by remember { mutableStateOf(false) }

    // Sync input when editingCategory changes
    LaunchedEffect(editingCategory) {
        if (editingCategory != null) {
            categoryNameInput = editingCategory!!.name
            selectedColorHex = editingCategory!!.colorHex
            isAddingNew = true
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .testTag("manage_categories_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.Category,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                AppStrings.manageCategories(lang),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_categories_btn")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = AppStrings.close(lang))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                )

                // Subtitle Info
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            AppStrings.manageCategoriesHint(lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Add or Edit Section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (editingCategory != null) AppStrings.editCategory(lang) else AppStrings.addCategory(lang),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (editingCategory != null) {
                                TextButton(
                                    onClick = {
                                        editingCategory = null
                                        categoryNameInput = ""
                                        selectedColorHex = CATEGORY_COLORS[0]
                                    }
                                ) {
                                    Text(AppStrings.cancel(lang), fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        OutlinedTextField(
                            value = categoryNameInput,
                            onValueChange = { categoryNameInput = it },
                            label = { Text(AppStrings.categoryName(lang)) },
                            placeholder = { Text(AppStrings.categoryNameHint(lang)) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("category_name_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(Modifier.height(10.dp))

                        // Color picker
                        Text(
                            text = AppStrings.selectColor(lang),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CATEGORY_COLORS.forEach { hex ->
                                val color = Color(android.graphics.Color.parseColor(hex))
                                val isSelected = selectedColorHex.equals(hex, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .clickable { selectedColorHex = hex }
                                        .then(
                                            if (isSelected) {
                                                Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                            } else Modifier
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (categoryNameInput.isNotBlank()) {
                                    if (editingCategory != null) {
                                        onUpdateCategory(
                                            editingCategory!!.copy(
                                                name = categoryNameInput.trim(),
                                                colorHex = selectedColorHex
                                            )
                                        )
                                        editingCategory = null
                                    } else {
                                        onAddCategory(categoryNameInput.trim(), selectedColorHex)
                                    }
                                    categoryNameInput = ""
                                    selectedColorHex = CATEGORY_COLORS[0]
                                }
                            },
                            enabled = categoryNameInput.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_category_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                if (editingCategory != null) Icons.Default.Done else Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (editingCategory != null) AppStrings.edit(lang) else AppStrings.addCategory(lang),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Category List
                Text(
                    text = "${AppStrings.categoryLabel(lang)} (${categories.size})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories, key = { it.id }) { cat ->
                        CategoryItemRow(
                            category = cat,
                            onEdit = {
                                editingCategory = cat
                            },
                            onDelete = {
                                showDeleteConfirmId = cat.id
                            }
                        )
                    }
                }

                // Bottom Close Action
                Surface(
                    tonalElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("dismiss_categories_dialog_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(AppStrings.close(lang))
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmId != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmId = null },
            title = { Text(AppStrings.deleteCategoryConfirm(lang), fontWeight = FontWeight.Bold) },
            text = {
                val catToDelete = categories.firstOrNull { it.id == showDeleteConfirmId }
                Text("Category: ${catToDelete?.let { AppStrings.getCategoryName(it.name, lang) } ?: ""}")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmId?.let { onDeleteCategory(it) }
                        showDeleteConfirmId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(AppStrings.delete(lang))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmId = null }) {
                    Text(AppStrings.cancel(lang))
                }
            }
        )
    }
}

@Composable
fun CategoryItemRow(
    category: Category,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val parsedColor = try {
        Color(android.graphics.Color.parseColor(category.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color dot
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(parsedColor)
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = AppStrings.getCategoryName(category.name, lang),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                    if (category.name != AppStrings.getCategoryName(category.name, lang)) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "(${category.name})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(2.dp))

                // Badge
                if (category.isDefault) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(AppStrings.defaultCategoryBadge(lang), fontSize = 10.sp) },
                        modifier = Modifier.height(24.dp)
                    )
                } else {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(AppStrings.customCategoryBadge(lang), fontSize = 10.sp) },
                        modifier = Modifier.height(24.dp),
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        )
                    )
                }
            }

            // Action buttons
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = AppStrings.edit(lang),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            if (!category.isDefault) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = AppStrings.delete(lang),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
