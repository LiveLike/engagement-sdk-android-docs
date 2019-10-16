package com.livelike.engagementsdk.widget.viewModel

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.AnalyticsWidgetInteractionInfo
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.WidgetInfos
import com.livelike.engagementsdk.data.repository.ProgramRepository
import com.livelike.engagementsdk.data.repository.UserRepository
import com.livelike.engagementsdk.services.messaging.ClientMessage
import com.livelike.engagementsdk.services.messaging.ConnectionStatus
import com.livelike.engagementsdk.services.messaging.Error
import com.livelike.engagementsdk.services.messaging.MessagingClient
import com.livelike.engagementsdk.services.messaging.MessagingEventListener
import com.livelike.engagementsdk.services.messaging.pubnub.PubnubMessagingClient
import com.livelike.engagementsdk.services.network.EngagementDataClientImpl
import com.livelike.engagementsdk.services.network.WidgetDataClient
import com.livelike.engagementsdk.utils.AndroidResource
import com.livelike.engagementsdk.utils.SubscriptionManager
import com.livelike.engagementsdk.utils.gson
import com.livelike.engagementsdk.utils.logDebug
import com.livelike.engagementsdk.utils.toAnalyticsString
import com.livelike.engagementsdk.widget.WidgetManager
import com.livelike.engagementsdk.widget.WidgetType
import com.livelike.engagementsdk.widget.model.Resource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


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
) : ViewModel() {

    var localVoteCount = 0
    private var debounceJob: Job? = null
    private var voteUrl: String? = null
    private val VOTE_THRASHHOLD = 10
    private var pubnub: PubnubMessagingClient? = null
    val results: SubscriptionManager<Resource> = SubscriptionManager()
    val data: SubscriptionManager<CheerMeterWidget> = SubscriptionManager()
    private var currentWidgetId: String = ""
    private var currentWidgetType: WidgetType? = null
    private val interactionData = AnalyticsWidgetInteractionInfo()
    var timeoutStarted = false
    var animationEggTimerProgress = 0f
    var animationProgress = 0f
    private val dataClient: WidgetDataClient = EngagementDataClientImpl()

    init {
        sdkConfiguration.pubNubKey.let {
            pubnub = PubnubMessagingClient(it)
            pubnub?.addMessagingEventListener(object : MessagingEventListener {
                override fun onClientMessageEvent(client: MessagingClient, event: ClientMessage) {
                    println("Client MESsage-> ")
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
                //api call
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

    private fun pushVoteData(voteCount: Int) {
        voteUrl?.let {
            uiScope.launch {
                if (voteCount > 0)
                    dataClient.voteAsync(it, voteCount, "", true)
            }
        }
    }


    private fun initVote(url: String) {
        println("INIT VOTE")
        uiScope.launch {
            voteUrl = dataClient.voteAsync(url, 0, "", false)
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

    fun startDismissTimout(timeout: String) {
        if (!timeoutStarted && timeout.isNotEmpty()) {
            timeoutStarted = true
            uiScope.launch {
                delay(AndroidResource.parseDuration(timeout))
//                confirmationState()
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
        onDismiss()
        cleanUp()
    }

    private fun cleanUp() {
        pubnub?.unsubscribeAll()
        timeoutStarted = false
//        animationResultsProgress = 0f
//        animationPath = ""
//        voteUrl = null
        data.onNext(null)
        results.onNext(null)
        animationEggTimerProgress = 0f
//        currentVoteId.onNext(null)

        interactionData.reset()
        currentWidgetId = ""
        currentWidgetType = null
        viewModelJob.cancel("Widget Cleanup")
    }


}