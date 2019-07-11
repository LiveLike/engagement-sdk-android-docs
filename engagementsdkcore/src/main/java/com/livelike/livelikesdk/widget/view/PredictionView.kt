package com.livelike.livelikesdk.widget.view

import android.content.Context
import android.support.v7.widget.LinearLayoutManager
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.utils.AndroidResource
import com.livelike.livelikesdk.widget.DismissAction
import com.livelike.livelikesdk.widget.SpecifiedWidgetView
import com.livelike.livelikesdk.widget.adapters.WidgetOptionsViewAdapter
import com.livelike.livelikesdk.widget.util.SpanningLinearLayoutManager
import com.livelike.livelikesdk.widget.viewModel.PredictionViewModel
import com.livelike.livelikesdk.widget.viewModel.PredictionWidget
import com.livelike.livelikesdk.widget.viewModel.WidgetViewModel
import kotlinx.android.synthetic.main.widget_image_option_selection.view.imageEggTimer
import kotlinx.android.synthetic.main.widget_text_option_selection.view.confirmationMessage
import kotlinx.android.synthetic.main.widget_text_option_selection.view.followupAnimation
import kotlinx.android.synthetic.main.widget_text_option_selection.view.textEggTimer
import kotlinx.android.synthetic.main.widget_text_option_selection.view.textRecyclerView
import kotlinx.android.synthetic.main.widget_text_option_selection.view.titleView

class PredictionView(context: Context, attr: AttributeSet? = null) : SpecifiedWidgetView(context, attr) {

    override var dismissFunc: ((action: DismissAction) -> Unit)? = { viewModel?.dismissWidget(it) }

    private var viewModel: PredictionViewModel? = null

    private var viewManager: LinearLayoutManager =
        LinearLayoutManager(context).apply { orientation = LinearLayout.VERTICAL }
    private var inflated = false

    override var widgetViewModel: WidgetViewModel? = null
        get() = super.widgetViewModel
        set(value) {
            field = value
            viewModel = value as PredictionViewModel
            viewModel?.data?.subscribe(javaClass) { widgetObserver(it) }
            viewModel?.state?.subscribe(javaClass) { stateObserver(it) }
        }

    private fun widgetObserver(widget: PredictionWidget?) {
        widget?.apply {
            val optionList = resource.getMergedOptions() ?: return
            val isImageWidget = optionList.elementAtOrNull(0)?.image_url?.isNotEmpty() ?: false
            if (!inflated) {
                inflated = true
                if (isImageWidget) {
                    inflate(context, R.layout.widget_image_option_selection, this@PredictionView)
                } else {
                    inflate(context, R.layout.widget_text_option_selection, this@PredictionView)
                }
            }
            if (isImageWidget) {
                if (optionList.size > 3) {
                    viewManager =
                        LinearLayoutManager(context).apply { orientation = LinearLayout.HORIZONTAL }
                } else {
                    viewManager =
                        SpanningLinearLayoutManager(context)
                            .apply { orientation = LinearLayout.HORIZONTAL }
                }
            }

            titleView.title = resource.question
            titleView.background = R.drawable.prediciton_rounded_corner

            viewModel?.adapter = viewModel?.adapter ?: WidgetOptionsViewAdapter(
                optionList,
                {
                    viewModel?.onOptionClicked()
                },
                widget.type,
                resource.correct_option_id,
                if (resource.text_prediction_id.isNullOrEmpty()) resource.image_prediction_id else resource.text_prediction_id
            )

            textRecyclerView.apply {
                this.layoutManager = viewManager
                this.adapter = viewModel?.adapter
                setHasFixedSize(true)
            }

            val isFollowUp = resource.correct_option_id.isNotEmpty()
            viewModel?.startDismissTimout(resource.timeout, isFollowUp)

            val animationLength = AndroidResource.parseDuration(resource.timeout).toFloat()
            if (viewModel?.animationEggTimerProgress!! < 1f && !isFollowUp) {
                listOf(textEggTimer, imageEggTimer).forEach { v ->
                    viewModel?.animationEggTimerProgress?.let {
                        v?.startAnimationFrom(it, animationLength, {
                            viewModel?.animationEggTimerProgress = it
                        }) {
                        }
                    }
                }
            }
        }

        if (widget == null) {
            inflated = false
        }
    }

    private fun stateObserver(state: String?) {
        when (state) {
            "confirmation" -> {
                confirmationMessage.apply {
                    text = viewModel?.data?.currentData?.resource?.confirmation_message ?: ""
                    viewModel?.animationPath?.let { viewModel?.animationProgress?.let { it1 -> startAnimation(it, it1) } }
                    subscribeToAnimationUpdates { value ->
                        viewModel?.animationProgress = value
                    }
                    visibility = View.VISIBLE
                }
                listOf(textEggTimer, imageEggTimer).forEach { it?.showCloseButton() {
                    viewModel?.dismissWidget(it)
                } }
            }
            "followup" -> {
                followupAnimation.apply {
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
                listOf(textEggTimer, imageEggTimer).forEach { it?.showCloseButton() {
                    viewModel?.dismissWidget(it)
                } }
            }
        }
    }
}
