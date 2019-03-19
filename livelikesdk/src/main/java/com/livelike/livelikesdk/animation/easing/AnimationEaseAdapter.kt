package com.livelike.livelikesdk.animation.easing

import android.animation.ValueAnimator
import com.daimajia.easing.Glider
import com.daimajia.easing.Skill
import com.livelike.livelikesdk.animation.easing.AnimationEaseInterpolator.Ease

/**
 * Adapter layer for keeping the Easing library at the system boundary.
 */
internal class AnimationEaseAdapter : AnimationEaseInterpolator {

    override fun createAnimationEffectWith(ease: AnimationEaseInterpolator.Ease,
                                           forDuration: Float,
                                           animator: ValueAnimator): ValueAnimator {
        return when (ease) {
            Ease.EaseInBounce -> {
                Glider.glide(Skill.BounceEaseIn, forDuration, animator)
            }

            Ease.EaseInElastic -> {
                Glider.glide(Skill.ElasticEaseIn, forDuration, animator)
            }

            Ease.EaseOutElastic -> {
                Glider.glide(Skill.ElasticEaseOut, forDuration, animator)
            }

            Ease.EaseOutQuad -> {
                Glider.glide(Skill.QuadEaseOut, forDuration, animator)
            }

            Ease.EaseOutCubic -> {
                Glider.glide(Skill.CubicEaseOut, forDuration, animator)
            }
        }
    }
}