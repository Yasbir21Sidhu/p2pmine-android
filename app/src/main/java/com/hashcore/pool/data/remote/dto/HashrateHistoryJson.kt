package com.hashcore.pool.data.remote.dto

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.reflect.Type

/**
 * Tolerant parser for hashrate history series from P2PMine.
 *
 * Production payloads vary:
 * - `[{ "time": "14:00", "hashrateHs": 1.2e14 }, …]`
 * - `[{ "t": 1710000000, "hs": … }]`
 * - `[{ "timestamp": "…Z", "hashrate": … }]`
 * - `[{ "hashrateThs": 120, "label": "14:00" }]`
 * - `[[ts, hs], …]` pairs
 * - string numbers, nulls, extra keys
 *
 * Strict Moshi List&lt;HashratePointDto&gt; fails the **entire** response on one bad
 * element — which made worker detail fall back to the list endpoint (no chart).
 */
object HashrateHistoryParser {

    fun parseArray(reader: JsonReader): List<HashratePointDto> {
        if (reader.peek() == JsonReader.Token.NULL) {
            reader.nextNull<Unit>()
            return emptyList()
        }
        if (reader.peek() != JsonReader.Token.BEGIN_ARRAY) {
            reader.skipValue()
            return emptyList()
        }
        val out = ArrayList<HashratePointDto>()
        reader.beginArray()
        while (reader.hasNext()) {
            when (reader.peek()) {
                JsonReader.Token.BEGIN_OBJECT -> parseObject(reader)?.let { out.add(it) }
                JsonReader.Token.BEGIN_ARRAY -> parsePair(reader)?.let { out.add(it) }
                JsonReader.Token.NULL -> reader.nextNull<Unit>()
                else -> reader.skipValue()
            }
        }
        reader.endArray()
        return out
    }

    /** Best-effort extract from a full JSON object string (worker detail / history). */
    fun extractFromJsonObject(json: String): List<HashratePointDto> {
        if (json.isBlank()) return emptyList()
        return try {
            val reader = JsonReader.of(okio.Buffer().writeUtf8(json))
            if (reader.peek() != JsonReader.Token.BEGIN_OBJECT) {
                if (reader.peek() == JsonReader.Token.BEGIN_ARRAY) return parseArray(reader)
                return emptyList()
            }
            var best: List<HashratePointDto> = emptyList()
            reader.beginObject()
            while (reader.hasNext()) {
                val name = reader.nextName()
                if (name.equals("history", true) ||
                    name.equals("history24h", true) ||
                    name.equals("hashrateHistory", true) ||
                    name.equals("points", true) ||
                    name.equals("series", true)
                ) {
                    val series = parseArray(reader)
                    if (series.size > best.size) best = series
                } else if (name.equals("worker", true) && reader.peek() == JsonReader.Token.BEGIN_OBJECT) {
                    val nested = extractFromJsonObject(readValueAsString(reader))
                    if (nested.size > best.size) best = nested
                } else {
                    reader.skipValue()
                }
            }
            reader.endObject()
            best
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun readValueAsString(reader: JsonReader): String {
        val buffer = okio.Buffer()
        val writer = JsonWriter.of(buffer)
        writer.isLenient = true
        writer.serializeNulls = true
        copyValue(reader, writer)
        writer.close()
        return buffer.readUtf8()
    }

    private fun copyValue(reader: JsonReader, writer: JsonWriter) {
        when (reader.peek()) {
            JsonReader.Token.BEGIN_ARRAY -> {
                reader.beginArray()
                writer.beginArray()
                while (reader.hasNext()) copyValue(reader, writer)
                reader.endArray()
                writer.endArray()
            }
            JsonReader.Token.BEGIN_OBJECT -> {
                reader.beginObject()
                writer.beginObject()
                while (reader.hasNext()) {
                    writer.name(reader.nextName())
                    copyValue(reader, writer)
                }
                reader.endObject()
                writer.endObject()
            }
            JsonReader.Token.STRING -> writer.value(reader.nextString())
            JsonReader.Token.NUMBER -> writer.value(reader.nextDouble())
            JsonReader.Token.BOOLEAN -> writer.value(reader.nextBoolean())
            JsonReader.Token.NULL -> {
                reader.nextNull<Unit>()
                writer.nullValue()
            }
            else -> reader.skipValue()
        }
    }

    private fun parseObject(reader: JsonReader): HashratePointDto? {
        var t: FlexibleEpochMs? = null
        var time: FlexibleTimeLabel? = null
        var ts: FlexibleEpochMs? = null
        var timestamp: FlexibleEpochMs? = null
        var hashrateHs: Double? = null
        var hs: Double? = null
        var hashrate: Double? = null
        var hashrateThs: Double? = null
        var value: Double? = null

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName().lowercase()) {
                "t" -> t = readEpoch(reader)
                "ts" -> ts = readEpoch(reader)
                "timestamp" -> timestamp = readEpoch(reader)
                "time", "label", "bucket", "x" -> time = readTimeLabel(reader)
                "hashratehs", "hashrate_hs" -> hashrateHs = readDouble(reader)
                "hs" -> hs = readDouble(reader)
                "hashrate", "rate", "y", "value", "v" -> {
                    val d = readDouble(reader)
                    if (hashrate == null) hashrate = d
                    if (value == null) value = d
                }
                "hashrateths", "hashrate_ths", "ths" -> hashrateThs = readDouble(reader)
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        val resolvedHs = hashrateHs
            ?: hs
            ?: hashrate
            ?: value
            ?: hashrateThs?.let { it * 1e12 }
            ?: return null

        return HashratePointDto(
            t = t,
            time = time,
            ts = ts,
            timestamp = timestamp,
            hashrateHs = resolvedHs,
            hs = hs,
            hashrate = hashrate,
        )
    }

    private fun parsePair(reader: JsonReader): HashratePointDto? {
        reader.beginArray()
        val values = ArrayList<Any?>()
        while (reader.hasNext()) {
            when (reader.peek()) {
                JsonReader.Token.NUMBER -> values.add(reader.nextDouble())
                JsonReader.Token.STRING -> values.add(reader.nextString())
                JsonReader.Token.NULL -> {
                    reader.nextNull<Unit>()
                    values.add(null)
                }
                else -> reader.skipValue()
            }
        }
        reader.endArray()
        if (values.size < 2) return null
        val a = values[0]
        val b = values[1]
        // Prefer [ts, hs]
        val epoch = when (a) {
            is Number -> EpochMsParser.normalize(a.toLong())
            is String -> EpochMsParser.parse(a)
            else -> null
        }
        val rate = when (b) {
            is Number -> b.toDouble()
            is String -> b.toDoubleOrNull()
            else -> null
        } ?: return null
        return HashratePointDto(
            t = epoch?.let { FlexibleEpochMs(it) },
            hashrateHs = rate,
        )
    }

    private fun readDouble(reader: JsonReader): Double? = when (reader.peek()) {
        JsonReader.Token.NULL -> {
            reader.nextNull<Unit>()
            null
        }
        JsonReader.Token.NUMBER -> reader.nextDouble()
        JsonReader.Token.STRING -> reader.nextString().replace(",", "").toDoubleOrNull()
        JsonReader.Token.BOOLEAN -> {
            reader.skipValue()
            null
        }
        else -> {
            reader.skipValue()
            null
        }
    }

    private fun readEpoch(reader: JsonReader): FlexibleEpochMs? = when (reader.peek()) {
        JsonReader.Token.NULL -> {
            reader.nextNull<Unit>()
            null
        }
        JsonReader.Token.NUMBER -> FlexibleEpochMs(EpochMsParser.normalize(reader.nextDouble().toLong()))
        JsonReader.Token.STRING -> EpochMsParser.parse(reader.nextString())?.let { FlexibleEpochMs(it) }
        else -> {
            reader.skipValue()
            null
        }
    }

    private fun readTimeLabel(reader: JsonReader): FlexibleTimeLabel? = when (reader.peek()) {
        JsonReader.Token.NULL -> {
            reader.nextNull<Unit>()
            null
        }
        JsonReader.Token.NUMBER -> {
            val ms = EpochMsParser.normalize(reader.nextDouble().toLong())
            FlexibleTimeLabel(label = null, epochMs = ms)
        }
        JsonReader.Token.STRING -> {
            val s = reader.nextString()
            FlexibleTimeLabel(label = s, epochMs = EpochMsParser.parse(s))
        }
        else -> {
            reader.skipValue()
            null
        }
    }
}

/** Moshi factory: all `List<HashratePointDto>` use the tolerant parser. */
class HashratePointListAdapterFactory : JsonAdapter.Factory {
    override fun create(
        type: Type,
        annotations: MutableSet<out Annotation>,
        moshi: Moshi,
    ): JsonAdapter<*>? {
        if (annotations.isNotEmpty()) return null
        val raw = Types.getRawType(type)
        if (raw != List::class.java && raw != MutableList::class.java) return null
        val element = Types.collectionElementType(type, List::class.java)
        if (Types.getRawType(element) != HashratePointDto::class.java) return null
        return object : JsonAdapter<List<HashratePointDto>>() {
            override fun fromJson(reader: JsonReader): List<HashratePointDto> =
                HashrateHistoryParser.parseArray(reader)

            override fun toJson(writer: JsonWriter, value: List<HashratePointDto>?) {
                if (value == null) {
                    writer.nullValue()
                    return
                }
                writer.beginArray()
                for (p in value) {
                    writer.beginObject()
                    p.t?.value?.let { writer.name("t"); writer.value(it) }
                    p.time?.label?.let { writer.name("time"); writer.value(it) }
                    p.hashrateHs?.let { writer.name("hashrateHs"); writer.value(it) }
                        ?: p.hs?.let { writer.name("hs"); writer.value(it) }
                        ?: p.hashrate?.let { writer.name("hashrate"); writer.value(it) }
                    writer.endObject()
                }
                writer.endArray()
            }
        }.nullSafe()
    }
}
