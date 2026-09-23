package com.tripmind.uber.detector

import com.tripmind.core.model.Offer
import java.math.BigDecimal
import java.time.Instant
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class OfferDetectionTest {
    private fun offer(at: Instant, amount: Long = 8_400) = Offer(
        detectedAt = at,
        offeredAmountMinor = amount,
        estimatedDistanceKm = BigDecimal("7.0"),
        estimatedDurationSeconds = 1_800,
        pickupName = "Comercio",
        dropoffAddress = "Destino",
    )

    @Test fun suppressesSameOfferWithinThirtySeconds() {
        val deduplicator = OfferDeduplicator()
        val start = Instant.parse("2026-09-22T20:00:00Z")
        assertFalse(deduplicator.isDuplicate(offer(start)))
        assertTrue(deduplicator.isDuplicate(offer(start.plusSeconds(20))))
        assertFalse(deduplicator.isDuplicate(offer(start.plusSeconds(31))))
    }

    @Test fun differentAmountIsNotDuplicate() {
        val deduplicator = OfferDeduplicator()
        val start = Instant.EPOCH
        assertFalse(deduplicator.isDuplicate(offer(start)))
        assertFalse(deduplicator.isDuplicate(offer(start.plusSeconds(2), amount = 8_500)))
    }

    @Test fun recognizesOnlyConservativeTripStateSignals() {
        val tracker = UberTripStateTracker()
        assertEquals(TripStateSignal.Accepted, tracker.detect("Dirígete al punto de recogida"))
        assertEquals(TripStateSignal.Completed, tracker.detect("Entrega completada"))
        assertEquals(null, tracker.detect("Tu resumen semanal"))
    }
}
