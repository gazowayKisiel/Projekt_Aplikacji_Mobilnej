package com.example.co_ogladamy.model

import com.google.gson.annotations.SerializedName

data class Movie(
    val id: Int,
    val title: String,
    @SerializedName("overview")
    val description: String,
    @SerializedName("poster_path")
    val posterUrl: String
)

data class MovieResponse(
    val results: List<Movie>
)