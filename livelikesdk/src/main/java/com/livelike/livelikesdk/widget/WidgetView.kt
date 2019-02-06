package com.livelike.livelikesdk.widget

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.LayoutInflater
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.livelike.livelikesdk.LiveLikeContentSession
import com.livelike.livelikesdk.R

import kotlinx.android.synthetic.main.widget_view.view.webview
import java.util.*
import kotlin.concurrent.timer

class WidgetView(context: Context, attrs: AttributeSet?): ConstraintLayout(context, attrs), WidgetRenderer {

    companion object {
        const val AUTO_DISMISS_DELAY = 5000L
    }

    val webInterface = WebAppInterface(context)
    override var widgetListener : WidgetEventListener? = null

    init {
        LayoutInflater.from(context)
                .inflate(R.layout.widget_view, this, true)

        webview!!.settings.javaScriptEnabled = true
        webview!!.addJavascriptInterface(webInterface, "Android")
        webview!!.setInitialScale(110)
        webview!!.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                view?.setBackgroundColor(Color.TRANSPARENT)
                super.onPageFinished(view, url)
            }
        }
    }

    fun setSession(liveLikeContentSession: LiveLikeContentSession) {
        liveLikeContentSession.renderer = this
    }

    class WebAppInterface(private val mContext: Context) {
        @JavascriptInterface
        fun reporting(reportingString: String) {

        }
    }

    override fun displayWidget(widgetData: Any) {
        Handler(Looper.getMainLooper()).post {
            webview!!.loadUrl("https://dev.redspace.com/animations-test/index.html")
            Toast.makeText(context, widgetData.toString(), Toast.LENGTH_SHORT).show()
            val timerTask = object : TimerTask() {
                override fun run() {
                    widgetListener?.onWidgetEvent(WidgetEvent.WIDGET_DISMISS)
                }
            }
            Timer().schedule(timerTask, AUTO_DISMISS_DELAY)
        }
    }
}

