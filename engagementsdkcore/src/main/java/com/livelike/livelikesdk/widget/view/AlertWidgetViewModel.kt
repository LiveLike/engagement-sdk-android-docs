package com.livelike.livelikesdk.widget.view

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.os.Handler
import com.google.gson.JsonObject
import com.livelike.livelikesdk.LiveLikeSDK.Companion.currentSession
import com.livelike.livelikesdk.util.AndroidResource
import com.livelike.livelikesdk.util.gson
import com.livelike.livelikesdk.util.logDebug
import com.livelike.livelikesdk.widget.model.Alert

internal class AlertWidgetViewModel : ViewModel() {
    private var timeoutStarted = false
    var data: MutableLiveData<Alert?> = MutableLiveData()

    init {
        currentSession.widgetPayloadStream.subscribe(this::class.java) { observePayload(it) }
    }

    // should also be able to filter by widget type
    private fun observePayload(payload: JsonObject?) {
        if (payload != null) data.postValue(gson.fromJson(payload.toString(), Alert::class.java) ?: null)
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

    fun voteForOption(optionId: String) {
        //
    }

    fun dismiss() {
        currentSession.widgetTypeStream.onNext(null)
        currentSession.widgetPayloadStream.onNext(null)
        timeoutStarted = false
    }

    override fun onCleared() {
        // NEED  TO CLEAR THE viewModel when timer expires
        logDebug { "ViewModel is cleared" }
        // need to clear
        currentSession.widgetPayloadStream.unsubscribe(this::class.java)
    }
}