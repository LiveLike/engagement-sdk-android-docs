package com.livelike.livelikedemo

import android.content.Context
import android.os.Bundle
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.LiveLikeContentSession
import com.livelike.engagementsdk.core.AccessTokenDelegate
import com.livelike.engagementsdk.publicapis.ErrorDelegate
import com.livelike.livelikedemo.channel.ChannelManager
import kotlinx.android.synthetic.main.activity_sample_app.chat_view
import kotlinx.android.synthetic.main.activity_sample_app.widget_view

class SampleAppActivity : AppCompatActivity() {

    private var session: LiveLikeContentSession? = null
    private lateinit var channelManager: ChannelManager
    private var engagementSDK: EngagementSDK? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Default)
        setContentView(R.layout.activity_sample_app)
        initSDK()
    }

    private fun initSDK() {
        channelManager = (application as LiveLikeApplication).channelManager
        engagementSDK =
            EngagementSDK(BuildConfig.APP_CLIENT_ID, applicationContext, object : ErrorDelegate() {
                override fun onError(error: String) {
                    android.os.Handler(Looper.getMainLooper()).postDelayed({
                        initSDK()
                    }, 1000)
                }
            }, accessTokenDelegate = object : AccessTokenDelegate {
                override fun getAccessToken(): String? {
                    return getSharedPreferences(PREFERENCES_APP_ID, Context.MODE_PRIVATE).getString(
                        PREF_USER_ACCESS_TOKEN_1,
                        null
                    ).apply {
                        println("Token:$this")
                    }
                }

                override fun storeAccessToken(accessToken: String?) {
                    getSharedPreferences(PREFERENCES_APP_ID, Context.MODE_PRIVATE).edit().putString(
                        PREF_USER_ACCESS_TOKEN_1, accessToken
                    ).apply()
                }
            })
        channelManager.selectedChannel.let { channel ->
            if (channel != ChannelManager.NONE_CHANNEL) {
                session =
                    engagementSDK?.createContentSession(
                        channel.llProgram!!,
                        errorDelegate = object :
                            ErrorDelegate() {
                            override fun onError(error: String) {
                                println("Error:$error")
                            }
                        })
                chat_view.setSession(session!!.chatSession)

                widget_view.setSession(session!!)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        session!!.resume()
    }

    override fun onPause() {
        super.onPause()
        session!!.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        session!!.close()
        engagementSDK = null
    }
}