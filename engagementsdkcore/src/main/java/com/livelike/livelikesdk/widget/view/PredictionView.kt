package com.livelike.livelikesdk.widget.view

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.support.constraint.ConstraintLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.utils.AndroidResource
import com.livelike.livelikesdk.widget.adapters.WidgetOptionsViewAdapter
import com.livelike.livelikesdk.widget.util.SpanningLinearLayoutManager
import com.livelike.livelikesdk.widget.view.components.EggTimerView
import com.livelike.livelikesdk.widget.viewModel.PredictionViewModel
import com.livelike.livelikesdk.widget.viewModel.PredictionWidget
import kotlinx.android.synthetic.main.widget_image_option_selection.view.imageCloseButton
import kotlinx.android.synthetic.main.widget_image_option_selection.view.imageEggTimer
import kotlinx.android.synthetic.main.widget_text_option_selection.view.confirmationMessage
import kotlinx.android.synthetic.main.widget_text_option_selection.view.followupAnimation
import kotlinx.android.synthetic.main.widget_text_option_selection.view.textCloseButton
import kotlinx.android.synthetic.main.widget_text_option_selection.view.textEggTimer
import kotlinx.android.synthetic.main.widget_text_option_selection.view.textRecyclerView
import kotlinx.android.synthetic.main.widget_text_option_selection.view.titleView

class PredictionView(context: Context, attr: AttributeSet? = null) : ConstraintLayout(context, attr) {

    private var viewModel =
        ViewModelProviders.of(context as AppCompatActivity).get(PredictionViewModel::class.java)

    private var viewManager: LinearLayoutManager =
        LinearLayoutManager(context).apply { orientation = LinearLayout.VERTICAL }
    private var inflated = false

    init {
        context as AppCompatActivity
        viewModel.data.observe(context, widgetObserver())
        viewModel.state.observe(context, stateObserver())
    }

    private fun widgetObserver() = Observer<PredictionWidget> { widget ->
        widget?.apply {
            val optionList = resource.getMergedOptions() ?: return@Observer
            val isImageWidget = optionList.isNotEmpty() && !optionList[0].image_url.isNullOrEmpty()

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

            viewModel.adapter = viewModel.adapter ?: WidgetOptionsViewAdapter(
                optionList,
                {},
                widget.type,
                resource.correct_option_id,
                if (resource.text_prediction_id.isNullOrEmpty()) resource.image_prediction_id else resource.text_prediction_id
            )

            textRecyclerView.apply {
                this.layoutManager = viewManager
                this.adapter = viewModel.adapter
                setHasFixedSize(true)
            }

            viewModel.startDismissTimout(resource.timeout, resource.correct_option_id.isNotEmpty())

            val animationLength = AndroidResource.parseDuration(resource.timeout).toFloat()
            if (viewModel.animationEggTimerProgress < 1f) {
                imageEggTimer?.setupEggTimer(animationLength)
                textEggTimer?.setupEggTimer(animationLength)
            }
        }

        if (widget == null) {
            inflated = false
        }
    }

    private fun EggTimerView.setupEggTimer(animationLength: Float) {
        visibility = View.VISIBLE
        startAnimationFrom(viewModel.animationEggTimerProgress, animationLength) {
            viewModel.animationEggTimerProgress = it
            if (it >= 1) {
                textCloseButton?.visibility = View.VISIBLE
                imageCloseButton?.visibility = View.VISIBLE
                textEggTimer?.visibility = View.VISIBLE
                imageEggTimer?.visibility = View.VISIBLE
            }
        }
    }

    private fun stateObserver() = Observer<String> {
        when (it) {
            "confirmation" -> {
                confirmationMessage.apply {
                    text = viewModel.data.value?.resource?.confirmation_message ?: ""
                    startAnimation(viewModel.animationPath, viewModel.animationProgress)
                    subscribeToAnimationUpdates { value ->
                        viewModel.animationProgress = value
                    }
                    visibility = View.VISIBLE
                }
            }
            "followup" -> {
                followupAnimation.apply {
                    setAnimation(viewModel.animationPath)
                    progress = viewModel.animationProgress
                    addAnimatorUpdateListener { valueAnimator ->
                        viewModel.animationProgress = valueAnimator.animatedFraction
                    }
                    if (progress != 1f) {
                        resumeAnimation()
                    }
                    visibility = View.VISIBLE
                }
                textEggTimer?.visibility = View.GONE
                imageEggTimer?.visibility = View.GONE
                textCloseButton?.visibility = View.VISIBLE
                imageCloseButton?.visibility = View.VISIBLE
            }
        }
    }
}
