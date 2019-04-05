package com.livelike.livelikesdk.widget.view

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.constraint.ConstraintLayout
import android.support.v4.content.ContextCompat.startActivity
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import com.bumptech.glide.Glide
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.animation.ViewAnimation
import com.livelike.livelikesdk.util.AndroidResource
import com.livelike.livelikesdk.util.AndroidResource.Companion.parseDuration
import com.livelike.livelikesdk.widget.model.Alert
import kotlinx.android.synthetic.main.alert_widget.view.*

internal class AlertWidget : ConstraintLayout {
    private lateinit var viewAnimation: ViewAnimation
    private var layout = ConstraintLayout(context, null, 0)
    private var dismissWidget: (() -> Unit)? = null
    lateinit var resourceAlert: Alert

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun initialize(dismissWidget: () -> Unit, alertData: Alert, viewAnimation: ViewAnimation) {
        this.dismissWidget = dismissWidget
        this.resourceAlert = alertData
        this.viewAnimation = viewAnimation
        inflate(context)
    }


    private fun inflate(context: Context) {
        layout = LayoutInflater.from(context).inflate(R.layout.alert_widget, this, true) as ConstraintLayout

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
            val params = bodyText.layoutParams as ConstraintLayout.LayoutParams
            params.rightMargin = AndroidResource.dpToPx(16)
            bodyText.requestLayout()
        } else {
            resourceAlert.image_url.apply {
                Handler(Looper.getMainLooper()).post {
                    Glide.with(context)
                        .load(resourceAlert.image_url)
                        .into(bodyImage)
                }
            }
        }

        if (resourceAlert.title.isNullOrEmpty()) {
            labelBackground.visibility = View.GONE
            labelText.visibility = View.GONE
            val params = bodyBackground.layoutParams as ConstraintLayout.LayoutParams
            params.topMargin = AndroidResource.dpToPx(0)
            bodyBackground.requestLayout()
        } else {
            val params = bodyBackground.layoutParams as ConstraintLayout.LayoutParams
            params.topMargin = AndroidResource.dpToPx(12)
            bodyBackground.requestLayout()
        }

        if (resourceAlert.text.isNullOrEmpty()) {
            bodyText.visibility = View.GONE
            if (!resourceAlert.image_url.isNullOrEmpty()) {
                // Image Only
                val params = widgetContainer.layoutParams as ConstraintLayout.LayoutParams
                params.height = AndroidResource.dpToPx(200)
                widgetContainer.requestLayout()
            }
        }

        viewAnimation.startWidgetTransitionInAnimation {}

        // Start dismiss timeout
        var timeout = parseDuration(resourceAlert.timeout)

        Handler().postDelayed({ dismissWidget?.invoke() }, timeout)
    }

    private fun openBrowser(context: Context) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(resourceAlert.link_url))
        startActivity(context, browserIntent, Bundle.EMPTY)
    }
}
