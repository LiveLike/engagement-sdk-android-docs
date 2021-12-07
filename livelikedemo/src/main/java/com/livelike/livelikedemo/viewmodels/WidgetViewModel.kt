import androidx.lifecycle.AndroidViewModel
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.LiveLikeContentSession
import com.livelike.engagementsdk.core.services.messaging.proxies.WidgetInterceptor
import com.livelike.livelikedemo.LiveLikeApplication
import com.livelike.livelikedemo.channel.ChannelManager

class WidgetViewModel constructor(
    application: LiveLikeApplication
) : AndroidViewModel(application) {

    var publicSession: LiveLikeContentSession? = null

    private val channelManager = application.channelManager

    private val engagementSDK = application.sdk

    private val contentSession =
        createPublicSession(getChannelManager().selectedChannel.llProgram.toString())

    fun getSession(): LiveLikeContentSession {
        println("widgetViewModel.getSession->$contentSession")
        return contentSession
    }

    private fun getChannelManager(): ChannelManager {
        return channelManager
    }

    fun getEngagementSDK(): EngagementSDK {
        return engagementSDK
    }

    fun pauseSession() {
        contentSession.pause()
    }

    fun resumeSession() {
        contentSession.resume()
    }

    fun closeSession() {
        contentSession.close()
    }

    fun createPublicSession(
        sessionId: String,
        widgetInterceptor: WidgetInterceptor? = null,
    ): LiveLikeContentSession {
        if (publicSession == null || publicSession?.contentSessionId() != sessionId) {
            publicSession?.close()
            publicSession =
                getEngagementSDK().createContentSession(sessionId)
        }
        publicSession!!.widgetInterceptor = widgetInterceptor
        return publicSession as LiveLikeContentSession
    }
}
