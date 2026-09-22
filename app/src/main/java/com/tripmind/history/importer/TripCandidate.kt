package com.tripmind.history.importer

import com.tripmind.core.model.Trip
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/** Mutable only in the review UI. OCR output never enters Room without user confirmation. */
data class TripCandidate(
    val id: String = UUID.randomUUID().toString(),
    val sourceUri: String,
    val rawText: String,
    val completedAt: Instant? = null,
    val earningsMinor: Long? = null,
    val tipMinor: Long? = null,
    val cashCollectedMinor: Long? = null,
    val actualDurationSeconds: Long? = null,
    val actualDistanceKm: BigDecimal? = null,
    val pickupName: String? = null,
    val pickupAddress: String? = null,
    val dropoffAddress: String? = null,
    val tripType: String? = null,
    val confidence: Double = 0.0,
    val warnings: List<String> = emptyList(),
) {
    fun toTrip(): Trip = Trip(
        id = id,
        completedAt = completedAt,
        actualDurationSeconds = actualDurationSeconds,
        actualDistanceKm = actualDistanceKm,
        earningsMinor = earningsMinor,
        tipMinor = tipMinor,
        cashCollectedMinor = cashCollectedMinor,
        pickupName = pickupName,
        pickupAddress = pickupAddress,
        dropoffAddress = dropoffAddress,
    )
}
