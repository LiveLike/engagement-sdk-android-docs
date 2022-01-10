package com.livelike.engagementsdk.widget.view.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.livelike.engagementsdk.databinding.AtomWidgetConfirmationMessageBinding



class ConfirmMessageView(context: Context, attr: AttributeSet) : ConstraintLayout(context, attr) {
    private var binding: AtomWidgetConfirmationMessageBinding? = null
    init {
        binding = AtomWidgetConfirmationMessageBinding.inflate(LayoutInflater.from(context), this@ConfirmMessageView, true)

    }

    var text: String = ""
        set(value) {
            field = value
//            confirmMessageText.text = value
        }

    fun startAnimation(animationPath: String, progress: Float) {
        binding?.confirmMessageAnimation?.setAnimation(animationPath)
        binding?.confirmMessageAnimation?.progress = progress
        if (progress != 1f) {
            binding?.confirmMessageAnimation?.resumeAnimation()
        }
    }

    fun subscribeToAnimationUpdates(onUpdate: (Float) -> Unit) {
        binding?.confirmMessageAnimation?.addAnimatorUpdateListener { onUpdate(it.animatedFraction) }
    }
}
