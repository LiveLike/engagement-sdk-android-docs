package com.livelike.livelikesdk.widget.viewModel

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.os.Handler
import com.livelike.engagementsdkapi.WidgetInfos
import com.livelike.livelikesdk.LiveLikeSDK.Companion.currentSession
import com.livelike.livelikesdk.utils.AndroidResource
import com.livelike.livelikesdk.utils.gson
import com.livelike.livelikesdk.utils.logDebug
import com.livelike.livelikesdk.widget.WidgetType
import com.livelike.livelikesdk.widget.model.Alert

internal class AlertWidgetViewModel : ViewModel() {
    private var timeoutStarted = false
    var data: MutableLiveData<Alert?> = MutableLiveData()
    val handler = Handler()

    init {
        currentSession.currentWidgetInfosStream.subscribe(this::class.java) { widgetInfos: WidgetInfos? ->
            widgetObserver(widgetInfos)
        }
    }

    private fun widgetObserver(widgetInfos: WidgetInfos?) {
        if (widgetInfos != null && WidgetType.fromString(widgetInfos.type) == WidgetType.ALERT) {
            data.postValue(gson.fromJson(widgetInfos.payload.toString(), Alert::class.java) ?: null)
        } else {
            data.postValue(null)
            cleanup()
        }
    }

    fun startDismissTimout(timeout: String) {
        if (!timeoutStarted && timeout.isNotEmpty()) {
            timeoutStarted = true
            handler.postDelayed({
                dismiss()
                timeoutStarted = false
            }, AndroidResource.parseDuration(timeout))
        }
    }

    private fun dismiss() {
        currentSession.currentWidgetInfosStream.onNext(null)
    }

    private fun cleanup() {
        timeoutStarted = false
        handler.removeCallbacksAndMessages(null)
    }

    override fun onCleared() {
        // NEED  TO CLEAR THE viewModel when timer expires
        logDebug { "ViewModel is cleared" }
        // need to clear
        currentSession.currentWidgetInfosStream.unsubscribe(this::class.java)
    }
}