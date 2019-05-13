//package com.livelike.livelikesdk.widget.view.prediction.text
//
//import android.content.Context
//import android.os.Handler
//import android.util.AttributeSet
//import android.widget.ImageView
//import com.livelike.engagementsdkapi.WidgetTransientState
//import com.livelike.livelikesdk.R
//import com.livelike.livelikesdk.animation.ViewAnimationManager
//import com.livelike.livelikesdk.widget.model.VoteOption
//
//internal class PredictionTextFollowUpWidgetView :
//    TextOptionWidgetBase {
//    constructor(context: Context?) : super(context)
//    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
//    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
//
//    private lateinit var viewAnimation: ViewAnimationManager
//    private var timeout = 0L
//
//    fun initialize(
//        dismiss: () -> Unit,
//        startingState: WidgetTransientState,
//        progressedState: WidgetTransientState,
//        fetch: () -> Unit,
//        parentWidth: Int,
//        viewAnimation: ViewAnimationManager,
//        progressedStateCallback: (WidgetTransientState) -> Unit
//    ) {
//        showResults = true
//        buttonClickEnabled = false
//        this.viewAnimation = viewAnimation
//        pieTimerViewStub.layoutResource = R.layout.cross_image
//        pieTimerViewStub.inflate()
//        val imageView = findViewById<ImageView>(R.id.prediction_followup_image_cross)
//        imageView.setImageResource(R.mipmap.widget_ic_x)
//        imageView.setOnClickListener { dismissWidget() }
//        this.timeout = startingState.widgetTimeout
//    }
//
//    override fun optionListUpdated(
//        voteOptions: List<VoteOption>,
//        optionSelectedCallback: (String?) -> Unit,
//        correctOptionWithUserSelection: Pair<String?, String?>
//    ) {
//        super.optionListUpdated(voteOptions, optionSelectedCallback, correctOptionWithUserSelection)
//        super.showResultsAnimation(correctOptionWithUserSelection)
//        transitionAnimation()
//    }
//
//    private fun transitionAnimation() {
//        viewAnimation.startWidgetTransitionInAnimation {
//            //            viewAnimation.startResultAnimation(lottieAnimationPath, context, prediction_result, {
////                transientState.pieTimerProgress = it
////                state.invoke(transientState)
////            }, {
////                transientState.resultPath = it
////                state.invoke(transientState)
////            })
//            viewAnimation.startWidgetTransitionInAnimation {
//            }
//            Handler().postDelayed({ dismissWidget?.invoke() }, timeout)
//        }
//    }
//}
