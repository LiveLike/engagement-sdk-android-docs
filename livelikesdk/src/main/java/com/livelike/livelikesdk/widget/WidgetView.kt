package com.livelike.livelikesdk.widget

import android.content.Context
import android.graphics.Color
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import com.livelike.livelikesdk.R
import kotlinx.android.synthetic.main.widget_view.view.*

class WidgetView(context: Context, attrs: AttributeSet?): ConstraintLayout(context, attrs) {
    val webInterface = WebAppInterface(context)
    init {
        LayoutInflater.from(context)
                .inflate(R.layout.widget_view, this, true)

        webview!!.settings.javaScriptEnabled = true;
        webview!!.addJavascriptInterface(webInterface, "Android")
        webview!!.setInitialScale(110)
        webview!!.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                Log.d("SHANE", "Trying to get Message from HTML5 ")

                view?.setBackgroundColor(Color.TRANSPARENT)
                super.onPageFinished(view, url)
            }
        }
        webview!!.loadUrl("https://dev.redspace.com/animations-test/index.html")
    }

    class WebAppInterface(private val mContext: Context) {
        @JavascriptInterface
        fun reporting(reportingString: String) {
            Log.d("SHANE", "Message from HTML5 " + reportingString)
        }
    }
}

