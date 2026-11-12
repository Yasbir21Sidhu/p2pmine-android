package com.hashcore.pool.data.remote.dto

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import java.time.Instant
import java.time.format.DateTimeParseException

/**
 * Epoch milliseconds that accept either:
 * - JSON number: 1710000000000
 * - ISO-8601 string: "2026-07-25T18:55:13.499Z"
 *
 * P2PMine API returns ISO strings for lastShareAt / createdAt, etc.
 */
@JvmInline
value class FlexibleEpochMs(val value: Long)

object EpochMsParser {
    fun parse(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        val trimmed = raw.trim()
        trimmed.toLongOrNull()?.let { return normalize(it) }
        trimmed.toDoubleOrNull()?.let { return normalize(it.toLong()) }
        return try {
            Instant.parse(trimmed).toEpochMilli()
        } catch (_: DateTimeParseException) {
            // rare malformed: "2026-07-25T18:5513.499Z" missing colon
            try {
                val fixed = trimmed.replace(
                    Regex("""T(\d{2}):(\d{2})(\d{2})\.(\d+)Z$"""),
                    "T$1:$2:$3.$4Z",
                )
                Instant.parse(fixed).toEpochMilli()
            } catch (_: Exception) {
                null
            }
        }
    }

    fun normalize(v: Long): Long =
        if (v in 1_000_000_000L..9_999_999_999L) v * 1000L else v
}

class FlexibleEpochMsAdapter {
    @FromJson
    fun fromJson(reader: JsonReader): FlexibleEpochMs? {
        return when (reader.peek()) {
            JsonReader.Token.NULL -> {
                reader.nextNull<Unit>()
                null
            }
            JsonReader.Token.NUMBER -> {
                val n = reader.nextDouble()
                FlexibleEpochMs(EpochMsParser.normalize(n.toLong()))
            }
            JsonReader.Token.STRING -> {
                val ms = EpochMsParser.parse(reader.nextString()) ?: return null
                FlexibleEpochMs(ms)
            }
            else -> {
                reader.skipValue()
                null
            }
        }
    }

    @ToJson
    fun toJson(writer: JsonWriter, value: FlexibleEpochMs?) {
        if (value == null) writer.nullValue()
        else writer.value(value.value)
    }
}
