package com.tripmind.analyzer.cost

import com.tripmind.core.model.VehicleProfile
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TripCostCalculatorTest {
    private val calculator = TripCostCalculator()
    private val vehicle = VehicleProfile(
        id = "vehicle",
        name = "Auto",
        fuelPriceMinorPerLiter = BigDecimal("2400"),
        fuelEfficiencyKmPerLiter = BigDecimal("12"),
        maintenanceMinorPerKm = BigDecimal("30.125"),
        depreciationMinorPerKm = BigDecimal("20.50"),
        targetNetHourlyMinor = 12_000,
        targetNetPerKmMinor = BigDecimal("800"),
    )

    @Test fun calculatesFuelMaintenanceDepreciationAndNetPrecisely() {
        val result = calculator.calculate(BigDecimal("9"), vehicle)

        assertEquals(0, BigDecimal("1800").compareTo(result.fuelCostMinor))
        assertEquals(0, BigDecimal("271.125").compareTo(result.maintenanceCostMinor))
        assertEquals(0, BigDecimal("184.50").compareTo(result.depreciationCostMinor))
        assertEquals(0, BigDecimal("2255.625").compareTo(result.totalCostMinor))
        assertEquals(0, BigDecimal("6744.375").compareTo(result.netMinor(BigDecimal("9000"))))
    }

    @Test fun zeroDistanceHasNoVehicleCost() {
        val result = calculator.calculate(BigDecimal.ZERO, vehicle)

        assertEquals(0, BigDecimal.ZERO.compareTo(result.totalCostMinor))
    }

    @Test fun rejectsNegativeDistance() {
        assertThrows(IllegalArgumentException::class.java) {
            calculator.calculate(BigDecimal("-0.1"), vehicle)
        }
    }
}
