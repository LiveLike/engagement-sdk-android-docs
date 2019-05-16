package com.livelike.livelikesdk.widget.viewModel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import android.os.Handler
import android.support.v7.widget.RecyclerView
import com.livelike.engagementsdkapi.WidgetInfos
import com.livelike.livelikesdk.LiveLikeSDK
import com.livelike.livelikesdk.LiveLikeSDK.Companion.currentSession
import com.livelike.livelikesdk.services.messaging.ClientMessage
import com.livelike.livelikesdk.services.messaging.ConnectionStatus
import com.livelike.livelikesdk.services.messaging.Error
import com.livelike.livelikesdk.services.messaging.MessagingClient
import com.livelike.livelikesdk.services.messaging.MessagingEventListener
import com.livelike.livelikesdk.services.messaging.pubnub.PubnubMessagingClient
import com.livelike.livelikesdk.services.network.LiveLikeDataClientImpl
import com.livelike.livelikesdk.utils.AndroidResource
import com.livelike.livelikesdk.utils.gson
import com.livelike.livelikesdk.utils.logDebug
import com.livelike.livelikesdk.widget.WidgetDataClient
import com.livelike.livelikesdk.widget.WidgetType
import com.livelike.livelikesdk.widget.adapters.WidgetOptionsViewAdapter
import com.livelike.livelikesdk.widget.model.Resource

internal class PollWidget(
    val type: WidgetType,
    val resource: Resource
)

internal class PollViewModel(application: Application) : AndroidViewModel(application) {
    val data: MutableLiveData<PollWidget> = MutableLiveData()
    val results: MutableLiveData<Resource> = MutableLiveData()

    var state: MutableLiveData<String> = MutableLiveData() // confirmation, followup
    private val dataClient: WidgetDataClient = LiveLikeDataClientImpl()

    var adapter: WidgetOptionsViewAdapter? = null
    var timeoutStarted = false
    var animationProgress = 0f
    private var animationPath = ""
    var voteUrl: String? = null
    private var pubnub: PubnubMessagingClient? = null

    init {
        LiveLikeSDK.configuration?.pubNubKey?.let {
            pubnub = PubnubMessagingClient(it)
            pubnub?.addMessagingEventListener(object : MessagingEventListener {
                override fun onClientMessageEvent(client: MessagingClient, event: ClientMessage) {
                    val widgetType = event.message.get("event").asString ?: ""
                    logDebug { "type is : $widgetType" }
                    val payload = event.message["payload"].asJsonObject
                    // TODO: need to debounce
                    results.postValue(gson.fromJson(payload.toString(), Resource::class.java) ?: null)
                }

                override fun onClientMessageError(client: MessagingClient, error: Error) {}
                override fun onClientMessageStatus(client: MessagingClient, status: ConnectionStatus) {}

            })
        }
        currentSession.currentWidgetInfosStream.subscribe(this::class.java) { widgetInfos: WidgetInfos? ->
            widgetObserver(widgetInfos)
        }

    }

    fun vote() {
        adapter?.showPercentage = true // TODO: need to be for all items
        // TODO: this needs to be debounced
        adapter?.apply {
            if (voteUrl == null) {
                myDataset[selectedPosition].getMergedVoteUrl()
                    ?.let { url -> dataClient.vote(url) { voteUrl = it } }
            } else {
                voteUrl?.apply {
                    myDataset[selectedPosition].getMergedVoteUrl()
                        ?.let { dataClient.changeVote(this, myDataset[selectedPosition].id) {} }
                }
            }
        }
    }

    private fun widgetObserver(widgetInfos: WidgetInfos?) {
        if (widgetInfos != null
            && (WidgetType.fromString(widgetInfos.type) == WidgetType.TEXT_POLL
                    || WidgetType.fromString(widgetInfos.type) == WidgetType.IMAGE_POLL)
        ) {
            val resource = gson.fromJson(widgetInfos.payload.toString(), Resource::class.java) ?: null
            resource?.apply {
                pubnub?.subscribe(listOf(resource.subscribe_channel))
                data.postValue(PollWidget(WidgetType.fromString(widgetInfos.type), resource))
            }
        } else {
            cleanUp()
        }
    }

    fun startDismissTimout(timeout: String) {
        if (!timeoutStarted && timeout.isNotEmpty()) {
            timeoutStarted = true
            Handler().removeCallbacks { confirmationState() }
            Handler().postDelayed({ confirmationState() }, AndroidResource.parseDuration(timeout))
        }
    }

    fun dismiss() {
        currentSession.currentWidgetInfosStream.onNext(null)
    }

    private fun confirmationState() {
        if (adapter?.selectedPosition == RecyclerView.NO_POSITION) {
            // If the user never selected an option dismiss the widget with no confirmation
            dismiss()
            return
        }

        adapter?.selectionLocked = true

        Handler().removeCallbacks { dismiss() }
        Handler().postDelayed({ dismiss() }, 6000)
    }

    private fun cleanUp() {
        pubnub?.unsubscribeAll()
        timeoutStarted = false
        adapter = null
        animationProgress = 0f
        animationPath = ""
        voteUrl = null
        state.postValue("")
        data.postValue(null)
        results.postValue(null)
    }

    override fun onCleared() {
        currentSession.currentWidgetInfosStream.unsubscribe(this::class.java)
    }
}