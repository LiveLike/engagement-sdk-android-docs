package com.livelike.engagementsdk.widget.viewModel

import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.AnalyticsWidgetInteractionInfo
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.LiveLikeWidget
import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.WidgetInfos
import com.livelike.engagementsdk.core.data.respository.ProgramRepository
import com.livelike.engagementsdk.core.data.respository.UserRepository
import com.livelike.engagementsdk.core.services.network.RequestType
import com.livelike.engagementsdk.core.utils.AndroidResource
import com.livelike.engagementsdk.core.utils.SubscriptionManager
import com.livelike.engagementsdk.core.utils.gson
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.core.utils.map
import com.livelike.engagementsdk.widget.WidgetManager
import com.livelike.engagementsdk.widget.WidgetType
import com.livelike.engagementsdk.widget.model.LiveLikeWidgetResult
import com.livelike.engagementsdk.widget.model.Resource
import com.livelike.engagementsdk.widget.utils.toAnalyticsString
import com.livelike.engagementsdk.widget.widgetModel.CheerMeterWidgetmodel
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.RequestBody

internal class CheerMeterWidget(
    val type: WidgetType,
    val resource: Resource
)

internal class CheerMeterViewModel(
    val widgetInfos: WidgetInfos,
    private val analyticsService: AnalyticsService,
    private val sdkConfiguration: EngagementSDK.SdkConfiguration,
    val onDismiss: () -> Unit,
    private val userRepository: UserRepository,
    private val programRepository: ProgramRepository? = null,
    val widgetMessagingClient: WidgetManager? = null
) : BaseViewModel(analyticsService), CheerMeterWidgetmodel {

    var totalVoteCount = 0

    /**
     *this is equal to size of list of options containing vote count to synced with server for each option
     *first request is post to create the vote then after to update the count on that option, patch request will be used
     **/
    var voteStateList: MutableList<CheerMeterVoteState> = mutableListOf<CheerMeterVoteState>()

    private var pushVoteJob: Job? = null
    val results: Stream<Resource> =
        SubscriptionManager()
    val
            voteEnd: SubscriptionManager<Boolean> =
        SubscriptionManager()
    val data: SubscriptionManager<CheerMeterWidget> =
        SubscriptionManager()
    private var currentWidgetId: String = ""
    private var currentWidgetType: WidgetType? = null
    private val interactionData = AnalyticsWidgetInteractionInfo()
    var animationEggTimerProgress = 0f
    var animationProgress = 0f

    init {

        widgetObserver(widgetInfos)
    }

    fun incrementVoteCount(teamIndex: Int) {
        interactionData.incrementInteraction()
        totalVoteCount++
        voteStateList.getOrNull(teamIndex)?.let {
            it.voteCount++
        }
        wouldSendVote()
    }

    private fun wouldSendVote() {
        if (pushVoteJob == null || pushVoteJob?.isCompleted == false) {
            pushVoteJob?.cancel()
            pushVoteJob = uiScope.launch {
                delay(1000L)
                voteStateList.forEach {
                    pushVoteStateData(it)
                }
                pushVoteJob = null
            }
        }
    }

    private suspend fun pushVoteStateData(voteState: CheerMeterVoteState) {
        if (voteState.voteCount > 0) {
            val count = voteState.voteCount
            val voteUrl = dataClient.voteAsync(
                voteState.voteUrl,
                body = RequestBody.create(
                    MediaType.parse("application/json"),
                    "{\"vote_count\":${voteState.voteCount}}"
                ),
                accessToken = userRepository.userAccessToken,
                type = voteState.requestType,
                useVoteUrl = false,
                userRepository = userRepository
            )
            voteUrl?.let {
                voteState.voteUrl = it
                voteState.requestType = RequestType.PATCH
            }
            if (count < voteState.voteCount)
                voteState.voteCount = voteState.voteCount - count
            else
                voteState.voteCount = 0
        }
    }

    fun voteEnd() {
        currentWidgetType?.let {
            data.latest()?.resource?.program_id?.let { programId ->
                analyticsService.trackWidgetInteraction(
                    it.toAnalyticsString(),
                    currentWidgetId,
                    programId,
                    interactionData
                )
            }
        }
    }

    private fun widgetObserver(widgetInfos: WidgetInfos?) {
        if (widgetInfos != null) {
            val resource =
                gson.fromJson(widgetInfos.payload.toString(), Resource::class.java) ?: null
            resource?.apply {

                resource.getMergedOptions()?.forEach { option ->
                    voteStateList.add(
                        CheerMeterVoteState(
                            0,
                            option.vote_url ?: "",
                            RequestType.POST
                        )
                    )
                }
                subscribeWidgetResults(resource.subscribe_channel,sdkConfiguration,userRepository.currentUserStream,widgetInfos.widgetId,results)
                data.onNext(WidgetType.fromString(widgetInfos.type)?.let {
                    CheerMeterWidget(
                        it,
                        resource
                    )
                })
            }
            currentWidgetId = widgetInfos.widgetId
            currentWidgetType = WidgetType.fromString(widgetInfos.type)
            interactionData.widgetDisplayed()

            currentWidgetType?.let {
                data.latest()?.resource?.program_id?.let { programId ->
                    analyticsService.trackWidgetInteraction(
                        it.toAnalyticsString(),
                        currentWidgetId,
                        programId,
                        interactionData
                    )
                }
            }
        }
    }

    fun startDismissTimout(timeout: String) {
        if (timeout.isNotEmpty()) {
            uiScope.launch {
                delay(AndroidResource.parseDuration(timeout))
                if (totalVoteCount == 0) {
                    dismissWidget(DismissAction.TIMEOUT)
                } else {
                    widgetState.onNext(WidgetStates.RESULTS)
                }
            }
        }
    }

    override fun finish() {
        onDismiss()
        cleanUp()
    }

    fun dismissWidget(action: DismissAction) {
        currentWidgetType?.let {
            analyticsService.trackWidgetDismiss(
                it.toAnalyticsString(),
                currentWidgetId,
                interactionData,
                false,
                action
            )
        }
        logDebug { "dismiss Alert Widget, reason:${action.name}" }
        onDismiss()
        cleanUp()
    }

    private fun cleanUp() {
        unsubscribeWidgetResults()
        data.onNext(null)
        results.onNext(null)
        animationEggTimerProgress = 0f
        interactionData.reset()
        currentWidgetId = ""
        currentWidgetType = null
        viewModelJob.cancel("Widget Cleanup")
    }

    override val widgetData: LiveLikeWidget
        get() = gson.fromJson(widgetInfos.payload, LiveLikeWidget::class.java)


    override val voteResults: Stream<LiveLikeWidgetResult>
        get() = results.map { it.toLiveLikeWidgetResult() }


    override fun submitVote(optionID: String) {
        trackWidgetEngagedAnalytics(currentWidgetType, currentWidgetId)
        data.currentData?.let { widget ->
            val option = widget.resource.getMergedOptions()?.find { it.id == optionID }
            widget.resource.getMergedOptions()?.indexOf(option)?.let {
                incrementVoteCount(it)
            }
        }
    }


}

data class CheerMeterVoteState(
    var voteCount: Int,
    var voteUrl: String,
    var requestType: RequestType
)
