package com.livelike.livelikesdk.util

import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.lang.reflect.Type
import java.text.ParseException


fun JsonObject.extractStringOrEmpty(propertyName: String): String {
    return if (this.has(propertyName) && !this[propertyName].isJsonNull) this[propertyName].asString else ""
}

fun JsonObject.extractLong(propertyName: String, default: Long = 0): Long {
    var returnVal = default
    try {
        returnVal = if (this.has(propertyName) && !this[propertyName].isJsonNull) this[propertyName].asLong else default
    } catch (e: NumberFormatException) {
        logError { "Failed to extractLong: $e" }
    }
    return returnVal
}

val gson = GsonBuilder()
    .registerTypeAdapter(ZonedDateTime::class.java, DateDeserializer())
    .registerTypeAdapter(ZonedDateTime::class.java, DateSerializer())
    .create()!!

private val isoUTCDateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneId.of("UTC"))

class DateDeserializer : JsonDeserializer<ZonedDateTime> {

    override fun deserialize(element: JsonElement, arg1: Type, arg2: JsonDeserializationContext): ZonedDateTime? {
        val date = element.asString

        return try {
            ZonedDateTime.parse(date, isoUTCDateTimeFormatter)
        } catch (e: ParseException) {
            Log.e("Deserialize", "Failed to parse Date due to:", e)
            null
        }
    }
}

class DateSerializer : JsonSerializer<ZonedDateTime> {
    override fun serialize(src: ZonedDateTime?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        val obj = JsonObject()
        obj.addProperty("program_date_time", isoUTCDateTimeFormatter.format(src).toString())
        return obj.get("program_date_time")
    }
}