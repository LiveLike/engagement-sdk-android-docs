package com.livelike.livelikesdk.widget.view

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.os.Handler
import com.google.gson.JsonObject
import com.livelike.livelikesdk.LiveLikeSDK.Companion.currentSession
import com.livelike.livelikesdk.util.AndroidResource
import com.livelike.livelikesdk.util.gson
import com.livelike.livelikesdk.util.logDebug
import com.livelike.livelikesdk.widget.WidgetType
import com.livelike.livelikesdk.widget.model.Alert

internal class AlertWidgetViewModel : ViewModel() {
    private var timeoutStarted = false
    var data: MutableLiveData<Alert?> = MutableLiveData()

    init {
        currentSession.widgetStream.subscribe(this::class.java) { s: String?, j: JsonObject? ->
            widgetObserver(
                s ?: "",
                j
            )
        }
    }

    private fun widgetObserver(type: String, payload: JsonObject?) {
        if (payload != null && WidgetType.fromString(type) == WidgetType.ALERT) {
            data.postValue(gson.fromJson(payload.toString(), Alert::class.java) ?: null)
        }
    }

    fun startDismissTimout(timeout: String) {
        if (!timeoutStarted && timeout.isNotEmpty()) {
            timeoutStarted = true
            Handler().postDelayed({
                dismiss()
                timeoutStarted = false
            }, AndroidResource.parseDuration(timeout))
        }
    }

    fun dismiss() {
        currentSession.widgetStream.onNext(null, null)
        timeoutStarted = false
    }

    override fun onCleared() {
        // NEED  TO CLEAR THE viewModel when timer expires
        logDebug { "ViewModel is cleared" }
        // need to clear
        currentSession.widgetStream.unsubscribe(this::class.java)
    }
}