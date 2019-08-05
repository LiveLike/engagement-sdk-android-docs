package com.livelike.livelikesdk.widget.view

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v4.content.ContextCompat.startActivity
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.utils.AndroidResource
import com.livelike.livelikesdk.widget.DismissAction
import com.livelike.livelikesdk.widget.SpecifiedWidgetView
import com.livelike.livelikesdk.widget.model.Alert
import com.livelike.livelikesdk.widget.viewModel.AlertWidgetViewModel
import com.livelike.livelikesdk.widget.viewModel.WidgetViewModel
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

    var viewModel: AlertWidgetViewModel? = null

    override var dismissFunc: ((action: DismissAction) -> Unit)? =
        {
            viewModel?.dismissWidget(it)
            removeAllViews()
        }

    override var widgetViewModel: WidgetViewModel? = null
        set(value) {
            field = value
            viewModel = value as AlertWidgetViewModel
            viewModel?.data?.subscribe(javaClass) {
                if (it != null) {
                    inflate(context, it)
                    viewModel?.startDismissTimout(it.timeout) { removeAllViews() }
                } else {
                    removeAllViews()
                    parent?.let { par -> (par as ViewGroup)?.removeAllViews() }
                }
            }
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
        viewModel?.onClickLink()
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(linkUrl)).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(context, browserIntent, Bundle.EMPTY)
    }
}
