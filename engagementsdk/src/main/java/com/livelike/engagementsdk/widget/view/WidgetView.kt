package com.livelike.engagementsdk.widget.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import com.livelike.engagementsdk.LiveLikeContentSession
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.utils.AndroidResource
import com.livelike.engagementsdk.utils.logError
import com.livelike.engagementsdk.widget.WidgetViewThemeAttributes

class WidgetView(context: Context,private val attr: AttributeSet) : FrameLayout(context, attr) {

    private val widgetViewThemeAttributes = WidgetViewThemeAttributes()

    fun setSession(session: LiveLikeContentSession) {
        context.obtainStyledAttributes(
            attr,
            R.styleable.LiveLike_WidgetView,
            0, 0
        ).apply {
            try{
                widgetViewThemeAttributes.init(this)
                session.setWidgetViewThemeAttribute(widgetViewThemeAttributes)
            } finally {
                recycle()
            }
        }
        session.setWidgetContainer(this)
        session.analyticService.trackOrientationChange(resources.configuration.orientation == 1)
    }
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthDp = AndroidResource.pxToDp(width)
        if (widthDp < 292 && widthDp != 0) {
            logError { "[CONFIG ERROR] Current WidgetView Width is $widthDp, it must be more than 292dp or won't display on the screen." }
            setMeasuredDimension(0, 0)
            return
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}
