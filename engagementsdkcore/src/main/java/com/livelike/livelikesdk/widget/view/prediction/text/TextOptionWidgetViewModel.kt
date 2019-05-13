package com.livelike.livelikesdk.widget.view.prediction.text

import android.arch.lifecycle.ViewModel
import com.google.gson.JsonObject
import com.livelike.livelikesdk.LiveLikeSDK.Companion.currentSession
import com.livelike.livelikesdk.util.gson
import com.livelike.livelikesdk.util.logDebug
import com.livelike.livelikesdk.widget.model.Resource
import com.livelike.livelikesdk.widget.view.AlertWidgetView

internal class TextOptionWidgetViewModel : ViewModel() {
    var optionAdapter: TextOptionWidgetBase.TextOptionAdapter? = null
    var userSelection: String? = ""

    init {
        currentSession.widgetPayloadStream.subscribe(AlertWidgetView::class.java) { observePayload(it) }
    }

    private fun observePayload(payload: JsonObject?) {
        data = gson.fromJson(payload.toString(), Resource::class.java) ?: error("hello from json")
    }
    var data = Resource()

    fun dismiss() {
        currentSession.widgetTypeStream.onNext(null)
    }

    override fun onCleared() {
        // NEED  TO CLEAR THE viewModel when timer expires
        logDebug { "ViewModel is cleared" }
        currentSession.widgetPayloadStream.unsubscribe(AlertWidgetView::class.java)
    }
}