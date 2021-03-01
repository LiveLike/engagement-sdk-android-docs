package com.livelike.engagementsdk.widget.viewModel

import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.LiveLikeWidget
import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.WidgetInfos
import com.livelike.engagementsdk.core.data.respository.ProgramRepository
import com.livelike.engagementsdk.core.data.respository.UserRepository
import com.livelike.engagementsdk.core.utils.gson
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.core.utils.map
import com.livelike.engagementsdk.widget.WidgetManager
import com.livelike.engagementsdk.widget.WidgetType
import com.livelike.engagementsdk.widget.model.ImageSliderEntity
import com.livelike.engagementsdk.widget.model.LiveLikeWidgetResult
import com.livelike.engagementsdk.widget.utils.toAnalyticsString
import com.livelike.engagementsdk.widget.widgetModel.ImageSliderWidgetModel
import kotlinx.coroutines.launch
import okhttp3.FormBody

internal class EmojiSliderWidgetViewModel(
    widgetInfos: WidgetInfos,
    analyticsService: AnalyticsService,
    sdkConfiguration: EngagementSDK.SdkConfiguration,
    onDismiss: () -> Unit,
    userRepository: UserRepository,
    programRepository: ProgramRepository? = null,
    widgetMessagingClient: WidgetManager? = null
) : WidgetViewModel<ImageSliderEntity>(
    widgetInfos,
    sdkConfiguration,
    userRepository,
    programRepository,
    widgetMessagingClient,
    onDismiss,
    analyticsService
), ImageSliderWidgetModel {

    init {

        widgetObserver(widgetInfos)
    }

    override fun confirmInteraction() {
        currentVote.currentData?.let {
            vote(it)
        }
        super.confirmInteraction()
    }

    override fun vote(value: String) {
        uiScope.launch {
            data.latest()?.voteUrl?.let {
                dataClient.voteAsync(
                    it, "", userRepository?.userAccessToken, FormBody.Builder()
                        .add("magnitude", value).build(),
                    userRepository = userRepository
                )
            }
        }
    }

    private fun widgetObserver(widgetInfos: WidgetInfos) {
        val resource =
            gson.fromJson(widgetInfos.payload.toString(), ImageSliderEntity::class.java) ?: null
        resource?.apply {
            subscribeWidgetResults(resource.subscribe_channel,sdkConfiguration,userRepository.currentUserStream,widgetInfos.widgetId,results)
            data.onNext(resource)
            widgetState.onNext(WidgetStates.READY)
        }
        currentWidgetId = widgetInfos.widgetId
        programId = data.currentData?.program_id.toString()
        currentWidgetType = WidgetType.fromString(widgetInfos.type)
        interactionData.widgetDisplayed()
    }

    override fun dismissWidget(action: DismissAction) {
        super.dismissWidget(action)
        currentWidgetType?.let {
                analyticsService.trackWidgetDismiss(
                    it.toAnalyticsString(),
                    currentWidgetId,
                    programId,
                    interactionData,
                    false,
                    action
                )

            logDebug { "dismiss EmojiSlider Widget, reason:${action.name}" }
        }
    }


    override val widgetData: LiveLikeWidget
        get() = gson.fromJson(widgetInfos?.payload, LiveLikeWidget::class.java)

    override val voteResults: Stream<LiveLikeWidgetResult>
        get() = results.map { it.toLiveLikeWidgetResult() }


    override fun finish() {
        onDismiss()
        onClear()
    }

    override fun lockInVote(magnitude: Double) {
        data.latest()?.program_id?.let {
            trackWidgetEngagedAnalytics(currentWidgetType, currentWidgetId,
                it
            )
        }
        vote(magnitude.toString())
    }

    override fun onClear() {
        super.onClear()
        unsubscribeWidgetResults()
    }
}
