package com.example.co_ogladamy.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.co_ogladamy.model.Movie
import com.example.co_ogladamy.viewmodel.MovieViewModel
import com.example.co_ogladamy.viewmodel.RoomViewModel

@Composable
fun MainScreen(
    viewModel: MovieViewModel,
    roomViewModel: RoomViewModel,
    onLeaveRoom: () -> Unit
) {
    val movies by viewModel.movies.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()


    Column(modifier = Modifier.fillMaxSize()) {

        // pasek z przyciskiem wyjscia
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Pokój: ${roomViewModel.currentRoomCode ?: "Brak"}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Button(
                onClick = {
                    // wywoluje usuniecie z Firebase i wraca do WelcomeScreen
                    roomViewModel.leaveCurrentRoom(onNavigateBack = onLeaveRoom)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Wyjdź z pokoju")
            }
        }


        // lista filmow
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                    items(movies) { movie ->
                        MovieCard(movie)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun MovieCard(movie: Movie) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            AsyncImage(
                model = "https://image.tmdb.org/t/p/w500${movie.posterUrl}",
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.66f), // proporcja plakatu 2:3 (wysokosc-szerokosc)
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = movie.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}