package com.livelike.livelikesdk.widget.viewModel

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.os.Handler
import com.livelike.engagementsdkapi.LiveLikeContentSession
import com.livelike.engagementsdkapi.WidgetInfos
import com.livelike.livelikesdk.services.analytics.AnalyticsWidgetInteractionInfo
import com.livelike.livelikesdk.services.analytics.analyticService
import com.livelike.livelikesdk.utils.AndroidResource
import com.livelike.livelikesdk.utils.gson
import com.livelike.livelikesdk.utils.logDebug
import com.livelike.livelikesdk.widget.DismissAction
import com.livelike.livelikesdk.widget.WidgetType
import com.livelike.livelikesdk.widget.model.Alert

internal class AlertWidgetViewModel : ViewModel() {
    private var timeoutStarted = false
    var data: MutableLiveData<Alert?> = MutableLiveData()
    val handler = Handler()
    var currentSession: LiveLikeContentSession? = null
        set(value) {
            field = value
            value?.currentWidgetInfosStream?.subscribe(this::class.java) { widgetInfos: WidgetInfos? ->
                widgetObserver(widgetInfos)
            }
        }

    private var currentWidgetId: String = ""
    private var currentWidgetType: WidgetType? = null
    private val interactionData = AnalyticsWidgetInteractionInfo()

    private fun widgetObserver(widgetInfos: WidgetInfos?) {
        if (widgetInfos != null && WidgetType.fromString(widgetInfos.type) == WidgetType.ALERT) {
            data.postValue(gson.fromJson(widgetInfos.payload.toString(), Alert::class.java) ?: null)
            interactionData.widgetDisplayed()
            currentWidgetId = widgetInfos.widgetId
            currentWidgetType = WidgetType.fromString(widgetInfos.type)
        } else {
            data.postValue(null)
            cleanup()
        }
    }

    fun dismissWidget(action: DismissAction) {
        currentWidgetType?.let {
            analyticService.trackWidgetDismiss(
                it,
                currentWidgetId,
                interactionData,
                false,
                action
            )
        }
        currentSession?.currentWidgetInfosStream?.onNext(null)
    }

    fun startDismissTimout(timeout: String) {
        if (!timeoutStarted && timeout.isNotEmpty()) {
            timeoutStarted = true
            handler.postDelayed({
                dismissWidget(DismissAction.TIMEOUT)
                timeoutStarted = false
            }, AndroidResource.parseDuration(timeout))
        }
    }

    private fun cleanup() {
        timeoutStarted = false
        handler.removeCallbacksAndMessages(null)

        currentWidgetType = null
        currentWidgetId = ""
        interactionData.reset()
    }

    override fun onCleared() {
        // NEED  TO CLEAR THE viewModel when timer expires
        logDebug { "ViewModel is cleared" }
        // need to clear
        currentSession?.currentWidgetInfosStream?.unsubscribe(this::class.java)
    }

    fun setSession(currentSession: LiveLikeContentSession?) {
        this.currentSession = currentSession
    }
}