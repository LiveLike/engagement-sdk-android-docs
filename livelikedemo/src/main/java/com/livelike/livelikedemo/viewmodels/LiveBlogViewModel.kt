package com.livelike.livelikedemo.viewmodels

import androidx.lifecycle.AndroidViewModel
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.LiveLikeContentSession
import com.livelike.engagementsdk.core.services.messaging.proxies.WidgetInterceptor
import com.livelike.engagementsdk.widget.WidgetType
import com.livelike.engagementsdk.widget.timeline.WidgetTimeLineViewModel
import com.livelike.livelikedemo.LiveLikeApplication
import com.livelike.livelikedemo.channel.ChannelManager

class LiveBlogViewModel constructor(
    val application: LiveLikeApplication
) : AndroidViewModel(application) {

    var publicSession: LiveLikeContentSession? = null
    var timeLineViewModel: WidgetTimeLineViewModel? = null
    var showAlertOnly = false
        set(value) {
            field = value
            createTimeLineViewModel()
        }

    private var channelManager: ChannelManager? = null
    private var engagementSDK: EngagementSDK? = null
    private var contentSession: LiveLikeContentSession? = null

    init {
        channelManager = application.channelManager
        engagementSDK = application.sdk
        contentSession =
            createPublicSession(getChannelManager()?.selectedChannel?.llProgram.toString())
        createTimeLineViewModel()
    }

    private fun getSession(): LiveLikeContentSession? {
        return contentSession
    }

    private fun getChannelManager(): ChannelManager? {
        return channelManager
    }

    fun getEngagementSDK(): EngagementSDK? {
        return engagementSDK
    }

    /**
     * timeline view model created
     *
     **/
    private fun createTimeLineViewModel() {
        timeLineViewModel = WidgetTimeLineViewModel(getSession()!!) { widget ->
            if (showAlertOnly)
                widget.getWidgetType() == WidgetType.ALERT
            else true
        }
    }

    /**
     * session created
     **/
    private fun createPublicSession(
        sessionId: String,
        widgetInterceptor: WidgetInterceptor? = null,
    ): LiveLikeContentSession? {
        if (publicSession == null || publicSession?.contentSessionId() != sessionId) {
            publicSession?.close()
            publicSession =
                getEngagementSDK()?.createContentSession(sessionId)
        }
        publicSession!!.widgetInterceptor = widgetInterceptor
        return publicSession as LiveLikeContentSession
    }

    /**
     * widget timeline view model connection/scopes closed
     **/
    override fun onCleared() {
        timeLineViewModel?.clear()
        publicSession?.close()
        application.publicSession = null
        super.onCleared()
    }
}
