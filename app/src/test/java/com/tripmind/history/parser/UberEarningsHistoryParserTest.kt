package com.tripmind.history.parser

import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UberEarningsHistoryParserTest {
    private val parser = UberEarningsHistoryParser(ZoneOffset.UTC)

    @Test fun parsesCompletedDeliveryAndTipWithoutDoubleCountingIt() {
        val result = parser.parse(
            """
            15/09/2026
            11:29 p. m.
            $54.50
            Delivery · 29 min 11 seg · 10.04 km
            Comercio: Taquería Los Sentados
            Destino: 13 de Mayo, Puebla
            $14.74 Propinas otorgadas por usuarios
            """.trimIndent(),
        )

        assertEquals(5_450L, result.earningsMinor)
        assertEquals(1_474L, result.tipMinor)
        assertNull(result.cashCollectedMinor)
        assertEquals(1_751L, result.actualDurationSeconds)
        assertEquals(BigDecimal("10.04"), result.actualDistanceKm)
        assertEquals("Taquería Los Sentados", result.pickupName)
        assertEquals("13 de Mayo, Puebla", result.dropoffAddress)
        assertEquals(Instant.parse("2026-09-15T23:29:00Z"), result.completedAt)
        assertTrue(result.warnings.isEmpty())
    }

    @Test fun keepsCashSeparateFromEarnings() {
        val result = parser.parse(
            """
            16/09/2026 08:02 a. m.
            $54.78
            Delivery · 41 min 42 seg · 8.66 km
            $169.00 Efectivo recibido
            """.trimIndent(),
        )

        assertEquals(5_478L, result.earningsMinor)
        assertEquals(16_900L, result.cashCollectedMinor)
        assertEquals(2_502L, result.actualDurationSeconds)
        assertEquals(BigDecimal("8.66"), result.actualDistanceKm)
    }

    @Test fun acceptsDecimalCommaAndReportsMissingCoreFields() {
        val result = parser.parse("Delivery · 12 min · 8,7 km")

        assertNull(result.earningsMinor)
        assertEquals(720L, result.actualDurationSeconds)
        assertEquals(BigDecimal("8.7"), result.actualDistanceKm)
        assertTrue(result.warnings.contains("No se detectó la ganancia"))
        assertTrue(result.warnings.contains("No se detectaron fecha y hora completas"))
    }

    @Test fun selectsTripEarningsWhenCashAppearsEarlierInHistoryCard() {
        val result = parser.parse(
            """
            Historial de ganancias
            14/9 - 21/9
            $376.84 Efectivo recibido
            La Cruda Bendita (Puebla)
            Calle Valle Castellana, Lomas del Valle, 72595, Puebla, MX
            $50.61 8:30 p. m.
            Delivery · 36 min 11 seg · 10.38 km
            """.trimIndent(),
        )

        assertEquals(5_061L, result.earningsMinor)
        assertEquals(37_684L, result.cashCollectedMinor)
        assertEquals(2_171L, result.actualDurationSeconds)
        assertEquals(BigDecimal("10.38"), result.actualDistanceKm)
    }

    @Test fun parsesSeveralTripCardsFromOneScreenshot() {
        val results = parser.parseAll(
            """
            Historial de ganancias
            $376.84
            Efectivo recibido
            La Cruda Bendita (Puebla)
            Calle Valle Castellana, Puebla, MX
            $50.61
            8:30 p. m.
            Delivery · 36 min 11 seg · 10.38 km
            $224.74 Efectivo recibido
            Amorcito Corazon (Huexotitla)
            103 Poniente, 72480, Puebla, MX
            $30.85 8:05 p. m.
            Delivery · 18 min 2 seg · 6.40 km
            """.trimIndent(),
            sourceUri = "screenshot",
        )

        assertEquals(2, results.size)
        assertEquals(5_061L, results[0].earningsMinor)
        assertEquals(37_684L, results[0].cashCollectedMinor)
        assertEquals(3_085L, results[1].earningsMinor)
        assertEquals(22_474L, results[1].cashCollectedMinor)
        assertEquals("screenshot#trip-1", results[0].sourceUri)
        assertEquals("screenshot#trip-2", results[1].sourceUri)
    }
}
