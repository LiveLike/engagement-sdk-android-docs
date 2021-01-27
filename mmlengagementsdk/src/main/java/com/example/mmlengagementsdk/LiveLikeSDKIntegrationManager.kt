package com.example.mmlengagementsdk

import android.content.Context
import android.support.v4.app.Fragment
import com.example.mmlengagementsdk.fragments.ChatFragment
import com.example.mmlengagementsdk.fragments.WidgetsFragment
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.core.AccessTokenDelegate
/**
 * LiveLike MMl-2021 entry point for integration
 * This class provides the fragment required to place in chat tab and interact tab as shown @link{https://www.figma.com/proto/Naz9dNAqgZm2xGoFI0pYiz/MML?scaling=min-zoom&node-id=139%3A81}
 **/
class LiveLikeSDKIntegrationManager(
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

    fun getChatFragment(): Fragment {
        val chatFragment = ChatFragment()
        chatFragment.setSession(session.chatSession)
        return chatFragment
    }

    fun getWidgetsFragment(): Fragment {
        return WidgetsFragment().apply {
            sdk = engagementSDK
            session = this@LiveLikeSDKIntegrationManager.session
        }
    }
}