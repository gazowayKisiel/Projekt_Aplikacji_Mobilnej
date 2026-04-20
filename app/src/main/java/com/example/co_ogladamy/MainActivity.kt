package com.example.co_ogladamy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.co_ogladamy.ui.theme.Co_OgladamyTheme
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.co_ogladamy.ui.*
import com.example.co_ogladamy.viewmodel.MovieViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var currentScreen by remember { mutableStateOf("welcome") }
            val movieViewModel: MovieViewModel = viewModel()

            when (currentScreen) {
                "welcome" -> WelcomeScreen(onNavigateToMain = { code ->
                    // Tu w przyszłości dodasz logikę kolegi (kod pokoju)
                    currentScreen = "main"
                })
                "main" -> MainScreen(viewModel = movieViewModel)
            }
        }
    }
}