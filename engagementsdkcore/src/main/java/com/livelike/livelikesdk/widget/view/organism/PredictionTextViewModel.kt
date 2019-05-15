package com.livelike.livelikesdk.widget.view.organism

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import android.os.Handler
import android.support.v7.widget.RecyclerView
import com.google.gson.JsonObject
import com.livelike.livelikesdk.LiveLikeSDK.Companion.currentSession
import com.livelike.livelikesdk.network.LiveLikeDataClientImpl
import com.livelike.livelikesdk.util.AndroidResource
import com.livelike.livelikesdk.util.gson
import com.livelike.livelikesdk.util.liveLikeSharedPrefs.addWidgetPredictionVoted
import com.livelike.livelikesdk.util.liveLikeSharedPrefs.getWidgetPredictionVotedAnswerIdOrEmpty
import com.livelike.livelikesdk.widget.WidgetDataClient
import com.livelike.livelikesdk.widget.WidgetType
import com.livelike.livelikesdk.widget.model.Resource
import com.livelike.livelikesdk.widget.view.molecule.TextViewAdapter

internal class PredictionTextViewModel(application: Application) : AndroidViewModel(application) {
    val data: MutableLiveData<Resource> = MutableLiveData()
    private val dataClient: WidgetDataClient = LiveLikeDataClientImpl()
    var state: MutableLiveData<String> = MutableLiveData() // confirmation, followup

    var adapter: TextViewAdapter? = null
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
        if (payload != null
            && (type == WidgetType.IMAGE_PREDICTION
                    || type == WidgetType.IMAGE_PREDICTION_RESULTS
                    || type == WidgetType.TEXT_PREDICTION
                    || type == WidgetType.TEXT_PREDICTION_RESULTS)
        ) {
            data.postValue(gson.fromJson(payload.toString(), Resource::class.java) ?: null)
        } else {
            cleanUp()
        }
    }

    fun startDismissTimout(timeout: String, isFollowup: Boolean) {
        if (!timeoutStarted && timeout.isNotEmpty()) {
            timeoutStarted = true
            Handler().removeCallbacks { confirmationState() }
            if (isFollowup) {
                Handler().postDelayed({ dismiss() }, AndroidResource.parseDuration(timeout))
                data.value?.apply {
                    followupState(text_prediction_id, correct_option_id)
                }
            } else {
                Handler().postDelayed({ confirmationState() }, AndroidResource.parseDuration(timeout))
            }
        }
    }

    fun dismiss() {
        data.value?.let {
            adapter?.apply {
                if (selectedPosition != RecyclerView.NO_POSITION) { // User has selected an option
                    val selectedOption = it.getMergedOptions()?.get(selectedPosition)

                    // Prediction widget votes on dismiss
                    selectedOption?.getMergedVoteUrl()?.let { it1 -> dataClient.vote(it1) }

                    // Save widget id and voted option for followup widget
                    addWidgetPredictionVoted(it.id, selectedOption?.id ?: "")
                }
            }
        }

        currentSession.widgetStream.onNext(null, null)
    }

    private fun followupState(textPredictionId: String, correctOptionId: String) {
        adapter?.predictionSelected = getWidgetPredictionVotedAnswerIdOrEmpty(textPredictionId)
        adapter?.predictionCorrect = correctOptionId
        adapter?.selectionLocked = true

        val rootPath = if (adapter?.predictionCorrect == adapter?.predictionSelected) "correctAnswer" else "wrongAnswer"
        animationPath = AndroidResource.selectRandomLottieAnimation(rootPath, getApplication()) ?: ""

        state.postValue("followup")
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