package com.example.co_ogladamy.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WelcomeScreen(onNavigateToMain: (String?) -> Unit) {
    var roomCode by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Co Oglądamy?", fontSize = 32.sp)
        Spacer(modifier = Modifier.height(40.dp))

        Button(onClick = { onNavigateToMain(null) }, modifier = Modifier.fillMaxWidth()) {
            Text("Stwórz nowy pokój")
        }

        Text("lub", modifier = Modifier.padding(16.dp))

        OutlinedTextField(
            value = roomCode,
            onValueChange = { roomCode = it },
            label = { Text("Wpisz kod pokoju") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { onNavigateToMain(roomCode) },
            modifier = Modifier.fillMaxWidth(),
            enabled = roomCode.isNotEmpty()
        ) {
            Text("Dołącz")
        }
    }
}