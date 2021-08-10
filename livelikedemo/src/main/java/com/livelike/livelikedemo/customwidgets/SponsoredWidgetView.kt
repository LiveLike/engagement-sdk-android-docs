package com.livelike.livelikedemo.customwidgets

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.livelike.engagementsdk.LiveLikeWidget
import com.livelike.livelikedemo.R

class SponsoredWidgetView(
    context: Context,
    private val widgetView: View,
    private val widget: LiveLikeWidget
) : LinearLayout(context) {

    init {
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        orientation = LinearLayout.VERTICAL
        addView(widgetView)
        val sponsorView = inflate(context, R.layout.sponsor_layout, null)
        addView(sponsorView)
        bindData(sponsorView, widget)
    }

    private fun bindData(sponsorView: View?, widget: LiveLikeWidget) {
        if (widget.sponsors?.size ?: 0 > 0) {
            sponsorView?.findViewById<TextView>(R.id.sponsor_title)?.text =
                "Sponsored By ${widget.sponsors?.get(0)?.name}"
        }
    }
}
