package com.livelike.livelikesdk.widget.view

import android.arch.lifecycle.ViewModel
import android.os.Handler
import com.google.gson.JsonObject
import com.livelike.engagementsdkapi.LiveLikeContentSession
import com.livelike.livelikesdk.util.AndroidResource
import com.livelike.livelikesdk.util.gson
import com.livelike.livelikesdk.util.logDebug
import com.livelike.livelikesdk.widget.model.Alert
import com.livelike.livelikesdk.widget.model.Resource

internal class AlertWidgetViewModel : ViewModel() {
    var payload: JsonObject = JsonObject()
        set(value) {
            field = value
            data = gson.fromJson(payload.toString(), Alert::class.java)
                ?: error("hello from json") // could have this in the data layer, doesn't really belong
            val timeout = AndroidResource.parseDuration(gson.fromJson(payload, Resource::class.java).timeout)
            Handler().postDelayed({ dismiss() }, timeout)
        }
    var session: LiveLikeContentSession? = null
    var data = Alert()

    fun voteForOption(optionId: String) {
        //
    }

    fun dismiss() {
        session?.widgetStream?.onNext(null)
    }

    override fun onCleared() {
        logDebug { "ViewModel is cleared" }
        payload = JsonObject()
        session = null
    }
}
