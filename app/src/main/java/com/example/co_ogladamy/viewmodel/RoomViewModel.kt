package com.example.co_ogladamy.viewmodel

import androidx.lifecycle.ViewModel
import com.example.co_ogladamy.data.RoomRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

class RoomViewModel : ViewModel() {
    private val repository = RoomRepository()

    // Docelowo podmienicie to na FirebaseAuth.getInstance().currentUser?.uid
    val myUserId: String = UUID.randomUUID().toString()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage

    var currentRoomCode: String? = null
        private set

    private val _isHost = MutableStateFlow(false)
    val isHost: StateFlow<Boolean> = _isHost

    private val _shouldStartMovies = MutableStateFlow(false)
    val shouldStartMovies: StateFlow<Boolean> = _shouldStartMovies

    // Nowe stany: lista uczestników w lobby i wybrany gatunek
    private val _participants = MutableStateFlow<List<String>>(emptyList())
    val participants: StateFlow<List<String>> = _participants

    private val _selectedGenreId = MutableStateFlow<Int?>(null)
    val selectedGenreId: StateFlow<Int?> = _selectedGenreId

    fun selectGenre(genreId: Int?) {
        _selectedGenreId.value = genreId
    }

    fun createNewRoom(username: String = "Host") {
        _statusMessage.value = "Tworzenie pokoju..."
        repository.createRoom(myUserId, username, _selectedGenreId.value) { code ->
            if (code != null) {
                currentRoomCode = code
                _isHost.value = true
                _statusMessage.value = "Pokój utworzony! Kod: $code\nCzekaj na znajomych..."
                listenForStartSignal(code)
                repository.listenToParticipants(code) { names ->
                    _participants.value = names
                }
            } else {
                _statusMessage.value = "Błąd: Nie udało się utworzyć pokoju."
            }
        }
    }

    fun joinExistingRoom(code: String, username: String = "Gość") {
        if (code.isBlank()) return

        _statusMessage.value = "Dołączanie..."
        repository.joinRoom(code, myUserId, username) { success ->
            if (success) {
                currentRoomCode = code
                _isHost.value = false
                _statusMessage.value = "Dołączono! Czekaj aż Host wystartuje..."
                listenForStartSignal(code)
                repository.listenToParticipants(code) { names ->
                    _participants.value = names
                }
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
            repository.leaveRoom(code, myUserId, _isHost.value)
            currentRoomCode = null
            _statusMessage.value = null
            _shouldStartMovies.value = false
            _participants.value = emptyList()
        }
        onNavigateBack()
    }
}