package com.livelike.livelikesdk.utils

import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type
import java.text.ParseException
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.DateTimeParseException

internal fun JsonObject.extractStringOrEmpty(propertyName: String): String {
    return if (this.has(propertyName) && !this[propertyName].isJsonNull) this[propertyName].asString else ""
}

internal fun JsonObject.extractLong(propertyName: String, default: Long = 0): Long {
    var returnVal = default
    try {
        returnVal = if (this.has(propertyName) && !this[propertyName].isJsonNull) this[propertyName].asLong else default
    } catch (e: NumberFormatException) {
        logError { "Failed to extractLong: $e" }
    }
    return returnVal
}

internal val gson = GsonBuilder()
    .registerTypeAdapter(ZonedDateTime::class.java, DateDeserializer())
    .registerTypeAdapter(ZonedDateTime::class.java, DateSerializer())
    .create()!!

private val isoUTCDateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneId.of("UTC"))

internal class DateDeserializer : JsonDeserializer<ZonedDateTime> {

    override fun deserialize(element: JsonElement, arg1: Type, arg2: JsonDeserializationContext): ZonedDateTime? {
        val date = element.asString

        return try {
            ZonedDateTime.parse(date, isoUTCDateTimeFormatter)
        } catch (e: ParseException) {
            Log.e("Deserialize", "Failed to parse Date due to:", e)
            null
        } catch (e: DateTimeParseException) {
            Log.e("Deserialize", "Failed to parse Date due to:", e)
            null
        }
    }
}

internal class DateSerializer : JsonSerializer<ZonedDateTime> {
    override fun serialize(src: ZonedDateTime?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        val obj = JsonObject()
        obj.addProperty("program_date_time", isoUTCDateTimeFormatter.format(src).toString())
        return obj.get("program_date_time")
    }
}
