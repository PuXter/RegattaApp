package com.example.regattaapp.ui.screen.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Location
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.regattaapp.R
import com.example.regattaapp.viewmodel.RoomViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(roomId: String, navController: NavController) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val coroutineScope = rememberCoroutineScope()
    val viewModel: RoomViewModel = viewModel()

    val room by viewModel.currentRoom.collectAsState()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    var location by remember { mutableStateOf<Location?>(null) }
    var mapView by remember { mutableStateOf<MapView?>(null) }

    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        getLastLocation(context, fusedLocationClient) { loc -> location = loc }
        viewModel.joinRoom(roomId)
    }

    LaunchedEffect(location, mapView, room?.coursePoints) {
        val map = mapView ?: return@LaunchedEffect
        val loc = location ?: return@LaunchedEffect
        val points = room?.coursePoints ?: return@LaunchedEffect

        map.overlays.clear()

        // Twoja pozycja
        val userGeo = GeoPoint(loc.latitude, loc.longitude)
        map.controller.setCenter(userGeo)

        val userMarker = Marker(map).apply {
            position = userGeo
            title = "Twoja pozycja"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        map.overlays.add(userMarker)

        // Znaczniki punktów regatowych
        for (point in points) {
            val geo = GeoPoint(point.latitude, point.longitude)
            val marker = Marker(map).apply {
                position = geo
                title = point.name
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = when (point.name) {
                    "RC" -> getScaledIcon(context, map, R.drawable.rc, 10.0)
                    "Start" -> getScaledIcon(context, map, R.drawable.start_buoy, 10.0)
                    "1" -> getScaledIcon(context, map, R.drawable.buoy_1, 10.0)
                    "2" -> getScaledIcon(context, map, R.drawable.buoy_2, 10.0)
                    "3" -> getScaledIcon(context, map, R.drawable.buoy_3, 10.0)
                    "4" -> getScaledIcon(context, map, R.drawable.buoy_4, 10.0)
                    else -> getScaledIcon(context, map, R.drawable.buoy_default, 10.0)
                }
            }
            map.overlays.add(marker)
        }

        map.invalidate()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(room?.name ?: "Mapa") },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Wróć",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    if (room?.createdBy == currentUserId) {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    val success = viewModel.deleteCurrentRoom()
                                    if (success) {
                                        Toast.makeText(context, "Pokój usunięty", Toast.LENGTH_SHORT).show()
                                        navController.navigate("home") {
                                            popUpTo("map/$roomId") { inclusive = true }
                                        }
                                    } else {
                                        Toast.makeText(context, "Błąd przy usuwaniu pokoju", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color.Red, shape = MaterialTheme.shapes.extraSmall),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("X", color = Color.Black, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (location != null) {
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(15.0)
                        mapView = this
                    }
                },
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@SuppressLint("MissingPermission")
fun getLastLocation(
    context: Context,
    fusedLocationClient: FusedLocationProviderClient,
    onLocationReady: (Location?) -> Unit
) {
    if (
        ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
        ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
    ) {
        onLocationReady(null)
        return
    }

    fusedLocationClient.lastLocation
        .addOnSuccessListener { location: Location? ->
            onLocationReady(location)
        }
}

fun metersToPixels(mapView: MapView, meters: Double): Float {
    val projection = mapView.projection
    val center = mapView.mapCenter
    val geoPointA = center
    val geoPointB = GeoPoint(geoPointA.latitude + 0.00001, geoPointA.longitude)

    val pointA = projection.toPixels(geoPointA, null)
    val pointB = projection.toPixels(geoPointB, null)

    val results = FloatArray(1)
    android.location.Location.distanceBetween(
        geoPointA.latitude, geoPointA.longitude,
        geoPointB.latitude, geoPointB.longitude,
        results
    )
    val distInMeters = results[0].takeIf { it > 0 } ?: return 30f
    val distInPixels = kotlin.math.abs(pointB.y - pointA.y)

    return (distInPixels / distInMeters * meters).toFloat()
}

fun getScaledIcon(context: Context, mapView: MapView, resId: Int, desiredMeters: Double): Drawable {
    val original = ResourcesCompat.getDrawable(context.resources, resId, null) as? BitmapDrawable
        ?: return ResourcesCompat.getDrawable(context.resources, android.R.drawable.ic_dialog_alert, null)!!

    val bitmap = original.bitmap
    val sizeInPixels = metersToPixels(mapView, desiredMeters).toInt().coerceAtLeast(1)
    val scaledBitmap = Bitmap.createScaledBitmap(bitmap, sizeInPixels, sizeInPixels, true)
    return BitmapDrawable(context.resources, scaledBitmap)
}
