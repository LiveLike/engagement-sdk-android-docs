//package com.livelike.livelikesdk.animation
//
//import android.animation.Animator
//import android.animation.ValueAnimator
//import android.os.Handler
//import android.util.Log
//import android.view.View
//import android.widget.Button
//import kotlinx.android.synthetic.main.prediction_text_widget.view.*
//
//class PieTimeView {
//    init {
//
//    }
//
//    private fun performTimerAnimation() {
//        bindListenerToTimerAnimation()
//        prediction_pie_updater_animation.playAnimation()
//        val animator = ValueAnimator.ofFloat(0f, 1f)
//        animator.duration = widgetDismissDuration
//
//        animator.addUpdateListener { animation ->
//            prediction_pie_updater_animation.progress = animation.animatedValue as Float
//        }
//        animator.start()
//
//        bindListenerToConfirmMessageAnimation()
//    }
//
//    private fun bindListenerToTimerAnimation() {
//        prediction_pie_updater_animation.addAnimatorListener(object : Animator.AnimatorListener {
//            override fun onAnimationStart(animation: Animator) { Log.e("Animation:", "start") }
//            override fun onAnimationEnd(animation: Animator) {
//                Log.e("Animation:", "end")
//                if (optionSelected) {
//                    prediction_confirm_message_textView.visibility = View.VISIBLE
//                    prediction_confirm_message_animation.visibility = View.VISIBLE
//                    prediction_confirm_message_animation.playAnimation()
//                    performPredictionWidgetFadeOutOperations()
//                    Handler().postDelayed({ this@PredictionWidget.visibility = View.INVISIBLE }, widgetDismissDuration)
//                } else hideWidget()
//            }
//
//            fun performPredictionWidgetFadeOutOperations() {
//                buttonList.forEach { button ->
//                    disableButtons(button)
//                    setViewTranslucent(button)
//                }
//                setViewTranslucent(prediction_question_textView)
//                setViewTranslucent(prediction_pie_updater_animation)
//            }
//
//            fun setViewTranslucent(view: View) { view.alpha = widgetOpacityFactor }
//            fun disableButtons(button: Button) { button.isEnabled = false }
//            override fun onAnimationCancel(animation: Animator) { Log.e("Animation:", "cancel") }
//            override fun onAnimationRepeat(animation: Animator) { Log.e("Animation:", "repeat") }
//        })
//    }
//
//    private fun bindListenerToConfirmMessageAnimation() {
//        prediction_confirm_message_animation.addAnimatorListener(object : Animator.AnimatorListener {
//            override fun onAnimationStart(animation: Animator) { Log.e("Animation:", "start") }
//            override fun onAnimationEnd(animation: Animator) {
//                Log.e("Animation:", "end")
//                Handler().postDelayed({ hideWidget() }, 3000)
//            }
//
//            override fun onAnimationCancel(animation: Animator) { Log.e("Animation:", "cancel") }
//            override fun onAnimationRepeat(animation: Animator) { Log.e("Animation:", "repeat") }
//        })
//    }
//
//    private fun hideWidget() {
//        layout.visibility = View.INVISIBLE
//    }
//}