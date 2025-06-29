package com.example.regattaapp.ui.screen.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.regattaapp.viewmodel.RoomViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, viewModel: RoomViewModel = viewModel()) {
    val rooms by viewModel.allRooms.collectAsState()
    rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.loadAllRooms()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Regaty") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            // Górne przyciski
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { navController.navigate("create") },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text("Stwórz pokój", color = MaterialTheme.colorScheme.onSurface)
                }
                Button(
                    onClick = { navController.navigate("join") },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text("Dołącz do pokoju", color = MaterialTheme.colorScheme.onSurface)
                }
            }

            // Lista pokoi
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val sortedRooms = rooms.sortedBy { it.room.name }
                items(sortedRooms) { roomWithId ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = roomWithId.room.name)

                            Button(
                                onClick = {
                                    navController.navigate("map/${roomWithId.id}")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Dołącz")
                            }
                        }
                    }
                }
            }
        }
    }
}