package com.livelike.livelikesdk.widget.view.atom

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.util.logDebug
import kotlinx.android.synthetic.main.atom_widget_confirmation_message.view.confirmMessageAnimation
import kotlinx.android.synthetic.main.atom_widget_confirmation_message.view.confirmMessageText

class ConfirmMessageView(context: Context, attr: AttributeSet) : ConstraintLayout(context, attr) {
    init {
        inflate(context, R.layout.atom_widget_confirmation_message, this)
    }

    var text: String = ""
        set(value) {
            field = value
            confirmMessageText.text = value
        }

    fun startAnimation(animationPath: String, progress: Float) {
        logDebug { "Animation is: $animationPath" }
        confirmMessageAnimation.setAnimation(animationPath)
        confirmMessageAnimation.progress = progress
        if (progress != 1f) {
            confirmMessageAnimation.resumeAnimation()
        }
    }
}