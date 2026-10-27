package com.example.co_ogladamy.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.co_ogladamy.model.UserProfile
import com.example.co_ogladamy.viewmodel.AuthViewModel
import com.example.co_ogladamy.viewmodel.RoomViewModel

@Composable
fun WelcomeScreen(
    onNavigateToMain: () -> Unit,
    roomViewModel: RoomViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    var inputCode by remember { mutableStateOf("") }
    var showFriendsDialog by remember { mutableStateOf(false) }

    val statusMessage by roomViewModel.statusMessage.collectAsState()
    val isHost by roomViewModel.isHost.collectAsState()
    val shouldStart by roomViewModel.shouldStartMovies.collectAsState()

    val userEmail by authViewModel.currentUserEmail.collectAsState()
    val friendsList by authViewModel.friendsList.collectAsState()
    val incomingInvite by authViewModel.incomingInvite.collectAsState()
    val authMessage by authViewModel.authMessage.collectAsState()

    LaunchedEffect(shouldStart) {
        if (shouldStart) {
            onNavigateToMain()
        }
    }

    // Okno dialogowe znajomych
    if (showFriendsDialog) {
        FriendsDialog(
            friends = friendsList,
            currentRoomCode = roomViewModel.currentRoomCode,
            authMessage = authMessage,
            onAddFriend = { email -> authViewModel.addFriend(email) },
            onInviteFriend = { friendUid, code -> authViewModel.inviteFriendToRoom(friendUid, code) },
            onDismiss = {
                showFriendsDialog = false
                authViewModel.clearMessage()
            }
        )
    }

    // Popup z zaproszeniem do pokoju od znajomego
    if (incomingInvite != null && roomViewModel.currentRoomCode == null) {
        val (inviteCode, fromEmail) = incomingInvite!!
        AlertDialog(
            onDismissRequest = { authViewModel.clearInvite() },
            title = { Text("Zaproszenie do wspólnej sesji!") },
            text = { Text("Znajomy ($fromEmail) zaprasza Cię do pokoju: $inviteCode") },
            confirmButton = {
                Button(onClick = {
                    authViewModel.clearInvite()
                    roomViewModel.joinExistingRoom(inviteCode)
                }) {
                    Text("Dołącz teraz")
                }
            },
            dismissButton = {
                TextButton(onClick = { authViewModel.clearInvite() }) {
                    Text("Odrzuć")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Górny pasek użytkownika i znajomych
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Zalogowany jako:", style = MaterialTheme.typography.labelSmall)
                Text(
                    text = userEmail,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { showFriendsDialog = true }) {
                    Text("Znajomi (${friendsList.size})")
                }
                OutlinedButton(onClick = { authViewModel.logout() }) {
                    Text("Wyloguj")
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Text("Co Oglądamy?", fontSize = 32.sp, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(32.dp))

        if (statusMessage != null) {
            Text(
                text = statusMessage!!,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (roomViewModel.currentRoomCode != null) {
            // POCZEKALNIA POKOJU
            if (isHost) {
                Button(
                    onClick = { roomViewModel.startRoom() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Text("Rozpocznij losowanie!", fontSize = 20.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { showFriendsDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Zaproś znajomego z listy do tego pokoju")
                }
            } else {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Oczekiwanie na rozpoczęcie przez hosta...")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { roomViewModel.leaveCurrentRoom(onNavigateBack = {}) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Opuść pokój")
            }
        } else {
            // MENU GŁÓWNE TWORZENIA / DOŁĄCZANIA
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

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { roomViewModel.joinExistingRoom(inputCode) },
                modifier = Modifier.fillMaxWidth(),
                enabled = inputCode.isNotEmpty()
            ) {
                Text("Dołącz")
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun FriendsDialog(
    friends: List<UserProfile>,
    currentRoomCode: String?,
    authMessage: String?,
    onAddFriend: (String) -> Unit,
    onInviteFriend: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var friendEmailInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Twoi Znajomi") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (authMessage != null) {
                    Text(
                        text = authMessage,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Pole dodawania nowego znajomego
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = friendEmailInput,
                        onValueChange = { friendEmailInput = it },
                        label = { Text("E-mail znajomego") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        onAddFriend(friendEmailInput)
                        friendEmailInput = ""
                    }) {
                        Text("Dodaj")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                if (friends.isEmpty()) {
                    Text("Brak znajomych na liście. Dodaj kogoś wpisując jego e-mail powyżej!")
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 250.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(friends) { friend ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = friend.email,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                // Jeśli jesteśmy w pokoju, możemy jednym kliknięciem zaprosić znajomego
                                if (currentRoomCode != null) {
                                    Button(
                                        onClick = { onInviteFriend(friend.uid, currentRoomCode) },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text("Zaproś")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Zamknij")
            }
        }
    )
}