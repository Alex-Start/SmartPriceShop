package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*
import com.example.util.AppStrings
import com.example.util.LocalAppLanguage
import kotlinx.coroutines.launch

@Composable
fun ImportExportDialog(
    onDismiss: () -> Unit,
    onExportJson: suspend () -> String,
    onImportJson: (String, Boolean, (Boolean, String) -> Unit) -> Unit,
    onExportZip: (suspend () -> ByteArray)? = null,
    onImportZipUri: ((android.net.Uri, Boolean, (Boolean, String) -> Unit) -> Unit)? = null,
    onClearHistory: () -> Unit,
    onResetAll: () -> Unit,
    onReloadSampleData: () -> Unit
) {
    val context = LocalContext.current
    val lang = LocalAppLanguage.current
    val coroutineScope = rememberCoroutineScope()

    var jsonContent by remember { mutableStateOf("") }
    var overwriteMode by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isErrorStatus by remember { mutableStateOf(false) }

    var showClearHistoryConfirm by remember { mutableStateOf(false) }
    var showResetAllConfirm by remember { mutableStateOf(false) }

    // File picker for import
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val text = stream.bufferedReader().readText()
                    jsonContent = text
                    statusMessage = AppStrings.loadedFileReadyToImport(lang, text.length)
                    isErrorStatus = false
                }
            } catch (e: Exception) {
                statusMessage = "${AppStrings.failedToReadFile(lang)}: ${e.localizedMessage}"
                isErrorStatus = true
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.90f)
                .testTag("import_export_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(HighDensityBorder)
            )
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
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SapphireContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.SwapVert,
                                contentDescription = null,
                                tint = SapphireOnContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = AppStrings.backupAndSync(lang),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = HighDensityTextPrimary
                            )
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = AppStrings.close(lang), tint = HighDensityTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Status banner if present
                    if (statusMessage != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isErrorStatus) Color(0xFFFEF2F2) else DealGreenBg
                            ),
                            shape = RoundedCornerShape(10.dp),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(
                                    if (isErrorStatus) Color(0xFFFECACA) else DealGreenBorder
                                )
                            )
                        ) {
                            Text(
                                text = statusMessage!!,
                                modifier = Modifier.padding(10.dp),
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = if (isErrorStatus) AccentRed else DealGreen
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // 1. Export Section
                    Text(
                        text = "1. Export Data to Another Device",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = SapphireBrand
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Exports all your registered goods, supermarkets, current prices, and full price history table as standard JSON format.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = HighDensityTextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val json = onExportJson()
                                    jsonContent = json
                                    // Share intent
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, json)
                                        type = "application/json"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Export Smart Price Data")
                                    context.startActivity(shareIntent)
                                    statusMessage = AppStrings.backupReadyToShare(lang)
                                    isErrorStatus = false
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_share_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = SapphireBrand),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(AppStrings.shareBackup(lang), style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    val json = onExportJson()
                                    jsonContent = json
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("PriceTracker_Backup", json)
                                    clipboard.setPrimaryClip(clip)
                                    statusMessage = AppStrings.copied(lang)
                                    isErrorStatus = false
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_copy_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(AppStrings.copyToClipboard(lang), style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Export ZIP actions (if provided)
                    if (onExportZip != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val createZipLauncher = rememberLauncherForActivityResult(
                                contract = ActivityResultContracts.CreateDocument("application/zip")
                            ) { uri: Uri? ->
                                if (uri != null) {
                                    coroutineScope.launch {
                                        try {
                                            val bytes = onExportZip()
                                            context.contentResolver.openOutputStream(uri)?.use { out ->
                                                out.write(bytes)
                                            }
                                            statusMessage = "${AppStrings.exportJson(lang)}: ${AppStrings.backupReadyToShare(lang)}"
                                            isErrorStatus = false
                                        } catch (e: Exception) {
                                            statusMessage = "${AppStrings.failedToReadFile(lang)} ZIP: ${e.localizedMessage}"
                                            isErrorStatus = true
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = { createZipLauncher.launch("smartprice_backup_${System.currentTimeMillis()}.zip") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("export_zip_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = SapphireBrand),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(imageVector = Icons.Outlined.FolderZip, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(AppStrings.exportZipManifest(lang), style = MaterialTheme.typography.labelMedium)
                            }

                            OutlinedButton(
                                onClick = {
                                    // copy as base64 maybe - not implemented
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("export_zip_copy_button"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(imageVector = Icons.Outlined.Link, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(AppStrings.exportZipSave(lang), style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = HighDensityBorder)
                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. Import Section
                    Text(
                        text = "2. Import Data from JSON",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = SapphireBrand
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Paste exported JSON text or select a backup file from another device.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = HighDensityTextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Outlined.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(AppStrings.pickBackupFile(lang), style = MaterialTheme.typography.labelMedium)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // ZIP import launcher
                    val zipPickerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.GetContent()
                    ) { uri: Uri? ->
                        if (uri != null && onImportZipUri != null) {
                            coroutineScope.launch {
                                onImportZipUri(uri, overwriteMode) { success, msg ->
                                    statusMessage = msg
                                    isErrorStatus = !success
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { zipPickerLauncher.launch("application/zip") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Outlined.FolderZip, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(AppStrings.importJson(lang), style = MaterialTheme.typography.labelMedium)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = jsonContent,
                        onValueChange = { jsonContent = it },
                        label = { Text(AppStrings.backupJsonContent(lang)) },
                        placeholder = { Text(AppStrings.pasteJsonBackupString(lang)) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .testTag("import_json_input"),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = overwriteMode,
                            onCheckedChange = { overwriteMode = it },
                            modifier = Modifier.testTag("import_overwrite_switch"),
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = SapphireBrand)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (overwriteMode) AppStrings.overwriteExistingDatabase(lang) else AppStrings.mergeWithExistingData(lang),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = HighDensityTextPrimary
                                )
                            )
                            Text(
                                text = if (overwriteMode) AppStrings.replaceAllDataWarning(lang) else AppStrings.preserveDataWarning(lang),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    color = HighDensityTextSecondary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (jsonContent.isNotBlank()) {
                                onImportJson(jsonContent, overwriteMode) { success, msg ->
                                    statusMessage = msg
                                    isErrorStatus = !success
                                }
                            }
                        },
                        enabled = jsonContent.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("confirm_import_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = SapphireBrand),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Outlined.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(AppStrings.importAndRestoreData(lang), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = HighDensityBorder)
                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. Clean Up & Reset Section
                    Text(
                        text = AppStrings.cleanUpMaintenance(lang),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = HighDensityTextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showClearHistoryConfirm = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("clear_history_table_btn"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRed),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFFECACA))
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Outlined.CleaningServices, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(AppStrings.clearHistory(lang), style = MaterialTheme.typography.labelSmall)
                        }

                        OutlinedButton(
                            onClick = { showResetAllConfirm = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("reset_all_data_btn"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRed),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFFECACA))
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Outlined.DeleteForever, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(AppStrings.resetAll(lang), style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    FilledTonalButton(
                        onClick = {
                            onReloadSampleData()
                            statusMessage = "Loaded sample dataset (Tesco, Lidl, Aldi, Walmart, Carrefour)!"
                            isErrorStatus = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reload_sample_data_btn"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Outlined.Store, contentDescription = null, modifier = Modifier.size(15.dp), tint = SapphireBrand)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(AppStrings.reloadSampleDataset(lang), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }

    // Clear History Dialog
    if (showClearHistoryConfirm) {
        AlertDialog(
            onDismissRequest = { showClearHistoryConfirm = false },
            title = { Text(AppStrings.clearAllPriceHistory(lang), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
            text = { Text(AppStrings.clearHistoryWarning(lang), style = MaterialTheme.typography.bodySmall) },
            confirmButton = {
                Button(
                    onClick = {
                        showClearHistoryConfirm = false
                        onClearHistory()
                        statusMessage = AppStrings.allPriceHistoryCleared(lang)
                        isErrorStatus = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(AppStrings.clearAllPriceHistory(lang))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryConfirm = false }) {
                    Text(AppStrings.cancel(lang))
                }
            }
        )
    }

    // Reset All Confirmation Dialog
    if (showResetAllConfirm) {
        AlertDialog(
            onDismissRequest = { showResetAllConfirm = false },
            title = { Text(AppStrings.resetEntireDatabase(lang), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
            text = { Text(AppStrings.resetDatabaseWarning(lang), style = MaterialTheme.typography.bodySmall) },
            confirmButton = {
                Button(
                    onClick = {
                        showResetAllConfirm = false
                        onResetAll()
                        statusMessage = AppStrings.databaseResetComplete(lang)
                        isErrorStatus = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(AppStrings.deleteEverything(lang))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetAllConfirm = false }) {
                    Text(AppStrings.cancel(lang))
                }
            }
        )
    }
}

