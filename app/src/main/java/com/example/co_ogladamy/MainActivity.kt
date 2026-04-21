package com.example.co_ogladamy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.co_ogladamy.ui.MainScreen
import com.example.co_ogladamy.ui.WelcomeScreen
import com.example.co_ogladamy.ui.theme.Co_OgladamyTheme
import com.example.co_ogladamy.viewmodel.MovieViewModel
import com.example.co_ogladamy.viewmodel.RoomViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Co_OgladamyTheme {
                var currentScreen by remember { mutableStateOf("welcome") }
                val movieViewModel: MovieViewModel = viewModel()


                val roomViewModel: RoomViewModel = viewModel()

                when (currentScreen) {
                    "welcome" -> WelcomeScreen(
                        onNavigateToMain = { currentScreen = "main" },
                        roomViewModel = roomViewModel
                    )
                    "main" -> MainScreen(
                        viewModel = movieViewModel,
                        roomViewModel = roomViewModel,
                        onLeaveRoom = {
                            currentScreen = "welcome"
                        }
                    )
                }
            }
        }
    }
}
