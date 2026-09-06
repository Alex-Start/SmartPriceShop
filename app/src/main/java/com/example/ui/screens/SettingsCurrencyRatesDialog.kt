package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.ui.viewmodel.PriceTrackerViewModel
import com.example.util.AppCurrency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsCurrencyRatesDialog(
    viewModel: PriceTrackerViewModel,
    onDismiss: () -> Unit
) {
    // Load current rates from ViewModel
    val initialRates = remember { viewModel.getCurrencyRates() }

    // Use a snapshot state map for editable text values (strings)
    val ratesMap = remember { mutableStateMapOf<String, String>() }
    LaunchedEffect(initialRates) {
        ratesMap.clear()
        // Ensure common currencies present first
        val preferred = listOf(AppCurrency.USD.code, AppCurrency.CZK.code, AppCurrency.EUR.code, AppCurrency.UAH.code)
        for (c in preferred) {
            val v = initialRates[c] ?: initialRates[c.uppercase()] ?: 0.0
            ratesMap[c] = v.toString()
        }
        // add any other currencies present
        for ((k, v) in initialRates) {
            if (!ratesMap.containsKey(k)) ratesMap[k] = v.toString()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Currency Rates", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for ((code, textVal) in ratesMap.toList()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text(code, modifier = Modifier.width(56.dp))
                            OutlinedTextField(
                                value = ratesMap[code] ?: "",
                                onValueChange = { ratesMap[code] = it },
                                singleLine = true,
                                label = { Text("units per 1 USD") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        // Save all rates into ViewModel
                        for ((k, v) in ratesMap) {
                            val parsed = v.toDoubleOrNull()
                            if (parsed != null && parsed > 0.0) {
                                viewModel.setCurrencyRate(k, parsed)
                            }
                        }
                        onDismiss()
                    }) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
