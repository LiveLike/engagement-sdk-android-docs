package com.livelike.livelikesdk.widget.view.poll

import android.content.Context
import com.google.gson.JsonObject
import com.livelike.livelikesdk.util.gson
import com.livelike.livelikesdk.widget.model.Resource

internal class PollTextWidgetData(payload: JsonObject, context: Context) {
    val widgetResource = gson.fromJson(payload.toString(), Resource::class.java)
//    val view = PollTextWidget(context).bind(this)
}