package com.livelike.livelikedemo

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_live_blog.web_view

class LiveBlogActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_blog)
        var env = ""
        if (BuildConfig.BUILD_TYPE.equals("debug")) {
            env = "staging"
        } else if (BuildConfig.BUILD_TYPE.equals("qa")) {
            env = "qa"
        } else {
            env = "prod"
        }
        val programID =
            (application as LiveLikeApplication).channelManager.selectedChannel.llProgram
        web_view.settings.javaScriptEnabled = true
        web_view.loadUrl("https://producer.livelikecdn.com/liveblog.html?env=$env&program=$programID")
    }
}
