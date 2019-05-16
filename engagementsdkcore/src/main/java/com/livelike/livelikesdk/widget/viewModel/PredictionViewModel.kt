package com.livelike.livelikesdk.widget.viewModel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import android.os.Handler
import android.support.v7.widget.RecyclerView
import com.livelike.engagementsdkapi.WidgetInfos
import com.livelike.livelikesdk.LiveLikeSDK.Companion.currentSession
import com.livelike.livelikesdk.services.network.LiveLikeDataClientImpl
import com.livelike.livelikesdk.utils.AndroidResource
import com.livelike.livelikesdk.utils.gson
import com.livelike.livelikesdk.utils.liveLikeSharedPrefs.addWidgetPredictionVoted
import com.livelike.livelikesdk.utils.liveLikeSharedPrefs.getWidgetPredictionVotedAnswerIdOrEmpty
import com.livelike.livelikesdk.widget.WidgetDataClient
import com.livelike.livelikesdk.widget.WidgetType
import com.livelike.livelikesdk.widget.adapters.WidgetOptionsViewAdapter
import com.livelike.livelikesdk.widget.model.Resource

internal class PredictionWidget(
    val type: WidgetType,
    val resource: Resource
)

internal class PredictionViewModel(application: Application) : AndroidViewModel(application) {
    val data: MutableLiveData<PredictionWidget> = MutableLiveData()
    private val dataClient: WidgetDataClient = LiveLikeDataClientImpl()
    var state: MutableLiveData<String> = MutableLiveData() // confirmation, followup

    var adapter: WidgetOptionsViewAdapter? = null
    var timeoutStarted = false
    var animationProgress = 0f
    var animationPath = ""

    init {
        currentSession.currentWidgetInfosStream.subscribe(this::class.java) { widgetInfos: WidgetInfos? ->
            widgetObserver(widgetInfos)
        }
    }

    private fun widgetObserver(widgetInfos: WidgetInfos?) {
        if (widgetInfos != null) {
            val type = WidgetType.fromString(widgetInfos.type)
            if (type == WidgetType.IMAGE_PREDICTION ||
                type == WidgetType.IMAGE_PREDICTION_FOLLOW_UP ||
                type == WidgetType.TEXT_PREDICTION ||
                type == WidgetType.TEXT_PREDICTION_FOLLOW_UP
            ) {
                val resource = gson.fromJson(widgetInfos.payload.toString(), Resource::class.java) ?: null
                resource?.apply {
                    data.postValue(PredictionWidget(type, resource))
                }
                // Specify to Prediction widgetInfos here
            } else {
                cleanUp()
            }
        } else {
            cleanUp()
        }
    }

    // TODO: need to move the followup logic back to the widget observer instead of there
    fun startDismissTimout(timeout: String, isFollowup: Boolean) {
        if (!timeoutStarted && timeout.isNotEmpty()) {
            timeoutStarted = true
            Handler().removeCallbacks { confirmationState() }
            if (isFollowup) {
                Handler().postDelayed({ dismiss() }, AndroidResource.parseDuration(timeout))
                data.value?.apply {
                    followupState(resource.text_prediction_id, resource.correct_option_id)
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
                    val selectedOption = it.resource.getMergedOptions()?.get(selectedPosition)

                    // Prediction widget votes on dismiss
                    selectedOption?.getMergedVoteUrl()?.let { it1 -> dataClient.vote(it1) }

                    // Save widget id and voted option for followup widget
                    addWidgetPredictionVoted(it.resource.id, selectedOption?.id ?: "")
                }
            }
        }

        currentSession.currentWidgetInfosStream.onNext(null)
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
        currentSession.currentWidgetInfosStream.unsubscribe(this::class.java)
    }
}