package com.livelike.livelikesdk.widget.view.prediction.text

import android.arch.lifecycle.ViewModel
import android.os.Handler
import com.google.gson.JsonObject
import com.livelike.engagementsdkapi.LiveLikeContentSession
import com.livelike.livelikesdk.util.AndroidResource
import com.livelike.livelikesdk.util.gson
import com.livelike.livelikesdk.widget.model.Resource
import com.livelike.livelikesdk.widget.view.LiveLikeViewModel

internal class TextOptionWidgetViewModel() : ViewModel(), LiveLikeViewModel {
    var optionAdapter: TextOptionWidgetBase.TextOptionAdapter? = null
    var userSelection: String? = ""
    override var payload: JsonObject = JsonObject()
        set(value) {
            field = value
            data = gson.fromJson(payload.toString(), Resource::class.java) ?: error("hello from json")
            val timeout = AndroidResource.parseDuration(gson.fromJson(payload, Resource::class.java).timeout)
            Handler().postDelayed({ dismiss() }, timeout)
        }
    override var session: LiveLikeContentSession? = null
    var data = Resource()


    fun dismiss() {
        session?.widgetStream?.onNext(null)
    }
}