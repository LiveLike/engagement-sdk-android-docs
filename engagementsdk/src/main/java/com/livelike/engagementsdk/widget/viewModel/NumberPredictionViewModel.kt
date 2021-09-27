package com.livelike.engagementsdk.widget.viewModel

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.LiveLikeWidget
import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.WidgetInfos
import com.livelike.engagementsdk.core.data.models.NumberPredictionVotes
import com.livelike.engagementsdk.core.data.respository.UserRepository
import com.livelike.engagementsdk.core.services.network.RequestType
import com.livelike.engagementsdk.core.services.network.Result
import com.livelike.engagementsdk.core.utils.SubscriptionManager
import com.livelike.engagementsdk.core.utils.gson
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.formatIsoZoned8601
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.widget.WidgetManager
import com.livelike.engagementsdk.widget.WidgetType
import com.livelike.engagementsdk.widget.data.models.NumberPredictionWidgetUserInteraction
import com.livelike.engagementsdk.widget.data.models.WidgetKind
import com.livelike.engagementsdk.widget.data.respository.WidgetInteractionRepository
import com.livelike.engagementsdk.widget.model.Resource
import com.livelike.engagementsdk.widget.utils.livelikeSharedPrefs.addWidgetNumberPredictionVoted
import com.livelike.engagementsdk.widget.utils.livelikeSharedPrefs.getWidgetNumberPredictionVotedAnswerList
import com.livelike.engagementsdk.widget.widgetModel.NumberPredictionFollowUpWidgetModel
import com.livelike.engagementsdk.widget.widgetModel.NumberPredictionWidgetModel
import kotlinx.coroutines.launch
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.threeten.bp.ZonedDateTime
import java.io.IOException


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
) : BaseViewModel(analyticsService), NumberPredictionWidgetModel,
    NumberPredictionFollowUpWidgetModel {



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
     * submission of prediction votes (prediction for all options is mandatory)
     */
    override fun lockInVote(options: List<NumberPredictionVotes>) {
        if (options.isNullOrEmpty()) return
        data.currentData?.let { widget ->
            if (options.size < widget.resource.getMergedOptions()?.size!!) {
                logDebug { "submit prediction for all options" }
                return
            }
            val jsonArray = JsonArray()
            val votesObj = JsonObject()
            for (item in options) {
                jsonArray.add(
                    JsonObject().apply {
                        addProperty("option_id", item.optionId)
                        addProperty("number", item.number)
                    }
                )
                votesObj.add("votes", jsonArray)
            }
            submitVoteApi(votesObj)
            // save interaction locally
            saveInteraction(options)

            // Save widget id and voted options (number and option id) for followup widget
            addWidgetNumberPredictionVoted(widget.resource.id, options)
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
                    userRepository = userRepository,
                    widgetId = currentWidgetId
                )
            }
        }
    }

    /**
     * Returns the votes submitted
     */
    override fun getPredictionVotes(): List<NumberPredictionVotes>? {
        val resource = data.currentData?.resource
        return getWidgetNumberPredictionVotedAnswerList(if (resource?.textNumberPredictionId.isNullOrEmpty()) resource?.imageNumberPredictionId else resource?.textNumberPredictionId)
    }


    private fun getNumberPredictionId(it: NumberPredictionWidget): String {
        if (it.resource.textNumberPredictionId.isNullOrEmpty()) {
            return it.resource.imageNumberPredictionId
        }
        return it.resource.textNumberPredictionId
    }


    override fun claimRewards() {
        claimPredictionRewards()
    }

    /**
     * claim rewards
     */
    private fun claimPredictionRewards() {
        data.currentData?.let { resources ->
            val widgetId =
                if (resources.resource.textNumberPredictionId.isEmpty()) (resources.resource.imageNumberPredictionId) else (resources.resource.textNumberPredictionId)
            widgetInfos.widgetId = widgetId
            uiScope.launch {
                widgetInteractionRepository?.fetchRemoteInteractions(
                    widgetId = widgetInfos.widgetId,
                    widgetKind = widgetInfos.type
                )
                var claimToken = EngagementSDK.predictionWidgetVoteRepository.get(
                    (getNumberPredictionId(resources)?:""))
                if(claimToken.isNullOrEmpty()) claimToken = getUserInteraction()?.claimToken ?: ""
                resources.resource.claim_url?.let { url ->
                    dataClient.voteAsync(
                        url,
                        useVoteUrl = false,
                        body = FormBody.Builder()
                            .add("claim_token", claimToken).build(),
                        type = RequestType.POST,
                        accessToken = userRepository.userAccessToken,
                        userRepository = userRepository,
                        widgetId = currentWidgetId
                    )
                }
            }
        }
    }

    /**
     * Returns the last interaction
     */
    override fun getUserInteraction(): NumberPredictionWidgetUserInteraction? {
        return widgetInteractionRepository?.getWidgetInteraction(
            widgetInfos.widgetId,
            WidgetKind.fromString(widgetInfos.type)
        )
    }

    /**
     * loads interaction history
     */
    override fun loadInteractionHistory(liveLikeCallback: LiveLikeCallback<List<NumberPredictionWidgetUserInteraction>>) {
        uiScope.launch {
            try {
                val results =
                    widgetInteractionRepository?.fetchRemoteInteractions(
                        widgetId = widgetInfos.widgetId,
                        widgetKind = widgetInfos.type
                    )

                if (results is Result.Success) {
                    if (WidgetType.fromString(widgetInfos.type) == WidgetType.TEXT_NUMBER_PREDICTION) {
                        liveLikeCallback.onResponse(
                            results.data.interactions.textNumberPrediction, null
                        )
                    } else if (WidgetType.fromString(widgetInfos.type) == WidgetType.IMAGE_NUMBER_PREDICTION) {
                        liveLikeCallback.onResponse(
                            results.data.interactions.imageNumberPrediction, null
                        )
                    }
                } else if (results is Result.Error) {
                    liveLikeCallback.onResponse(
                        null, results.exception.message
                    )
                }
            } catch (e: JsonParseException) {
                e.printStackTrace()
                liveLikeCallback.onResponse(null, e.message)
            } catch (e: IOException) {
                e.printStackTrace()
                liveLikeCallback.onResponse(null, e.message)
            }
        }
    }

    override val widgetData: LiveLikeWidget
        get() = gson.fromJson(widgetInfos.payload, LiveLikeWidget::class.java)


    override fun finish() {
        onDismiss()
        cleanUp()
    }

    override fun markAsInteractive() {
        trackWidgetBecameInteractive(currentWidgetType, currentWidgetId, programId)
    }

    internal fun saveInteraction(option: List<NumberPredictionVotes>) {
        widgetInteractionRepository?.saveWidgetInteraction(
            NumberPredictionWidgetUserInteraction(
                "",
                "",
                ZonedDateTime.now().formatIsoZoned8601(),
                getUserInteraction()?.url,
                option,
                widgetInfos.widgetId,
                widgetInfos.type
            )
        )
    }

    override fun onClear() {
        cleanUp()
    }

    private fun cleanUp() {
        currentWidgetType = null
        currentWidgetId = ""
    }

}