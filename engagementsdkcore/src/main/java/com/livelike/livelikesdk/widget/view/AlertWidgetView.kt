package com.livelike.livelikesdk.widget.view

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v4.content.ContextCompat.startActivity
import android.support.v7.app.AppCompatActivity
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import com.bumptech.glide.Glide
import com.livelike.engagementsdkapi.LiveLikeContentSession
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.services.analytics.AnalyticsWidgetInteractionInfo
import com.livelike.livelikesdk.utils.AndroidResource
import com.livelike.livelikesdk.widget.DismissAction
import com.livelike.livelikesdk.widget.SpecifiedWidgetView
import com.livelike.livelikesdk.widget.model.Alert
import com.livelike.livelikesdk.widget.viewModel.AlertWidgetViewModel
import kotlinx.android.synthetic.main.widget_alert.view.bodyBackground
import kotlinx.android.synthetic.main.widget_alert.view.bodyImage
import kotlinx.android.synthetic.main.widget_alert.view.bodyText
import kotlinx.android.synthetic.main.widget_alert.view.labelBackground
import kotlinx.android.synthetic.main.widget_alert.view.labelText
import kotlinx.android.synthetic.main.widget_alert.view.linkArrow
import kotlinx.android.synthetic.main.widget_alert.view.linkBackground
import kotlinx.android.synthetic.main.widget_alert.view.linkText
import kotlinx.android.synthetic.main.widget_alert.view.widgetContainer

internal class AlertWidgetView : SpecifiedWidgetView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private var inflated = false

    private val viewModel: AlertWidgetViewModel =
        ViewModelProviders.of(context as AppCompatActivity).get(AlertWidgetViewModel::class.java)

    override var currentSession: LiveLikeContentSession? = null
        set(value) {
            field = value
            viewModel.setSession(currentSession)
        }

    override var dismissFunc: ((action: DismissAction) -> Unit)? = { viewModel.dismissWidget(it) }

    init {
        viewModel.data.observe(context as AppCompatActivity, Observer {
            if (it != null) {
                inflate(context, it)
                viewModel.startDismissTimout(it.timeout)
            } else {
                inflated = false
            }
        })
    }

    private fun inflate(context: Context, resourceAlert: Alert) {
        if (!inflated) {
            inflated = true
            LayoutInflater.from(context).inflate(R.layout.widget_alert, this, true) as ConstraintLayout
        }

        bodyText.text = resourceAlert.text
        labelText.text = resourceAlert.title
        linkText.text = resourceAlert.link_label

        if (!resourceAlert.link_url.isNullOrEmpty()) {
            linkBackground.setOnClickListener {
                openBrowser(context, resourceAlert.link_url)
            }
            bodyBackground.setOnClickListener {
                openBrowser(context, resourceAlert.link_url)
            }
        } else {
            linkArrow.visibility = View.GONE
            linkBackground.visibility = View.GONE
            linkText.visibility = View.GONE
        }

        if (resourceAlert.image_url.isNullOrEmpty()) {
            bodyImage.visibility = View.GONE
            val params = bodyText.layoutParams as LayoutParams
            params.rightMargin = AndroidResource.dpToPx(16)
            bodyText.requestLayout()
        } else {
            resourceAlert.image_url.apply {
                Glide.with(context)
                    .load(resourceAlert.image_url)
                    .into(bodyImage)
                }
            }

        if (resourceAlert.title.isNullOrEmpty()) {
            labelBackground.visibility = View.GONE
            labelText.visibility = View.GONE
            val params = bodyBackground.layoutParams as LayoutParams
            params.topMargin = AndroidResource.dpToPx(0)
            bodyBackground.requestLayout()
        } else {
            val params = bodyBackground.layoutParams as LayoutParams
            params.topMargin = AndroidResource.dpToPx(12)
            bodyBackground.requestLayout()
        }

        if (resourceAlert.text.isNullOrEmpty()) {
            bodyText.visibility = View.GONE
            if (!resourceAlert.image_url.isNullOrEmpty()) {
                // Image Only
                val params = widgetContainer.layoutParams as LayoutParams
                params.height = AndroidResource.dpToPx(200)
                widgetContainer.requestLayout()
            }
        }
    }

    private fun openBrowser(context: Context, linkUrl: String) {
        viewModel.onClickLink()
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(linkUrl))
        startActivity(context, browserIntent, Bundle.EMPTY)
    }
}
