package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.PriceTrackerViewModel
import com.example.util.LocalAppCurrency
import com.example.util.LocalAppLanguage

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val trackerViewModel: PriceTrackerViewModel = viewModel()
      val currentLang by trackerViewModel.currentLanguage.collectAsStateWithLifecycle()
      val currentCurrency by trackerViewModel.currentCurrency.collectAsStateWithLifecycle()

      MyApplicationTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          CompositionLocalProvider(
            LocalAppLanguage provides currentLang,
            LocalAppCurrency provides currentCurrency
          ) {
            HomeScreen(viewModel = trackerViewModel)
          }
        }
      }
    }
  }
}

