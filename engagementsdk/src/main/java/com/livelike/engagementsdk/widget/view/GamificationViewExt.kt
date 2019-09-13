package com.livelike.engagementsdk.widget.view

import com.livelike.engagementsdk.data.models.Badge
import com.livelike.engagementsdk.data.models.ProgramGamificationProfile
import com.livelike.engagementsdk.data.models.RewardsType
import com.livelike.engagementsdk.widget.SpecifiedWidgetView
import com.livelike.engagementsdk.widget.view.components.ProgressionMeterView

/**
 * extensions related to gamification views like progression meter
 */

/** TODO later this extension to more concrete widgetView so we pass less params here*/
fun SpecifiedWidgetView.wouldShowProgressionMeter(
    rewardsType: RewardsType?,
    latest: ProgramGamificationProfile?,
    progressionMeterView: ProgressionMeterView
) {
    latest?.let {
        if (rewardsType!! == RewardsType.BADGES) {
            val nextBadgeToDisplay: Badge? = if (it.newBadges == null || it.newBadges.isEmpty()) {
                it.nextBadge
            } else {
                it.newBadges?.max()!!
            }
            nextBadgeToDisplay?.let { nextBadgeToDisplay ->
                progressionMeterView.animatePointsBadgeProgression(
                    it.points - it.newPoints,
                    it.newPoints,
                    nextBadgeToDisplay.points,
                    nextBadgeToDisplay.imageFile
                )
            }
        }
    }
}
