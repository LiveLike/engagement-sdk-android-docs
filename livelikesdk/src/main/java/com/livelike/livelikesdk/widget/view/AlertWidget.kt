package com.livelike.livelikesdk.widget.view

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.support.constraint.ConstraintLayout
import android.support.v4.content.ContextCompat.startActivity
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import com.bumptech.glide.Glide
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.animation.AnimationHandler
import com.livelike.livelikesdk.animation.ViewAnimation
import com.livelike.livelikesdk.widget.model.Alert
import kotlinx.android.synthetic.main.alert_widget.view.*
import org.threeten.bp.Duration

class AlertWidget : ConstraintLayout {
    private val animationHandler = AnimationHandler()
    private lateinit var viewAnimation: ViewAnimation
    private var layout = ConstraintLayout(context, null, 0)
    private var dismissWidget: (() -> Unit)? = null
    lateinit var resourceAlert: Alert

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun initialize(dismissWidget: () -> Unit, alertData: Alert) {
        this.dismissWidget = dismissWidget
        this.resourceAlert = alertData
        inflate(context)
    }

    private fun inflate(context: Context) {
        layout = LayoutInflater.from(context).inflate(R.layout.alert_widget, this, true) as ConstraintLayout
        viewAnimation = ViewAnimation(alertWidget, animationHandler)

        bodyText.text = resourceAlert.text
        labelText.text = resourceAlert.title
        linkText.text = resourceAlert.link_label
        resourceAlert.image_url.apply {
            if (!isNullOrEmpty()) {
                Glide.with(context)
                    .load(resourceAlert.image_url)
                    .into(bodyImage)
            }
        }

        if (!resourceAlert.link_url.isNullOrEmpty()) {
            linkBackground.setOnClickListener {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(resourceAlert.link_url))
                startActivity(context, browserIntent, Bundle.EMPTY)
            }
        } else {
            linkArrow.visibility = View.GONE
            linkBackground.visibility = View.GONE
            linkText.visibility = View.GONE
        }


        if (resourceAlert.image_url.isNullOrEmpty()) {
            bodyImage.visibility = View.GONE
        } // TODO: Modify the margin of the text body to be 0 instead of 90 when the image is not here

        if (resourceAlert.title.isNullOrEmpty()) {
            labelBackground.visibility = View.GONE
            labelText.visibility = View.GONE
            val params = bodyBackground.layoutParams as ConstraintLayout.LayoutParams
            params.topMargin = 0.dpToPx(resources)
            bodyBackground.requestLayout()
        } else {
            val params = bodyBackground.layoutParams as ConstraintLayout.LayoutParams
            params.topMargin = 12.dpToPx(resources)
            bodyBackground.requestLayout()
        }

        if (resourceAlert.text.isNullOrEmpty()) {
            bodyText.visibility = View.GONE
        }

        // Show the widget
        viewAnimation.startWidgetTransitionInAnimation()

        // Start dismiss timeout
        val timeout = Duration.parse(resourceAlert.timeout).toMillis()
        Handler().postDelayed({ viewAnimation.triggerTransitionOutAnimation { dismissWidget?.invoke() } }, timeout)
    }
}

private fun Int.dpToPx(resources: Resources?): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this.toFloat(),
        resources?.displayMetrics
    ).toInt()
}
