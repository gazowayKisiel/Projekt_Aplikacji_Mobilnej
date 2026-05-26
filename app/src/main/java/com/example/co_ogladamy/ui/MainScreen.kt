package com.example.co_ogladamy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.co_ogladamy.model.Movie
import com.example.co_ogladamy.viewmodel.MovieViewModel
import com.example.co_ogladamy.viewmodel.RoomViewModel
import kotlin.math.roundToInt

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
            .graphicsLayer(rotationZ = offsetX / 20f)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        if (offsetX > 250) onSwipeRight()
                        else if (offsetX < -250) onSwipeLeft()
                        else {
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

@Composable
fun MainScreen(
    viewModel: MovieViewModel,
    roomViewModel: RoomViewModel,
    onLeaveRoom: () -> Unit
) {
    val movies by viewModel.movies.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val matchedMovie by viewModel.matchedMovie.collectAsState()
    val allMatches by viewModel.allMatches.collectAsState() // NOWOŚĆ: Obserwujemy listę matchów

    val roomCode = roomViewModel.currentRoomCode
    val myUserId = roomViewModel.myUserId

    // Stan kontrolujący wyświetlanie osobnego okna z listą matchów
    var showMatchesDialog by remember { mutableStateOf(false) }

    LaunchedEffect(roomCode, myUserId) {
        if (roomCode != null) {
            viewModel.initRoom(roomCode, myUserId)
        }
    }

    // Wyświetlanie osobnego okna z listą wszystkich matchów grupy
    if (showMatchesDialog) {
        MatchesListDialog(
            matches = allMatches,
            onDismiss = { showMatchesDialog = false }
        )
    }

    if (matchedMovie != null) {
        MatchScreen(
            movie = matchedMovie!!,
            onContinue = { viewModel.dismissMatch() } // Kliknięcie chowa popup i odblokowuje dalsze szukanie!
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
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                // Dynamiczny przycisk otwierający okno ze wszystkimi matchami
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { showMatchesDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Matche (${allMatches.size})")
                    }
                    Button(
                        onClick = { roomViewModel.leaveCurrentRoom(onNavigateBack = onLeaveRoom) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Wyjdź")
                    }
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
                text = "Grupa chce obejrzeć:",
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
                Text("Oglądamy dalej! (Wróć)")
            }
        }
    }
}

// Komponent osobnego okna (Dialogu) prezentujący dotychczasowe dopasowania pokoju
@Composable
fun MatchesListDialog(
    matches: List<Movie>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dopasowane filmy pokoju") },
        text = {
            if (matches.isEmpty()) {
                Text("Brak dopasowań do tej pory. Głosujcie dalej!")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(matches) { movie ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = "https://image.tmdb.org/t/p/w200${movie.posterUrl}",
                                contentDescription = null,
                                modifier = Modifier
                                    .size(60.dp, 90.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = movie.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = movie.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(top = 12.dp))
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