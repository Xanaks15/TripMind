package com.tripmind.settings

import com.tripmind.core.model.VehicleProfile
import java.math.BigDecimal

object DefaultVehicleProfile {
    const val ID = "default-vehicle"

    fun create() = VehicleProfile(
        id = ID,
        name = "Mi vehículo",
        fuelPriceMinorPerLiter = BigDecimal("2400"),
        fuelEfficiencyKmPerLiter = BigDecimal("12"),
        maintenanceMinorPerKm = BigDecimal("50"),
        depreciationMinorPerKm = BigDecimal("30"),
        targetNetHourlyMinor = 12_000,
        targetNetPerKmMinor = BigDecimal("800"),
    )
}
