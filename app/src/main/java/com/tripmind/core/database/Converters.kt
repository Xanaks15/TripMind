package com.tripmind.core.database

import androidx.room.TypeConverter
import com.tripmind.core.model.CaptureMethod
import com.tripmind.core.model.OfferStatus
import java.math.BigDecimal
import java.time.Instant

class Converters {
    // Fixed-width UTC encoding retains nanoseconds and chronological TEXT ordering (years 0000–9999).
    @TypeConverter fun instantToText(value: Instant?): String? = value?.let {
        require(it >= MIN_INSTANT && it <= MAX_INSTANT)
        INSTANT_FORMAT.format(it)
    }
    @TypeConverter fun textToInstant(value: String?): Instant? = value?.let(Instant::parse)
    @TypeConverter fun decimalToText(value: BigDecimal?): String? = value?.toPlainString()
    @TypeConverter fun textToDecimal(value: String?): BigDecimal? = value?.let(::BigDecimal)
    @TypeConverter fun statusToText(value: OfferStatus): String = value.name
    @TypeConverter fun textToStatus(value: String): OfferStatus = OfferStatus.valueOf(value)
    @TypeConverter fun captureToText(value: CaptureMethod): String = value.name
    @TypeConverter fun textToCapture(value: String): CaptureMethod = CaptureMethod.valueOf(value)

    companion object {
        private val INSTANT_FORMAT = java.time.format.DateTimeFormatterBuilder().appendInstant(9).toFormatter()
        private val MIN_INSTANT = Instant.parse("0000-01-01T00:00:00Z")
        private val MAX_INSTANT = Instant.parse("9999-12-31T23:59:59.999999999Z")
    }
}
