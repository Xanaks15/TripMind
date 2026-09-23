package com.tripmind.analyzer

import com.tripmind.analyzer.cost.TripCostBreakdown
import com.tripmind.analyzer.cost.TripCostCalculator
import com.tripmind.core.model.Offer
import com.tripmind.core.model.VehicleProfile
import java.math.BigDecimal
import java.math.MathContext

enum class Recommendation { GREEN, YELLOW, RED }

data class TripAnalysis(
    val recommendation: Recommendation,
    val grossPerHourMinor: BigDecimal?,
    val grossPerKmMinor: BigDecimal?,
    val costs: TripCostBreakdown?,
    val netEarningsMinor: BigDecimal?,
    val netPerHourMinor: BigDecimal?,
    val netPerKmMinor: BigDecimal?,
    val reasons: List<String>,
)

class TripAnalyzer(private val costCalculator: TripCostCalculator = TripCostCalculator()) {
    fun analyze(offer: Offer, vehicle: VehicleProfile): TripAnalysis {
        val duration = offer.estimatedDurationSeconds?.takeIf { it > 0 }
        val distance = offer.estimatedDistanceKm?.takeIf { it.signum() > 0 }
        val gross = BigDecimal.valueOf(offer.offeredAmountMinor)
        val grossPerHour = duration?.let { perHour(gross, it) }
        val grossPerKm = distance?.let { gross.divide(it, MathContext.DECIMAL128) }
        val costs = distance?.let { costCalculator.calculate(it, vehicle) }
        val net = costs?.let { gross.subtract(it.totalCostMinor) }
        val netPerHour = if (net != null && duration != null) perHour(net, duration) else null
        val netPerKm = if (net != null && distance != null) net.divide(distance, MathContext.DECIMAL128) else null

        val reasons = mutableListOf<String>()
        if (duration == null) reasons += "Tiempo estimado no disponible"
        if (distance == null) reasons += "Distancia estimada no disponible"
        if (netPerHour != null) reasons += if (netPerHour >= BigDecimal.valueOf(vehicle.targetNetHourlyMinor)) {
            "Cumple el objetivo neto por hora"
        } else {
            "No cumple el objetivo neto por hora"
        }
        if (netPerKm != null) reasons += if (netPerKm >= vehicle.targetNetPerKmMinor) {
            "Cumple el objetivo neto por kilómetro"
        } else {
            "No cumple el objetivo neto por kilómetro"
        }

        val hourlyPass = netPerHour?.let { it >= BigDecimal.valueOf(vehicle.targetNetHourlyMinor) }
        val kilometerPass = netPerKm?.let { it >= vehicle.targetNetPerKmMinor }
        val recommendation = when {
            hourlyPass == null || kilometerPass == null -> Recommendation.YELLOW
            hourlyPass && kilometerPass -> Recommendation.GREEN
            !hourlyPass && !kilometerPass -> Recommendation.RED
            else -> Recommendation.YELLOW
        }
        return TripAnalysis(
            recommendation = recommendation,
            grossPerHourMinor = grossPerHour,
            grossPerKmMinor = grossPerKm,
            costs = costs,
            netEarningsMinor = net,
            netPerHourMinor = netPerHour,
            netPerKmMinor = netPerKm,
            reasons = reasons,
        )
    }

    private fun perHour(amountMinor: BigDecimal, durationSeconds: Long): BigDecimal =
        amountMinor.multiply(BigDecimal.valueOf(3600)).divide(BigDecimal.valueOf(durationSeconds), MathContext.DECIMAL128)
}
