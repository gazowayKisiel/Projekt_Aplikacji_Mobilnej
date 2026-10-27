package com.example.co_ogladamy.viewmodel

import androidx.lifecycle.ViewModel
import com.example.co_ogladamy.data.AuthAndFriendsRepository
import com.example.co_ogladamy.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthViewModel : ViewModel() {
    private val repo = AuthAndFriendsRepository()

    private val _isLoggedIn = MutableStateFlow(repo.currentUser != null)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentUserEmail = MutableStateFlow(repo.currentUser?.email ?: "")
    val currentUserEmail: StateFlow<String> = _currentUserEmail.asStateFlow()

    val currentUserId: String
        get() = repo.currentUser?.uid ?: ""

    private val _authMessage = MutableStateFlow<String?>(null)
    val authMessage: StateFlow<String?> = _authMessage.asStateFlow()

    private val _friendsList = MutableStateFlow<List<UserProfile>>(emptyList())
    val friendsList: StateFlow<List<UserProfile>> = _friendsList.asStateFlow()

    // Para: (roomCode, fromEmail)
    private val _incomingInvite = MutableStateFlow<Pair<String, String>?>(null)
    val incomingInvite: StateFlow<Pair<String, String>?> = _incomingInvite.asStateFlow()

    init {
        if (_isLoggedIn.value) {
            startListeners()
        }
    }

    private fun startListeners() {
        repo.listenToFriends { friends ->
            _friendsList.value = friends
        }
        repo.listenToRoomInvite { code, fromEmail ->
            if (code != null && fromEmail != null) {
                _incomingInvite.value = Pair(code, fromEmail)
            } else {
                _incomingInvite.value = null
            }
        }
    }

    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _authMessage.value = "Wypełnij wszystkie pola!"
            return
        }
        repo.login(email, pass) { success, error ->
            if (success) {
                _currentUserEmail.value = repo.currentUser?.email ?: ""
                _isLoggedIn.value = true
                _authMessage.value = null
                startListeners()
            } else {
                _authMessage.value = error
            }
        }
    }

    fun register(email: String, pass: String) {
        if (email.isBlank() || pass.length < 6) {
            _authMessage.value = "Podaj poprawny e-mail i hasło (min. 6 znaków)!"
            return
        }
        repo.register(email, pass) { success, error ->
            if (success) {
                _currentUserEmail.value = repo.currentUser?.email ?: ""
                _isLoggedIn.value = true
                _authMessage.value = null
                startListeners()
            } else {
                _authMessage.value = error
            }
        }
    }

    fun logout() {
        repo.logout()
        _isLoggedIn.value = false
        _currentUserEmail.value = ""
        _friendsList.value = emptyList()
        _incomingInvite.value = null
    }

    fun addFriend(email: String) {
        if (email.isBlank()) return
        repo.addFriendByEmail(email) { _, msg ->
            _authMessage.value = msg
        }
    }

    fun inviteFriendToRoom(friendUid: String, roomCode: String) {
        repo.sendRoomInvite(friendUid, roomCode)
        _authMessage.value = "Wysłano zaproszenie do pokoju!"
    }

    fun clearInvite() {
        repo.clearRoomInvite()
        _incomingInvite.value = null
    }

    fun clearMessage() {
        _authMessage.value = null
    }
}