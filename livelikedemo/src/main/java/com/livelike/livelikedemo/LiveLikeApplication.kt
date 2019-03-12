package com.livelike.livelikedemo

import android.app.Application
import com.livelike.livelikedemo.channel.ChannelManager

class LiveLikeApplication : Application() {

    companion object {
        const val TEST_CONFIG_URL =
            "https://livelike-webs.s3.amazonaws.com/mobile-pilot/video-backend-sdk-android-demo.json"
    }

    lateinit var channelManager: ChannelManager

    override fun onCreate() {
        super.onCreate()
        channelManager = ChannelManager(TEST_CONFIG_URL, applicationContext)
    }
}