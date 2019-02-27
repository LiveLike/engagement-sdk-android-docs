package com.livelike.livelikesdk.util

import android.util.Log
import com.google.gson.JsonObject

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