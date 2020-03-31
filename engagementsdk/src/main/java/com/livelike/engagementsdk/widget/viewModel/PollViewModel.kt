package com.livelike.engagementsdk.widget.viewModel

import android.os.Handler
import android.os.Looper
import android.support.v7.widget.RecyclerView
import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.AnalyticsWidgetInteractionInfo
import com.livelike.engagementsdk.AnalyticsWidgetSpecificInfo
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.WidgetInfos
import com.livelike.engagementsdk.data.models.ProgramGamificationProfile
import com.livelike.engagementsdk.data.models.RewardsType
import com.livelike.engagementsdk.data.repository.ProgramRepository
import com.livelike.engagementsdk.data.repository.UserRepository
import com.livelike.engagementsdk.domain.GamificationManager
import com.livelike.engagementsdk.services.messaging.ClientMessage
import com.livelike.engagementsdk.services.messaging.ConnectionStatus
import com.livelike.engagementsdk.services.messaging.Error
import com.livelike.engagementsdk.services.messaging.MessagingClient
import com.livelike.engagementsdk.services.messaging.MessagingEventListener
import com.livelike.engagementsdk.services.messaging.pubnub.PubnubMessagingClient
import com.livelike.engagementsdk.services.messaging.pubnub.PubnubMessagingClientReplay
import com.livelike.engagementsdk.services.messaging.pubnub.asBehaviourSubject
import com.livelike.engagementsdk.services.network.EngagementDataClientImpl
import com.livelike.engagementsdk.services.network.WidgetDataClient
import com.livelike.engagementsdk.utils.AndroidResource
import com.livelike.engagementsdk.utils.SubscriptionManager
import com.livelike.engagementsdk.utils.debounce
import com.livelike.engagementsdk.utils.gson
import com.livelike.engagementsdk.utils.logDebug
import com.livelike.engagementsdk.utils.toAnalyticsString
import com.livelike.engagementsdk.widget.WidgetManager
import com.livelike.engagementsdk.widget.WidgetType
import com.livelike.engagementsdk.widget.adapters.WidgetOptionsViewAdapter
import com.livelike.engagementsdk.widget.model.Resource
import com.livelike.engagementsdk.widget.view.addGamificationAnalyticsData
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class PollWidget(
    val type: WidgetType,
    val resource: Resource
)

internal class PollViewModel(
    widgetInfos: WidgetInfos,
    private val analyticsService: AnalyticsService,
    sdkConfiguration: EngagementSDK.SdkConfiguration,
    val onDismiss: () -> Unit,
    private val userRepository: UserRepository,
    private val programRepository: ProgramRepository,
    val widgetMessagingClient: WidgetManager
) : ViewModel() {
    lateinit var onWidgetInteractionCompleted: () -> Unit
    //    TODO remove points for all view models and make it follow dry, move it to gamification stream
    var points: SubscriptionManager<Int?> = SubscriptionManager(false)
    val gamificationProfile: Stream<ProgramGamificationProfile>
        get() = programRepository.programGamificationProfileStream
    val rewardsType: RewardsType
        get() = programRepository.rewardType
    val data: SubscriptionManager<PollWidget> = SubscriptionManager()
    val results: SubscriptionManager<Resource> = SubscriptionManager()
    val currentVoteId: SubscriptionManager<String?> = SubscriptionManager()
    private val debouncer = currentVoteId.debounce()
    private val dataClient: WidgetDataClient = EngagementDataClientImpl()

    var adapter: WidgetOptionsViewAdapter? = null
    var timeoutStarted = false
    var animationResultsProgress = 0f
    private var animationPath = ""
    var voteUrl: String? = null
    private var pubnub: PubnubMessagingClientReplay? = null
    var animationEggTimerProgress = 0f
    private var currentWidgetId: String = ""
    private var currentWidgetType: WidgetType? = null

    private val interactionData = AnalyticsWidgetInteractionInfo()
    private val widgetSpecificInfo = AnalyticsWidgetSpecificInfo()

    init {
        sdkConfiguration.pubNubKey.let {
            pubnub = PubnubMessagingClient.getInstance(it, userRepository.currentUserStream.latest()?.id)?.asBehaviourSubject()
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

        debouncer.subscribe(javaClass.simpleName) {
            if (it != null) vote()
        }

        widgetObserver(widgetInfos)
    }

    private fun vote() {
        if (adapter?.selectedPosition == RecyclerView.NO_POSITION) return // Nothing has been clicked

        uiScope.launch {
            adapter?.run {
                val url = myDataset[selectedPosition].getMergedVoteUrl()
                url?.let {
                    dataClient.voteAsync(
                        it,
                        myDataset[selectedPosition].id,
                        userRepository.userAccessToken
                    )
                }
            }
        }
    }

    private fun widgetObserver(widgetInfos: WidgetInfos?) {
        if (widgetInfos != null &&
            (WidgetType.fromString(widgetInfos.type) == WidgetType.TEXT_POLL ||
                    WidgetType.fromString(widgetInfos.type) == WidgetType.IMAGE_POLL)
        ) {
            val resource =
                gson.fromJson(widgetInfos.payload.toString(), Resource::class.java) ?: null
            resource?.apply {
                pubnub?.subscribe(listOf(resource.subscribe_channel))
                data.onNext(WidgetType.fromString(widgetInfos.type)?.let {
                    PollWidget(
                        it,
                        resource
                    )
                })
            }
            currentWidgetId = widgetInfos.widgetId
            currentWidgetType = WidgetType.fromString(widgetInfos.type)
            interactionData.widgetDisplayed()
        }
    }

    fun startDismissTimout(timeout: String) {
        if (!timeoutStarted && timeout.isNotEmpty()) {
            timeoutStarted = true
            uiScope.launch {
                delay(AndroidResource.parseDuration(timeout))
                confirmationState()
            }
        }
    }

    fun dismissWidget(action: DismissAction) {
        currentWidgetType?.let {
            analyticsService.trackWidgetDismiss(
                it.toAnalyticsString(),
                currentWidgetId,
                interactionData,
                adapter?.selectionLocked,
                action
            )
        }
        onDismiss()
        cleanUp()
    }

    private fun confirmationState() {
        if (adapter?.selectedPosition == RecyclerView.NO_POSITION) {
            // If the user never selected an option dismiss the widget with no confirmation
            dismissWidget(DismissAction.TIMEOUT)
            return
        }

        adapter?.selectionLocked = true
        onWidgetInteractionCompleted.invoke()

        uiScope.launch {
            data.currentData?.resource?.rewards_url?.let {
                userRepository.getGamificationReward(it, analyticsService)?.let { pts ->
                    programRepository.programGamificationProfileStream.onNext(pts)
                    publishPoints(pts.newPoints)
                    GamificationManager.checkForNewBadgeEarned(pts, widgetMessagingClient)
                    interactionData.addGamificationAnalyticsData(pts)
                }
            }

            currentWidgetType?.let {
                analyticsService.trackWidgetInteraction(
                    it.toAnalyticsString(),
                    currentWidgetId,
                    interactionData
                )
            }
            delay(3000)
            dismissWidget(DismissAction.TIMEOUT)
        }
    }

    private fun publishPoints(pts: Int) {
        this.points.onNext(pts)
    }

    private fun cleanUp() {
        vote() // Vote on dismiss
        pubnub?.unsubscribeAll()
        timeoutStarted = false
        adapter = null
        animationResultsProgress = 0f
        animationPath = ""
        voteUrl = null
        data.onNext(null)
        results.onNext(null)
        animationEggTimerProgress = 0f
        currentVoteId.onNext(null)

        interactionData.reset()
        widgetSpecificInfo.reset()
        currentWidgetId = ""
        currentWidgetType = null
        viewModelJob.cancel("Widget Cleanup")
    }

    var firstClick = true

    fun onOptionClicked() {
        if (firstClick) {
            firstClick = false
        }
        interactionData.incrementInteraction()
    }
}
