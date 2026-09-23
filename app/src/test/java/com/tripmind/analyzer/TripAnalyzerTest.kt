package com.tripmind.analyzer

import com.tripmind.core.model.Offer
import com.tripmind.core.model.VehicleProfile
import java.math.BigDecimal
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TripAnalyzerTest {
    private val analyzer = TripAnalyzer()
    private val vehicle = VehicleProfile(
        name = "Auto",
        fuelPriceMinorPerLiter = BigDecimal("2400"),
        fuelEfficiencyKmPerLiter = BigDecimal("12"),
        maintenanceMinorPerKm = BigDecimal("50"),
        depreciationMinorPerKm = BigDecimal("30"),
        targetNetHourlyMinor = 10_000,
        targetNetPerKmMinor = BigDecimal("600"),
    )

    private fun offer(amount: Long, minutes: Long, km: String) = Offer(
        detectedAt = Instant.EPOCH,
        offeredAmountMinor = amount,
        estimatedDurationSeconds = minutes * 60,
        estimatedDistanceKm = BigDecimal(km),
    )

    @Test fun recommendsGreenWhenBothNetTargetsPass() {
        val result = analyzer.analyze(offer(10_000, 40, "9"), vehicle)
        assertEquals(Recommendation.GREEN, result.recommendation)
        assertNotNull(result.grossPerHourMinor)
        assertNotNull(result.grossPerKmMinor)
        assertNotNull(result.netEarningsMinor)
    }

    @Test fun recommendsRedWhenBothNetTargetsFail() {
        val result = analyzer.analyze(offer(4_000, 60, "10"), vehicle)
        assertEquals(Recommendation.RED, result.recommendation)
    }

    @Test fun recommendsYellowWhenInformationIsIncomplete() {
        val result = analyzer.analyze(
            Offer(detectedAt = Instant.EPOCH, offeredAmountMinor = 9_000,
                estimatedDistanceKm = null, estimatedDurationSeconds = 2_400),
            vehicle,
        )
        assertEquals(Recommendation.YELLOW, result.recommendation)
    }
}
