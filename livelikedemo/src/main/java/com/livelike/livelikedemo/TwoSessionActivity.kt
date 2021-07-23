package com.livelike.livelikedemo

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.LiveLikeContentSession
import com.livelike.engagementsdk.core.AccessTokenDelegate
import com.livelike.engagementsdk.publicapis.ErrorDelegate
import com.livelike.livelikedemo.channel.ChannelManager
import kotlinx.android.synthetic.main.two_sessions_activity.chat_view
import kotlinx.android.synthetic.main.two_sessions_activity.widget_view

class TwoSessionActivity : Activity() {

    private lateinit var engagementSDK: EngagementSDK
    private lateinit var channelManager: ChannelManager
    private lateinit var session: LiveLikeContentSession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.two_sessions_activity)
        val sharedPref = getSharedPreferences("app", Context.MODE_PRIVATE)
        channelManager = (application as LiveLikeApplication).channelManager

        engagementSDK = EngagementSDK(
            BuildConfig.APP_CLIENT_ID, applicationContext,
            object : ErrorDelegate() {
                override fun onError(error: String) {
                }
            },
            accessTokenDelegate = object : AccessTokenDelegate {
                override fun getAccessToken(): String? {
                    return sharedPref.getString("access", null)
                }

                override fun storeAccessToken(accessToken: String?) {
                    sharedPref.edit().putString("access", accessToken).apply()

                    session =
                        engagementSDK.createContentSession(channelManager.selectedChannel.llProgram.toString())
                    widget_view.setSession(session)

                    chat_view.setSession(session.chatSession)
                }
            }
        )
    }
}
