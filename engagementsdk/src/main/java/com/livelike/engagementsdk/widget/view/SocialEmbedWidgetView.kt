package com.livelike.engagementsdk.widget.view

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.LiveLikeWidget
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.widget.SpecifiedWidgetView
import com.livelike.engagementsdk.widget.viewModel.BaseViewModel
import com.livelike.engagementsdk.widget.viewModel.SocialEmbedViewModel
import com.livelike.engagementsdk.widget.viewModel.WidgetStates
import kotlinx.android.synthetic.main.widget_social_embed.view.progress_bar
import kotlinx.android.synthetic.main.widget_social_embed.view.titleView
import kotlinx.android.synthetic.main.widget_social_embed.view.web_view
import kotlinx.android.synthetic.main.widget_text_option_selection.view.textEggTimer

internal class SocialEmbedWidgetView(context: Context) : SpecifiedWidgetView(context) {


    var viewModel: SocialEmbedViewModel? = null

    override var dismissFunc: ((action: DismissAction) -> Unit)? = { viewModel?.dismissWidget(it) }

    override var widgetViewModel: BaseViewModel? = null
        set(value) {
            field = value
            viewModel = value as SocialEmbedViewModel
        }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        viewModel?.data?.latest()?.let {
            inflate(context, it)
        }

        viewModel?.widgetState?.subscribe(javaClass) { widgetStates ->
            widgetStates?.let { state ->
                if (viewModel?.enableDefaultWidgetTransition != false) {
                    defaultStateTransitionManager(state)
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


    private fun inflate(context: Context, liveLikeWidget: LiveLikeWidget) {
        if (titleView == null) {
            LayoutInflater.from(context)
                .inflate(R.layout.widget_social_embed, this, true) as ConstraintLayout

            titleView.text = liveLikeWidget.comment ?: ""

            liveLikeWidget.socialEmbedItems?.get(0)?.let { oembed ->
                web_view.settings.javaScriptEnabled = true
                web_view.settings.domStorageEnabled = true

                showTimer(liveLikeWidget.timeout ?: "", 0f, textEggTimer, {
                }, {
                    viewModel?.dismissWidget(it)
                })
                web_view.loadDataWithBaseURL(
                    oembed.oEmbed.providerUrl,
                    oembed.oEmbed.html, "text/html", "utf-8", ""
                )

                web_view.webViewClient = object : WebViewClient() {

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        Log.d("widget", "onPageFinished")
                        progress_bar.hide()
                    }

                    override fun onPageCommitVisible(view: WebView?, url: String?) {
                        super.onPageCommitVisible(view, url)
                        Log.d("widget", "onPageCommitVisible")
                        progress_bar.hide()
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
                        progress_bar.hide()

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            request?.url?.let { url ->
                                openLink(context, url?.toString())
                            }
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




