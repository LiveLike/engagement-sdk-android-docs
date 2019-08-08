package com.livelike.livelikesdk.widget.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import com.livelike.engagementsdkapi.LiveLikeContentSession
import com.livelike.livelikesdk.utils.AndroidResource
import com.livelike.livelikesdk.utils.logError

class WidgetView(context: Context, attr: AttributeSet) : FrameLayout(context, attr) {
    fun setSession(session: LiveLikeContentSession) {
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
