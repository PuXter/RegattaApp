package com.example.regattaapp.ui.screen.create

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.regattaapp.data.RegattaPoint
import com.example.regattaapp.utils.computeOffset
import com.example.regattaapp.viewmodel.RoomViewModel
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRoomScreen(navController: NavController, viewModel: RoomViewModel = viewModel()) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val coroutineScope = rememberCoroutineScope()

    var regattaName by remember { mutableStateOf("") }
    var windDirection by remember { mutableStateOf("") }
    var firstBuoyDistance by remember { mutableStateOf("") }

    val boatClasses = listOf("Optimist", "Cadet", "ILCA")
    var selectedBoatClass by remember { mutableStateOf(boatClasses[0]) }

    val courseTypes = listOf("trójkąt", "trapez")
    var selectedCourseType by remember { mutableStateOf(courseTypes[0]) }

    var boatClassMenuExpanded by remember { mutableStateOf(false) }
    var courseTypeMenuExpanded by remember { mutableStateOf(false) }

    var crewCount by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Utwórz pokój") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wstecz")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = regattaName,
                onValueChange = { regattaName = it },
                label = { Text("Nazwa regat") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = windDirection,
                onValueChange = {
                    if (it.all { char -> char.isDigit() }) windDirection = it
                },
                label = { Text("Kierunek wiatru (0–360)") },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(
                expanded = boatClassMenuExpanded,
                onExpandedChange = { boatClassMenuExpanded = !boatClassMenuExpanded }
            ) {
                OutlinedTextField(
                    readOnly = true,
                    value = selectedBoatClass,
                    onValueChange = {},
                    label = { Text("Klasa łódki") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(boatClassMenuExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = boatClassMenuExpanded,
                    onDismissRequest = { boatClassMenuExpanded = false }
                ) {
                    boatClasses.forEach {
                        DropdownMenuItem(
                            text = { Text(it) },
                            onClick = {
                                selectedBoatClass = it
                                boatClassMenuExpanded = false
                            }
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = courseTypeMenuExpanded,
                onExpandedChange = { courseTypeMenuExpanded = !courseTypeMenuExpanded }
            ) {
                OutlinedTextField(
                    readOnly = true,
                    value = selectedCourseType,
                    onValueChange = {},
                    label = { Text("Typ trasy") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(courseTypeMenuExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = courseTypeMenuExpanded,
                    onDismissRequest = { courseTypeMenuExpanded = false }
                ) {
                    courseTypes.forEach {
                        DropdownMenuItem(
                            text = { Text(it) },
                            onClick = {
                                selectedCourseType = it
                                courseTypeMenuExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = firstBuoyDistance,
                onValueChange = {
                    if (it.all { c -> c.isDigit() }) firstBuoyDistance = it
                },
                label = { Text("Odległość do boi nr 1 (200–5000 m)") },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = crewCount,
                onValueChange = {
                    if (it.all { c -> c.isDigit() }) crewCount = it
                },
                label = { Text("Ilość załóg (min. 1)") },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    coroutineScope.launch {
                        // Walidacja
                        val crew = crewCount.toIntOrNull()
                        val wind = windDirection.toIntOrNull()
                        val buoyDist = firstBuoyDistance.toIntOrNull()

                        if (regattaName.isBlank() || wind == null || buoyDist == null || crew == null ||
                            wind !in 0..360 || buoyDist !in 200..5000 || crew < 1
                        ) {
                            // Można dodać snackbar lub Toast
                            return@launch
                        }

                        // Uprawnienia lokalizacji
                        if (
                            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        ) {
                            // W produkcji należy poprosić o uprawnienia
                            return@launch
                        }

                        val location: Location? = fusedLocationClient.lastLocation.await()
                        val roomId = viewModel.createRoom(
                            regattaName = regattaName,
                            windDirection = wind,
                            boatClass = selectedBoatClass,
                            courseType = selectedCourseType,
                            firstBuoyDistance = buoyDist,
                            crewCount = crew,
                            startLat = location?.latitude ?: 0.0,
                            startLng = location?.longitude ?: 0.0
                        )

                        if (roomId != null) {
                            val bearingToStartBuoy = ((wind - 90 + 360) % 360)

                            val boatLength = when (selectedBoatClass) {
                                "Optimist" -> 2.3
                                "Cadet" -> 3.2
                                "ILCA" -> 4.2
                                else -> 3.0 // fallback
                            }

                            val startBuoyDistance = boatLength * 1.5 * crew// metry
                            // RC i Start są już obliczone
                            val rcLat = location?.latitude ?: 0.0
                            val rcLng = location?.longitude ?: 0.0

                            val (startBuoyLat, startBuoyLng) = computeOffset(rcLat, rcLng, startBuoyDistance, bearingToStartBuoy.toDouble())
                            val rcMidLat = (rcLat + startBuoyLat) / 2
                            val rcMidLng = (rcLng + startBuoyLng) / 2

                            val buoy1LatLng = computeOffset(rcMidLat, rcMidLng, buoyDist.toDouble(), wind.toDouble())
                            val buoy2LatLng = computeOffset(buoy1LatLng.first, buoy1LatLng.second, buoyDist * 1.5, ((wind - 120 + 360) % 360).toDouble())
                            val buoy3LatLng = computeOffset(buoy2LatLng.first, buoy2LatLng.second, buoyDist * 1.5, ((wind + 120) % 360).toDouble())

                            val points = if (selectedCourseType == "trójkąt") {
                                listOf(
                                    RegattaPoint("RC", rcLat, rcLng),
                                    RegattaPoint("Start", startBuoyLat, startBuoyLng),
                                    RegattaPoint("1", buoy1LatLng.first, buoy1LatLng.second),
                                    RegattaPoint("2", buoy2LatLng.first, buoy2LatLng.second),
                                    RegattaPoint("3", buoy3LatLng.first, buoy3LatLng.second)
                                )
                            } else { // trapez
                                val sideLength = buoyDist / 1.4

                                val buoy2LatLng = computeOffset(buoy1LatLng.first, buoy1LatLng.second, sideLength, ((wind - 105 + 360) % 360).toDouble())
                                val buoy4LatLng = computeOffset(buoy1LatLng.first, buoy1LatLng.second, buoyDist * 1.5, ((wind + 180) % 360).toDouble())
                                val buoy3LatLng = computeOffset(buoy4LatLng.first, buoy4LatLng.second, sideLength, ((wind - 75 + 360) % 360).toDouble())

                                listOf(
                                    RegattaPoint("RC", rcLat, rcLng),
                                    RegattaPoint("Start", startBuoyLat, startBuoyLng),
                                    RegattaPoint("1", buoy1LatLng.first, buoy1LatLng.second),
                                    RegattaPoint("2", buoy2LatLng.first, buoy2LatLng.second),
                                    RegattaPoint("3", buoy3LatLng.first, buoy3LatLng.second),
                                    RegattaPoint("4", buoy4LatLng.first, buoy4LatLng.second)
                                )
                            }

                            viewModel.addCoursePoints(roomId, points)

                            viewModel.waitForCoursePoints(roomId) {
                                navController.navigate("map/$roomId")
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Utwórz pokój")
            }
        }
    }
}