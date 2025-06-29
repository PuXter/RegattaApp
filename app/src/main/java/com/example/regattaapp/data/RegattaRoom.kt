package com.example.regattaapp.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint

data class RegattaRoom(
    val name: String = "",
    val createdBy: String = "",
    val windDirection: Int = 0,
    val boatClass: String = "",
    val courseType: String = "",
    val firstBuoyDistance: Int = 0,
    val crewCount: Int = 0,
    val startLocation: GeoPoint? = null,
    val coursePoints: List<RegattaPoint> = emptyList(),
    val users: List<String> = emptyList(),
    val createdAt: Timestamp = Timestamp.now()
)