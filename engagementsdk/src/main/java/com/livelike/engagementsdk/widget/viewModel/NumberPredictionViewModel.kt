package com.livelike.engagementsdk.widget.viewModel

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.LiveLikeWidget
import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.WidgetInfos
import com.livelike.engagementsdk.core.data.models.NumberPredictionVotes
import com.livelike.engagementsdk.core.data.respository.UserRepository
import com.livelike.engagementsdk.core.services.network.RequestType
import com.livelike.engagementsdk.core.utils.SubscriptionManager
import com.livelike.engagementsdk.core.utils.gson
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.widget.WidgetManager
import com.livelike.engagementsdk.widget.WidgetType
import com.livelike.engagementsdk.widget.data.models.NumberPredictionWidgetUserInteraction
import com.livelike.engagementsdk.widget.data.respository.WidgetInteractionRepository
import com.livelike.engagementsdk.widget.model.Resource
import com.livelike.engagementsdk.widget.utils.livelikeSharedPrefs.addWidgetNumberPredictionVoted
import com.livelike.engagementsdk.widget.widgetModel.NumberPredictionWidgetModel
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody


internal class NumberPredictionWidget(
    val type: WidgetType,
    val resource: Resource
)

internal class NumberPredictionViewModel(
    val widgetInfos: WidgetInfos,
    private val analyticsService: AnalyticsService,
    private val sdkConfiguration: EngagementSDK.SdkConfiguration,
    val onDismiss: () -> Unit,
    private val userRepository: UserRepository,
    val widgetMessagingClient: WidgetManager? = null,
    val widgetInteractionRepository: WidgetInteractionRepository?
) : BaseViewModel(analyticsService), NumberPredictionWidgetModel {


  //  var predictionStateList: MutableList<NumberPredictionState> = mutableListOf()

    val data: SubscriptionManager<NumberPredictionWidget> =
        SubscriptionManager()
    var results: Stream<Resource> =
        SubscriptionManager()
    private var currentWidgetId: String = ""
    private var programId: String = ""
    private var currentWidgetType: WidgetType? = null


    init {
        widgetObserver(widgetInfos)
    }


    private fun widgetObserver(widgetInfos: WidgetInfos?) {
        if (widgetInfos != null) {
            val resource =
                gson.fromJson(widgetInfos.payload.toString(), Resource::class.java) ?: null
            resource?.apply {
                subscribeWidgetResults(
                    resource.subscribe_channel,
                    sdkConfiguration,
                    userRepository.currentUserStream,
                    widgetInfos.widgetId,
                    results
                )
                data.onNext(
                    WidgetType.fromString(widgetInfos.type)?.let {
                        NumberPredictionWidget(
                            it,
                            resource
                        )
                    }
                )
            }
            currentWidgetId = widgetInfos.widgetId
            programId = data.latest()?.resource?.program_id.toString()
            currentWidgetType = WidgetType.fromString(widgetInfos.type)

        }
    }

    /**
     * creates the request for submission of prediction votes (prediction for all options is mandatory)
     */
    override fun lockInVote(options: List<NumberPredictionVotes>) {
        //if predicted score size does not match with option, fill in with default value 0
        data.currentData?.let { widget ->
            if (options.isNotEmpty()) {
                val voteList = options.toMutableList()
                if (voteList.size < widget.resource.getMergedOptions()?.size!!) {
                    for (i in widget.resource.getMergedOptions()!!.indices) {
                        val option =
                            voteList.find { it.optionId == widget.resource.getMergedOptions()!![i].id }
                        if (option == null) {
                            voteList.add(
                                NumberPredictionVotes(
                                    widget.resource.getMergedOptions()!![i].id,
                                    0
                                )
                            )
                        }
                    }
                }
                val jsonArray = JsonArray()
                val votesObj = JsonObject()

                for (item in voteList) {
                    jsonArray.add(
                        JsonObject().apply {
                            addProperty("option_id", item.optionId)
                            addProperty("number", item.number)
                        }
                    )
                    votesObj.add("votes", jsonArray)
                }
                submitVoteApi(votesObj)

                // Save widget id and voted options (value and option id) for followup widget
                addWidgetNumberPredictionVoted(widget.resource.id,voteList)
            }
        }
    }


    /**
     * call the vote api
     */
    private fun submitVoteApi(votesObj: JsonObject) {
        uiScope.launch {
            data.latest()?.resource?.vote_url?.let {
                dataClient.voteAsync(
                    it,
                    body = votesObj.toString()
                        .toRequestBody("application/json".toMediaTypeOrNull()),
                    accessToken = userRepository.userAccessToken,
                    type = RequestType.POST,
                    useVoteUrl = false,
                    userRepository = userRepository
                )
            }
        }
    }


    /**
     * get the last interaction
     */
    override fun getUserInteraction(): NumberPredictionWidgetUserInteraction? {
        return null
    }


    override fun loadInteractionHistory(liveLikeCallback: LiveLikeCallback<List<NumberPredictionWidgetUserInteraction>>) {

    }

    override val widgetData: LiveLikeWidget
        get() = gson.fromJson(widgetInfos.payload, LiveLikeWidget::class.java)


    override fun finish() {
        cleanUp()
    }

    override fun markAsInteractive() {

    }

    override fun onClear() {
        cleanUp()
    }

    private fun cleanUp() {
        currentWidgetType = null
        currentWidgetId = ""
    }

}