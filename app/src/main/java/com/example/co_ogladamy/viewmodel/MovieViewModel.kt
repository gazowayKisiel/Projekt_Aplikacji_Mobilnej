package com.example.co_ogladamy.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.co_ogladamy.api.RetrofitInstance
import com.example.co_ogladamy.data.RoomRepository
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.example.co_ogladamy.model.Movie
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MovieViewModel : ViewModel() {
    private val repository = RoomRepository()

    private val _movies = MutableStateFlow<List<Movie>>(emptyList())
    val movies: StateFlow<List<Movie>> = _movies.asStateFlow()

    private var allMoviesBackup = emptyList<Movie>()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var roomCode: String? = null
    private var userId: String = ""

    private var votesListener: ValueEventListener? = null
    // Słuchacz dedykowany dla bazy dopasowań
    private var matchesListener: ValueEventListener? = null

    private val _matchedMovie = MutableStateFlow<Movie?>(null)
    val matchedMovie: StateFlow<Movie?> = _matchedMovie.asStateFlow()

    // Strumień przechowujący pełne dane wszystkich trafionych filmów
    private val _allMatches = MutableStateFlow<List<Movie>>(emptyList())
    val allMatches: StateFlow<List<Movie>> = _allMatches.asStateFlow()

    // Lokalny zestaw ID popupów, które użytkownik już zamknął kliknięciem "Wróć"
    private val shownMatchIds = mutableSetOf<String>()
    private var currentMatchIdsFromDb = emptyList<String>()

    init {
        fetchMovies()
    }

    private fun fetchMovies() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val response = RetrofitInstance.api.getMovies()
                allMoviesBackup = response.results
                _movies.value = response.results

                // Jeśli baza danych zwróciła matche zanim API skończyło się pobierać, parsujemy je teraz
                if (currentMatchIdsFromDb.isNotEmpty()) {
                    processIncomingMatches(currentMatchIdsFromDb)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun initRoom(code: String, uId: String) {
        this.roomCode = code
        this.userId = uId

        fetchMovies()

        if (votesListener == null) {
            votesListener = repository.listenToVotes(code)
        }

        // NOWOŚĆ: Rozpoczynamy słuchanie listy zatwierdzonych dopasowań
        if (matchesListener == null) {
            matchesListener = repository.listenToMatches(code) { matchIds ->
                processIncomingMatches(matchIds)
            }
        }
    }

    // Przetwarzanie identyfikatorów matchów z bazy na obiekty graficzne filmów
    private fun processIncomingMatches(matchIds: List<String>) {
        currentMatchIdsFromDb = matchIds
        if (allMoviesBackup.isEmpty()) return // Czekamy, aż API dostarczy bazę filmów

        // Mapujemy surowe ID z Firebase na obiekty filmowe z TMDB dla listy głównej
        val matchedMoviesList = matchIds.mapNotNull { id ->
            allMoviesBackup.find { it.id.toString() == id }
        }
        _allMatches.value = matchedMoviesList

        // Sprawdzamy, czy pojawił się nowy match, którego ten telefon JESZCZE NIE WYŚWIETLIŁ jako popup
        val newMatchId = matchIds.firstOrNull { id -> !shownMatchIds.contains(id) }
        if (newMatchId != null) {
            val match = allMoviesBackup.find { it.id.toString() == newMatchId }
            if (match != null) {
                _matchedMovie.value = match
            }
        }
    }

    fun onSwipeRight(movie: Movie) {
        roomCode?.let { code ->
            repository.saveVote(code, movie.id.toString(), userId, true)
        }
        _movies.value = _movies.value.filter { it.id != movie.id }
    }

    fun onSwipeLeft(movie: Movie) {
        roomCode?.let { code ->
            repository.saveVote(code, movie.id.toString(), userId, false)
        }
        _movies.value = _movies.value.filter { it.id != movie.id }
    }

    // Kliknięcie "Wróć" zapisuje film jako "odznaczony", dzięki czemu szukamy kolejnych
    fun dismissMatch() {
        _matchedMovie.value?.let { movie ->
            shownMatchIds.add(movie.id.toString()) // Zapamiętaj, że ten popup został zamknięty
        }
        _matchedMovie.value = null // Ukryj popup
    }

    override fun onCleared() {
        super.onCleared()
        roomCode?.let { code ->
            votesListener?.let { listener ->
                FirebaseDatabase.getInstance("https://coogladamy-default-rtdb.europe-west1.firebasedatabase.app")
                    .reference.child("rooms").child(code).removeEventListener(listener)
            }
            matchesListener?.let { listener ->
                FirebaseDatabase.getInstance("https://coogladamy-default-rtdb.europe-west1.firebasedatabase.app")
                    .reference.child("rooms").child(code).child("matches").removeEventListener(listener)
            }
        }
    }
}