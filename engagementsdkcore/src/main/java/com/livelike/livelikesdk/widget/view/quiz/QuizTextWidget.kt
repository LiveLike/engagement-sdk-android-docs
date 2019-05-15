// package com.livelike.livelikesdk.widget.view.quiz
//
// import android.content.Context
// import android.os.Handler
// import android.support.v7.content.res.AppCompatResources
// import android.util.AttributeSet
// import android.view.View
// import com.livelike.engagementsdkapi.WidgetTransientState
// import com.livelike.livelikesdk.R
// import com.livelike.livelikesdk.animation.ViewAnimationManager
// import com.livelike.livelikesdk.widget.model.VoteOption
// import com.livelike.livelikesdk.widget.view.prediction.text.TextOptionWidgetBase
// import kotlinx.android.synthetic.main.prediction_text_widget.view.questionTextView
//
// internal class QuizTextWidget : TextOptionWidgetBase {
//    constructor(context: Context?) : super(context)
//    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
//    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
//
//    private lateinit var viewAnimation: ViewAnimationManager
//    private var fetchResult: (() -> Unit)? = null
//
//    fun initialize(
//        dismiss: () -> Unit,
//        startingState: WidgetTransientState,
//        progressedState: WidgetTransientState,
//        fetch: () -> Unit,
//        parentWidth: Int,
//        viewAnimation: ViewAnimationManager,
//        state: (WidgetTransientState) -> Unit
//    ) {
//        val selectedButtonDrawable =
//                AppCompatResources.getDrawable(context, com.livelike.livelikesdk.R.drawable.button_quiz_answer_outline)
//        questionTextView.background =
//                AppCompatResources.getDrawable(
//                    context,
//                    com.livelike.livelikesdk.R.drawable.quiz_textview_rounded_corner
//                )
//
//        fetchResult = fetch
//        this.viewAnimation = viewAnimation
// //        startWidgetAnimation()
//    }
//
// //    private fun startWidgetAnimation() {
// //        if (currentPhase == WidgetTransientState.Phase.INTERACTION) {
// //            pieTimerViewStub.layoutResource = R.layout.pie_timer
// //            val pieTimer = pieTimerViewStub.inflate()
// //            startingState.phaseTimeouts[WidgetTransientState.Phase.INTERACTION]?.let {
// //                startPieTimer(pieTimer, it)
// //                startingState.phaseTimeouts[WidgetTransientState.Phase.RESULT]?.let { it2 ->
// //                    Handler().postDelayed({
// //                        future.cancel(false)
// //                        dismissWidget?.invoke() }, it + it2)
// //                }
// //            }
// //        } else {
// //            pieTimerViewStub.layoutResource = R.layout.cross_image
// //            pieTimerViewStub.inflate()
// //            showResults = true
// //        }
// //    }
// //
// //    private fun startPieTimer(pieTimer: View, timeout: Long) {
// //        viewAnimation.startTimerAnimation(pieTimer, timeout, startingState, {
// //            fetchResult?.invoke()
// //            showResults = true
// //            currentPhase = WidgetTransientState.Phase.RESULT
// //            progressedState.currentPhase = currentPhase
// //            progressedStateCallback.invoke(progressedState)
// //
// //            buttonClickEnabled = false
// //        }, {
// //            progressedState.timerAnimatorStartPhase = it
// //            progressedStateCallback.invoke(progressedState)
// //        })
// //    }
//
//    override fun optionListUpdated(
//        voteOptions: List<VoteOption>,
//        optionSelectedCallback: (String?) -> Unit,
//        correctOptionWithUserSelection: Pair<String?, String?>
//    ) {
//        super.optionListUpdated(voteOptions, optionSelectedCallback, correctOptionWithUserSelection)
//        if (showResults) {
//            super.showResultsAnimation(correctOptionWithUserSelection)
// //            startingState.phaseTimeouts[WidgetTransientState.Phase.RESULT]?.let {
// //                Handler().postDelayed({
// //                    future.cancel(false)
// //                    dismissWidget?.invoke() }, it)
// //            }
//        }
//    }
// }