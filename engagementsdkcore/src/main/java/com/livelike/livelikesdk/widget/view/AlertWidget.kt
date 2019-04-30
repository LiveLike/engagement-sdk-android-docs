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
import com.livelike.engagementsdkapi.WidgetTransientState
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.animation.ViewAnimationManager
import com.livelike.livelikesdk.util.AndroidResource
import com.livelike.livelikesdk.util.AndroidResource.Companion.parseDuration
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
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

internal class AlertWidget : ConstraintLayout {
    private lateinit var viewAnimation: ViewAnimationManager
    private lateinit var progressedStateCallback: (WidgetTransientState) -> Unit
    private lateinit var progressedState: WidgetTransientState
    private var layout = ConstraintLayout(context, null, 0)
    private var dismissWidget: (() -> Unit)? = null
    lateinit var resourceAlert: Alert
    private var timeout = 0L
    private var initialTimeout = 0L
    // TODO: Duplicate of text follow up. Move out.
    private var executor = ScheduledThreadPoolExecutor(15)
    lateinit var future: ScheduledFuture<*>

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun initialize(
        dismissWidget: () -> Unit,
        alertData: Alert,
        progressedState: WidgetTransientState,
        viewAnimation: ViewAnimationManager,
        progressedStateCallback: (WidgetTransientState) -> Unit
    ) {
        this.dismissWidget = dismissWidget
        this.resourceAlert = alertData
        this.viewAnimation = viewAnimation
        this.progressedState = progressedState
        this.progressedStateCallback = progressedStateCallback
        inflate(context)
        future = executor.scheduleAtFixedRate(Updater(), 0, 1, TimeUnit.SECONDS)
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

    inner class Updater : Runnable {
        override fun run() {
            progressedState.timeout = timeout - initialTimeout
            progressedStateCallback.invoke(progressedState)
            val updateRate = 1000
            initialTimeout += updateRate
            if (timeout == initialTimeout) {
                future.cancel(false)
            }
        }
    }

    private fun openBrowser(context: Context) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(resourceAlert.link_url))
        startActivity(context, browserIntent, Bundle.EMPTY)
    }
}
