package com.livelike.livelikedemo.ui.main

import android.content.Context
import android.util.AttributeSet
import com.livelike.engagementsdk.chat.ChatView
import java.text.SimpleDateFormat
import java.util.*

class CustomChatView(context: Context, attributes: AttributeSet?) : ChatView(context, attributes) {

    override fun formatMessageDateTime(messageTimeStamp: Long?): String {
        if (messageTimeStamp == null || messageTimeStamp == 0L) {
            return ""
        }
        val dateTime = Date()
        dateTime.time = messageTimeStamp
        return SimpleDateFormat(
            "MMM d, hh:mma",
            Locale.getDefault()
        ).format(dateTime)
    }
}