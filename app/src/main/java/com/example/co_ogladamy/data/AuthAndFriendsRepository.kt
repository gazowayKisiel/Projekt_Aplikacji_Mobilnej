package com.example.co_ogladamy.data

import com.example.co_ogladamy.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class AuthAndFriendsRepository {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance("https://coogladamy-default-rtdb.europe-west1.firebasedatabase.app").reference

    val currentUser get() = auth.currentUser

    fun login(email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email.trim(), pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) onResult(true, null)
                else onResult(false, task.exception?.localizedMessage ?: "Błąd logowania")
            }
    }

    fun register(email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        val cleanEmail = email.trim().lowercase()
        auth.createUserWithEmailAndPassword(cleanEmail, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid ?: return@addOnCompleteListener
                    val userMap = mapOf("uid" to uid, "email" to cleanEmail)
                    // Zapisujemy profil użytkownika w bazie, aby można go było wyszukać po e-mailu
                    database.child("users").child(uid).setValue(userMap)
                        .addOnSuccessListener { onResult(true, null) }
                        .addOnFailureListener { onResult(false, it.localizedMessage) }
                } else {
                    onResult(false, task.exception?.localizedMessage ?: "Błąd rejestracji")
                }
            }
    }

    fun logout() {
        auth.signOut()
    }

    fun addFriendByEmail(friendEmail: String, onResult: (Boolean, String) -> Unit) {
        val myUid = currentUser?.uid ?: return
        val myEmail = currentUser?.email ?: return
        val cleanEmail = friendEmail.trim().lowercase()

        if (cleanEmail == myEmail.lowercase()) {
            onResult(false, "Nie możesz dodać samego siebie!")
            return
        }

        // Szukamy użytkownika o podanym adresie e-mail w gałęzi "users"
        database.child("users").orderByChild("email").equalTo(cleanEmail)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        onResult(false, "Nie znaleziono użytkownika z tym adresem e-mail.")
                        return
                    }
                    for (userSnap in snapshot.children) {
                        val friendUid = userSnap.key ?: continue
                        // Dodajemy relację obustronnie
                        database.child("users").child(myUid).child("friends").child(friendUid).setValue(cleanEmail)
                        database.child("users").child(friendUid).child("friends").child(myUid).setValue(myEmail)
                        onResult(true, "Dodano znajomego: $cleanEmail")
                        break
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    onResult(false, "Błąd bazy danych: ${error.message}")
                }
            })
    }

    fun listenToFriends(onFriendsUpdated: (List<UserProfile>) -> Unit): ValueEventListener? {
        val myUid = currentUser?.uid ?: return null
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<UserProfile>()
                for (child in snapshot.children) {
                    val uid = child.key ?: continue
                    val email = child.getValue(String::class.java) ?: ""
                    list.add(UserProfile(uid = uid, email = email))
                }
                onFriendsUpdated(list)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        database.child("users").child(myUid).child("friends").addValueEventListener(listener)
        return listener
    }

    // Wysyłanie zaproszenia do pokoju do znajomego
    fun sendRoomInvite(friendUid: String, roomCode: String) {
        val myEmail = currentUser?.email ?: "Znajomy"
        val inviteData = mapOf(
            "roomCode" to roomCode,
            "fromEmail" to myEmail
        )
        database.child("users").child(friendUid).child("roomInvite").setValue(inviteData)
    }

    // Nasłuchiwanie czy ktoś zaprosił nas do pokoju
    fun listenToRoomInvite(onInviteReceived: (String?, String?) -> Unit): ValueEventListener? {
        val myUid = currentUser?.uid ?: return null
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val roomCode = snapshot.child("roomCode").getValue(String::class.java)
                val fromEmail = snapshot.child("fromEmail").getValue(String::class.java)
                onInviteReceived(roomCode, fromEmail)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        database.child("users").child(myUid).child("roomInvite").addValueEventListener(listener)
        return listener
    }

    fun clearRoomInvite() {
        val myUid = currentUser?.uid ?: return
        database.child("users").child(myUid).child("roomInvite").removeValue()
    }
}