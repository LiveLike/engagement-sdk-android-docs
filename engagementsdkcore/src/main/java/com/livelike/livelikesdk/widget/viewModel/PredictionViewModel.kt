package com.livelike.livelikesdk.widget.viewModel

import android.content.Context
import android.support.v7.widget.RecyclerView
import com.livelike.engagementsdkapi.Stream
import com.livelike.engagementsdkapi.WidgetInfos
import com.livelike.livelikesdk.services.analytics.AnalyticsService
import com.livelike.livelikesdk.services.analytics.AnalyticsWidgetInteractionInfo
import com.livelike.livelikesdk.services.network.EngagementDataClientImpl
import com.livelike.livelikesdk.utils.AndroidResource
import com.livelike.livelikesdk.utils.SubscriptionManager
import com.livelike.livelikesdk.utils.gson
import com.livelike.livelikesdk.utils.liveLikeSharedPrefs.addWidgetPredictionVoted
import com.livelike.livelikesdk.utils.liveLikeSharedPrefs.getWidgetPredictionVotedAnswerIdOrEmpty
import com.livelike.livelikesdk.widget.DismissAction
import com.livelike.livelikesdk.widget.WidgetDataClient
import com.livelike.livelikesdk.widget.WidgetType
import com.livelike.livelikesdk.widget.adapters.WidgetOptionsViewAdapter
import com.livelike.livelikesdk.widget.model.Resource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class PredictionWidget(
    val type: WidgetType,
    val resource: Resource
)

internal class PredictionViewModel(widgetInfos: WidgetInfos, private val appContext: Context, private val analyticsService: AnalyticsService) : WidgetViewModel() {
    val data: SubscriptionManager<PredictionWidget?> = SubscriptionManager()
    private val dataClient: WidgetDataClient = EngagementDataClientImpl()
    var state: Stream<String?> = SubscriptionManager() // confirmation, followup

    var adapter: WidgetOptionsViewAdapter? = null
    var timeoutStarted = false
    var animationProgress = 0f
    var animationEggTimerProgress = 0f
    var animationPath = ""

    private var currentWidgetId: String = ""
    private var currentWidgetType: WidgetType? = null
    private val interactionData = AnalyticsWidgetInteractionInfo()

    init {
        widgetObserver(widgetInfos)
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
                    data.onNext(PredictionWidget(type, resource))
                }

                currentWidgetId = widgetInfos.widgetId
                currentWidgetType = type
                interactionData.widgetDisplayed()
            }
        } else {
            data.onNext(null)
        }
    }

    private val runnable = Runnable { }

    // TODO: need to move the followup logic back to the widget observer instead of there
    fun startDismissTimout(timeout: String, isFollowup: Boolean) {
        if (!timeoutStarted && timeout.isNotEmpty()) {
            timeoutStarted = true
            if (isFollowup) {
                uiScope.launch {
                    delay(AndroidResource.parseDuration(timeout))
                    dismissWidget(DismissAction.TIMEOUT)
                }
                data.currentData?.apply {
                    val selectedPredictionId = getWidgetPredictionVotedAnswerIdOrEmpty(if (resource.text_prediction_id.isEmpty()) resource.image_prediction_id else resource.text_prediction_id)
                    uiScope.launch {
                        delay(if (selectedPredictionId.isNotEmpty()) AndroidResource.parseDuration(timeout) else 0)
                        dismissWidget(DismissAction.TIMEOUT)
                    }
                    followupState(
                        selectedPredictionId,
                        resource.correct_option_id
                    )
                }
            } else {
                uiScope.launch {
                    delay(AndroidResource.parseDuration(timeout))
                    confirmationState()
                }
            }
        }
    }

    fun dismissWidget(action: DismissAction) {
        currentWidgetType?.let {
            analyticsService.trackWidgetDismiss(
                it,
                currentWidgetId,
                interactionData,
                adapter?.selectionLocked,
                action
            )
        }
        cleanUp()
    }

    fun onOptionClicked() {
        interactionData.incrementInteraction()
    }

    private fun followupState(selectedPredictionId: String, correctOptionId: String) {
        adapter?.correctOptionId = correctOptionId
        adapter?.userSelectedOptionId = selectedPredictionId
        adapter?.selectionLocked = true

        data.onNext(data.currentData?.apply {
            resource.getMergedOptions()?.forEach { opt ->
                opt.percentage = opt.getPercent(resource.getMergedTotal().toFloat())
            }
        })

        val isUserCorrect = adapter?.myDataset?.find { it.id == selectedPredictionId }?.is_correct ?: false
        val rootPath = if (isUserCorrect) "correctAnswer" else "wrongAnswer"
        animationPath = AndroidResource.selectRandomLottieAnimation(rootPath, appContext) ?: ""

        state.onNext("followup")
    }

    private fun confirmationState() {
        if (adapter?.selectedPosition == RecyclerView.NO_POSITION) {
            // If the user never selected an option dismiss the widget with no confirmation
            dismissWidget(DismissAction.TIMEOUT)
            return
        }

        adapter?.selectionLocked = true
        animationPath = AndroidResource.selectRandomLottieAnimation("confirmMessage", appContext) ?: ""

        state.onNext("confirmation")

        uiScope.launch {
            delay(6000)
            dismissWidget(DismissAction.TIMEOUT)
        }

        currentWidgetType?.let { analyticsService.trackWidgetInteraction(it, currentWidgetId, interactionData) }
    }

    private fun cleanUp() {
        // Vote for the selected option before starting the confirm animation
        data.currentData?.let {
            adapter?.apply {
                if (selectedPosition != RecyclerView.NO_POSITION) { // User has selected an option
                    val selectedOption = it.resource.getMergedOptions()?.get(selectedPosition)

                    uiScope.launch {
                        // Prediction widget votes on dismiss
                        selectedOption?.getMergedVoteUrl()?.let { url ->
                            dataClient.voteAsync(url, selectedOption.id)
                        }
                    }

                    // Save widget id and voted option for followup widget
                    addWidgetPredictionVoted(it.resource.id, selectedOption?.id ?: "")
                }
            }
        }
        timeoutStarted = false
        adapter = null
        animationProgress = 0f
        animationPath = ""
        state.onNext("")
        data.onNext(null)
        animationEggTimerProgress = 0f

        currentWidgetType = null
        currentWidgetId = ""
        interactionData.reset()
    }
}