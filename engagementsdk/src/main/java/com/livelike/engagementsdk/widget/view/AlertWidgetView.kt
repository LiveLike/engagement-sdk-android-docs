package com.livelike.engagementsdk.widget.view

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat.startActivity
import com.bumptech.glide.Glide
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.core.utils.AndroidResource
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.databinding.WidgetAlertBinding
import com.livelike.engagementsdk.widget.SpecifiedWidgetView
import com.livelike.engagementsdk.widget.WidgetType
import com.livelike.engagementsdk.widget.WidgetsTheme
import com.livelike.engagementsdk.widget.model.Alert
import com.livelike.engagementsdk.widget.viewModel.AlertWidgetViewModel
import com.livelike.engagementsdk.widget.viewModel.BaseViewModel
import com.livelike.engagementsdk.widget.viewModel.WidgetStates


internal class AlertWidgetView : SpecifiedWidgetView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private var inflated = false
    private var binding: WidgetAlertBinding? = null

    var viewModel: AlertWidgetViewModel? = null

    override var dismissFunc: ((action: DismissAction) -> Unit)? =
        {
            viewModel?.dismissWidget(it)
            removeAllViews()
        }

    override var widgetViewModel: BaseViewModel? = null
        set(value) {
            field = value
            viewModel = value as AlertWidgetViewModel
        }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewModel?.data?.subscribe(javaClass) {
            logDebug { "showing the Alert WidgetView" }
            it?.let {
                inflate(context, it)
            }
        }
        viewModel?.widgetState?.subscribe(javaClass) { widgetStates ->
            logDebug { "Current State: $widgetStates" }
            widgetStates?.let {
                if (widgetStates == WidgetStates.INTERACTING && (!viewModel?.data?.latest()?.link_url.isNullOrEmpty())) {
                    // will only be fired if link is available in alert widget
                    viewModel?.markAsInteractive()
                }
                if (viewModel?.enableDefaultWidgetTransition == true) {
                    defaultStateTransitionManager(widgetStates)
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
                    viewModel?.startDismissTimout(it.timeout) {
                        viewModel?.widgetState?.onNext(WidgetStates.FINISHED)
                    }
                }
            }
            WidgetStates.FINISHED -> {
                removeAllViews()
                parent?.let { (it as ViewGroup).removeAllViews() }
            }
            WidgetStates.RESULTS -> {
                // not needed presently
            }

        }
    }

    override fun applyTheme(theme: WidgetsTheme) {
        super.applyTheme(theme)
        viewModel?.data?.latest()?.let { _ ->
            theme.getThemeLayoutComponent(WidgetType.ALERT)?.let { themeComponent ->
                AndroidResource.updateThemeForView(
                    binding?.labelText,
                    themeComponent.title,
                    fontFamilyProvider
                )
                if (themeComponent.header?.background != null) {
                    binding?.labelText?.background = AndroidResource.createDrawable(themeComponent.header)
                }
                themeComponent.header?.padding?.let {
                    AndroidResource.setPaddingForView(binding?.labelText, themeComponent.header.padding)
                }

                binding?.widgetContainer?.background =
                    AndroidResource.createDrawable(themeComponent.body)
                AndroidResource.updateThemeForView(
                    binding?.bodyText,
                    themeComponent.body,
                    fontFamilyProvider
                )
                AndroidResource.updateThemeForView(
                    binding?.linkText,
                    themeComponent.body,
                    fontFamilyProvider
                )

                if (themeComponent.footer?.background != null) {
                    binding?.linkBackground?.background = AndroidResource.createDrawable(themeComponent.footer)
                }
            }
        }
    }

    override fun moveToNextState() {
        super.moveToNextState()
        if (widgetViewModel?.widgetState?.latest() == WidgetStates.INTERACTING) {
            widgetViewModel?.widgetState?.onNext(WidgetStates.FINISHED)
        } else {
            super.moveToNextState()
        }
    }

    private fun inflate(context: Context, resourceAlert: Alert) {
        if (!inflated) {
            inflated = true
          /* LayoutInflater.from(context)
                .inflate(R.layout.widget_alert, this, true)*/
            binding = WidgetAlertBinding.inflate(LayoutInflater.from(context), this@AlertWidgetView, true)
        }

        binding?.apply {
            bodyText.text = resourceAlert.text
            labelText.text = resourceAlert.title
            linkText.text = resourceAlert.link_label
        }

        if (!resourceAlert.link_url.isNullOrEmpty()) {
            binding?.linkBackground?.setOnClickListener {
                openBrowser(context, resourceAlert.link_url)
            }
        } else {
            binding?.apply {
                linkArrow.visibility = View.GONE
                linkBackground.visibility = View.GONE
                linkText.visibility = View.GONE
            }
        }

        if (resourceAlert.image_url.isNullOrEmpty()) {
            binding?.bodyImage?.visibility = View.GONE
            val params = binding?.bodyText?.layoutParams as ConstraintLayout.LayoutParams
            params.rightMargin = AndroidResource.dpToPx(16)
            binding?.bodyText?.requestLayout()
        } else {
            resourceAlert.image_url.apply {
                Glide.with(context.applicationContext)
                    .load(resourceAlert.image_url)
                    .into(binding?.bodyImage!!)
            }
        }

        if (resourceAlert.title.isNullOrEmpty()) {
            binding?.labelText?.visibility = View.GONE
            val params = binding?.widgetContainer?.layoutParams as ConstraintLayout.LayoutParams
            params.topMargin = AndroidResource.dpToPx(0)
            binding?.widgetContainer?.requestLayout()
        } else {
           binding?.widgetContainer?.requestLayout()
        }

        if (resourceAlert.text.isNullOrEmpty()) {
            binding?.bodyText?.visibility = View.GONE
            if (!resourceAlert.image_url.isNullOrEmpty()) {
                // Image Only
                val params = binding?.widgetContainer!!.layoutParams
                params.height = AndroidResource.dpToPx(200)
                binding?.widgetContainer!!.requestLayout()
            }
        }
        widgetsTheme?.let {
            applyTheme(it)
        }
    }

    private fun openBrowser(context: Context, linkUrl: String) {
        viewModel?.onClickLink(linkUrl)
        val universalLinkIntent =
            Intent(Intent.ACTION_VIEW, Uri.parse(linkUrl)).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (universalLinkIntent.resolveActivity(context.packageManager) != null) {
            startActivity(context, universalLinkIntent, Bundle.EMPTY)
        }
    }
}
