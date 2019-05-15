package com.livelike.livelikesdk.widget.viewModel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import android.os.Handler
import android.support.v7.widget.RecyclerView
import com.google.gson.JsonObject
import com.livelike.livelikesdk.LiveLikeSDK.Companion.currentSession
import com.livelike.livelikesdk.services.network.LiveLikeDataClientImpl
import com.livelike.livelikesdk.utils.AndroidResource
import com.livelike.livelikesdk.utils.gson
import com.livelike.livelikesdk.widget.WidgetDataClient
import com.livelike.livelikesdk.widget.WidgetType
import com.livelike.livelikesdk.widget.adapters.PollViewAdapter
import com.livelike.livelikesdk.widget.model.Resource

internal class PollViewModel(application: Application) : AndroidViewModel(application) {
    val data: MutableLiveData<Resource> = MutableLiveData()
    val results: MutableLiveData<Resource> = MutableLiveData()
    private val dataClient: WidgetDataClient = LiveLikeDataClientImpl()

    var state: MutableLiveData<String> = MutableLiveData() // confirmation, followup

    var adapter: PollViewAdapter? = null
    var timeoutStarted = false
    var animationProgress = 0f
    var animationPath = ""


    init {
        currentSession.widgetStream.subscribe(this::class.java) { s: String?, j: JsonObject? ->
            widgetObserver(
                WidgetType.fromString(s ?: ""),
                j
            )
        }
    }

    private fun widgetObserver(type: WidgetType, payload: JsonObject?) {
        if (payload != null && (type == WidgetType.TEXT_POLL || type == WidgetType.IMAGE_POLL)) {
            data.postValue(gson.fromJson(payload.toString(), Resource::class.java) ?: null)
        } else if (payload != null && (type == WidgetType.TEXT_POLL_RESULT || type == WidgetType.IMAGE_POLL_RESULT)) {
            // TODO: need to debounce
            results.postValue(gson.fromJson(payload.toString(), Resource::class.java) ?: null)
        } else {
            cleanUp()
        }
    }

    fun startDismissTimout(timeout: String) {
        if (!timeoutStarted && timeout.isNotEmpty()) {
            timeoutStarted = true
            Handler().removeCallbacks { confirmationState() }
            Handler().postDelayed({ confirmationState() }, AndroidResource.parseDuration(timeout))
        }
    }

    fun dismiss() {
        data.value?.let {
            adapter?.apply {
                if (selectedPosition != RecyclerView.NO_POSITION) { // User has selected an option
                    val selectedOption = it.getMergedOptions()?.get(selectedPosition)

                    // Prediction widget votes on dismiss
                    selectedOption?.getMergedVoteUrl()?.let { it1 -> dataClient.vote(it1) }
                }
            }
        }

        currentSession.widgetStream.onNext(null, null)
    }

    private fun confirmationState() {
        if (adapter?.selectedPosition == RecyclerView.NO_POSITION) {
            // If the user never selected an option dismiss the widget with no confirmation
            dismiss()
            return
        }

        adapter?.selectionLocked = true
        animationPath = AndroidResource.selectRandomLottieAnimation("confirmMessage", getApplication()) ?: ""

        state.postValue("confirmation")

        Handler().removeCallbacks { dismiss() }
        Handler().postDelayed({ dismiss() }, 6000)
    }

    private fun cleanUp() {
        timeoutStarted = false
        adapter = null
        animationProgress = 0f
        animationPath = ""
        state.postValue("")
        data.postValue(null)
    }

    override fun onCleared() {
        currentSession.widgetStream.unsubscribe(this::class.java)
    }
}