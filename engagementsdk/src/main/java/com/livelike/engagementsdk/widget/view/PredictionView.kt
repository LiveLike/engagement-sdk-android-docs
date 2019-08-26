package com.livelike.engagementsdk.widget.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.utils.AndroidResource
import com.livelike.engagementsdk.widget.SpecifiedWidgetView
import com.livelike.engagementsdk.widget.adapters.WidgetOptionsViewAdapter
import com.livelike.engagementsdk.widget.viewModel.PredictionViewModel
import com.livelike.engagementsdk.widget.viewModel.PredictionWidget
import com.livelike.engagementsdk.widget.viewModel.WidgetViewModel
import kotlinx.android.synthetic.main.widget_text_option_selection.view.confirmationMessage
import kotlinx.android.synthetic.main.widget_text_option_selection.view.followupAnimation
import kotlinx.android.synthetic.main.widget_text_option_selection.view.pointView
import kotlinx.android.synthetic.main.widget_text_option_selection.view.textEggTimer
import kotlinx.android.synthetic.main.widget_text_option_selection.view.textRecyclerView
import kotlinx.android.synthetic.main.widget_text_option_selection.view.titleView

class PredictionView(context: Context, attr: AttributeSet? = null) : SpecifiedWidgetView(context, attr) {

    private var viewModel: PredictionViewModel? = null

    private var inflated = false

    override var dismissFunc: ((action: DismissAction) -> Unit)? = { viewModel?.dismissWidget(it) }

    override var widgetViewModel: WidgetViewModel? = null
        get() = super.widgetViewModel
        set(value) {
            field = value
            viewModel = value as PredictionViewModel
            viewModel?.data?.subscribe(javaClass) { widgetObserver(it) }
            viewModel?.state?.subscribe(javaClass) { stateObserver(it) }
        }

    // Refresh the view when re-attached to the activity
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewModel?.data?.subscribe(javaClass) { widgetObserver(it) }
        viewModel?.state?.subscribe(javaClass) { stateObserver(it) }
    }

    private fun widgetObserver(widget: PredictionWidget?) {
        widget?.apply {
            val optionList = resource.getMergedOptions() ?: return
            if (!inflated) {
                inflated = true
                inflate(context, R.layout.widget_text_option_selection, this@PredictionView)
            }

            titleView.title = resource.question
            titleView.background = R.drawable.header_rounded_corner_prediciton

            viewModel?.adapter = viewModel?.adapter ?: WidgetOptionsViewAdapter(
                optionList,
                {
                    viewModel?.onOptionClicked()
                },
                widget.type,
                resource.correct_option_id,
                (if (resource.text_prediction_id.isNullOrEmpty()) resource.image_prediction_id else resource.text_prediction_id) ?: ""
            )

            textRecyclerView.apply {
                this.adapter = viewModel?.adapter
                setHasFixedSize(true)
            }

            val isFollowUp = resource.kind.contains("follow-up")
            viewModel?.startDismissTimout(resource.timeout, isFollowUp)

            val animationLength = AndroidResource.parseDuration(resource.timeout).toFloat()
            if (viewModel?.animationEggTimerProgress!! < 1f && !isFollowUp) {
                listOf(textEggTimer).forEach { v ->
                    viewModel?.animationEggTimerProgress?.let { time ->
                        v?.startAnimationFrom(time, animationLength, {
                            viewModel?.animationEggTimerProgress = it
                        }) {
                        }
                    }
                }
            }
        }

        if (widget == null) {
            inflated = false
            removeAllViews()
            parent?.let { (it as ViewGroup).removeAllViews() }
        }
    }

    private fun stateObserver(state: String?) {
        when (state) {
            "confirmation" -> {
                titleView?.alpha = 0.2f
                textEggTimer?.alpha = 0.2f
                textRecyclerView?.alpha = 0.2f
                confirmationMessage?.apply {
                    text = viewModel?.data?.currentData?.resource?.confirmation_message ?: ""
                    viewModel?.animationPath?.let { viewModel?.animationProgress?.let { it1 -> startAnimation(it, it1) } }
                    subscribeToAnimationUpdates { value ->
                        viewModel?.animationProgress = value
                    }
                    visibility = View.VISIBLE
                }
                listOf(textEggTimer).forEach { it?.showCloseButton() {
                    viewModel?.dismissWidget(it)
                } }
                viewModel?.points?.let { pointView.startAnimation(it) }
            }
            "followup" -> {
                followupAnimation?.apply {
                    setAnimation(viewModel?.animationPath)
                    progress = viewModel?.animationProgress!!
                    addAnimatorUpdateListener { valueAnimator ->
                        viewModel?.animationProgress = valueAnimator.animatedFraction
                    }
                    if (progress != 1f) {
                        resumeAnimation()
                    }
                    visibility = View.VISIBLE
                }
                listOf(textEggTimer).forEach { it?.showCloseButton() {
                    viewModel?.dismissWidget(it)
                } }

                viewModel?.points?.let { pointView.startAnimation(it) }
            }
        }
    }
}
