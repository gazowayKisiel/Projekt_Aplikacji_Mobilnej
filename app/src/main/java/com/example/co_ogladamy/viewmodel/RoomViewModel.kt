package com.example.co_ogladamy.viewmodel

import androidx.lifecycle.ViewModel
import com.example.co_ogladamy.data.RoomRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class RoomViewModel : ViewModel() {
    private val repository = RoomRepository()

    // Pobieramy prawdziwe UID zalogowanego użytkownika z Firebase Auth
    val myUserId: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: "anonim"

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage

    var currentRoomCode: String? = null
        private set

    private val _isHost = MutableStateFlow(false)
    val isHost: StateFlow<Boolean> = _isHost

    private val _shouldStartMovies = MutableStateFlow(false)
    val shouldStartMovies: StateFlow<Boolean> = _shouldStartMovies

    fun createNewRoom() {
        _statusMessage.value = "Tworzenie pokoju..."
        repository.createRoom(myUserId) { code ->
            if (code != null) {
                currentRoomCode = code
                _isHost.value = true
                _statusMessage.value = "Pokój utworzony! Kod: $code\nZaproś znajomych z listy lub podaj im kod."
                listenForStartSignal(code)
            } else {
                _statusMessage.value = "Błąd: Nie udało się utworzyć pokoju."
            }
        }
    }

    fun joinExistingRoom(code: String) {
        if (code.isBlank()) return

        _statusMessage.value = "Dołączanie..."
        repository.joinRoom(code, myUserId) { success ->
            if (success) {
                currentRoomCode = code
                _isHost.value = false
                _statusMessage.value = "Dołączono do pokoju $code! Czekaj na start..."
                listenForStartSignal(code)
            } else {
                _statusMessage.value = "Błąd: Taki pokój nie istnieje."
            }
        }
    }

    fun startRoom() {
        currentRoomCode?.let { code ->
            repository.startRoom(code)
        }
    }

    private fun listenForStartSignal(code: String) {
        repository.listenToRoomStatus(code) { newStatus ->
            if (newStatus == "ready") {
                _shouldStartMovies.value = true
            }
        }
    }

    fun leaveCurrentRoom(onNavigateBack: () -> Unit) {
        currentRoomCode?.let { code ->
            repository.leaveRoom(code)
            currentRoomCode = null
            _statusMessage.value = null
            _shouldStartMovies.value = false
        }
        onNavigateBack()
    }
}