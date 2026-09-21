package com.tripmind.core.database

import com.tripmind.core.model.*
import java.math.BigDecimal
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class ModelTest {
    @Test fun rejectsInvalidConfidence() {
        assertThrows(IllegalArgumentException::class.java) {
            Offer(detectedAt = Instant.EPOCH, offeredAmountMinor = 100,
                estimatedDistanceKm = null, estimatedDurationSeconds = null, confidence = Double.NaN)
        }
    }

    @Test fun rejectsNegativeDistance() {
        assertThrows(IllegalArgumentException::class.java) {
            Trip(actualDistanceKm = BigDecimal("-1"))
        }
    }

    @Test fun unknownResultsRemainUnknown() {
        val trip = Trip()
        assertNull(trip.earningsMinor)
        assertNull(trip.tipMinor)
        assertNull(trip.cashCollectedMinor)
        assertNull(trip.actualDurationSeconds)
    }

    @Test fun rejectsImpossibleSessionTotals() {
        assertThrows(IllegalArgumentException::class.java) {
            DriverSession(connectedAt = Instant.EPOCH, onlineSeconds = 60, activeSeconds = 50, idleSeconds = 20)
        }
    }

    @Test fun converterPreservesNanosecondsAndExactDecimal() {
        val converter = Converters()
        val instant = Instant.parse("2026-09-15T23:29:00.123456789Z")
        assertEquals(instant, converter.textToInstant(converter.instantToText(instant)))
        val amount = BigDecimal("0.123456789")
        assertEquals(amount, converter.textToDecimal(converter.decimalToText(amount)))
    }
}
