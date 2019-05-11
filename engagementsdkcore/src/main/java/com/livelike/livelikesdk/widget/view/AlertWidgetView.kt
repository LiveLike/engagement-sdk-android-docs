package com.livelike.livelikesdk.widget.view

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
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.util.AndroidResource
import com.livelike.livelikesdk.widget.model.Alert
import kotlinx.android.synthetic.main.alert_widget.view.bodyBackground
import kotlinx.android.synthetic.main.alert_widget.view.bodyImage
import kotlinx.android.synthetic.main.alert_widget.view.bodyText
import kotlinx.android.synthetic.main.alert_widget.view.labelBackground
import kotlinx.android.synthetic.main.alert_widget.view.labelText
import kotlinx.android.synthetic.main.alert_widget.view.linkArrow
import kotlinx.android.synthetic.main.alert_widget.view.linkBackground
import kotlinx.android.synthetic.main.alert_widget.view.linkText
import kotlinx.android.synthetic.main.alert_widget.view.widgetContainer

internal class AlertWidgetView : ConstraintLayout {
    private var resourceAlert: Alert

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private var alertWidgetViewModel: AlertWidgetViewModel =
        ViewModelProviders.of(context as AppCompatActivity).get(AlertWidgetViewModel::class.java)

    init {
        this.resourceAlert = alertWidgetViewModel.data
        inflate(context)
    }

    private fun inflate(context: Context) {
        LayoutInflater.from(context).inflate(R.layout.alert_widget, this, true) as ConstraintLayout

        bodyText.text = resourceAlert.text
        labelText.text = resourceAlert.title
        linkText.text = resourceAlert.link_label

        if (!resourceAlert.link_url.isNullOrEmpty()) {
            linkBackground.setOnClickListener {
                openBrowser(context)
            }
            bodyBackground.setOnClickListener {
                openBrowser(context)
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

        // User clicks here
        alertWidgetViewModel.voteForOption("optionID")

        // View wants to dismiss itself
//        alertWidgetViewModel.dismiss()

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

    private fun openBrowser(context: Context) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(resourceAlert.link_url))
        startActivity(context, browserIntent, Bundle.EMPTY)
    }
}
