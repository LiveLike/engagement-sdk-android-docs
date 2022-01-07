package com.livelike.engagementsdk.widget.view

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.ContextCompat
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.LiveLikeWidget
import com.livelike.engagementsdk.core.utils.AndroidResource
import com.livelike.engagementsdk.databinding.WidgetSocialEmbedBinding
import com.livelike.engagementsdk.widget.SpecifiedWidgetView
import com.livelike.engagementsdk.widget.WidgetType
import com.livelike.engagementsdk.widget.WidgetsTheme
import com.livelike.engagementsdk.widget.viewModel.BaseViewModel
import com.livelike.engagementsdk.widget.viewModel.SocialEmbedViewModel
import com.livelike.engagementsdk.widget.viewModel.WidgetStates
/*import kotlinx.android.synthetic.main.widget_social_embed.view.progress_bar
import kotlinx.android.synthetic.main.widget_social_embed.view.titleView
import kotlinx.android.synthetic.main.widget_social_embed.view.txtTitleBackground
import kotlinx.android.synthetic.main.widget_social_embed.view.web_view
import kotlinx.android.synthetic.main.widget_social_embed.view.widgetContainer
import kotlinx.android.synthetic.main.widget_text_option_selection.view.textEggTimer*/

internal class SocialEmbedWidgetView(context: Context) : SpecifiedWidgetView(context) {

    var viewModel: SocialEmbedViewModel? = null
    private var inflated = false
    private var binding: WidgetSocialEmbedBinding? = null

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

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        viewModel?.data?.unsubscribe(javaClass)
        viewModel?.widgetState?.unsubscribe(javaClass)
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
            WidgetStates.RESULTS -> {
              //not required
            }
            null -> {
                // not required

            }
        }
    }

    private fun inflate(context: Context, liveLikeWidget: LiveLikeWidget) {
        if (!inflated){
            inflated = true
            binding = WidgetSocialEmbedBinding.inflate(LayoutInflater.from(context), this@SocialEmbedWidgetView, true)
        }

           // binding = WidgetSocialEmbedBinding.inflate(LayoutInflater.from(context), this@SocialEmbedWidgetView, true)
            binding?.titleView?.text = liveLikeWidget.comment ?: ""

            liveLikeWidget.socialEmbedItems?.get(0)?.let { oembed ->
                binding?.webView?.settings?.javaScriptEnabled = true
                binding?.webView?.settings?.domStorageEnabled = true

                showTimer(
                    liveLikeWidget.timeout ?: "", binding?.textEggTimer,
                    {
                    },
                    {
                        viewModel?.dismissWidget(it)
                    }
                )
                binding?.webView?.loadDataWithBaseURL(
                    oembed.oEmbed.providerUrl,
                    oembed.oEmbed.html, "text/html", "utf-8", ""
                )

                binding?.webView?.webViewClient = object : WebViewClient() {

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        Log.d("widget", "onPageFinished")
                        binding?.progressBar?.hide()
                    }

                    override fun onPageCommitVisible(view: WebView?, url: String?) {
                        super.onPageCommitVisible(view, url)
                        Log.d("widget", "onPageCommitVisible")
                        binding?.progressBar?.hide()
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
                        binding?.progressBar?.hide()

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            request?.url?.let { url ->
                                openLink(context, url.toString())
                            }
                        }
                        return true
                    }
                }
            }

            widgetsTheme?.let {
                applyTheme(it)
            }

    }

    override fun applyTheme(theme: WidgetsTheme) {
        super.applyTheme(theme)
        viewModel?.data?.latest()?.let { _ ->
            theme.getThemeLayoutComponent(WidgetType.SOCIAL_EMBED)?.let { themeComponent ->
                AndroidResource.updateThemeForView(
                    binding?.titleView,
                    themeComponent.title,
                    fontFamilyProvider
                )

                if (themeComponent.header?.background != null) {
                    binding?.txtTitleBackground?.background = AndroidResource.createDrawable(themeComponent.header)
                }

                themeComponent.header?.padding?.let {
                    AndroidResource.setPaddingForView(binding?.titleView, themeComponent.header.padding)
                }

                binding?.widgetContainer?.background =
                    AndroidResource.createDrawable(themeComponent.body)
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
