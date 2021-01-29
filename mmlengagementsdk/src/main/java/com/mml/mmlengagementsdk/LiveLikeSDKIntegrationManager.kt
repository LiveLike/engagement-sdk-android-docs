package com.mml.mmlengagementsdk

import android.content.Context
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.core.AccessTokenDelegate
import com.mml.mmlengagementsdk.chat.MMLChatView
import com.mml.mmlengagementsdk.widgets.timeline.WidgetsTimeLineView

/**
 * LiveLike MMl-2021 entry point for integration
 * This class provides the views required to place in chat tab and interact tab as shown @link{https://www.figma.com/proto/Naz9dNAqgZm2xGoFI0pYiz/MML?scaling=min-zoom&node-id=139%3A81}
 **/
class LiveLikeSDKIntegrationManager(
    val applicationContext: Context,
    private val clientId: String,
    private val programId: String
) {
    private val sharedPreference =
        applicationContext.getSharedPreferences("MMLSharedPrefs", Context.MODE_PRIVATE)
    private val engagementSDK =
        EngagementSDK(clientId, applicationContext, accessTokenDelegate = object :
            AccessTokenDelegate {
            override fun getAccessToken(): String? {
                return sharedPreference.getString("accessToken", null)
            }

            override fun storeAccessToken(accessToken: String?) {
                sharedPreference.edit().putString("accessToken", accessToken).apply()
            }
        })
    private val session = engagementSDK.createContentSession(programId)


    fun getChatView(context: Context): MMLChatView {
        return MMLChatView(context).apply {
            this.chatSession = session.chatSession
        }
    }

    fun getWidgetsView(context: Context): WidgetsTimeLineView {
        return WidgetsTimeLineView(context, session, engagementSDK)
    }

    /** Pause the current Chat and widget sessions. This generally happens when ads are presented */
    fun pause(){
        session.pause()
    }

    /** Resume the current Chat and widget sessions. This generally happens when ads are completed */
    fun resume(){
        session.resume()
    }
    /** Closes the current session. Basically, it should be called to cleanup/teardown resources used*/
    fun destroy(){
        session.close()
    }


}