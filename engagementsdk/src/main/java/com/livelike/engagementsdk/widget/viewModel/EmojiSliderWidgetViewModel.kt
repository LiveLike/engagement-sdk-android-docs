package com.livelike.engagementsdk.widget.viewModel

import com.livelike.engagementsdk.AnalyticsService
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
import com.livelike.engagementsdk.utils.gson
import com.livelike.engagementsdk.utils.toAnalyticsString
import com.livelike.engagementsdk.widget.WidgetManager
import com.livelike.engagementsdk.widget.WidgetType
import com.livelike.engagementsdk.widget.model.ImageSliderEntity
import kotlinx.coroutines.launch
import okhttp3.FormBody

internal class EmojiSliderWidgetViewModel(
    widgetInfos: WidgetInfos,
    analyticsService: AnalyticsService,
    sdkConfiguration: EngagementSDK.SdkConfiguration,
    onDismiss: () -> Unit,
    userRepository: UserRepository,
    programRepository: ProgramRepository,
    widgetMessagingClient: WidgetManager
) : WidgetViewModel<ImageSliderEntity>(widgetInfos, sdkConfiguration, userRepository, programRepository, widgetMessagingClient, onDismiss, analyticsService) {

    private val dataClient: WidgetDataClient = EngagementDataClientImpl()

    private var pubnub: MessagingClient? = null

    init {
        sdkConfiguration.pubNubKey.let {
//            pubnub = PubnubMessagingClient(it).asBehaviourSubject()
            pubnub = PubnubMessagingClient(it)
            pubnub?.addMessagingEventListener(object : MessagingEventListener {
                override fun onClientMessageEvent(client: MessagingClient, event: ClientMessage) {
                    val payload = event.message["payload"].asJsonObject
                    uiScope.launch {
                        results.onNext(gson.fromJson(payload.toString(), ImageSliderEntity::class.java) ?: null)
                    }
                }

                override fun onClientMessageError(client: MessagingClient, error: Error) {}
                override fun onClientMessageStatus(client: MessagingClient, status: ConnectionStatus) {}
            })
        }

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
                dataClient.voteAsync(it, "", userRepository?.userAccessToken, FormBody.Builder()
                    .add("magnitude", value).build())
            }
        }
    }

    private fun widgetObserver(widgetInfos: WidgetInfos) {
            val resource = gson.fromJson(widgetInfos.payload.toString(), ImageSliderEntity::class.java) ?: null
            resource?.apply {
                pubnub?.subscribe(listOf(resource.subscribe_channel))
                data.onNext(resource)
            }
            currentWidgetId = widgetInfos.widgetId
            currentWidgetType = WidgetType.fromString(widgetInfos.type)
            interactionData.widgetDisplayed()
    }

    override fun dismissWidget(action: DismissAction) {
        super.dismissWidget(action)
        currentWidgetType?.let {
            analyticsService.trackWidgetDismiss(
                it.toAnalyticsString(),
                currentWidgetId,
                interactionData,
                false,
                action
            )
        }
    }

    override fun onClear() {
        super.onClear()
        pubnub?.unsubscribeAll()
    }
}
