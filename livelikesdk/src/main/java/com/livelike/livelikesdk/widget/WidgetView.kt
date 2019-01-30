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
import com.livelike.livelikesdk.messaging.ClientMessage
import com.livelike.livelikesdk.messaging.ConnectionStatus
import com.livelike.livelikesdk.messaging.Error
import com.livelike.livelikesdk.messaging.MessagingClient
import com.livelike.livelikesdk.messaging.MessagingEventListener
import kotlinx.android.synthetic.main.widget_view.view.webview

class WidgetView(context: Context, attrs: AttributeSet?): ConstraintLayout(context, attrs), MessagingEventListener {

    val webInterface = WebAppInterface(context)

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
        webview!!.loadUrl("https://dev.redspace.com/animations-test/index.html")
    }

    fun setSession(liveLikeContentSession: LiveLikeContentSession) {
        liveLikeContentSession.setWidgetSourceListener(this)
    }

    override fun onClientMessageEvent(client: MessagingClient, event: ClientMessage) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, event.event + " " + event.message["url"], Toast.LENGTH_SHORT).show()
        }
    }

    override fun onClientMessageError(client: MessagingClient, error: Error) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onClientMessageStatus(client: MessagingClient, status: ConnectionStatus) {
        Toast.makeText(context, status.name, Toast.LENGTH_SHORT).show()
    }

    class WebAppInterface(private val mContext: Context) {
        @JavascriptInterface
        fun reporting(reportingString: String) {

        }
    }
}
