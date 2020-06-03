package com.livelike.engagementsdk.widget.viewModel

import android.os.Handler
import android.os.Looper
import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.AnalyticsWidgetInteractionInfo
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.WidgetInfos
import com.livelike.engagementsdk.core.data.respository.ProgramRepository
import com.livelike.engagementsdk.core.data.respository.UserRepository
import com.livelike.engagementsdk.core.services.messaging.ClientMessage
import com.livelike.engagementsdk.core.services.messaging.ConnectionStatus
import com.livelike.engagementsdk.core.services.messaging.Error
import com.livelike.engagementsdk.core.services.messaging.MessagingClient
import com.livelike.engagementsdk.core.services.messaging.MessagingEventListener
import com.livelike.engagementsdk.core.services.network.RequestType
import com.livelike.engagementsdk.core.utils.AndroidResource
import com.livelike.engagementsdk.core.utils.SubscriptionManager
import com.livelike.engagementsdk.core.utils.gson
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.widget.WidgetManager
import com.livelike.engagementsdk.widget.WidgetType
import com.livelike.engagementsdk.widget.model.Resource
import com.livelike.engagementsdk.widget.services.messaging.pubnub.PubnubMessagingClient
import com.livelike.engagementsdk.widget.services.network.WidgetDataClient
import com.livelike.engagementsdk.widget.services.network.WidgetDataClientImpl
import com.livelike.engagementsdk.widget.utils.toAnalyticsString
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
    widgetInfos: WidgetInfos,
    private val analyticsService: AnalyticsService,
    sdkConfiguration: EngagementSDK.SdkConfiguration,
    val onDismiss: () -> Unit,
    private val userRepository: UserRepository,
    private val programRepository: ProgramRepository? = null,
    val widgetMessagingClient: WidgetManager? = null
) : BaseViewModel() {

    var totalVoteCount = 0

    /**
     *this is equal to size of list of options containing vote count to synced with server for each option
     *first request is post to create the vote then after to update the count on that option, patch request will be used
     **/
     var voteStateList: MutableList<CheerMeterVoteState> = mutableListOf<CheerMeterVoteState>()

    private var pushVoteJob: Job? = null
    private var voteUrl: String? = null
    private val VOTE_THRASHHOLD = 10
    private var pubnub: PubnubMessagingClient? = null
    val results: SubscriptionManager<Resource> =
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
    private val dataClient: WidgetDataClient = WidgetDataClientImpl()

    init {
        sdkConfiguration.pubNubKey.let {
            pubnub = PubnubMessagingClient.getInstance(it, userRepository.currentUserStream.latest()?.id)
            pubnub?.addMessagingEventListener(object : MessagingEventListener {
                override fun onClientMessageEvent(client: MessagingClient, event: ClientMessage) {
                    val widgetType = event.message.get("event").asString ?: ""
                    logDebug { "type is : $widgetType" }
                    val payload = event.message["payload"].asJsonObject
                    Handler(Looper.getMainLooper()).post {
                        results.onNext(
                            gson.fromJson(payload.toString(), Resource::class.java) ?: null
                        )
                    }
                }

                override fun onClientMessageError(client: MessagingClient, error: Error) {}
                override fun onClientMessageStatus(
                    client: MessagingClient,
                    status: ConnectionStatus
                ) {
                }
            })
        }
        widgetObserver(widgetInfos)
    }

    fun incrementVoteCount(teamIndex : Int) {
        interactionData.incrementInteraction()
        totalVoteCount++
        voteStateList.getOrNull(teamIndex)?.let {
            it.voteCount++
        }
        wouldSendVote()
        println("vote state ${voteStateList.getOrNull(teamIndex).toString()}")
    }

    private fun wouldSendVote() {
        voteStateList.forEach {
            if (it.voteCount > VOTE_THRASHHOLD) {
                uiScope.launch { pushVoteStateData(it) }
            }
        }
        if(pushVoteJob == null || pushVoteJob?.isCompleted == false){
            pushVoteJob?.cancel()
            pushVoteJob = uiScope.launch {
                delay(1000L)
                voteStateList.forEach {
                    pushVoteStateData(it)
                }
            }
        }
    }

    private suspend fun pushVoteStateData(voteState : CheerMeterVoteState){
        if (voteState.voteCount > 0) {
            voteUrl = dataClient.voteAsync(voteState.voteUrl, body = RequestBody.create(MediaType.parse("application/json"), "{\"vote_count\":${voteState.voteCount}}"), accessToken =  userRepository.userAccessToken, type = voteState.requestType)
            voteUrl?.let {
                voteState.voteUrl = it
                voteState.requestType = RequestType.PATCH }
            voteState.voteCount = 0
            println("vote state request ${voteState.toString()}")
        }
    }

    fun voteEnd() {
        currentWidgetType?.let {
            analyticsService.trackWidgetInteraction(
                it.toAnalyticsString(),
                currentWidgetId,
                interactionData
            )
        }
    }

    private fun widgetObserver(widgetInfos: WidgetInfos?) {
        if (widgetInfos != null) {
            val resource =
                gson.fromJson(widgetInfos.payload.toString(), Resource::class.java) ?: null
            resource?.apply {

                resource.getMergedOptions()?.forEach { option ->
                    voteStateList.add(CheerMeterVoteState(0, option.vote_url ?: "", RequestType.POST))
                }

                pubnub?.subscribe(listOf(resource.subscribe_channel))
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
                analyticsService.trackWidgetInteraction(
                    it.toAnalyticsString(),
                    currentWidgetId,
                    interactionData
                )
            }
        }
    }

    fun startDismissTimout(timeout: String) {
        if (timeout.isNotEmpty()) {
            uiScope.launch {
                delay(AndroidResource.parseDuration(timeout))
                    if (totalVoteCount == 0) {
                        dismissWidget(DismissAction.TIMEOUT)
                    }else{
                        voteEnd.onNext(true)
                        voteEnd()
                }
            }
        }
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
        pubnub?.unsubscribeAll()
        voteUrl = null
        data.onNext(null)
        results.onNext(null)
        animationEggTimerProgress = 0f
        interactionData.reset()
        currentWidgetId = ""
        currentWidgetType = null
        viewModelJob.cancel("Widget Cleanup")
    }
}


data class CheerMeterVoteState(var voteCount: Int, var voteUrl: String, var requestType: RequestType) {
}
