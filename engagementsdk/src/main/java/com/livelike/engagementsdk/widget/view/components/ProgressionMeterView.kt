package com.livelike.engagementsdk.widget.view.components

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.livelike.engagementsdk.core.utils.AndroidResource
import com.livelike.engagementsdk.core.utils.animators.buildRotationAnimator
import com.livelike.engagementsdk.core.utils.animators.buildScaleAnimator
import com.livelike.engagementsdk.databinding.AtomGamificationProgressionMeterBinding
import com.livelike.engagementsdk.widget.view.loadImage
import kotlin.math.min

class ProgressionMeterView(context: Context, attr: AttributeSet) : FrameLayout(context, attr) {

    private var progression: Int = 0
        set(value) {
            field = value
           binding?.progressionMeterText?.text = "$value/$totalPointsToNextbadge"
        }

    private var totalPointsToNextbadge: Int = 0
    private var binding:AtomGamificationProgressionMeterBinding? = null

    init {
        binding = AtomGamificationProgressionMeterBinding.inflate(LayoutInflater.from(context), this@ProgressionMeterView, true)
        visibility = View.GONE
    }

    fun animatePointsBadgeProgression(
        currentPointsForNextBadge: Int,
        newPoints: Int,
        totalPointsNextBadge: Int,
        badgeIconURL: String
    ) {
        visibility = View.VISIBLE

        binding?.gamificationBadgeIv?.loadImage(badgeIconURL, AndroidResource.dpToPx(30))
        totalPointsToNextbadge = totalPointsNextBadge

        var colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(0f)
        var filter = ColorMatrixColorFilter(colorMatrix)
        binding?.gamificationBadgeIv?.colorFilter = filter

        val newBadgeEarned = totalPointsToNextbadge <= currentPointsForNextBadge + newPoints
        if (newBadgeEarned) {
            binding?.gamificationBadgeIv?.postDelayed(
                {
                    colorMatrix = ColorMatrix()
                    colorMatrix.setSaturation(1f)
                    filter = ColorMatrixColorFilter(colorMatrix)
                    binding?.gamificationBadgeIv?.colorFilter = filter
                    binding?.gamificationBadgeIv!!.buildRotationAnimator(2000).apply {
                        addListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator?) {
                                super.onAnimationEnd(animation)
                                binding?.newBadgeLabel?.visibility = View.VISIBLE
                                val listener = object : Animator.AnimatorListener {
                                    override fun onAnimationRepeat(animation: Animator?) {
                                    }

                                    override fun onAnimationEnd(animation: Animator?) {
                                        animate().translationY(60f).alpha(0f).setStartDelay(600).start()
                                    }

                                    override fun onAnimationCancel(animation: Animator?) {
                                    }

                                    override fun onAnimationStart(animation: Animator?) {
                                    }
                                }
                                val animator =  binding?.newBadgeLabel?.buildScaleAnimator(0f, 1f, 300)
                                animator?.addListener(listener)
                                animator?.start()
                            }
                        })
                    }.start()
                },
                500
            )
        } else {
            binding?.newBadgeLabel?.visibility = View.GONE
        }
        ValueAnimator.ofInt(currentPointsForNextBadge, currentPointsForNextBadge + newPoints)
            .apply {
                addUpdateListener {
                    progression = it.animatedValue as Int
                }
                duration = 1000
                start()
            }
        val startPercentage = (currentPointsForNextBadge / totalPointsToNextbadge.toFloat()) * 100
        val endPercentage = min(
            100f,
            ((currentPointsForNextBadge + newPoints) / totalPointsToNextbadge.toFloat()) * 100
        )
        ValueAnimator.ofInt(
            ((startPercentage * AndroidResource.dpToPx(100)) / 100).toInt(),
            (endPercentage.toInt() * AndroidResource.dpToPx(100) / 100).toInt()
        ).apply {
            addUpdateListener {
                val layoutParams = binding?.progressionMeterProgressView?.layoutParams
                layoutParams?.width = it.animatedValue as Int
                binding?.progressionMeterProgressView?.layoutParams = layoutParams
            }
            duration = 1000
            start()
        }
//        Add transition out using transition choreography api(TransitionSet),
//        Also we can add some kotlin's DSL from here : https://github.com/arunkumar9t2/transition-x
    }
}
