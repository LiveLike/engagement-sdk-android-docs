package com.livelike.engagementsdk.widget.view

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.LiveLikeWidget
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.widget.SpecifiedWidgetView
import com.livelike.engagementsdk.widget.WidgetsTheme
import com.livelike.engagementsdk.widget.viewModel.BaseViewModel
import com.livelike.engagementsdk.widget.viewModel.SocialEmbedViewModel
import com.livelike.engagementsdk.widget.viewModel.WidgetStates
import kotlinx.android.synthetic.main.widget_social_embed.view.social_embed_container
import kotlinx.android.synthetic.main.widget_social_embed.view.titleView
import kotlinx.android.synthetic.main.widget_social_embed.view.web_view
import kotlinx.android.synthetic.main.widget_text_option_selection.view.textEggTimer

internal class SocialEmbedWidgetView(context: Context) : SpecifiedWidgetView(context) {

    private var inflated = false

    var viewModel: SocialEmbedViewModel? = null

    override var dismissFunc: ((action: DismissAction) -> Unit)? =
        {
            viewModel?.dismissWidget(it)
            removeAllViews()
        }

    override var widgetViewModel: BaseViewModel? = null
        set(value) {
            field = value
            viewModel = value as SocialEmbedViewModel
        }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewModel?.widgetState?.subscribe(javaClass) { widgetStates ->
            logDebug { "Current State: $widgetStates" }
            widgetStates?.let {
                when (widgetStates) {
                    WidgetStates.READY -> {
                        viewModel?.data?.latest()?.let {
                            logDebug { "showing the Alert WidgetView" }
                            inflate(context, it)
                        }
                    }
                }
                if (viewModel?.enableDefaultWidgetTransition == true) {
                    defaultStateTransitionManager(widgetStates)
                }
            }
        }
    }

    private fun defaultStateTransitionManager(widgetStates: WidgetStates?) {
        when (widgetStates) {
            WidgetStates.READY -> {
                viewModel?.widgetState?.onNext(WidgetStates.INTERACTING)
            }
            WidgetStates.INTERACTING -> {
                viewModel?.data?.latest()?.let {
                    viewModel?.startDismissTimout(it.timeout ?: "") {
                        viewModel?.widgetState?.onNext(WidgetStates.FINISHED)
                    }
                }
            }
            WidgetStates.FINISHED -> {
                removeAllViews()
                parent?.let { (it as ViewGroup).removeAllViews() }
            }
        }
    }

    override fun applyTheme(theme: WidgetsTheme) {
        super.applyTheme(theme)
        // Json themeing to be defined for social embed widgets.
    }

    override fun moveToNextState() {
        super.moveToNextState()
        if (widgetViewModel?.widgetState?.latest() == WidgetStates.INTERACTING) {
            widgetViewModel?.widgetState?.onNext(WidgetStates.FINISHED)
        } else {
            super.moveToNextState()
        }
    }

    private fun inflate(context: Context, liveLikeWidget: LiveLikeWidget) {
        if (!inflated) {
            visibility = View.GONE
            inflated = true
            LayoutInflater.from(context)
                .inflate(R.layout.widget_social_embed, this, true) as ConstraintLayout

            titleView.title = liveLikeWidget.comment ?: ""

            liveLikeWidget.socialEmbedItems?.get(0)?.let { oembed ->
                web_view.settings.javaScriptEnabled = true
                web_view.settings.domStorageEnabled = true

                web_view.loadDataWithBaseURL(
                    oembed.oEmbed.providerUrl,
                    oembed.oEmbed.html, "text/html", "utf-8", ""
                )

                web_view.webViewClient = object : WebViewClient() {

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        Log.d("widget", "onPageFinished")

                    }

                    override fun onPageCommitVisible(view: WebView?, url: String?) {
                        super.onPageCommitVisible(view, url)
                        Log.d("widget", "onPageCommitVisible")

                        web_view.postDelayed(
                            {
                                this@SocialEmbedWidgetView.visibility = View.VISIBLE
                                social_embed_container.visibility = View.VISIBLE
                                showTimer(liveLikeWidget.timeout ?: "", 0f, textEggTimer, {
                                }, {
                                    viewModel?.dismissWidget(it)
                                })
                            },
                            2000
                        )

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
                        Log.d("url ", "${request?.url}")
                        request?.url?.let { url->
                            openLink(context, url?.toString())
                        }
                        return true
                    }


                }

            }

        }
    }

    private fun openLink(context: Context, linkUrl: String) {
        val universalLinkIntent =
            Intent(Intent.ACTION_VIEW, Uri.parse(linkUrl)).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (universalLinkIntent.resolveActivity(context.packageManager) != null) {
            ContextCompat.startActivity(context, universalLinkIntent, Bundle.EMPTY)
        }
    }


}




