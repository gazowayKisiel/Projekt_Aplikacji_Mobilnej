package com.example.co_ogladamy.data

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class RoomRepository {
    // pobiera root bazy danych (Twój poprawny link!)
    private val database = FirebaseDatabase.getInstance("https://coogladamy-default-rtdb.europe-west1.firebasedatabase.app").reference

    fun createRoom(onResult: (String?) -> Unit) {
        // generuje losowy 5 cyfrowy kod pokoju
        val roomCode = (10000..99999).random().toString()

        // dane do chmury
        val roomData = mapOf(
            "status" to "waiting",
            "createdAt" to System.currentTimeMillis()
        )

        // zapisuje bazy: tworzy galaz "rooms" -> i podgalaz z kodem
        database.child("rooms").child(roomCode).setValue(roomData)
            .addOnSuccessListener {
                onResult(roomCode) // sukces -> zwraca kod
            }
            .addOnFailureListener {
                onResult(null) // blad
            }
    }

    fun joinRoom(roomCode: String, onResult: (Boolean) -> Unit) {
        // szukanie pokoju o podanym kodzie w bazie
        database.child("rooms").child(roomCode).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    // potwierdza, ze kod jest poprawny i wpuszcza do poczekalni
                    onResult(true)
                } else {
                    onResult(false)
                }
            }
            .addOnFailureListener {
                onResult(false)
            }
    }


    fun startRoom(roomCode: String) {
        // funkcja tylko dla hosta pokoju
        database.child("rooms").child(roomCode).child("status").setValue("ready")
    }

    fun listenToRoomStatus(roomCode: String, onStatusChanged: (String) -> Unit) {
        // nasluchiwanie na zywo
        // jak Host kliknie start -> funkcja natychmiast o tym poinformuje wszystkie "telefony"
        database.child("rooms").child(roomCode).child("status")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val status = snapshot.getValue(String::class.java)
                    if (status != null) {
                        onStatusChanged(status)
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    // TODO: obsluga bledu
                }
            })
    }


    fun leaveRoom(roomCode: String) {
        // wchodzi do galezi "rooms" -> kod pokoju -> niszczy wszystko co tam jest
        database.child("rooms").child(roomCode).removeValue()
    }
}