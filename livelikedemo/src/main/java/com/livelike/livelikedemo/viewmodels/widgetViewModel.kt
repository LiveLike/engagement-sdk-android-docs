

import android.arch.lifecycle.AndroidViewModel

import com.livelike.engagementsdk.LiveLikeContentSession

import com.livelike.livelikedemo.LiveLikeApplication

public class widgetViewModel constructor(
    application: LiveLikeApplication
) : AndroidViewModel(application) {

    val engagementSDK = application.sdk

    val contentSession = application.publicSession

    fun getSession(): LiveLikeContentSession? {
        return contentSession
    }

    fun pauseSession() {
        contentSession?.pause()
    }

    fun resumeSession() {
        contentSession?.resume()
    }

    fun closeSession() {
        contentSession?.close()
    }
}
