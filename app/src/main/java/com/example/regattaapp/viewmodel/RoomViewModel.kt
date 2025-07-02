package com.example.regattaapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.regattaapp.data.RegattaPoint
import com.example.regattaapp.data.RegattaRoom
import com.example.regattaapp.data.RoomRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.example.regattaapp.data.RoomWithId
import com.example.regattaapp.utils.computeOffset
import android.location.Location
import kotlinx.coroutines.tasks.await

class RoomViewModel(
    private val repository: RoomRepository = RoomRepository()
) : ViewModel() {
    private val roomRepository = RoomRepository()

    private val _currentRoom = MutableStateFlow<RegattaRoom?>(null)
    val currentRoom: StateFlow<RegattaRoom?> = _currentRoom

    private var roomId: String? = null

    private val _allRooms = MutableStateFlow<List<RoomWithId>>(emptyList())
    val allRooms: StateFlow<List<RoomWithId>> = _allRooms

    suspend fun createRoom(
        regattaName: String,
        windDirection: Int,
        boatClass: String,
        courseType: String,
        firstBuoyDistance: Int,
        crewCount: Int,
        startLat: Double,
        startLng: Double
    ): String? {
        val createdRoomId = repository.createRoom(
            regattaName,
            windDirection,
            boatClass,
            courseType,
            firstBuoyDistance,
            crewCount,
            startLat,
            startLng
        )
        roomId = createdRoomId

        createdRoomId?.let {
            observeRoom(it)
        }

        return createdRoomId
    }

    suspend fun joinRoom(existingRoomId: String): Boolean {
        val success = repository.joinRoom(existingRoomId)
        if (success) {
            roomId = existingRoomId
            observeRoom(existingRoomId)
        }
        return success
    }

    suspend fun deleteCurrentRoom(): Boolean {
        val id = roomId ?: return false
        return repository.deleteRoom(id)
    }

    fun loadAllRooms() {
        viewModelScope.launch {
            try {
                val rooms = repository.getAllRooms()
                _allRooms.value = rooms
            } catch (e: Exception) {
                Log.e("RoomViewModel", "Failed to load rooms", e)
            }
        }
    }

    fun addCoursePoints(roomId: String, points: List<RegattaPoint>) {
        viewModelScope.launch {
            repository.addCoursePoints(roomId, points)
        }
    }

    private fun observeRoom(roomId: String) {
        repository.observeRoom(roomId) { room ->
            if (room != null) {
                Log.d("RoomViewModel", "Zaobserwowany pokój: ${room.name}, punkty: ${room.coursePoints}")
                _currentRoom.value = room
            } else {
                Log.w("RoomViewModel", "Room is null or no longer exists.")
            }
        }
    }

    fun waitForCoursePoints(roomId: String, onReady: () -> Unit) {
        repository.getRoomWithListener(roomId) { room ->
            if (!room?.coursePoints.isNullOrEmpty()) {
                onReady()
            }
        }
    }

    fun resetCourse(currentLocation: Location, onDone: () -> Unit) {
        val room = _currentRoom.value ?: return
        val id = roomId ?: return

        viewModelScope.launch {
            val wind = room.windDirection
            val buoyDist = room.firstBuoyDistance
            val crew = room.crewCount
            val courseType = room.courseType
            val boatLength = when (room.boatClass) {
                "Optimist" -> 2.3
                "Cadet" -> 3.2
                "ILCA" -> 4.2
                else -> 3.0
            }

            val rcLat = currentLocation.latitude
            val rcLng = currentLocation.longitude

            val bearingToStartBuoy = ((wind - 90 + 360) % 360)
            val startBuoyDistance = boatLength * 1.5 * crew
            val (startBuoyLat, startBuoyLng) = computeOffset(rcLat, rcLng, startBuoyDistance, bearingToStartBuoy.toDouble())
            val rcMidLat = (rcLat + startBuoyLat) / 2
            val rcMidLng = (rcLng + startBuoyLng) / 2

            val buoy1LatLng = computeOffset(rcMidLat, rcMidLng, buoyDist.toDouble(), wind.toDouble())

            val newPoints = if (courseType == "trójkąt") {
                val buoy2LatLng = computeOffset(buoy1LatLng.first, buoy1LatLng.second, buoyDist * 1.5, ((wind - 120 + 360) % 360).toDouble())
                val buoy3LatLng = computeOffset(buoy2LatLng.first, buoy2LatLng.second, buoyDist * 1.5, ((wind + 120) % 360).toDouble())
                listOf(
                    RegattaPoint("RC", rcLat, rcLng),
                    RegattaPoint("Start", startBuoyLat, startBuoyLng),
                    RegattaPoint("1", buoy1LatLng.first, buoy1LatLng.second),
                    RegattaPoint("2", buoy2LatLng.first, buoy2LatLng.second),
                    RegattaPoint("3", buoy3LatLng.first, buoy3LatLng.second)
                )
            } else {
                val sideLength = buoyDist / 2.0
                val angleTo2 = (wind - 60 + 360) % 360
                val buoy2 = computeOffset(buoy1LatLng.first, buoy1LatLng.second, sideLength, angleTo2.toDouble())
                val angleTo3 = (wind + 60) % 360
                val buoy3 = computeOffset(buoy1LatLng.first, buoy1LatLng.second, sideLength, angleTo3.toDouble())
                val midLat = (buoy2.first + buoy3.first) / 2
                val midLng = (buoy2.second + buoy3.second) / 2
                val vectorTo1Lat = buoy1LatLng.first - midLat
                val vectorTo1Lng = buoy1LatLng.second - midLng
                val buoy4Lat = midLat - vectorTo1Lat
                val buoy4Lng = midLng - vectorTo1Lng

                listOf(
                    RegattaPoint("RC", rcLat, rcLng),
                    RegattaPoint("Start", startBuoyLat, startBuoyLng),
                    RegattaPoint("1", buoy1LatLng.first, buoy1LatLng.second),
                    RegattaPoint("2", buoy2.first, buoy2.second),
                    RegattaPoint("3", buoy3.first, buoy3.second),
                    RegattaPoint("4", buoy4Lat, buoy4Lng)
                )
            }

            repository.addCoursePoints(id, newPoints)
            onDone()
        }
    }

    fun updateCourseFromMap(mapPoints: List<RegattaPoint>, onDone: () -> Unit) {
        val id = roomId ?: return
        viewModelScope.launch {
            repository.addCoursePoints(id, mapPoints)
            onDone()
        }
    }

    fun updateSingleMarkerPosition(roomId: String, pointName: String, newLat: Double, newLng: Double) {
        viewModelScope.launch {
            roomRepository.updateCoursePoint(roomId, pointName, newLat, newLng)
            joinRoom(roomId)
        }
    }
}
