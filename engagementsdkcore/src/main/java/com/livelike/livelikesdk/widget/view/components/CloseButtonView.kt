package com.livelike.livelikesdk.widget.view.components

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.View
import com.livelike.livelikesdk.LiveLikeSDK.Companion.currentSession
import com.livelike.livelikesdk.R
import kotlinx.android.synthetic.main.atom_widget_close_button.view.closeButton

class CloseButtonView(context: Context, attr: AttributeSet? = null) : ConstraintLayout(context, attr) {
    init {
        View.inflate(context, R.layout.atom_widget_close_button, this)
        closeButton.setOnClickListener {
            currentSession.currentWidgetInfosStream.onNext(null)
        }
    }
}