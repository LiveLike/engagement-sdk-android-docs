package com.livelike.livelikedemo.mml.widgets.utils

import android.util.Log
import org.threeten.bp.Duration
import java.time.format.DateTimeParseException


fun String.parseDuration(): Long {
    var timeout = 7000L
    try {
        timeout = Duration.parse(this).toMillis()
    } catch (e: DateTimeParseException) {
        Log.e("Error", "Duration $this can't be parsed.")
    }
    return timeout
}