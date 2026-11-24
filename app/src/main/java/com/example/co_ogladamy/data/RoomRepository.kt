package com.example.co_ogladamy.data

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class RoomRepository {
    private val database = FirebaseDatabase
        .getInstance("https://coogladamy-default-rtdb.europe-west1.firebasedatabase.app")
        .reference

    fun createRoom(userId: String, username: String, genreId: Int?, onResult: (String?) -> Unit) {
        val roomCode = (10000..99999).random().toString()
        val roomData = mutableMapOf<String, Any>(
            "status" to "waiting",
            "createdAt" to System.currentTimeMillis()
        )
        if (genreId != null) {
            roomData["genreId"] = genreId
        }

        database.child("rooms").child(roomCode).setValue(roomData)
            .addOnSuccessListener {
                database.child("rooms").child(roomCode).child("participants").child(userId).setValue(username)
                onResult(roomCode)
            }
            .addOnFailureListener { onResult(null) }
    }

    fun joinRoom(roomCode: String, userId: String, username: String, onResult: (Boolean) -> Unit) {
        database.child("rooms").child(roomCode).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    database.child("rooms").child(roomCode).child("participants").child(userId).setValue(username)
                    onResult(true)
                } else {
                    onResult(false)
                }
            }
            .addOnFailureListener { onResult(false) }
    }

    fun startRoom(roomCode: String) {
        database.child("rooms").child(roomCode).child("status").setValue("ready")
    }

    fun listenToRoomStatus(roomCode: String, onStatusChanged: (String) -> Unit) {
        database.child("rooms").child(roomCode).child("status")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.getValue(String::class.java)?.let { onStatusChanged(it) }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // POPRAWKA: Gość usuwa tylko siebie, Host usuwa cały pokój
    fun leaveRoom(roomCode: String, userId: String, isHost: Boolean) {
        if (isHost) {
            database.child("rooms").child(roomCode).removeValue()
        } else {
            database.child("rooms").child(roomCode).child("participants").child(userId).removeValue()
        }
    }

    // NOWOŚĆ: Nasłuchiwanie listy uczestników w lobby na żywo
    fun listenToParticipants(roomCode: String, onParticipantsUpdated: (List<String>) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val names = snapshot.children.mapNotNull { it.getValue(String::class.java) }
                onParticipantsUpdated(names)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        database.child("rooms").child(roomCode).child("participants").addValueEventListener(listener)
        return listener
    }

    // NOWOŚĆ: Pobranie gatunku pokoju, żeby wszyscy mieli te same filmy
    fun getRoomGenre(roomCode: String, onResult: (Int?) -> Unit) {
        database.child("rooms").child(roomCode).child("genreId").get()
            .addOnSuccessListener { snapshot ->
                onResult(snapshot.getValue(Int::class.java))
            }
            .addOnFailureListener { onResult(null) }
    }

    fun saveVote(roomCode: String, movieId: String, userId: String, isLike: Boolean) {
        database.child("rooms")
            .child(roomCode)
            .child("votes")
            .child(movieId)
            .child(userId)
            .setValue(isLike)
    }

    // POPRAWKA: Sprawdzamy czy match już istnieje, zanim go zapiszemy (brak pętli zapisów)
    fun listenToVotes(roomCode: String): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lobbySize = snapshot.child("participants").childrenCount
                if (lobbySize == 0L) return

                val votesSnapshot = snapshot.child("votes")
                val matchesSnapshot = snapshot.child("matches")

                for (movieSnapshot in votesSnapshot.children) {
                    val movieId = movieSnapshot.key ?: continue

                    // Jeśli ten film jest już zapisany jako match, pomijamy go
                    if (matchesSnapshot.hasChild(movieId)) continue

                    var positiveVotes = 0.0
                    for (userVote in movieSnapshot.children) {
                        if (userVote.getValue(Boolean::class.java) == true) {
                            positiveVotes++
                        }
                    }

                    val percentage = (positiveVotes / lobbySize.toDouble()) * 100
                    if (percentage >= 80.0) {
                        database.child("rooms").child(roomCode).child("matches").child(movieId).setValue(true)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }

        database.child("rooms").child(roomCode).addValueEventListener(listener)
        return listener
    }

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

    // NOWOŚĆ: Czyste odpinanie starych listenerów przy zmianie pokoju
    fun removeListeners(
        roomCode: String,
        votesListener: ValueEventListener?,
        matchesListener: ValueEventListener?
    ) {
        votesListener?.let {
            database.child("rooms").child(roomCode).removeEventListener(it)
        }
        matchesListener?.let {
            database.child("rooms").child(roomCode).child("matches").removeEventListener(it)
        }
    }
}