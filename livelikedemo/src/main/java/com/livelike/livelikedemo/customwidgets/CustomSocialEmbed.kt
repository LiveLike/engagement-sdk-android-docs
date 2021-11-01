package com.livelike.livelikedemo.customwidgets

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.livelike.engagementsdk.widget.widgetModel.SocialEmbedWidgetModel
import com.livelike.livelikedemo.databinding.CustomSocialEmbedBinding


class CustomSocialEmbed: ConstraintLayout {
    var socialEmbedWidgetModel: SocialEmbedWidgetModel? = null
    private lateinit var binding: CustomSocialEmbedBinding

    constructor(context: Context) : super(context) {
        init(null, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(attrs, defStyle)
    }

    private fun init(attrs: AttributeSet?, defStyle: Int) {
        binding = CustomSocialEmbedBinding.inflate(LayoutInflater.from(context), this@CustomSocialEmbed, true)
    }


    @SuppressLint("SetJavaScriptEnabled")
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        socialEmbedWidgetModel?.widgetData?.let { liveLikeWidget ->
            liveLikeWidget.socialEmbedItems?.get(0)?.let { oembed ->
                binding.webView.settings.javaScriptEnabled = true
                binding.webView.settings.domStorageEnabled = true


                binding.webView.loadDataWithBaseURL(
                    oembed.oEmbed.providerUrl,
                    oembed.oEmbed.html, "text/html", "utf-8", ""
                )

                binding.webView.webViewClient = object : WebViewClient() {

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)

                    }

                    override fun onPageCommitVisible(view: WebView?, url: String?) {
                        super.onPageCommitVisible(view, url)
                    }

                    override fun onLoadResource(view: WebView?, url: String?) {
                        super.onLoadResource(view, url)
                    }

                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        return super.shouldInterceptRequest(view, request)
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            request?.url?.let { url ->
                                    val universalLinkIntent =
                                        Intent(Intent.ACTION_VIEW, Uri.parse(url.toString())).setFlags(
                                            Intent.FLAG_ACTIVITY_NEW_TASK)
                                    if (universalLinkIntent.resolveActivity(context.packageManager) != null) {
                                        ContextCompat.startActivity(context, universalLinkIntent, Bundle.EMPTY)
                                    }

                            }
                        }
                        return true
                    }
                }
            }
        }
    }
}