package com.tripmind.uber.parser

import com.tripmind.core.model.CaptureMethod
import com.tripmind.core.model.Offer
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant

class UberIncomingOfferParser {
    fun parse(rawText: String, detectedAt: Instant = Instant.now()): Offer? {
        val lines = rawText.lines().map(String::trim).filter(String::isNotEmpty)
        val amount = MONEY.find(rawText)?.groupValues?.get(1)?.toMinorUnits() ?: return null
        val duration = DURATION.find(rawText)?.groupValues?.get(1)?.toLongOrNull()?.times(60)
        val distance = DISTANCE.find(rawText)?.groupValues?.get(1)?.replace(',', '.')?.let(::BigDecimal)
        val hasOfferMarker = OFFER_MARKER.containsMatchIn(rawText)
        if (!hasOfferMarker && (duration == null || distance == null)) return null

        val pickup = labeledValue(lines, PICKUP_LABELS)
        val dropoff = labeledValue(lines, DESTINATION_LABELS)
        val confidence = listOf(
            amount to 0.35,
            duration to 0.20,
            distance to 0.20,
            (pickup ?: dropoff) to 0.15,
            hasOfferMarker to 0.10,
        ).sumOf { (value, weight) -> if (value != null && value != false) weight else 0.0 }

        return Offer(
            detectedAt = detectedAt,
            offeredAmountMinor = amount,
            estimatedDistanceKm = distance,
            estimatedDurationSeconds = duration,
            pickupName = pickup,
            dropoffAddress = dropoff,
            rawText = rawText.take(MAX_RAW_TEXT_LENGTH),
            parserVersion = "incoming-v1",
            captureMethod = CaptureMethod.ACCESSIBILITY,
            confidence = confidence,
        )
    }

    private fun labeledValue(lines: List<String>, labels: Set<String>): String? {
        lines.forEachIndexed { index, line ->
            val label = labels.firstOrNull { line.startsWith(it, ignoreCase = true) } ?: return@forEachIndexed
            val inline = line.substring(label.length).trim(' ', ':', '-')
            if (inline.isNotEmpty()) return inline
            lines.getOrNull(index + 1)?.let { return it }
        }
        return null
    }

    private fun String.toMinorUnits(): Long? = runCatching {
        BigDecimal(replace(",", "")).movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()
    }.getOrNull()

    companion object {
        private const val MAX_RAW_TEXT_LENGTH = 20_000
        private val MONEY = Regex("\\$\\s*([0-9]+(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?)")
        private val DURATION = Regex("(\\d+)\\s*(?:min|minuto|minutos)\\b", RegexOption.IGNORE_CASE)
        private val DISTANCE = Regex("([0-9]+(?:[.,][0-9]+)?)\\s*km\\b", RegexOption.IGNORE_CASE)
        private val OFFER_MARKER = Regex("\\b(aceptar|rechazar|oferta|solicitud|delivery|entrega)\\b", RegexOption.IGNORE_CASE)
        private val PICKUP_LABELS = setOf("Comercio", "Restaurante", "Recogida", "Origen")
        private val DESTINATION_LABELS = setOf("Destino", "Entrega")
    }
}
