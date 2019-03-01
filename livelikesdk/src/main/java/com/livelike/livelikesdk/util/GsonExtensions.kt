package com.livelike.livelikesdk.util

import android.util.Log
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.lang.reflect.Type
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

fun JsonObject.extractStringOrEmpty(propertyName: String): String {
    return if (this.has(propertyName) && !this[propertyName].isJsonNull) this[propertyName].asString else ""
}

fun JsonObject.extractLong(propertyName: String, default: Long = 0): Long {
    var returnVal = default
    try {
        returnVal = if (this.has(propertyName) && !this[propertyName].isJsonNull) this[propertyName].asLong else default
    } catch (e: NumberFormatException) {
        Log.e(this.javaClass.canonicalName, "Failed to extractLong: ", e)
    }
    return returnVal
}

class DateDeserializer : JsonDeserializer<Date> {

    override fun deserialize(element: JsonElement, arg1: Type, arg2: JsonDeserializationContext): Date? {
        val date = element.asString

        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        formatter.timeZone = TimeZone.getTimeZone("UTC")

        return try {
            formatter.parse(date)
        } catch (e: ParseException) {
            Log.e("Deserialize", "Failed to parse Date due to:", e)
            null
        }

    }
}