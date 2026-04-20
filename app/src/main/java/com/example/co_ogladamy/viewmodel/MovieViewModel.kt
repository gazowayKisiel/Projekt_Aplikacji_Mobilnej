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
            // SYMULACJA NA REVIEW MEETING:
            _isLoading.value = true

            // Symulujemy opóźnienie internetu (pół sekundy)
            kotlinx.coroutines.delay(500)

            _movies.value = listOf(
                Movie(
                    id = 1,
                    title = "Incepcja",
                    description = "Dom Cobb jest zręcznym złodziejem, absolutnym mistrzem w niebezpiecznej sztuce ekstrakcji, polegającej na kradzieży cennych sekretów z podświadomości podczas snu.",
                    posterUrl = "/oYuLEt3zVCKq57qu2F8dT7NIa6f.jpg"
                ),
                Movie(
                    id = 2,
                    title = "Venom: Ostatni Taniec",
                    description = "Eddie i Venom uciekają. Ścigani przez oba ich światy i w obliczu zaciskającej się sieci, duet zmuszony jest podjąć druzgocącą decyzję.",
                    posterUrl = "/aosm8NMQ3UyoBVpSxyimorCQykC.jpg"
                )
            )

            _isLoading.value = false

            /* --- ZAKOMENTOWANY PRAWDZIWY KOD NA PÓŹNIEJ ---
            try {
                _isLoading.value = true
                val response = RetrofitInstance.api.getMovies()
                _movies.value = response.results
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
            */
        }
    }
}