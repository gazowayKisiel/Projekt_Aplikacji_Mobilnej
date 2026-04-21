package com.example.co_ogladamy.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.co_ogladamy.api.RetrofitInstance
import com.example.co_ogladamy.model.Movie
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MovieViewModel : ViewModel() {
    private val _movies = MutableStateFlow<List<Movie>>(emptyList())
    val movies: StateFlow<List<Movie>> = _movies.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        fetchMovies()
    }

    private fun fetchMovies() {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                // --- PRAWDZIWE POBIERANIE Z BAZY ---
                val response = RetrofitInstance.api.getMovies()
                _movies.value = response.results

                /* --- SYMULACJA / BACKUP NA WYPADEK AWARII (Zakomentowane) ---
                kotlinx.coroutines.delay(500)
                _movies.value = listOf(
                    Movie(id = 1, title = "Incepcja", description = "...", posterUrl = "/oYuLEt3zVCKq57qu2F8dT7NIa6f.jpg"),
                    Movie(id = 2, title = "Venom", description = "...", posterUrl = "/aosm8NMQ3UyoBVpSxyimorCQykC.jpg")
                )
                */

            } catch (e: Exception) {
                // Jeśli API wywali błąd (np. brak internetu),
                // tutaj możesz ewentualnie odkomentować listę testową jako "fallback"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}