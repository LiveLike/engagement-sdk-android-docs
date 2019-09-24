package com.livelike.engagementsdk.utils.animators

import android.animation.Keyframe
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.view.View
import android.view.ViewPropertyAnimator
import android.view.animation.BounceInterpolator
import kotlinx.android.synthetic.main.chat_user_profile_bar.view.gamification_badge_iv

/**
 * All builders related to building view and property animations
 * Refer for here more animators :
 * https://developer.android.com/guide/topics/graphics/prop-animation, https://www.programcreek.com/java-api-examples/index.php?api=android.animation.PropertyValuesHolder
 */
internal fun View.buildScaleAnimator(fromScale: Float, toScale: Float, duration:Long): ObjectAnimator {

    val kf0 = Keyframe.ofFloat(0f, fromScale)
    val kf1 = Keyframe.ofFloat(1f, toScale)
    val scaleX = PropertyValuesHolder.ofKeyframe("scaleX", kf0, kf1)
    val scaleY = PropertyValuesHolder.ofKeyframe("scaleY", kf0, kf1)
    val scaleAnimation =
        ObjectAnimator.ofPropertyValuesHolder(gamification_badge_iv, scaleX, scaleY)
    scaleAnimation.interpolator = BounceInterpolator()
//    TODO Debug later why BounceEaseOut shared by shu not workin for now added BounceInterpolator
//    Try using Bounce_out from https://github.com/MasayukiSuda/EasingInterpolator
//                scaleAnimation.setEvaluator(BounceEaseOut(duration))
    scaleAnimation.duration = duration
    return scaleAnimation
}
//                scaleAnimation.setEvaluator(BounceEaseOut(duration))
    scaleAnimation.duration = duration
    return scaleAnimation
}