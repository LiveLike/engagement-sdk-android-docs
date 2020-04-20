package com.livelike.engagementsdk.widget.viewModel

import android.os.Handler
import android.os.Looper
import android.util.Log
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
import com.livelike.engagementsdk.widget.services.messaging.pubnub.PubnubMessagingClient
import com.livelike.engagementsdk.core.services.network.RequestType
import com.livelike.engagementsdk.core.utils.AndroidResource
import com.livelike.engagementsdk.core.utils.SubscriptionManager
import com.livelike.engagementsdk.core.utils.gson
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.widget.utils.toAnalyticsString
import com.livelike.engagementsdk.widget.WidgetManager
import com.livelike.engagementsdk.widget.WidgetType
import com.livelike.engagementsdk.widget.model.Resource
import com.livelike.engagementsdk.widget.services.network.WidgetDataClient
import com.livelike.engagementsdk.widget.services.network.WidgetDataClientImpl
import kotlinx.coroutines.CoroutineScope
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
    private val programRepository: ProgramRepository,
    val widgetMessagingClient: WidgetManager
) : BaseViewModel() {

    var teamSelected = 0
    var localVoteCount = 0
    var timer = 3
    private var debounceJob: Job? = null
    private var voteUrl: String? = null
    private val VOTE_THRASHHOLD = 10
    private var pubnub: PubnubMessagingClient? = null
    val results: SubscriptionManager<Resource> =
        SubscriptionManager()
    val voteEnd: SubscriptionManager<Boolean> =
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

    private var voteCount = 0

    fun sendVote(url: String) {
        if (voteUrl == null) {
            if (url.isNotEmpty())
                initVote(url)
            else
                Log.e("Error", "Unable to initiate voting : $url")
        } else {
            interactionData.incrementInteraction()
            localVoteCount++
            voteCount++
            if (voteCount > VOTE_THRASHHOLD) {
                // api call
                pushVoteData(voteCount)
                voteCount = 0
            } else {
                uiScope.launch {
                    if (debounceJob == null || debounceJob?.isCompleted != false) {
                        debounceJob = CoroutineScope(coroutineContext).launch {
                            delay(1000L)
                            pushVoteData(voteCount)
                            voteCount = 0
                        }
                    }
                }
            }
        }
    }

    fun pushVoteData(voteCount: Int) {
        voteUrl?.let {
            uiScope.launch {
                if (voteCount > 0)
                    dataClient.voteAsync(it, body = RequestBody.create(MediaType.parse("application/json"), "{\"vote_count\":$voteCount}"), type = RequestType.PATCH)
            }
        }
    }

    private fun initVote(url: String) {
        uiScope.launch {
            voteUrl = dataClient.voteAsync(url, body = RequestBody.create(MediaType.parse("application/json"), "{\"vote_count\":0}"), type = RequestType.POST)
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

    fun startDismissTimout(timeout: String, isVotingStarted: Boolean = false) {
        if (timeout.isNotEmpty()) {
            uiScope.launch {
                delay(AndroidResource.parseDuration(timeout))
                if (isVotingStarted) {
                    voteEnd.onNext(true)
                    voteEnd()
                } else {
                    if (teamSelected == 0) {
                        dismissWidget(DismissAction.TIMEOUT)
                    }
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
