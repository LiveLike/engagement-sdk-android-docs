package com.livelike.livelikedemo.mml

import android.content.Context
import android.support.v4.app.Fragment
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.LiveLikeContentSession
import com.livelike.engagementsdk.core.AccessTokenDelegate
import com.livelike.livelikedemo.mml.fragments.ChatFragment
import com.livelike.livelikedemo.mml.fragments.WidgetsFragment

class LiveLikeSDKHelper(
    private val applicationContext: Context,
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

    fun getSession(): LiveLikeContentSession {
        return session
    }

    fun getEngagementSDK(): EngagementSDK {
        return engagementSDK
    }

    fun getChatFragment(): Fragment {
        val chatFragment = ChatFragment()
        chatFragment.setSession(session.chatSession)
        return chatFragment
    }

    fun getWidgetsFragment(): Fragment {
        val widgetsFragment = WidgetsFragment()
        widgetsFragment.setSession(session)
        return widgetsFragment
    }
}