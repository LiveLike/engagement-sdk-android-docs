package com.livelike.engagementsdk.widget.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.animation.LinearInterpolator
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.core.utils.AndroidResource
import com.livelike.engagementsdk.core.utils.animators.buildRotationAnimator
import com.livelike.engagementsdk.core.utils.animators.buildScaleAnimator
import com.livelike.engagementsdk.core.utils.animators.buildTranslateYAnimator
import com.livelike.engagementsdk.databinding.WidgetGamificationCollectBadgeBinding
import com.livelike.engagementsdk.widget.SpecifiedWidgetView
import com.livelike.engagementsdk.widget.data.models.Badge
import com.livelike.engagementsdk.widget.viewModel.BaseViewModel
import com.livelike.engagementsdk.widget.viewModel.CollectBadgeWidgetViewModel


class CollectBadgeWidgetView(context: Context, attr: AttributeSet? = null) :
    SpecifiedWidgetView(context, attr) {

    private var viewModel: CollectBadgeWidgetViewModel? = null
    override var widgetViewModel: BaseViewModel? = null
        set(value) {
            field = value
            viewModel = value as CollectBadgeWidgetViewModel
            viewModel?.run {
                startInteractionTimeout(5000) {
                    removeAllViews()
                }
                animateView(badge)
            }
        }
    private var binding:WidgetGamificationCollectBadgeBinding? = null

    init {
        binding = WidgetGamificationCollectBadgeBinding.inflate(LayoutInflater.from(context), this@CollectBadgeWidgetView, true)

    }

    private fun animateView(badge: Badge) {
        clipParents(false)
        binding?.badgeIv?.loadImage(badge.imageFile, AndroidResource.dpToPx(80))
        binding?.badgeNameTv?.text = badge.name
        binding?.badgeIv!!.buildRotationAnimator(1000).start()
        buildScaleAnimator(0f, 1f, 1000).start()
        binding?.collectBadgeButton?.setOnClickListener {
            viewModel?.let {
                it.analyticsService.trackBadgeCollectedButtonPressed(it.badge.id, it.badge.level)
            }
            choreogaphBadgeCollection()
        }
    }

    private fun choreogaphBadgeCollection() {
        binding?.collectBadgeButton?.animate()?.alpha(0f)?.setDuration(500)?.start()
        binding?.collectBadgeBox?.animate()?.alpha(0f)?.setDuration(500)?.start()
        //            badge_iv.animate().setDuration(500).setStartDelay(500)
        //                .translationY(badge_iv.translationY + collect_badge_box.image_height/2)
        val badgeTranslateDownCenter =  binding?.badgeIv?.buildTranslateYAnimator(
            100,
            binding?.badgeIv?.translationY,
            binding?.badgeIv!!.translationY + binding?.collectBadgeBox!!.height / 2,
            LinearInterpolator()
        )
        val badgeScaleUp = binding?.badgeIv?.buildScaleAnimator(1f, 1.5f, 1200)

        val badgeTranslateDownBox = binding?.badgeIv?.buildTranslateYAnimator(
            300,
            binding?.badgeIv!!.translationY + binding?.collectBadgeBox!!.height / 2,
            binding?.badgeIv!!.translationY + binding?.collectBadgeBox!!.height / 2 + AndroidResource.dpToPx(80),
            LinearInterpolator()
        )
        val badgeScaleDown = binding?.badgeIv?.buildScaleAnimator(1.5f, 0f, 300, LinearInterpolator())

        val animatorSet = AnimatorSet().apply {
            startDelay = 500
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(p0: Animator?) {
                    clipParents(true)
                    dismissFunc?.invoke(DismissAction.TIMEOUT)
                }
            })
        }
        animatorSet.play(badgeTranslateDownCenter).with(badgeScaleUp)
            .before(
                AnimatorSet().apply {
                    startDelay = 1000
                    playTogether(badgeTranslateDownBox, badgeScaleDown)
                }
            )
        animatorSet.start()
    }

    override var dismissFunc: ((action: DismissAction) -> Unit)? =
        {
            viewModel?.dismissWidget(it)
            removeAllViews()
        }
}
