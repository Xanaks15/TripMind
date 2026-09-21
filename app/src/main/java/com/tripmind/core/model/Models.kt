package com.tripmind.core.model

import java.math.BigDecimal
import java.time.Instant
import java.util.Currency
import java.util.UUID

enum class OfferStatus { DETECTED, ANALYZED, ACCEPTED, REJECTED, EXPIRED, UNKNOWN }
enum class CaptureMethod { MANUAL, SCREENSHOT_OCR, ACCESSIBILITY, SCREEN_CAPTURE_OCR }

/** All amounts are minor currency units (MXN centavos). No floating-point money. */
data class Offer(
    val id: String = UUID.randomUUID().toString(),
    val detectedAt: Instant,
    val offeredAmountMinor: Long,
    val currencyCode: String = "MXN",
    val estimatedDistanceKm: BigDecimal?,
    val estimatedDurationSeconds: Long?,
    val pickupName: String? = null,
    val pickupAddress: String? = null,
    val dropoffAddress: String? = null,
    val rawText: String = "",
    val status: OfferStatus = OfferStatus.DETECTED,
    val parserVersion: String = "manual-v1",
    val captureMethod: CaptureMethod = CaptureMethod.MANUAL,
    val confidence: Double? = null,
    val sessionId: String? = null,
) {
    init {
        require(id.isNotBlank())
        Currency.getInstance(currencyCode)
        require(offeredAmountMinor >= 0)
        require(estimatedDistanceKm == null || estimatedDistanceKm.signum() >= 0)
        require(estimatedDurationSeconds == null || estimatedDurationSeconds >= 0)
        require(confidence == null || (confidence.isFinite() && confidence in 0.0..1.0))
    }
}

/** earningsMinor is the final total, including tipMinor when present; never add cashCollectedMinor. */
data class Trip(
    val id: String = UUID.randomUUID().toString(),
    val offerId: String? = null,
    val acceptedAt: Instant? = null,
    val startedAt: Instant? = null,
    val completedAt: Instant? = null,
    val estimatedDurationSeconds: Long? = null,
    val actualDurationSeconds: Long? = null,
    val estimatedDistanceKm: BigDecimal? = null,
    val actualDistanceKm: BigDecimal? = null,
    val earningsMinor: Long? = null,
    val tipMinor: Long? = null,
    val cashCollectedMinor: Long? = null,
    val currencyCode: String = "MXN",
    val pickupName: String? = null,
    val pickupAddress: String? = null,
    val dropoffAddress: String? = null,
) {
    init {
        require(id.isNotBlank())
        Currency.getInstance(currencyCode)
        require(estimatedDurationSeconds == null || estimatedDurationSeconds >= 0)
        require(actualDurationSeconds == null || actualDurationSeconds >= 0)
        require(estimatedDistanceKm == null || estimatedDistanceKm.signum() >= 0)
        require(actualDistanceKm == null || actualDistanceKm.signum() >= 0)
        require(tipMinor == null || tipMinor >= 0)
        require(cashCollectedMinor == null || cashCollectedMinor >= 0)
        require(acceptedAt == null || startedAt == null || !startedAt.isBefore(acceptedAt))
        require(startedAt == null || completedAt == null || !completedAt.isBefore(startedAt))
        require(acceptedAt == null || completedAt == null || !completedAt.isBefore(acceptedAt))
    }
}

data class DriverSession(
    val id: String = UUID.randomUUID().toString(),
    val connectedAt: Instant,
    val disconnectedAt: Instant? = null,
    val startingZone: String? = null,
    val endingZone: String? = null,
    val offersReceived: Int = 0,
    val offersAccepted: Int = 0,
    val earningsMinor: Long = 0,
    val currencyCode: String = "MXN",
    val distanceKm: BigDecimal = BigDecimal.ZERO,
    val onlineSeconds: Long = 0,
    val activeSeconds: Long = 0,
    val idleSeconds: Long = 0,
) {
    init {
        require(id.isNotBlank())
        Currency.getInstance(currencyCode)
        require(disconnectedAt == null || !disconnectedAt.isBefore(connectedAt))
        require(offersReceived >= 0 && offersAccepted in 0..offersReceived)
        require(distanceKm.signum() >= 0)
        require(onlineSeconds >= 0 && activeSeconds in 0..onlineSeconds)
        require(idleSeconds in 0..(onlineSeconds - activeSeconds))
    }
}

/** Rates use decimal minor currency units, preserving fractions of a centavo per km. */
data class VehicleProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val currencyCode: String = "MXN",
    val fuelPriceMinorPerLiter: BigDecimal,
    val fuelEfficiencyKmPerLiter: BigDecimal,
    val maintenanceMinorPerKm: BigDecimal,
    val depreciationMinorPerKm: BigDecimal,
    val targetNetHourlyMinor: Long,
    val targetNetPerKmMinor: BigDecimal,
) {
    init {
        require(id.isNotBlank() && name.isNotBlank())
        Currency.getInstance(currencyCode)
        require(fuelEfficiencyKmPerLiter.signum() > 0)
        require(listOf(fuelPriceMinorPerLiter, maintenanceMinorPerKm, depreciationMinorPerKm,
            targetNetPerKmMinor).all { it.signum() >= 0 })
        require(targetNetHourlyMinor >= 0)
    }
}
