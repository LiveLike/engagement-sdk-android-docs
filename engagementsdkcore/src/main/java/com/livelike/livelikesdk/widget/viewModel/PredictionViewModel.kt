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
    var animationEggTimerProgress = 0f
    var animationPath = ""

    private val handler = Handler()

    init {
        currentSession.currentWidgetInfosStream.subscribe(this::class.java) { widgetInfos: WidgetInfos? ->
            widgetObserver(widgetInfos)
        }
    }

    private fun widgetObserver(widgetInfos: WidgetInfos?) {
        cleanUp()
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
            }
        } else {
            data.postValue(null)
        }
    }

    // TODO: need to move the followup logic back to the widget observer instead of there
    fun startDismissTimout(timeout: String, isFollowup: Boolean) {
        if (!timeoutStarted && timeout.isNotEmpty()) {
            timeoutStarted = true
            if (isFollowup) {
                handler.postDelayed({ dismiss() }, AndroidResource.parseDuration(timeout))
                data.value?.apply {
                    followupState(resource.text_prediction_id, resource.correct_option_id)
                }
            } else {
                handler.postDelayed({ confirmationState() }, AndroidResource.parseDuration(timeout))
            }
        }
    }

    private fun dismiss() {
        currentSession.currentWidgetInfosStream.onNext(null)
    }

    private fun followupState(textPredictionId: String, correctOptionId: String) {
        adapter?.correctOptionId = getWidgetPredictionVotedAnswerIdOrEmpty(textPredictionId)
        adapter?.userSelectedOptionId = correctOptionId
        adapter?.selectionLocked = true

        data.postValue(data.value?.apply {
            resource.getMergedOptions()?.forEach { opt ->
                opt.percentage = opt.getPercent(resource.getMergedTotal().toFloat())
            }
        })

        val rootPath = if (adapter?.userSelectedOptionId == adapter?.correctOptionId) "correctAnswer" else "wrongAnswer"
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

        handler.postDelayed({ dismiss() }, 6000)
    }

    private fun cleanUp() {
        // Vote for the selected option before starting the confirm animation
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
        handler.removeCallbacksAndMessages(null)
        timeoutStarted = false
        adapter = null
        animationProgress = 0f
        animationPath = ""
        state.postValue("")
        data.postValue(null)
        animationEggTimerProgress = 0f
    }

    override fun onCleared() {
        currentSession.currentWidgetInfosStream.unsubscribe(this::class.java)
    }
}