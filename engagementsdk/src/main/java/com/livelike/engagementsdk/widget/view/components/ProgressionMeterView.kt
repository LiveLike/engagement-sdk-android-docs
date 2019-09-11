package com.livelike.engagementsdk.widget.view.components

import android.animation.ValueAnimator
import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.FitCenter
import com.bumptech.glide.request.RequestOptions
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.utils.AndroidResource
import com.livelike.engagementsdk.widget.view.clipParents
import kotlin.math.min
import kotlinx.android.synthetic.main.atom_gamification_progression_meter.view.gamification_badge_iv
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
        ConstraintLayout.inflate(context, R.layout.atom_gamification_progression_meter, this)
        visibility = View.GONE
    }

    fun animatePointsBadgeProgression(currentPoints: Int, newPoints: Int, totalPointsNextBadge: Int, badgeIconURL: String) {
        visibility = View.VISIBLE
        clipChildren = false
        clipToPadding = false
        clipParents(false)

        Glide.with(context)
            .load(badgeIconURL)
            .apply(
                RequestOptions().override(AndroidResource.dpToPx(30), AndroidResource.dpToPx(30))
                    .transform(FitCenter())
            )
            .into(gamification_badge_iv)
        totalPointsToNextbadge = totalPointsNextBadge

        ValueAnimator.ofInt(currentPoints, currentPoints + newPoints).apply {
            addUpdateListener {
                progression = it.animatedValue as Int
            }
            duration = 500
            start()
        }
        val startPercentage = (currentPoints / totalPointsToNextbadge) * 100
        val endPercentage = min(100, ((currentPoints + newPoints) / totalPointsToNextbadge) *100)
        ValueAnimator.ofInt(AndroidResource.dpToPx(startPercentage*AndroidResource.dpToPx(115)),
            AndroidResource.dpToPx(endPercentage*AndroidResource.dpToPx(115))).apply {
            addUpdateListener {
                progression_meter_progress_view.layoutParams.width = it.animatedValue as Int
                progression_meter_progress_view.layoutParams = progression_meter_progress_view.layoutParams
            }
            duration = 500
            start()
        }
    }
}
