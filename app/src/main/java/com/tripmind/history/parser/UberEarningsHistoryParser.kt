package com.tripmind.history.parser

import com.tripmind.history.importer.TripCandidate
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class UberEarningsHistoryParser(private val zoneId: ZoneId = ZoneId.systemDefault()) {
    fun parse(rawText: String, sourceUri: String = "manual"): TripCandidate {
        val lines = rawText.lines().map(String::trim).filter(String::isNotEmpty)
        val earnings = lines.firstNotNullOfOrNull { line ->
            if (line.contains(TIP_WORDS) || line.contains(CASH_WORDS)) null
            else MONEY.find(line)?.groupValues?.get(1)?.toMinorUnits()
        }
        val tip = labeledMoney(lines, TIP_WORDS)
        val cash = labeledMoney(lines, CASH_WORDS)
        val duration = DURATION.find(rawText)?.let(::durationSeconds)
        val distance = DISTANCE.find(rawText)?.groupValues?.get(1)?.replace(',', '.')?.let(::BigDecimal)
        val completedAt = parseCompletedAt(rawText)
        val tripType = TYPE.find(rawText)?.value
        val pickupName = labeledValue(lines, PICKUP_LABELS)
        val pickupAddress = labeledValue(lines, ADDRESS_LABELS)
        val destination = labeledValue(lines, DESTINATION_LABELS)

        val warnings = buildList {
            if (earnings == null) add("No se detectó la ganancia")
            if (duration == null) add("No se detectó la duración")
            if (distance == null) add("No se detectó la distancia")
            if (completedAt == null) add("No se detectaron fecha y hora completas")
        }
        val confidence = listOf(
            earnings to 0.35,
            duration to 0.20,
            distance to 0.20,
            completedAt to 0.10,
            (pickupName ?: destination) to 0.15,
        ).sumOf { (value, weight) -> if (value != null) weight else 0.0 }

        return TripCandidate(
            sourceUri = sourceUri,
            rawText = rawText,
            completedAt = completedAt,
            earningsMinor = earnings,
            tipMinor = tip,
            cashCollectedMinor = cash,
            actualDurationSeconds = duration,
            actualDistanceKm = distance,
            pickupName = pickupName,
            pickupAddress = pickupAddress,
            dropoffAddress = destination,
            tripType = tripType,
            confidence = confidence,
            warnings = warnings,
        )
    }

    private fun labeledMoney(lines: List<String>, label: Regex): Long? =
        lines.firstOrNull { it.contains(label) }?.let { MONEY.find(it)?.groupValues?.get(1)?.toMinorUnits() }

    private fun labeledValue(lines: List<String>, labels: Set<String>): String? {
        lines.forEachIndexed { index, line ->
            val label = labels.firstOrNull { line.startsWith(it, ignoreCase = true) } ?: return@forEachIndexed
            val inline = line.substring(label.length).trim(' ', ':', '-')
            if (inline.isNotEmpty()) return inline
            lines.getOrNull(index + 1)?.let { return it }
        }
        return null
    }

    private fun parseCompletedAt(text: String) = runCatching {
        val dateMatch = DATE.find(text) ?: return null
        val timeMatch = TIME.find(text) ?: return null
        val date = LocalDate.of(
            dateMatch.groupValues[3].toInt(), dateMatch.groupValues[2].toInt(), dateMatch.groupValues[1].toInt(),
        )
        var hour = timeMatch.groupValues[1].toInt()
        val minute = timeMatch.groupValues[2].toInt()
        val marker = timeMatch.groupValues[3].lowercase().replace(Regex("[.\\s]"), "")
        if (marker == "pm" && hour < 12) hour += 12
        if (marker == "am" && hour == 12) hour = 0
        LocalDateTime.of(date, LocalTime.of(hour, minute)).atZone(zoneId).toInstant()
    }.getOrNull()

    private fun durationSeconds(match: MatchResult): Long {
        val hours = match.groups[1]?.value?.toLongOrNull() ?: 0
        val minutes = match.groups[2]?.value?.toLongOrNull() ?: 0
        val seconds = match.groups[3]?.value?.toLongOrNull() ?: 0
        return hours * 3600 + minutes * 60 + seconds
    }

    private fun String.toMinorUnits(): Long? = runCatching {
        decimalValue().movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()
    }.getOrNull()

    private fun String.decimalValue(): BigDecimal = BigDecimal(replace(",", ""))

    companion object {
        private val MONEY = Regex("\\$\\s*([0-9]+(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?)")
        private val DURATION = Regex("(?:(\\d+)\\s*(?:h|hr|hrs|hora|horas)\\s*)?(\\d+)\\s*(?:min|minuto|minutos)(?:\\s*(\\d+)\\s*(?:s|seg|segundo|segundos))?", RegexOption.IGNORE_CASE)
        private val DISTANCE = Regex("([0-9]+(?:[.,][0-9]+)?)\\s*km\\b", RegexOption.IGNORE_CASE)
        private val DATE = Regex("\\b(\\d{1,2})[/-](\\d{1,2})[/-](\\d{4})\\b")
        private val TIME = Regex("\\b(\\d{1,2}):(\\d{2})\\s*([ap]\\.?\\s*m\\.?)?", RegexOption.IGNORE_CASE)
        private val TYPE = Regex("\\b(?:Delivery|Entrega|Viaje)\\b", RegexOption.IGNORE_CASE)
        private val TIP_WORDS = Regex("propina", RegexOption.IGNORE_CASE)
        private val CASH_WORDS = Regex("efectivo(?:\\s+recibido)?", RegexOption.IGNORE_CASE)
        private val PICKUP_LABELS = setOf("Comercio", "Restaurante", "Recogida", "Origen")
        private val ADDRESS_LABELS = setOf("Dirección de recogida", "Dirección de origen")
        private val DESTINATION_LABELS = setOf("Destino", "Entrega", "Dirección de entrega")
    }
}
