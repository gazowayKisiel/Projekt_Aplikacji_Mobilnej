package com.example.co_ogladamy.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.co_ogladamy.api.RetrofitInstance
import com.example.co_ogladamy.data.RoomRepository
import com.example.co_ogladamy.model.Movie
import com.google.firebase.database.ValueEventListener
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
    private var matchesListener: ValueEventListener? = null

    private val _matchedMovie = MutableStateFlow<Movie?>(null)
    val matchedMovie: StateFlow<Movie?> = _matchedMovie.asStateFlow()

    private val _allMatches = MutableStateFlow<List<Movie>>(emptyList())
    val allMatches: StateFlow<List<Movie>> = _allMatches.asStateFlow()

    private val shownMatchIds = mutableSetOf<String>()
    private var currentMatchIdsFromDb = emptyList<String>()

    // Paginacja i filtrowanie po gatunku pokoju
    private var currentPage = 1
    private var currentGenreId: Int? = null
    private var isFetchingMore = false

    private fun fetchMovies(page: Int = 1, genreId: Int? = null) {
        if (isFetchingMore) return
        viewModelScope.launch {
            try {
                isFetchingMore = true
                if (page == 1) _isLoading.value = true

                val response = RetrofitInstance.api.getMovies(genreId = genreId, page = page)
                allMoviesBackup = allMoviesBackup + response.results
                _movies.value = _movies.value + response.results

                if (currentMatchIdsFromDb.isNotEmpty()) {
                    processIncomingMatches(currentMatchIdsFromDb)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
                isFetchingMore = false
            }
        }
    }

    fun initRoom(code: String, uId: String) {
        if (this.roomCode == code && matchesListener != null) return

        // 1. Odpinamy stare listenery z poprzedniego pokoju
        clearCurrentListeners()

        // 2. Resetujemy stan pod nowy pokój
        this.roomCode = code
        this.userId = uId
        shownMatchIds.clear()
        currentMatchIdsFromDb = emptyList()
        _matchedMovie.value = null
        _allMatches.value = emptyList()

        // 3. Pobieramy gatunek przypisany do pokoju i ładujemy pierwszą stronę filmów
        repository.getRoomGenre(code) { genreId ->
            currentGenreId = genreId
            currentPage = 1
            allMoviesBackup = emptyList()
            _movies.value = emptyList()
            fetchMovies(page = 1, genreId = genreId)
        }

        // 4. Podpinamy świeże listenery dla nowego pokoju
        votesListener = repository.listenToVotes(code)
        matchesListener = repository.listenToMatches(code) { matchIds ->
            processIncomingMatches(matchIds)
        }
    }

    private fun processIncomingMatches(matchIds: List<String>) {
        currentMatchIdsFromDb = matchIds
        if (allMoviesBackup.isEmpty()) return

        val matchedMoviesList = matchIds.mapNotNull { id ->
            allMoviesBackup.find { it.id.toString() == id }
        }
        _allMatches.value = matchedMoviesList

        if (_matchedMovie.value == null) {
            showNextUnshownMatch(matchIds)
        }
    }

    private fun showNextUnshownMatch(matchIds: List<String>) {
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
        checkAndLoadMoreMovies()
    }

    fun onSwipeLeft(movie: Movie) {
        roomCode?.let { code ->
            repository.saveVote(code, movie.id.toString(), userId, false)
        }
        _movies.value = _movies.value.filter { it.id != movie.id }
        checkAndLoadMoreMovies()
    }

    private fun checkAndLoadMoreMovies() {
        if (_movies.value.size <= 3 && !isFetchingMore) {
            currentPage++
            fetchMovies(page = currentPage, genreId = currentGenreId)
        }
    }

    fun dismissMatch() {
        _matchedMovie.value?.let { movie ->
            shownMatchIds.add(movie.id.toString())
        }
        _matchedMovie.value = null
        showNextUnshownMatch(currentMatchIdsFromDb)
    }

    private fun clearCurrentListeners() {
        roomCode?.let { oldCode ->
            repository.removeListeners(oldCode, votesListener, matchesListener)
        }
        votesListener = null
        matchesListener = null
    }

    override fun onCleared() {
        super.onCleared()
        clearCurrentListeners()
    }
}