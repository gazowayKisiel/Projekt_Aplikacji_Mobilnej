package com.example.co_ogladamy.model

import com.google.gson.annotations.SerializedName

data class Movie(
    val id: Int,
    val title: String,
    @SerializedName("overview")
    val description: String,
    @SerializedName("poster_path")
    val posterUrl: String?,
    @SerializedName("vote_average")
    val rating: Double = 0.0,
    @SerializedName("release_date")
    val releaseDate: String = ""
) {
    // Pomocnicze pole zwracające sam rok, np. "2024"
    val releaseYear: String
        get() = releaseDate.take(4)
}

data class MovieResponse(
    val results: List<Movie>
)

data class MovieGenre(val id: Int?, val name: String)

val AVAILABLE_GENRES = listOf(
    MovieGenre(null, "Wszystkie"),
    MovieGenre(28, "Akcja"),
    MovieGenre(35, "Komedia"),
    MovieGenre(27, "Horror"),
    MovieGenre(878, "Sci-Fi"),
    MovieGenre(18, "Dramat"),
    MovieGenre(10749, "Romans")
)