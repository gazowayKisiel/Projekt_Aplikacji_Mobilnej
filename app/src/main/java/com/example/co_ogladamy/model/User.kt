package com.example.co_ogladamy.model

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val inviteRoomCode: String? = null // Kod pokoju, jeśli znajomy wysłał zaproszenie
)