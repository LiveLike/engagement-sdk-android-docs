package com.livelike.livelikedemo

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.android.synthetic.main.activity_live_blog.progress_bar
import kotlinx.android.synthetic.main.activity_live_blog.web_view

class LiveBlogActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_blog)
        val programID =
            (application as LiveLikeApplication).channelManager.selectedChannel.llProgram
        var queryString: String
        queryString = when {
            BuildConfig.BUILD_TYPE == "debug" -> "env=staging&program=$programID"
            BuildConfig.BUILD_TYPE == "qa" -> "env=qa&program=$programID"
            else -> "program=$programID"
        }
        web_view.settings.javaScriptEnabled = true
        web_view.settings.domStorageEnabled = true
        web_view.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progress_bar.visibility = View.GONE
            }
        }
        web_view.loadUrl("https://producer.livelikecdn.com/liveblog.html?$queryString")
    }
}
