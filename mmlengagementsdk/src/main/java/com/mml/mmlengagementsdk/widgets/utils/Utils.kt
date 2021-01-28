package com.mml.mmlengagementsdk.widgets.utils

import android.graphics.Typeface
import android.util.Log
import android.widget.TextView
import org.threeten.bp.Duration
import org.threeten.bp.format.DateTimeParseException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


fun String.parseDuration(): Long {
    var timeout = 7000L
    try {
        timeout = Duration.parse(this).toMillis()
    } catch (e: DateTimeParseException) {
        Log.e("Error", "Duration $this can't be parsed.")
    }
    return timeout
}


private val DEFAULT_WIDGET_DATE_TIME_FORMATTER = SimpleDateFormat(
    "MMM d, h:mm a",
    Locale.getDefault()
)

fun getFormattedTime(time: String): String {
    val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S'Z'")
    try {
        val date: Date = format.parse(time)
        return DEFAULT_WIDGET_DATE_TIME_FORMATTER.format(date)
    } catch (e: ParseException) {
        e.printStackTrace()
    }
    return ""
}

fun setCustomFontWithTextStyle(
    textView: TextView,
    fontPath: String?
) {
    if (fontPath != null) {
        try {
            val typeFace =
                Typeface.createFromAsset(
                    textView.context.assets,
                    fontPath
                )
            textView.setTypeface(typeFace, Typeface.NORMAL)
        } catch (e: Exception) {
            e.printStackTrace()
            textView.setTypeface(null, Typeface.NORMAL)
        }
    } else {
        textView.setTypeface(null, Typeface.NORMAL)
    }
}