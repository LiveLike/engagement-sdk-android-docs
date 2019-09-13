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

    fun animatePointsBadgeProgression(currentPoints: Int, newPoints: Int, totalPointsNextBadge: Int, badgeIconURL: String) {
        visibility = View.VISIBLE
        clipChildren = false
        clipToPadding = false
        clipParents(false)

        gamification_badge_iv.loadImage(badgeIconURL, AndroidResource.dpToPx(30))
        totalPointsToNextbadge = totalPointsNextBadge

        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(0f)
        val filter = ColorMatrixColorFilter(colorMatrix)
        gamification_badge_iv.colorFilter = filter

        val newBadgeEarned = totalPointsToNextbadge == currentPoints + newPoints
        if (newBadgeEarned) {
            new_badge_label.visibility = View.VISIBLE
            gamification_badge_iv.postDelayed({
                val colorMatrix = ColorMatrix()
                colorMatrix.setSaturation(1f)
                val filter = ColorMatrixColorFilter(colorMatrix)
                gamification_badge_iv.colorFilter = filter
            }, 1000)
        } else {
            new_badge_label.visibility = View.GONE
        }

        ValueAnimator.ofInt(currentPoints, currentPoints + newPoints).apply {
            addUpdateListener {
                progression = it.animatedValue as Int
            }
            duration = 1000
            start()
        }
        val startPercentage = (currentPoints / totalPointsToNextbadge) * 100
        val endPercentage = min(100, ((currentPoints + newPoints) / totalPointsToNextbadge) *100)
        ValueAnimator.ofInt(AndroidResource.dpToPx(startPercentage*AndroidResource.dpToPx(100)),
            AndroidResource.dpToPx(endPercentage*AndroidResource.dpToPx(100))).apply {
            addUpdateListener {
                progression_meter_progress_view.layoutParams.width = it.animatedValue as Int
                progression_meter_progress_view.layoutParams = progression_meter_progress_view.layoutParams
            }
            duration = 1000
            start()
        }
    }
}
