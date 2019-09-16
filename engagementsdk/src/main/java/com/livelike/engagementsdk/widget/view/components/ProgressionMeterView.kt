package com.livelike.engagementsdk.widget.view.components

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.livelike.engagementsdk.utils.AndroidResource
import com.livelike.engagementsdk.widget.view.clipParents
import com.livelike.engagementsdk.widget.view.loadImage
import kotlin.math.min
import kotlinx.android.synthetic.main.atom_gamification_progression_meter.view.gamification_badge_iv
import kotlinx.android.synthetic.main.atom_gamification_progression_meter.view.new_badge_label
import kotlinx.android.synthetic.main.atom_gamification_progression_meter.view.progression_meter_progress_view
import kotlinx.android.synthetic.main.atom_gamification_progression_meter.view.progression_meter_text

class ProgressionMeterView(context: Context, attr: AttributeSet) : FrameLayout(context, attr) {

    private var progression: Int = 0
        set(value) {
            field = value
            progression_meter_text.text = "$value/$totalPointsToNextbadge"
        }

    private var totalPointsToNextbadge: Int = 0

    init {
        ConstraintLayout.inflate(context, com.livelike.engagementsdk.R.layout.atom_gamification_progression_meter, this)
        visibility = View.GONE
    }

    fun animatePointsBadgeProgression(currentPointsForNextBadge: Int, newPoints: Int, totalPointsNextBadge: Int, badgeIconURL: String) {
        visibility = View.VISIBLE
        clipChildren = false
        clipToPadding = false
        clipParents(false)

        gamification_badge_iv.loadImage(badgeIconURL, AndroidResource.dpToPx(30))
        totalPointsToNextbadge = totalPointsNextBadge

        var colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(0f)
        var filter = ColorMatrixColorFilter(colorMatrix)
        gamification_badge_iv.colorFilter = filter
        gamification_badge_iv.invalidate()

        val newBadgeEarned = totalPointsToNextbadge <= currentPointsForNextBadge + newPoints
        if (newBadgeEarned) {
            gamification_badge_iv.postDelayed({
                colorMatrix = ColorMatrix()
                colorMatrix.setSaturation(1f)
                filter = ColorMatrixColorFilter(colorMatrix)
                gamification_badge_iv.colorFilter = filter
                gamification_badge_iv.invalidate()
                new_badge_label.visibility = View.VISIBLE
            }, 500)
        } else {
            new_badge_label.visibility = View.GONE
        }

        ValueAnimator.ofInt(currentPointsForNextBadge, currentPointsForNextBadge + newPoints).apply {
            addUpdateListener {
                progression = it.animatedValue as Int
            }
            duration = 1000
            start()
        }
        val startPercentage = (currentPointsForNextBadge / totalPointsToNextbadge.toFloat()) * 100
        val endPercentage = min(100f, ((currentPointsForNextBadge + newPoints) / totalPointsToNextbadge.toFloat()) *100)
        ValueAnimator.ofInt(
            ((startPercentage * AndroidResource.dpToPx(100)) / 100).toInt(),
            (endPercentage.toInt() * AndroidResource.dpToPx(100) / 100).toInt()
        ).apply {
            addUpdateListener {
                val layoutParams = progression_meter_progress_view.layoutParams
                layoutParams.width = it.animatedValue as Int
                progression_meter_progress_view.layoutParams = layoutParams
            }
            duration = 1000
            start()
        }
    }
}
