package com.livelike.engagementsdk.widget.viewModel

import com.google.gson.JsonParseException
import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.LiveLikeWidget
import com.livelike.engagementsdk.OptionsItem
import com.livelike.engagementsdk.WidgetInfos
import com.livelike.engagementsdk.core.data.respository.UserRepository
import com.livelike.engagementsdk.core.services.network.RequestType
import com.livelike.engagementsdk.core.services.network.Result
import com.livelike.engagementsdk.core.utils.SubscriptionManager
import com.livelike.engagementsdk.core.utils.gson
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.widget.WidgetManager
import com.livelike.engagementsdk.widget.WidgetType
import com.livelike.engagementsdk.widget.data.models.NumberPredictionWidgetUserInteraction
import com.livelike.engagementsdk.widget.data.models.WidgetKind
import com.livelike.engagementsdk.widget.data.respository.WidgetInteractionRepository
import com.livelike.engagementsdk.widget.model.Resource
import com.livelike.engagementsdk.widget.widgetModel.NumberPredictionWidgetModel
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException


internal class NumberPredictionWidget(
    val type: WidgetType,
    val resource: Resource
)

internal class NumberPredictionViewModel(
    val widgetInfos: WidgetInfos,
    private val analyticsService: AnalyticsService,
    val onDismiss: () -> Unit,
    private val userRepository: UserRepository,
    val widgetMessagingClient: WidgetManager? = null,
    val widgetInteractionRepository: WidgetInteractionRepository?
) : BaseViewModel(analyticsService), NumberPredictionWidgetModel {

    /**
     *this is equal to size of list of options containing vote count to synced with server for each option
     *request is post to create the prediction vote, update will not be possible once vote is submitted
     **/
    var predictionStateList: MutableList<NumberPredictionState> = mutableListOf()

    val data: SubscriptionManager<NumberPredictionWidget> =
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
                resource.getMergedOptions()?.forEach { option ->
                    predictionStateList.add(
                        NumberPredictionState(
                            optionID = option.id,
                            predictionValue = 0
                        )
                    )
                }

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


    override fun lockInVote(options: List<OptionsItem>) {
        val jsonArray = JSONArray()
        val votesObj = JSONObject()

            //create the json array to be posted
            if (options.isNotEmpty()) {
                for (i in options.indices) {
                    data.currentData?.let { widget ->
                        val option =
                            widget.resource.getMergedOptions()?.find { it.id == options[i].id }
                        option?.number = options[i].number
                        option?.let{
                            try {
                                val jsonObject = JSONObject()
                                jsonObject.put("option_id", option.id)
                                jsonObject.put("number", option.number)
                                jsonArray.put(i, jsonObject)
                            } catch (e: JSONException) {
                                e.printStackTrace()
                            }
                            votesObj.put("votes", jsonArray)
                        }
                    }
                }
                submitVoteApi(votesObj)
            }
        }



    private fun submitVoteApi(votesObj: JSONObject) {
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


    override fun getUserInteraction(): NumberPredictionWidgetUserInteraction? {
        return widgetInteractionRepository?.getWidgetInteraction(
            widgetInfos.widgetId,
            WidgetKind.fromString(widgetInfos.type)
        )
    }

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

data class NumberPredictionState(
    var optionID: String,
    var predictionValue: Int
)