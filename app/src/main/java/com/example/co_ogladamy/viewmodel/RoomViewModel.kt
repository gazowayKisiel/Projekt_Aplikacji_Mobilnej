package com.example.co_ogladamy.viewmodel

import androidx.lifecycle.ViewModel
import com.example.co_ogladamy.data.RoomRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class RoomViewModel : ViewModel() {
    private val repository = RoomRepository()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage

    var currentRoomCode: String? = null
        private set


    private val _isHost = MutableStateFlow(false)
    val isHost: StateFlow<Boolean> = _isHost

    // host wcisnal start -> trzeba przelaczyc ekran na filmy
    private val _shouldStartMovies = MutableStateFlow(false)
    val shouldStartMovies: StateFlow<Boolean> = _shouldStartMovies



    fun createNewRoom() {
        _statusMessage.value = "Tworzenie pokoju..."
        repository.createRoom { code ->
            if (code != null) {
                currentRoomCode = code
                _isHost.value = true // osoba ktora stworzyla = host
                _statusMessage.value = "Pokój utworzony! Kod: $code\nCzekaj na znajomych..."
                listenForStartSignal(code) // nasluchuja az kliknie start
            } else {
                _statusMessage.value = "Błąd: Nie udało się utworzyć pokoju."
            }
        }
    }

    fun joinExistingRoom(code: String) {
        if (code.isBlank()) return

        _statusMessage.value = "Dołączanie..."
        repository.joinRoom(code) { success ->
            if (success) {
                currentRoomCode = code
                _isHost.value = false // osoba ktora dolaczyla = gosc
                _statusMessage.value = "Dołączono! Czekaj aż Host wystartuje..."
                listenForStartSignal(code) // nasluchiwanie bazy
            } else {
                _statusMessage.value = "Błąd: Taki pokój nie istnieje."
            }
        }
    }



    fun startRoom() {
        // tylko dla hosta
        currentRoomCode?.let { code ->
            repository.startRoom(code) // w bazie "waiting" -> "ready"
        }
    }

    private fun listenForStartSignal(code: String) {
        // odpala pokoj u szczystkich po zmianie statusu na "ready",
        // mozna zmienic ekran
        repository.listenToRoomStatus(code) { newStatus ->
            if (newStatus == "ready") {
                _shouldStartMovies.value = true
            }
        }
    }

    fun leaveCurrentRoom(onNavigateBack: () -> Unit) {
        currentRoomCode?.let { code ->
            repository.leaveRoom(code)   // usuwa z bazy
            currentRoomCode = null       // czysci pamiec
            _statusMessage.value = null  // czysci komunikat
            _shouldStartMovies.value = false // Gasi zielone światło startu
        }
        onNavigateBack() // wraca do WelcomeScreen
    }
}