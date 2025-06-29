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

class RoomViewModel(
    private val repository: RoomRepository = RoomRepository()
) : ViewModel() {

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
}
