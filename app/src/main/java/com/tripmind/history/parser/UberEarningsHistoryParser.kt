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
        return parseAll(rawText, sourceUri).first()
    }

    /** A history screenshot can contain several trip cards. Each detail row closes one card. */
    fun parseAll(rawText: String, sourceUri: String = "manual"): List<TripCandidate> {
        val lines = rawText.normalizedLines()
        val detailRows = lines.indices.filter { index -> lines[index].isTripDetailRow() }
        if (detailRows.isEmpty()) return listOf(parseSegment(lines, rawText, sourceUri))
        if (detailRows.size == 1) return listOf(parseSegment(lines, rawText, sourceUri))

        var start = 0
        return detailRows.mapIndexed { cardIndex, detailIndex ->
            val segment = lines.subList(start, detailIndex + 1)
            start = detailIndex + 1
            parseSegment(segment, rawText, "$sourceUri#trip-${cardIndex + 1}")
        }
    }

    private fun parseSegment(lines: List<String>, screenshotText: String, sourceUri: String): TripCandidate {
        val segmentText = lines.joinToString("\n")
        val detailIndex = lines.indexOfLast { it.isTripDetailRow() }.takeIf { it >= 0 }
        val earnings = findTripEarnings(lines, detailIndex)
        val tip = labeledMoney(lines, TIP_WORDS)
        val cash = labeledMoney(lines, CASH_WORDS)
        val duration = DURATION.find(segmentText)?.let(::durationSeconds)
        val distance = DISTANCE.find(segmentText)?.groupValues?.get(1)?.replace(',', '.')?.let(::BigDecimal)
        val completedAt = parseCompletedAt(segmentText, screenshotText)
        val tripType = TYPE.find(segmentText)?.value
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
            rawText = segmentText,
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

    private fun findTripEarnings(lines: List<String>, detailIndex: Int?): Long? {
        val indices = if (detailIndex == null) lines.indices.reversed() else (0..detailIndex).reversed()
        return indices.firstNotNullOfOrNull { index ->
            val line = lines[index]
            val nearbyLabel = listOfNotNull(lines.getOrNull(index - 1), line, lines.getOrNull(index + 1))
                .joinToString(" ")
            if (nearbyLabel.contains(TIP_WORDS) || nearbyLabel.contains(CASH_WORDS)) null
            else MONEY.find(line)?.groupValues?.get(1)?.toMinorUnits()
        }
    }

    private fun labeledMoney(lines: List<String>, label: Regex): Long? {
        lines.forEachIndexed { index, line ->
            if (!line.contains(label)) return@forEachIndexed
            listOfNotNull(line, lines.getOrNull(index - 1), lines.getOrNull(index + 1)).forEach { candidate ->
                MONEY.find(candidate)?.groupValues?.get(1)?.toMinorUnits()?.let { return it }
            }
        }
        return null
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

    private fun parseCompletedAt(segmentText: String, screenshotText: String) = runCatching {
        val dateMatch = DATE.find(segmentText) ?: DATE.find(screenshotText) ?: return null
        val timeMatch = TIME.find(segmentText) ?: return null
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

    private fun String.normalizedLines(): List<String> =
        lines().map(String::trim).filter(String::isNotEmpty)

    private fun String.isTripDetailRow(): Boolean =
        (contains(TYPE) && (contains(DURATION) || contains(DISTANCE))) ||
            (contains(DURATION) && contains(DISTANCE))

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
