package com.livelike.livelikesdk.widget.view.components

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.View
import com.livelike.livelikesdk.R
import kotlinx.android.synthetic.main.atom_widget_point.view.coinDroppingView
import kotlinx.android.synthetic.main.atom_widget_point.view.coinView
import kotlinx.android.synthetic.main.atom_widget_point.view.pointTextView

class PointView(context: Context, attr: AttributeSet) : ConstraintLayout(context, attr) {
    private var point: Int = 0
        set(value) {
            field = value
            pointTextView.text = "+$value"
        }

    init {
        inflate(context, R.layout.atom_widget_point, this)
        visibility = View.GONE
    }

    fun startAnimation(newPoint: Int) {
        visibility = View.VISIBLE
        ValueAnimator.ofInt(0, newPoint).apply {
            addUpdateListener {
                point = it.animatedValue as Int
            }
            duration = 500
            start()
        }

        val popping = AnimatorInflater.loadAnimator(context, R.animator.popping) as AnimatorSet
        popping.setTarget(coinView)

        val dropping = AnimatorInflater.loadAnimator(context, R.animator.dropping) as AnimatorSet
        dropping.setTarget(coinDroppingView)

        val bothAnimatorSet = AnimatorSet()
        bothAnimatorSet.playTogether(popping, dropping)
        bothAnimatorSet.start()
    }
}
