package com.tripmind.history.repository

import com.tripmind.core.model.Trip
import com.tripmind.history.importer.TripCandidate
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OfferTripMatcherTest {
    private val acceptedAt = Instant.parse("2026-09-22T20:00:00Z")

    @Test fun associatesImportedResultWithRecentOfferTrip() {
        val trip = Trip(id = "trip", offerId = "offer", acceptedAt = acceptedAt, dropoffAddress = "San Manuel")
        val candidate = TripCandidate(
            sourceUri = "image",
            rawText = "text",
            completedAt = acceptedAt.plusSeconds(1_800),
            earningsMinor = 8_000,
            dropoffAddress = "San Manuel, Puebla",
        )
        val match = OfferTripMatcher.find(candidate, listOf(trip))
        assertEquals(trip, match)
        assertEquals(8_000L, OfferTripMatcher.merge(trip, candidate).earningsMinor)
    }

    @Test fun doesNotAssociateResultBeforeAcceptance() {
        val trip = Trip(id = "trip", offerId = "offer", acceptedAt = acceptedAt)
        val candidate = TripCandidate(sourceUri = "image", rawText = "text", completedAt = acceptedAt.minusSeconds(1))
        assertNull(OfferTripMatcher.find(candidate, listOf(trip)))
    }
}
