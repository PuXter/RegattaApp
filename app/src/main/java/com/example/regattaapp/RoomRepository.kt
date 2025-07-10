package com.example.regattaapp.data

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.example.regattaapp.data.RoomWithId

class RoomRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val roomsCollection = db.collection("rooms")

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
        val uid = auth.currentUser?.uid ?: return null

        val room = RegattaRoom(
            name = regattaName,
            createdBy = uid,
            windDirection = windDirection,
            boatClass = boatClass,
            courseType = courseType,
            firstBuoyDistance = firstBuoyDistance,
            crewCount = crewCount,
            startLocation = GeoPoint(startLat, startLng),
            coursePoints = emptyList(), // 🔸 Dodane
            users = listOf(uid),
            createdAt = Timestamp.now()
        )


        return try {
            val docRef = roomsCollection.add(room).await()
            Log.d("RoomRepository", "Room created with ID: ${docRef.id}")
            docRef.id
        } catch (e: Exception) {
            Log.e("RoomRepository", "Błąd tworzenia pokoju: ${e.message}", e)
            null
        }
    }

    suspend fun joinRoom(roomId: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        val roomRef = roomsCollection.document(roomId)

        return try {
            db.runTransaction { transaction ->
                val snapshot = transaction.get(roomRef)
                val currentUsers = (snapshot.get("users") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                if (!currentUsers.contains(uid)) {
                    val updatedUsers = currentUsers + uid
                    transaction.update(roomRef, "users", updatedUsers)
                }
            }.await()
            true
        } catch (e: Exception) {
            Log.e("RoomRepository", "Error joining room", e)
            false
        }
    }

    suspend fun deleteRoom(roomId: String): Boolean {
        return try {
            roomsCollection.document(roomId).delete().await()
            Log.d("RoomRepository", "Pokój usunięty: $roomId")
            true
        } catch (e: Exception) {
            Log.e("RoomRepository", "Błąd przy usuwaniu pokoju", e)
            false
        }
    }

    suspend fun getAllRooms(): List<RoomWithId> {
        return db.collection("rooms")
            .get()
            .await()
            .documents
            .mapNotNull { doc ->
                val room = doc.toObject(RegattaRoom::class.java)
                room?.let { RoomWithId(id = doc.id, room = it) }
            }
            .sortedBy { it.room.name.lowercase() } // sortowanie alfabetyczne
    }

    suspend fun addCoursePoints(roomId: String, points: List<RegattaPoint>) {
        try {
            db.collection("rooms").document(roomId)
                .update("coursePoints", points)
                .await()
        } catch (e: Exception) {
            Log.e("RoomRepository", "Błąd przy zapisie punktów trasy", e)
        }
    }

    suspend fun getRoom(roomId: String): RegattaRoom? {
        return try {
            val snapshot = roomsCollection.document(roomId).get().await()
            snapshot.toObject(RegattaRoom::class.java)
        } catch (e: Exception) {
            Log.e("RoomRepository", "Error fetching room", e)
            null
        }
    }

    fun observeRoom(roomId: String, onChange: (RegattaRoom?) -> Unit): ListenerRegistration {
        return roomsCollection.document(roomId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("RoomRepository", "Listen failed", error)
                    onChange(null)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val data = snapshot.data
                    if (data != null) {
                        val coursePoints = (data["coursePoints"] as? List<*>)
                            ?.mapNotNull { point ->
                                if (point is Map<*, *>) {
                                    try {
                                        RegattaPoint(
                                            name = point["name"] as? String ?: "",
                                            latitude = (point["latitude"] as? Number)?.toDouble() ?: 0.0,
                                            longitude = (point["longitude"] as? Number)?.toDouble() ?: 0.0
                                        )
                                    } catch (e: Exception) {
                                        Log.e("RoomRepository", "Błąd parsowania punktu trasy: ${e.message}")
                                        null
                                    }
                                } else {
                                    null
                                }
                            } ?: emptyList()

                        val room = snapshot.toObject(RegattaRoom::class.java)
                        if (room != null) {
                            val updatedRoom = room.copy(coursePoints = coursePoints)
                            onChange(updatedRoom)
                            Log.d("RoomRepository", "Zaktualizowano pokój z punktami: ${updatedRoom.coursePoints}")
                        } else {
                            onChange(null)
                        }
                    } else {
                        onChange(null)
                    }
                } else {
                    onChange(null)
                }
            }
    }
    fun getRoomWithListener(roomId: String, callback: (RegattaRoom?) -> Unit) {
        Firebase.firestore.collection("rooms").document(roomId)
            .addSnapshotListener { snapshot, _ ->
                val room = snapshot?.toObject(RegattaRoom::class.java)
                callback(room)
            }
    }

    suspend fun updateCoursePoint(roomId: String, pointName: String, lat: Double, lng: Double) {
        val docRef = Firebase.firestore.collection("rooms").document(roomId)
        val snapshot = docRef.get().await()
        val data = snapshot.toObject(RegattaRoom::class.java) ?: return
        val updatedPoints = data.coursePoints.map {
            if (it.name == pointName) it.copy(latitude = lat, longitude = lng) else it
        }
        docRef.update("coursePoints", updatedPoints).await()
        Log.d("RoomRepository", "Zaktualizowano pozycję $pointName na ($lat, $lng)")
    }
}
