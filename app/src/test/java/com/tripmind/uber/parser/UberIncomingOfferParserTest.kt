package com.tripmind.uber.parser

import java.math.BigDecimal
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class UberIncomingOfferParserTest {
    private val parser = UberIncomingOfferParser()

    @Test fun parsesIncomingOfferAccessibilityText() {
        val offer = parser.parse(
            "$83.40\n32 min · 8.7 km\nComercio: McDonald's\nDestino: San Manuel\nAceptar",
            Instant.EPOCH,
        )
        assertNotNull(offer)
        assertEquals(8_340L, offer!!.offeredAmountMinor)
        assertEquals(1_920L, offer.estimatedDurationSeconds)
        assertEquals(BigDecimal("8.7"), offer.estimatedDistanceKm)
        assertEquals("McDonald's", offer.pickupName)
        assertEquals("San Manuel", offer.dropoffAddress)
    }

    @Test fun rejectsUnrelatedMoneyTextWithoutOfferMetricsOrMarker() {
        assertNull(parser.parse("Saldo disponible $900.00"))
    }
}
