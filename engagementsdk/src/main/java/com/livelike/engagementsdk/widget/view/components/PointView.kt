package com.livelike.engagementsdk.widget.view.components

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.os.Build
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.utils.AndroidResource
import com.livelike.engagementsdk.widget.view.clipParents
import kotlinx.android.synthetic.main.atom_widget_point.view.coinDroppingView
import kotlinx.android.synthetic.main.atom_widget_point.view.coinView
import kotlinx.android.synthetic.main.atom_widget_point.view.pointTextView
import kotlin.math.roundToInt

class PointView(context: Context, attr: AttributeSet) : ConstraintLayout(context, attr) {

    /** icon size is used to define the size of coin icon */
    private var iconSize: Int
    private var textSize: Float
    /** will hide/show plus before points number adjacent to coin */
    private var hidePlus: Boolean

    private var point: Int = 0
        set(value) {
            field = value
            pointTextView.text = if (hidePlus) "$value" else "+$value"
        }

    init {
        inflate(context, R.layout.atom_widget_point, this)
        context.theme.obtainStyledAttributes(
            attr,
            R.styleable.PointView,
            0, 0).apply {
            try {
                hidePlus = getBoolean(R.styleable.PointView_hidePlus, false)
                iconSize = getDimension(R.styleable.PointView_iconSize, 0f).roundToInt()
                textSize = getDimension(R.styleable.PointView_textSize, 0f)
            } finally {
                recycle()
            }
        }
        // Handling non-default case
        if (iconSize != 0) {
            (coinDroppingView.layoutParams as LayoutParams).apply {
                width = iconSize
                height = iconSize
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    marginStart = AndroidResource.dpToPx(8)
                    topMargin = AndroidResource.dpToPx(5)
                }
            }
            (coinView.layoutParams as LayoutParams).apply {
                width = iconSize
                height = iconSize
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    marginStart = AndroidResource.dpToPx(8)
                    topMargin = AndroidResource.dpToPx(5)
                }
            }

        }
        if (textSize != 0f) {
            pointTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
        }
    }

    fun startAnimation(newPoint: Int) {
        visibility = View.VISIBLE
        clipChildren = false
        clipToPadding = false
        clipParents(false)
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
        bothAnimatorSet.startDelay = 300
        bothAnimatorSet.start()
    }

    fun showPoints(points: Int) {
        visibility = View.VISIBLE
        point = points
    }
}
