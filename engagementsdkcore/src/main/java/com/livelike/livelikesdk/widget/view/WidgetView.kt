package com.livelike.livelikesdk.widget.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import com.livelike.engagementsdkapi.LiveLikeContentSession
import com.livelike.livelikesdk.widget.viewModel.WidgetContainerViewModel

class WidgetView(context: Context, attr: AttributeSet) : FrameLayout(context, attr) {
    fun setSession(session: LiveLikeContentSession) {
        WidgetContainerViewModel(this, session)
    }
}