package com.example.co_ogladamy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.co_ogladamy.model.Movie
import com.example.co_ogladamy.viewmodel.MovieViewModel
import com.example.co_ogladamy.viewmodel.RoomViewModel
import kotlin.math.roundToInt

// --- TWOJA WŁASNA, NIEZAWODNA KARTA TINDEROWA ---
@Composable
fun SwipeableCard(
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .graphicsLayer(
                rotationZ = offsetX / 20f // Lekki obrót karty przy przesuwaniu - daje świetny efekt!
            )
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        // Jeśli przesunięto mocno w prawo lub lewo - zatwierdzamy
                        if (offsetX > 250) {
                            onSwipeRight()
                        } else if (offsetX < -250) {
                            onSwipeLeft()
                        } else {
                            // Jeśli za słabo, karta wraca na środek
                            offsetX = 0f
                            offsetY = 0f
                        }
                    }
                ) { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            }
    ) {
        content()
    }
}
// ------------------------------------------------

@Composable
fun MainScreen(
    viewModel: MovieViewModel,
    roomViewModel: RoomViewModel,
    onLeaveRoom: () -> Unit
) {
    val movies by viewModel.movies.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val matchedMovie by viewModel.matchedMovie.collectAsState()

    if (matchedMovie != null) {
        MatchScreen(
            movie = matchedMovie!!,
            onContinue = { viewModel.dismissMatch() }
        )
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
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
                    onClick = { roomViewModel.leaveCurrentRoom(onNavigateBack = onLeaveRoom) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Wyjdź")
                }
            }

            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (isLoading) {
                    CircularProgressIndicator()
                } else if (movies.isEmpty()) {
                    Text("Oceniono wszystkie filmy!", style = MaterialTheme.typography.bodyLarge)
                } else {
                    movies.reversed().forEach { movie ->
                        SwipeableCard(
                            modifier = Modifier.padding(16.dp),
                            onSwipeRight = { viewModel.onSwipeRight(movie) },
                            onSwipeLeft = { viewModel.onSwipeLeft(movie) }
                        ) {
                            MovieCard(movie = movie)
                        }
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
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column {
            AsyncImage(
                model = "https://image.tmdb.org/t/p/w500${movie.posterUrl}",
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.66f),
                contentScale = ContentScale.Crop
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

@Composable
fun MatchScreen(movie: Movie, onContinue: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "IT'S A MATCH!",
                style = MaterialTheme.typography.displayMedium,
                color = Color.Green,
                fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(24.dp))

            AsyncImage(
                model = "https://image.tmdb.org/t/p/w500${movie.posterUrl}",
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .aspectRatio(0.66f)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Oboje chcecie obejrzeć:",
                style = MaterialTheme.typography.titleMedium,
                color = Color.LightGray
            )
            Text(
                text = movie.title,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onContinue,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Green, contentColor = Color.Black)
            ) {
                Text("Oglądamy! (Wróć)")
            }
        }
    }
}