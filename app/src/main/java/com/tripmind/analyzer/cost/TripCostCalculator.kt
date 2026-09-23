package com.tripmind.analyzer.cost

import com.tripmind.core.model.VehicleProfile
import java.math.BigDecimal
import java.math.MathContext

data class TripCostBreakdown(
    val distanceKm: BigDecimal,
    val fuelCostMinor: BigDecimal,
    val maintenanceCostMinor: BigDecimal,
    val depreciationCostMinor: BigDecimal,
) {
    val totalCostMinor: BigDecimal = fuelCostMinor
        .add(maintenanceCostMinor)
        .add(depreciationCostMinor)

    fun netMinor(grossEarningsMinor: BigDecimal): BigDecimal = grossEarningsMinor.subtract(totalCostMinor)
}

/** Deterministic vehicle-cost engine. All monetary values use minor currency units. */
class TripCostCalculator {
    fun calculate(distanceKm: BigDecimal, vehicle: VehicleProfile): TripCostBreakdown {
        require(distanceKm.signum() >= 0) { "Distance cannot be negative" }

        val fuelMinorPerKm = vehicle.fuelPriceMinorPerLiter.divide(
            vehicle.fuelEfficiencyKmPerLiter,
            MathContext.DECIMAL128,
        )
        return TripCostBreakdown(
            distanceKm = distanceKm,
            fuelCostMinor = distanceKm.multiply(fuelMinorPerKm),
            maintenanceCostMinor = distanceKm.multiply(vehicle.maintenanceMinorPerKm),
            depreciationCostMinor = distanceKm.multiply(vehicle.depreciationMinorPerKm),
        )
    }
}
