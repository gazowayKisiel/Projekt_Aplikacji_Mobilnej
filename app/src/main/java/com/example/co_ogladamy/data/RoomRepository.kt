package com.example.co_ogladamy.data

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class RoomRepository {
    private val database = FirebaseDatabase.getInstance("https://coogladamy-default-rtdb.europe-west1.firebasedatabase.app").reference

    fun createRoom(userId: String, onResult: (String?) -> Unit) {
        val roomCode = (10000..99999).random().toString()
        val roomData = mapOf(
            "status" to "waiting",
            "createdAt" to System.currentTimeMillis()
        )

        database.child("rooms").child(roomCode).setValue(roomData)
            .addOnSuccessListener {
                database.child("rooms").child(roomCode).child("participants").child(userId).setValue(true)
                onResult(roomCode)
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    fun joinRoom(roomCode: String, userId: String, onResult: (Boolean) -> Unit) {
        database.child("rooms").child(roomCode).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    database.child("rooms").child(roomCode).child("participants").child(userId).setValue(true)
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
        database.child("rooms").child(roomCode).child("status").setValue("ready")
    }

    fun listenToRoomStatus(roomCode: String, onStatusChanged: (String) -> Unit) {
        database.child("rooms").child(roomCode).child("status")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val status = snapshot.getValue(String::class.java)
                    if (status != null) {
                        onStatusChanged(status)
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    fun leaveRoom(roomCode: String) {
        database.child("rooms").child(roomCode).removeValue()
    }

    fun saveVote(roomCode: String, movieId: String, userId: String, isLike: Boolean) {
        database.child("rooms")
            .child(roomCode)
            .child("votes")
            .child(movieId)
            .child(userId)
            .setValue(isLike)
    }

    //  Algorytm po wykryciu dopasowania automatycznie zapisuje je do bazy jako trwały match pokoju
    fun listenToVotes(roomCode: String): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lobbySize = snapshot.child("participants").childrenCount
                if (lobbySize == 0L) return

                val votesSnapshot = snapshot.child("votes")

                for (movieSnapshot in votesSnapshot.children) {
                    val movieId = movieSnapshot.key ?: continue

                    var positiveVotes = 0.0
                    for (userVote in movieSnapshot.children) {
                        if (userVote.getValue(Boolean::class.java) == true) {
                            positiveVotes++
                        }
                    }

                    val percentage = (positiveVotes / lobbySize.toDouble()) * 100

                    if (percentage >= 80.0) {
                        // NOWOŚĆ: Rejestrujemy dopasowanie w chmurze w gałęzi "matches"
                        database.child("rooms").child(roomCode).child("matches").child(movieId).setValue(true)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        database.child("rooms").child(roomCode).addValueEventListener(listener)
        return listener
    }

    // Słuchacz, który pobiera gotową listę ID wszystkich dopasowanych filmów w pokoju
    fun listenToMatches(roomCode: String, onMatchesUpdated: (List<String>) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val matchIds = snapshot.children.mapNotNull { it.key }
                onMatchesUpdated(matchIds)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        database.child("rooms").child(roomCode).child("matches").addValueEventListener(listener)
        return listener
    }
}