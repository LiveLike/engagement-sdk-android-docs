package com.livelike.engagementsdk.widget.view.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.core.data.models.RewardsType
import com.livelike.engagementsdk.databinding.AtomWidgetPointsTutorialBinding
import com.livelike.engagementsdk.widget.SpecifiedWidgetView
import com.livelike.engagementsdk.widget.utils.livelikeSharedPrefs.getTotalPoints
import com.livelike.engagementsdk.widget.utils.livelikeSharedPrefs.pointTutorialSeen
import com.livelike.engagementsdk.widget.view.wouldShowProgressionMeter
import com.livelike.engagementsdk.widget.viewModel.BaseViewModel
import com.livelike.engagementsdk.widget.viewModel.PointTutorialWidgetViewModel


class PointsTutorialView(context: Context, attr: AttributeSet? = null) :
    SpecifiedWidgetView(context, attr) {

    private var viewModel: PointTutorialWidgetViewModel? = null
    override var widgetViewModel: BaseViewModel? = null
        set(value) {
            field = value
            viewModel = value as PointTutorialWidgetViewModel
            viewModel?.run {
                startInteractionTimeout(5000) {
                    removeAllViews()
                }
                binding?.pointsAnimation?.playAnimation()
                binding?.pointsTutoView?.startAnimation(getTotalPoints())

                if (rewardType == RewardsType.BADGES) {
                    postDelayed(
                        {
                           binding?.pointsProgressionMeterSwitcher?.showNext()
                            binding?.progressionMeterView?.visibility = View.GONE
                            wouldShowProgressionMeter(
                                rewardType,
                                programGamificationProfile,
                                binding?.progressionMeterView!!
                            )
                        },
                        1300
                    )
                }
            }
        }

    private var binding:AtomWidgetPointsTutorialBinding? = null

    init {
        pointTutorialSeen()
        binding = AtomWidgetPointsTutorialBinding.inflate(LayoutInflater.from(context), this@PointsTutorialView, true)
    }

    override var dismissFunc: ((action: DismissAction) -> Unit)? =
        {
            viewModel?.dismissWidget(it)
            removeAllViews()
        }
}
