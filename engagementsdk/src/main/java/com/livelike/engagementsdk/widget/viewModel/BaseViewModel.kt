package com.livelike.engagementsdk.widget.viewModel

import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.core.data.respository.UserRepository
import com.livelike.engagementsdk.core.utils.SubscriptionManager
import com.livelike.engagementsdk.widget.WidgetType
import com.livelike.engagementsdk.widget.services.network.WidgetDataClient
import com.livelike.engagementsdk.widget.services.network.WidgetDataClientImpl
import com.livelike.engagementsdk.widget.utils.toAnalyticsString
import kotlinx.coroutines.launch

abstract class BaseViewModel(private val analyticsService: AnalyticsService) : ViewModel() {

    internal val widgetState: Stream<WidgetStates> =
        SubscriptionManager<WidgetStates>(emitOnSubscribe = true)
    internal var enableDefaultWidgetTransition = true
    internal val dataClient: WidgetDataClient = WidgetDataClientImpl()

    internal fun voteApi(
        url: String,
        id: String,
        userRepository: UserRepository
    ) {
        uiScope.launch {
            dataClient.voteAsync(
                url,
                id,
                userRepository.userAccessToken,
                userRepository = userRepository
            )
        }
    }

    fun trackWidgetEngagedAnalytics(currentWidgetType: WidgetType?, currentWidgetId: String): Unit {
        currentWidgetType?.let {
            analyticsService.trackWidgetEngaged(
                currentWidgetType.toAnalyticsString(),
                currentWidgetId
            )
        }
    }
}

enum class WidgetStates {
    READY,//the data has received and ready to use to inject into view
    INTERACTING,//the data is injected into view and shown
    RESULTS,// interaction completed and result to be shown
    FINISHED,//dismiss the widget
}