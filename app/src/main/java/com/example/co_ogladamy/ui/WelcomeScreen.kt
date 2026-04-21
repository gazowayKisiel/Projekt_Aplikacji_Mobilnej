package com.example.co_ogladamy.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.co_ogladamy.viewmodel.RoomViewModel

@Composable
fun WelcomeScreen(
    onNavigateToMain: () -> Unit,
    roomViewModel: RoomViewModel = viewModel()
) {
    var inputCode by remember { mutableStateOf("") }

    // nasluchiwanie nowych zmiennych z ViewModelu
    val statusMessage by roomViewModel.statusMessage.collectAsState()
    val isHost by roomViewModel.isHost.collectAsState()
    val shouldStart by roomViewModel.shouldStartMovies.collectAsState()

    // kiedy ViewModel zapali zielone swaitlo (shouldStart = true)
    // aplikacja sama przerzuci nas do ekranu z filmami
    LaunchedEffect(shouldStart) {
        if (shouldStart) {
            onNavigateToMain()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Co Oglądamy?", fontSize = 32.sp, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(40.dp))


        if (statusMessage != null) {
            Text(
                text = statusMessage!!,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(32.dp))
        }


        if (roomViewModel.currentRoomCode != null) {
            // JESTES W POKOJU -> POKAZUJEMY POCZEKALNIĘ

            if (isHost) {
                // widok dla hosta
                Button(
                    onClick = { roomViewModel.startRoom() },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Text("Rozpocznij losowanie!", fontSize = 20.sp)
                }
            } else {
                // widok dla goscia
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Oczekiwanie na rozpoczęcie przez hosta...")
            }

            Spacer(modifier = Modifier.height(32.dp))

            // PRZYCISK WYJSCIA Z POCZEKALNI
            Button(
                onClick = {
                    roomViewModel.leaveCurrentRoom(onNavigateBack = { /* Ekran sam się odświeży */ })
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Opuść pokój")
            }

        } else {
            // NIE JESTES W POKOJU -> MENU GŁÓWNE

            Button(
                onClick = { roomViewModel.createNewRoom() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Stwórz nowy pokój")
            }

            Text("lub", modifier = Modifier.padding(16.dp))

            OutlinedTextField(
                value = inputCode,
                onValueChange = { inputCode = it },
                label = { Text("Wpisz kod pokoju") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { roomViewModel.joinExistingRoom(inputCode) },
                modifier = Modifier.fillMaxWidth(),
                enabled = inputCode.isNotEmpty()
            ) {
                Text("Dołącz")
            }
        }
    }
}