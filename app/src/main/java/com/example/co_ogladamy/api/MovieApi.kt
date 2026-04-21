package com.example.co_ogladamy.api

import com.example.co_ogladamy.model.MovieResponse
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

interface MovieApi {
    // Poprawiony endpoint: "movie/popular" zamiast "movies/popular"
    @GET("movie/popular")
    suspend fun getMovies(): MovieResponse
}

object RetrofitInstance {
    private const val BASE_URL = "https://api.themoviedb.org/3/"

    // 1. TUTAJ WKLEJ SWÓJ WYGENEROWANY KLUCZ Z TMDB
    private const val API_KEY = "21735d847a6453e818b2f4b0e4752c53"

    // 2. Interceptor - mechanizm, który automatycznie dokleja klucz do każdego zapytania
    private val requestInterceptor = Interceptor { chain ->
        val url = chain.request()
            .url
            .newBuilder()
            .addQueryParameter("api_key", API_KEY)
            .build()

        val request = chain.request()
            .newBuilder()
            .url(url)
            .build()

        chain.proceed(request)
    }

    // 3. Konfiguracja klienta z naszym interceptorem
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(requestInterceptor)
        .build()

    val api: MovieApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient) // 4. KLUCZOWE: Podpinamy klienta do Retrofita!
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MovieApi::class.java)
    }
}