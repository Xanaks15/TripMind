package com.tripmind.uber.detector

import com.tripmind.core.model.Offer
import java.time.Duration
import java.time.Instant

class OfferDeduplicator(private val window: Duration = Duration.ofSeconds(30)) {
    private val recent = linkedMapOf<String, Instant>()

    @Synchronized
    fun isDuplicate(offer: Offer): Boolean {
        recent.entries.removeAll { Duration.between(it.value, offer.detectedAt) > window }
        val key = offer.identityKey()
        val previous = recent[key]
        val duplicate = previous != null && !offer.detectedAt.isBefore(previous) &&
            Duration.between(previous, offer.detectedAt) <= window
        if (!duplicate) recent[key] = offer.detectedAt
        return duplicate
    }
}

fun Offer.identityKey(): String = listOf(
    offeredAmountMinor.toString(),
    estimatedDistanceKm?.stripTrailingZeros()?.toPlainString().orEmpty(),
    estimatedDurationSeconds?.toString().orEmpty(),
    pickupName.normalized(),
    dropoffAddress.normalized(),
).joinToString("|")

private fun String?.normalized(): String = this.orEmpty().lowercase().filter(Char::isLetterOrDigit)
