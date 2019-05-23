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
import debounce

internal class QuizWidget(
    val type: WidgetType,
    val resource: Resource
)

internal class QuizViewModel(application: Application) : AndroidViewModel(application) {
    val data: MutableLiveData<QuizWidget> = MutableLiveData()
    val results: MutableLiveData<Resource> = MutableLiveData()
    val currentVoteId: MutableLiveData<String?> = MutableLiveData()
    private val debouncer = currentVoteId.debounce()
    private val dataClient: WidgetDataClient = LiveLikeDataClientImpl()
    var state: MutableLiveData<String> = MutableLiveData() // results

    var adapter: WidgetOptionsViewAdapter? = null
    var timeoutStarted = false
    var animationProgress = 0f
    internal var animationPath = ""
    var voteUrl: String? = null
    private var pubnub: PubnubMessagingClient? = null
    private val handler = Handler()
    var animationEggTimerProgress = 0f

    init {
        LiveLikeSDK.configuration?.pubNubKey?.let {
            pubnub = PubnubMessagingClient(it)
            pubnub?.addMessagingEventListener(object : MessagingEventListener {
                override fun onClientMessageEvent(client: MessagingClient, event: ClientMessage) {
                    val widgetType = event.message.get("event").asString ?: ""
                    logDebug { "type is : $widgetType" }
                    val payload = event.message["payload"].asJsonObject
                    results.postValue(gson.fromJson(payload.toString(), Resource::class.java) ?: null)
                }

                override fun onClientMessageError(client: MessagingClient, error: Error) {}
                override fun onClientMessageStatus(client: MessagingClient, status: ConnectionStatus) {}
            })
        }
        currentSession.currentWidgetInfosStream.subscribe(this::class.java, this::widgetObserver)

        debouncer.observeForever {
            if (it != null) vote()
        }
    }

    private fun vote() {
        if (adapter?.selectedPosition == RecyclerView.NO_POSITION) return // Nothing has been clicked
        adapter?.apply {
            if (voteUrl == null) {
                myDataset[selectedPosition].getMergedVoteUrl()
                    ?.let { url -> dataClient.vote(url) { voteUrl = it } }
            } else {
                voteUrl?.apply {
                    dataClient.changeVote(this, myDataset[selectedPosition].id) {}
                }
            }
        }
        adapter?.showPercentage = true
        adapter?.notifyDataSetChanged()
    }

    private fun widgetObserver(widgetInfos: WidgetInfos?) {
        if (widgetInfos != null &&
            (WidgetType.fromString(widgetInfos.type) == WidgetType.IMAGE_QUIZ ||
                    WidgetType.fromString(widgetInfos.type) == WidgetType.TEXT_QUIZ)
        ) {
            val resource = gson.fromJson(widgetInfos.payload.toString(), Resource::class.java) ?: null
            resource?.apply {
                pubnub?.subscribe(listOf(resource.subscribe_channel))
                data.postValue(QuizWidget(WidgetType.fromString(widgetInfos.type), resource))
            }
        } else {
            cleanUp()
        }
    }

    fun startDismissTimout(timeout: String) {
        if (!timeoutStarted && timeout.isNotEmpty()) {
            timeoutStarted = true
            handler.postDelayed({ resultsState() }, AndroidResource.parseDuration(timeout))
        }
    }

    fun dismiss() {
        currentSession.currentWidgetInfosStream.onNext(null)
    }

    private fun resultsState() {
        if (adapter?.selectedPosition == RecyclerView.NO_POSITION) {
            // If the user never selected an option dismiss the widget with no confirmation
            dismiss()
            return
        }

        val isUserCorrect = adapter?.selectedPosition?.let { adapter?.myDataset?.get(it)?.is_correct } ?: false
        val rootPath = if (isUserCorrect) "correctAnswer" else "wrongAnswer"

        animationPath = AndroidResource.selectRandomLottieAnimation(rootPath, getApplication()) ?: ""
        adapter?.selectionLocked = true

        state.postValue("results")
        handler.postDelayed({ dismiss() }, 6000)
    }

    private fun cleanUp() {
        vote() // Vote on dismiss
        handler.removeCallbacksAndMessages(null)
        pubnub?.unsubscribeAll()
        timeoutStarted = false
        adapter = null
        animationProgress = 0f
        animationPath = ""
        voteUrl = null
        data.postValue(null)
        results.postValue(null)
        state.postValue(null)
        animationEggTimerProgress = 0f
    }

    override fun onCleared() {
        currentSession.currentWidgetInfosStream.unsubscribe(this::class.java)
    }

    // This is to update the vote value locally
    private var previousOptionClickedId: String? = null

    fun onOptionClicked(it: String?) {
        if (previousOptionClickedId == null) {
            vote() // Vote on first click
        }
        if (it != previousOptionClickedId) {
            data.value?.apply {
                val options = resource.getMergedOptions() ?: return
                options.forEach { opt ->
                    opt.apply {
                        if (opt.id == it) {
                            opt.answer_count = opt.answer_count?.plus(1) ?: 0
                        } else if (previousOptionClickedId == opt.id) {
                            opt.answer_count = opt.answer_count?.minus(1) ?: 0
                        }
                    }
                }
                options.forEach { opt ->
                    opt.apply {
                        opt.percentage = opt.getPercent((resource.getMergedTotal()).toFloat())
                    }
                }
                adapter?.myDataset = options
                adapter?.showPercentage = true
                adapter?.notifyDataSetChanged()
                previousOptionClickedId = it
            }
        }
    }
}