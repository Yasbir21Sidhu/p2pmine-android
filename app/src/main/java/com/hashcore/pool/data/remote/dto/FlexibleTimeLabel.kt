package com.hashcore.pool.data.remote.dto

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

/**
 * History point "time" on P2PMine is usually a display label ("14:00"),
 * but may also be epoch or ISO. Capture all and expose both label + epoch.
 */
data class FlexibleTimeLabel(
    val label: String? = null,
    val epochMs: Long? = null,
)

class FlexibleTimeLabelAdapter {
    @FromJson
    fun fromJson(reader: JsonReader): FlexibleTimeLabel? {
        return when (reader.peek()) {
            JsonReader.Token.NULL -> {
                reader.nextNull<Unit>()
                null
            }
            JsonReader.Token.NUMBER -> {
                val n = reader.nextDouble()
                val ms = EpochMsParser.normalize(n.toLong())
                FlexibleTimeLabel(label = null, epochMs = ms)
            }
            JsonReader.Token.STRING -> {
                val s = reader.nextString()
                FlexibleTimeLabel(
                    label = s,
                    epochMs = EpochMsParser.parse(s),
                )
            }
            else -> {
                reader.skipValue()
                null
            }
        }
    }

    @ToJson
    fun toJson(writer: JsonWriter, value: FlexibleTimeLabel?) {
        when {
            value == null -> writer.nullValue()
            value.label != null -> writer.value(value.label)
            value.epochMs != null -> writer.value(value.epochMs)
            else -> writer.nullValue()
        }
    }
}
