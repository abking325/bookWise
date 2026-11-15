package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.navigation.BookWiseAppShell
import com.example.ui.theme.BookWiseTheme
import com.example.viewmodel.BookWiseViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val viewModel: BookWiseViewModel = viewModel()
      val settings by viewModel.settings.collectAsStateWithLifecycle()
      val isReady by viewModel.isReady.collectAsStateWithLifecycle()

      BookWiseTheme(themeMode = settings.themeMode) {
        if (!isReady) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
          }
        } else {
          BookWiseAppShell(viewModel = viewModel)
        }
      }
    }
  }
}
