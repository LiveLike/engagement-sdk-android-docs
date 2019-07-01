package com.livelike.livelikesdk.widget.view

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import com.livelike.engagementsdkapi.LiveLikeContentSession
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.utils.AndroidResource
import com.livelike.livelikesdk.widget.SpecifiedWidgetView
import com.livelike.livelikesdk.widget.adapters.WidgetOptionsViewAdapter
import com.livelike.livelikesdk.widget.util.SpanningLinearLayoutManager
import com.livelike.livelikesdk.widget.viewModel.PredictionViewModel
import com.livelike.livelikesdk.widget.viewModel.PredictionWidget
import kotlinx.android.synthetic.main.widget_image_option_selection.view.imageEggTimer
import kotlinx.android.synthetic.main.widget_text_option_selection.view.confirmationMessage
import kotlinx.android.synthetic.main.widget_text_option_selection.view.followupAnimation
import kotlinx.android.synthetic.main.widget_text_option_selection.view.textEggTimer
import kotlinx.android.synthetic.main.widget_text_option_selection.view.textRecyclerView
import kotlinx.android.synthetic.main.widget_text_option_selection.view.titleView

class PredictionView(context: Context, attr: AttributeSet? = null) : SpecifiedWidgetView(context, attr) {

    override var currentSession: LiveLikeContentSession? = null
        set(value) {
            field = value
            viewModel.setSession(currentSession)
        }

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

            viewModel.adapter = viewModel.adapter ?: WidgetOptionsViewAdapter(
                optionList,
                {
                    val selectedId = viewModel.adapter?.myDataset?.get(viewModel.adapter?.selectedPosition ?: -1)?.id ?: ""
                    viewModel.onOptionClicked(selectedId)
                },
                widget.type,
                resource.correct_option_id,
                if (resource.text_prediction_id.isNullOrEmpty()) resource.image_prediction_id else resource.text_prediction_id
            )

            textRecyclerView.apply {
                this.layoutManager = viewManager
                this.adapter = viewModel.adapter
                setHasFixedSize(true)
            }

            val isFollowUp = resource.correct_option_id.isNotEmpty()
            viewModel.startDismissTimout(resource.timeout, isFollowUp)

            val animationLength = AndroidResource.parseDuration(resource.timeout).toFloat()
            if (viewModel.animationEggTimerProgress < 1f && !isFollowUp) {
                listOf(textEggTimer, imageEggTimer).forEach { v ->
                    v?.startAnimationFrom(viewModel.animationEggTimerProgress, animationLength, {
                        viewModel.animationEggTimerProgress = it
                    }, currentSession)
                }
            }
        }

        if (widget == null) {
            inflated = false
        }
    }

    private fun stateObserver() = Observer<String> { state ->
        when (state) {
            "confirmation" -> {
                confirmationMessage.apply {
                    text = viewModel.data.value?.resource?.confirmation_message ?: ""
                    startAnimation(viewModel.animationPath, viewModel.animationProgress)
                    subscribeToAnimationUpdates { value ->
                        viewModel.animationProgress = value
                    }
                    visibility = View.VISIBLE
                }
                listOf(textEggTimer, imageEggTimer).forEach { it?.showCloseButton(currentSession) }
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
                listOf(textEggTimer, imageEggTimer).forEach { it?.showCloseButton(currentSession) }
            }
        }
    }
}
