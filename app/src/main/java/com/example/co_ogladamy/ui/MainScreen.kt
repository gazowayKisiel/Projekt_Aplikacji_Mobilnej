package com.example.co_ogladamy.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.co_ogladamy.model.Movie
import com.example.co_ogladamy.viewmodel.MovieViewModel
import com.example.co_ogladamy.viewmodel.RoomViewModel
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

@Composable
fun MainScreen(
    viewModel: MovieViewModel,
    roomViewModel: RoomViewModel,
    onLeaveRoom: () -> Unit
) {
    val movies by viewModel.movies.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val matchedMovie by viewModel.matchedMovie.collectAsState()
    val allMatches by viewModel.allMatches.collectAsState()

    var showMatchesDialog by remember { mutableStateOf(false) }

    // Kluczowe dla synchronizacji: odpala listenery dokładnie raz dla danego kodu pokoju
    val roomCode = roomViewModel.currentRoomCode
    LaunchedEffect(roomCode) {
        if (roomCode != null) {
            viewModel.initRoom(roomCode, roomViewModel.myUserId)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Górny pasek: Wyjście, kod pokoju i przycisk z listą matchy
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { roomViewModel.leaveCurrentRoom(onLeaveRoom) }
            ) {
                Text("Wyjdź")
            }

            if (roomCode != null) {
                Text(
                    text = "Pokój: $roomCode",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(onClick = { showMatchesDialog = true }) {
                Text("Matche (${allMatches.size})")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Główna zawartość: ładowanie, brak filmów lub aktualna karta filmu
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator()
                }
                movies.isEmpty() -> {
                    Text(
                        text = "Brak kolejnych filmów do wyświetlenia.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                else -> {
                    val currentMovie = movies.first()
                    MovieCard(
                        movie = currentMovie,
                        onDislike = { viewModel.onSwipeLeft(currentMovie) },
                        onLike = { viewModel.onSwipeRight(currentMovie) }
                    )
                }
            }
        }
    }

    // Popup wyświetlany w tym samym momencie na wszystkich urządzeniach przy nowym matchu
    matchedMovie?.let { movie ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissMatch() },
            title = { Text("🎉 Mamy Match!") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (!movie.posterUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = "https://image.tmdb.org/t/p/w500${movie.posterUrl}",
                            contentDescription = movie.title,
                            modifier = Modifier
                                .height(220.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    Text(
                        text = movie.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "⭐ ${String.format("%.1f", movie.rating)} • ${movie.releaseYear}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.dismissMatch() }) {
                    Text("Wróć")
                }
            }
        )
    }

    // Okienko z listą wszystkich dotychczasowych dopasowań (z ExtraComponents.kt)
    if (showMatchesDialog) {
        MatchesListDialog(
            matches = allMatches,
            onDismiss = { showMatchesDialog = false }
        )
    }
}

@Composable
private fun MovieCard(
    movie: Movie,
    onDislike: () -> Unit,
    onLike: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (!movie.posterUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = "https://image.tmdb.org/t/p/w500${movie.posterUrl}",
                        contentDescription = movie.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "⭐ ${String.format("%.1f", movie.rating)} • ${movie.releaseYear}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = movie.description.ifBlank { "Brak opisu filmu." },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(
                    onClick = onDislike,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("👎 Nie")
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = onLike,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("👍 Tak")
                }
            }
        }
    }
}
@Composable
fun MatchesListDialog(
    matches: List<Movie>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Wspólne dopasowania (${matches.size})") },
        text = {
            if (matches.isEmpty()) {
                Text("Jeszcze brak wspólnych filmów. Swipujcie dalej!")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(matches) { movie ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${movie.title} (${movie.releaseYear})",
                                modifier = Modifier.weight(1f)
                            )
                            Text(text = "⭐ ${String.format("%.1f", movie.rating)}")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Zamknij")
            }
        }
    )
}