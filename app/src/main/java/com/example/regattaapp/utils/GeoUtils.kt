package com.example.regattaapp.utils

import kotlin.math.*

fun computeOffset(lat: Double, lng: Double, distanceMeters: Double, bearingDegrees: Double): Pair<Double, Double> {
    val R = 6371000.0
    val bearingRad = Math.toRadians(bearingDegrees.toDouble())

    val latRad = Math.toRadians(lat)
    val lngRad = Math.toRadians(lng)

    val newLat = asin(
        sin(latRad) * cos(distanceMeters / R) +
                cos(latRad) * sin(distanceMeters / R) * cos(bearingRad)
    )

    val newLng = lngRad + atan2(
        sin(bearingRad) * sin(distanceMeters / R) * cos(latRad),
        cos(distanceMeters / R) - sin(latRad) * sin(newLat)
    )

    return Math.toDegrees(newLat) to Math.toDegrees(newLng)
}

fun calculateDestination(
    lat: Double,
    lng: Double,
    bearing: Double,
    distanceMeters: Double
): Pair<Double, Double> {
    val earthRadius = 6371000.0

    val bearingRad = Math.toRadians(bearing % 360)
    val latRad = Math.toRadians(lat)
    val lngRad = Math.toRadians(lng)

    val distanceRatio = distanceMeters / earthRadius

    val newLat = asin(
        sin(latRad) * cos(distanceRatio) +
                cos(latRad) * sin(distanceRatio) * cos(bearingRad)
    )

    val newLng = lngRad + atan2(
        sin(bearingRad) * sin(distanceRatio) * cos(latRad),
        cos(distanceRatio) - sin(latRad) * sin(newLat)
    )

    return Pair(Math.toDegrees(newLat), Math.toDegrees(newLng))
}
