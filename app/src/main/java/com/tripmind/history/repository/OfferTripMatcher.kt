package com.tripmind.history.repository

import com.tripmind.core.model.Trip
import com.tripmind.history.importer.TripCandidate
import java.time.Duration

object OfferTripMatcher {
    fun find(candidate: TripCandidate, trips: List<Trip>): Trip? {
        val completedAt = candidate.completedAt ?: return null
        return trips.asSequence()
            .filter { it.offerId != null && it.earningsMinor == null && it.acceptedAt != null }
            .filter {
                val elapsed = Duration.between(it.acceptedAt, completedAt)
                !elapsed.isNegative && elapsed <= Duration.ofHours(24)
            }
            .filter { compatibleDestination(it.dropoffAddress, candidate.dropoffAddress) }
            .maxByOrNull { it.acceptedAt!! }
    }

    fun merge(trip: Trip, candidate: TripCandidate): Trip = trip.copy(
        completedAt = candidate.completedAt ?: trip.completedAt,
        actualDurationSeconds = candidate.actualDurationSeconds ?: trip.actualDurationSeconds,
        actualDistanceKm = candidate.actualDistanceKm ?: trip.actualDistanceKm,
        earningsMinor = candidate.earningsMinor ?: trip.earningsMinor,
        tipMinor = candidate.tipMinor ?: trip.tipMinor,
        cashCollectedMinor = candidate.cashCollectedMinor ?: trip.cashCollectedMinor,
        pickupName = candidate.pickupName ?: trip.pickupName,
        pickupAddress = candidate.pickupAddress ?: trip.pickupAddress,
        dropoffAddress = candidate.dropoffAddress ?: trip.dropoffAddress,
    )

    private fun compatibleDestination(first: String?, second: String?): Boolean {
        if (first.isNullOrBlank() || second.isNullOrBlank()) return true
        val a = first.lowercase().filter(Char::isLetterOrDigit)
        val b = second.lowercase().filter(Char::isLetterOrDigit)
        return a.contains(b) || b.contains(a)
    }
}
