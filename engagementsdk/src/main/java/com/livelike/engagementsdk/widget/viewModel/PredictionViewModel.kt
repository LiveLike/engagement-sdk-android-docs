package com.livelike.engagementsdk.widget.viewModel

import android.content.Context
import android.support.v7.widget.RecyclerView
import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.AnalyticsWidgetInteractionInfo
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.LiveLikeWidget
import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.WidgetInfos
import com.livelike.engagementsdk.core.data.models.RewardsType
import com.livelike.engagementsdk.core.data.respository.ProgramRepository
import com.livelike.engagementsdk.core.data.respository.UserRepository
import com.livelike.engagementsdk.core.services.network.RequestType
import com.livelike.engagementsdk.core.utils.AndroidResource
import com.livelike.engagementsdk.core.utils.SubscriptionManager
import com.livelike.engagementsdk.core.utils.gson
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.core.utils.map
import com.livelike.engagementsdk.widget.WidgetManager
import com.livelike.engagementsdk.widget.WidgetType
import com.livelike.engagementsdk.widget.WidgetViewThemeAttributes
import com.livelike.engagementsdk.widget.adapters.WidgetOptionsViewAdapter
import com.livelike.engagementsdk.widget.data.models.ProgramGamificationProfile
import com.livelike.engagementsdk.widget.domain.GamificationManager
import com.livelike.engagementsdk.widget.model.LiveLikeWidgetResult
import com.livelike.engagementsdk.widget.model.Resource
import com.livelike.engagementsdk.widget.utils.livelikeSharedPrefs.addWidgetPredictionVoted
import com.livelike.engagementsdk.widget.utils.livelikeSharedPrefs.getWidgetPredictionVotedAnswerIdOrEmpty
import com.livelike.engagementsdk.widget.utils.toAnalyticsString
import com.livelike.engagementsdk.widget.view.addGamificationAnalyticsData
import com.livelike.engagementsdk.widget.widgetModel.FollowUpWidgetViewModel
import com.livelike.engagementsdk.widget.widgetModel.PredictionWidgetViewModel
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.FormBody

internal class PredictionWidget(
    val type: WidgetType,
    val resource: Resource
)

internal class PredictionViewModel(
    val widgetInfos: WidgetInfos,
    private val appContext: Context,
    private val analyticsService: AnalyticsService,
    private  val sdkConfiguration: EngagementSDK.SdkConfiguration,
    val onDismiss: () -> Unit,
    private val userRepository: UserRepository,
    private val programRepository: ProgramRepository? = null,
    val widgetMessagingClient: WidgetManager? = null
) : BaseViewModel(analyticsService) , PredictionWidgetViewModel, FollowUpWidgetViewModel {
    var followUp: Boolean = false
    var points: Int? = null
    val gamificationProfile: Stream<ProgramGamificationProfile>
        get() = programRepository?.programGamificationProfileStream ?: SubscriptionManager()
    val rewardsType: RewardsType
        get() = programRepository?.rewardType ?: RewardsType.NONE
    val data: SubscriptionManager<PredictionWidget?> =
        SubscriptionManager()
    //    var state: Stream<String?> =
//        SubscriptionManager() // confirmation, followup
    var results: Stream<Resource> =
        SubscriptionManager()
    var adapter: WidgetOptionsViewAdapter? = null
    var timeoutStarted = false
    var animationProgress = 0f
    var animationEggTimerProgress = 0f
    var animationPath = ""

    private var currentWidgetId: String = ""
    private var currentWidgetType: WidgetType? = null
    private val interactionData = AnalyticsWidgetInteractionInfo()

    init {

        widgetObserver(widgetInfos)
    }

    private fun widgetObserver(widgetInfos: WidgetInfos?) {
        if (widgetInfos != null) {
            val type = WidgetType.fromString(widgetInfos.type)
            if (type == WidgetType.IMAGE_PREDICTION ||
                type == WidgetType.IMAGE_PREDICTION_FOLLOW_UP ||
                type == WidgetType.TEXT_PREDICTION ||
                type == WidgetType.TEXT_PREDICTION_FOLLOW_UP
            ) {
                val resource =
                    gson.fromJson(widgetInfos.payload.toString(), Resource::class.java) ?: null
                resource?.apply {
                    subscribeWidgetResults(resource.subscribe_channel,sdkConfiguration,userRepository.currentUserStream,widgetInfos.widgetId,results)
                    data.onNext(PredictionWidget(type, resource))
                }

                currentWidgetId = widgetInfos.widgetId
                currentWidgetType = type
                interactionData.widgetDisplayed()
            }
        } else {
            data.onNext(null)
        }
    }

    private val runnable = Runnable { }

    // TODO: need to move the followup logic back to the widget observer instead of there
    fun startDismissTimout(
        timeout: String,
        isFollowup: Boolean,
        widgetViewThemeAttributes: WidgetViewThemeAttributes
    ) {
        if (!timeoutStarted && timeout.isNotEmpty()) {
            timeoutStarted = true
            if (isFollowup) {
                uiScope.launch {
                    delay(AndroidResource.parseDuration(timeout))
                    dismissWidget(DismissAction.TIMEOUT)
                }
                data.currentData?.apply {
                    val selectedPredictionId =
                        getWidgetPredictionVotedAnswerIdOrEmpty(if (resource.text_prediction_id.isNullOrEmpty()) resource.image_prediction_id else resource.text_prediction_id)
                    uiScope.launch {
                        delay(
                            if (selectedPredictionId.isNotEmpty()) AndroidResource.parseDuration(
                                timeout
                            ) else 0
                        )
                        dismissWidget(DismissAction.TIMEOUT)
                    }
                }
            } else {
                uiScope.launch {
                    delay(AndroidResource.parseDuration(timeout))
                    confirmationState(widgetViewThemeAttributes)
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
                adapter?.selectionLocked,
                action
            )
        }
        widgetState.onNext(WidgetStates.FINISHED)
        logDebug { "dismiss Prediction Widget, reason:${action.name}" }
        onDismiss()
        cleanUp()
    }

    fun onOptionClicked() {
        uiScope.launch {
            vote()
        }
        interactionData.incrementInteraction()
    }

    internal fun followupState(
        selectedPredictionId: String,
        correctOptionId: String,
        widgetViewThemeAttributes: WidgetViewThemeAttributes
    ) {
        if (followUp)
            return
        followUp = true
        adapter?.correctOptionId = correctOptionId
        adapter?.userSelectedOptionId = selectedPredictionId
        adapter?.selectionLocked = true
        claimPredictionRewards()
        data.onNext(data.currentData?.apply {
            resource.getMergedOptions()?.forEach { opt ->
                opt.percentage = opt.getPercent(resource.getMergedTotal().toFloat())
            }
        })

        val isUserCorrect =
            adapter?.myDataset?.find { it.id == selectedPredictionId }?.is_correct ?: false
        val rootPath =
            if (isUserCorrect) widgetViewThemeAttributes.widgetWinAnimation else widgetViewThemeAttributes.widgetLoseAnimation
        animationPath = AndroidResource.selectRandomLottieAnimation(rootPath, appContext) ?: ""
        uiScope.launch {
            data.currentData?.resource?.rewards_url?.let {
                userRepository.getGamificationReward(it, analyticsService)?.let { pts ->
                    programRepository?.programGamificationProfileStream?.onNext(pts)
                    points = pts.newPoints
                    widgetMessagingClient?.let { widgetMessagingClient ->
                        GamificationManager.checkForNewBadgeEarned(pts, widgetMessagingClient)
                    }
                    interactionData.pointEarned = points ?: 0
                }
            }
//            state.onNext("followup")
            widgetState.onNext(WidgetStates.RESULTS)
        }
        logDebug { "Prediction Widget Follow Up isUserCorrect:$isUserCorrect" }
    }

    private fun confirmationState(widgetViewThemeAttributes: WidgetViewThemeAttributes) {
        if (adapter?.selectedPosition == RecyclerView.NO_POSITION) {
            // If the user never selected an option dismiss the widget with no confirmation
            dismissWidget(DismissAction.TIMEOUT)
            return
        }

        adapter?.selectionLocked = true
        widgetState.onNext(WidgetStates.RESULTS)
        logDebug { "Prediction Widget selected Position:${adapter?.selectedPosition}" }
        uiScope.launch {
            vote()
            data.currentData?.resource?.rewards_url?.let {
                userRepository.getGamificationReward(it, analyticsService)?.let { pts ->
                    programRepository?.programGamificationProfileStream?.onNext(pts)
                    points = pts.newPoints
                    widgetMessagingClient?.let { widgetMessagingClient ->
                        GamificationManager.checkForNewBadgeEarned(pts, widgetMessagingClient)
                    }
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

    private fun cleanUp() {
        unsubscribeWidgetResults()
        uiScope.cancel()
        timeoutStarted = false
        adapter = null
        animationProgress = 0f
        animationPath = ""
//        state.onNext("")
        data.onNext(null)
        animationEggTimerProgress = 0f
        currentWidgetType = null
        currentWidgetId = ""
        interactionData.reset()
    }

    private fun claimPredictionRewards(){
        data.currentData?.let {
            it.resource.claim_url?.let { url ->
                uiScope.launch {
                    dataClient.voteAsync(
                        url,
                        useVoteUrl = false,
                        body = FormBody.Builder()
                            .add("claim_token", EngagementSDK.predictionWidgetVoteRepository.get(
                                (getPredictionId(it)?:""))?:"").build(),
                        type = RequestType.POST,
                        accessToken = userRepository.userAccessToken,
                        userRepository = userRepository,
                        widgetId = currentWidgetId
                    )
                }
            }
        }
    }

    private fun getPredictionId(it: PredictionWidget) : String? {
        if(it.resource.text_prediction_id.isNullOrEmpty()){
            return it.resource.image_prediction_id
        }
        return it.resource.text_prediction_id
    }

    override val widgetData: LiveLikeWidget
        get() = gson.fromJson(widgetInfos.payload, LiveLikeWidget::class.java)

    override val voteResults: Stream<LiveLikeWidgetResult>
        get() = results.map { it.toLiveLikeWidgetResult() }

    override fun getPredictionVoteId(): String? {
        val resource = data.currentData?.resource
        return getWidgetPredictionVotedAnswerIdOrEmpty(if (resource?.text_prediction_id.isNullOrEmpty()) resource?.image_prediction_id else resource?.text_prediction_id)
    }

    override fun claimRewards() {
       claimPredictionRewards()
    }

    override fun finish() {
        onDismiss()
        cleanUp()
    }

    override fun lockInVote(optionID: String) {
        trackWidgetEngagedAnalytics(currentWidgetType, currentWidgetId)
        data.currentData?.let { widget ->
            val option = widget.resource.getMergedOptions()?.find { it.id == optionID }
            widget.resource.getMergedOptions()?.indexOf(option)?.let { position ->
                val url =  widget.resource.getMergedOptions()!![position].getMergedVoteUrl()
                url?.let {
                    voteApi(it, widget.resource.getMergedOptions()!![position].id, userRepository)
                }
            }
            // Save widget id and voted option for followup widget
            addWidgetPredictionVoted(widget.resource.id ?: "", option?.id ?: "")
        }
    }

    @Suppress("USELESS_ELVIS")
    private suspend fun vote() {
        data.currentData?.let {
            adapter?.apply {
                if (selectedPosition != RecyclerView.NO_POSITION) { // User has selected an option
                    val selectedOption = it.resource.getMergedOptions()?.get(selectedPosition)

                    // Prediction widget votes on dismiss
                    selectedOption?.getMergedVoteUrl()?.let { url ->
                        dataClient.voteAsync(
                            url,
                            selectedOption.id,
                            userRepository.userAccessToken,
                            userRepository = userRepository,
                            widgetId = currentWidgetId
                        )
                    }

                    // Save widget id and voted option for followup widget
                    addWidgetPredictionVoted(it.resource.id ?: "", selectedOption?.id ?: "")
                }
            }
        }
    }
}
