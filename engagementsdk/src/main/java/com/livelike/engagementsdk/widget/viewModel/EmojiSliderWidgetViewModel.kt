package com.livelike.engagementsdk.widget.viewModel

import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.LiveLikeWidget
import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.WidgetInfos
import com.livelike.engagementsdk.core.data.respository.ProgramRepository
import com.livelike.engagementsdk.core.data.respository.UserRepository
import com.livelike.engagementsdk.core.services.messaging.ClientMessage
import com.livelike.engagementsdk.core.services.messaging.ConnectionStatus
import com.livelike.engagementsdk.core.services.messaging.Error
import com.livelike.engagementsdk.core.services.messaging.MessagingClient
import com.livelike.engagementsdk.core.services.messaging.MessagingEventListener
import com.livelike.engagementsdk.core.utils.gson
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.core.utils.map
import com.livelike.engagementsdk.widget.WidgetManager
import com.livelike.engagementsdk.widget.WidgetType
import com.livelike.engagementsdk.widget.model.ImageSliderEntity
import com.livelike.engagementsdk.widget.model.LiveLikeWidgetResult
import com.livelike.engagementsdk.widget.services.messaging.pubnub.PubnubMessagingClient
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
    private var pubnub: MessagingClient? = null

    init {
        sdkConfiguration.pubNubKey.let {
            pubnub =
                PubnubMessagingClient.getInstance(it, userRepository.currentUserStream.latest()?.id)
            pubnub?.addMessagingEventListener(object : MessagingEventListener {
                override fun onClientMessageEvent(client: MessagingClient, event: ClientMessage) {
                    val payload = event.message["payload"].asJsonObject
                    uiScope.launch {
                        val data =
                            gson.fromJson(payload.toString(), ImageSliderEntity::class.java) ?: null
                        results.onNext(data)
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
            pubnub?.subscribe(listOf(resource.subscribe_channel))
            data.onNext(resource)
            widgetState.onNext(WidgetStates.READY)
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
        vote(magnitude.toString())
    }

    override fun onClear() {
        super.onClear()
        pubnub?.unsubscribeAll()
    }
}
