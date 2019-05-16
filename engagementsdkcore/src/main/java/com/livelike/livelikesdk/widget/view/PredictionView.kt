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
import com.livelike.livelikesdk.widget.WidgetType
import com.livelike.livelikesdk.widget.adapters.WidgetOptionsViewAdapter
import com.livelike.livelikesdk.widget.util.SpanningLinearLayoutManager
import com.livelike.livelikesdk.widget.viewModel.PredictionViewModel
import com.livelike.livelikesdk.widget.viewModel.PredictionWidget
import kotlinx.android.synthetic.main.atom_widget_confirmation_message.view.confirmMessageAnimation
import kotlinx.android.synthetic.main.organism_text_prediction.view.confirmationMessage
import kotlinx.android.synthetic.main.organism_text_prediction.view.followupAnimation
import kotlinx.android.synthetic.main.organism_text_prediction.view.textRecyclerView
import kotlinx.android.synthetic.main.organism_text_prediction.view.titleView

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
            if (!inflated) {
                inflated = true
                inflate(context, com.livelike.livelikesdk.R.layout.organism_text_prediction, this@PredictionView)
            }
            if (optionList.isNotEmpty() && !optionList[0].image_url.isNullOrEmpty()
            ) {
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

            // TODO: Pass the type directly...
            if (resource.correct_option_id.isNullOrEmpty()) {
                // Follow-up
                if (optionList.isNotEmpty() && !optionList[0].image_url.isNullOrEmpty()
                ) {
                    // IMAGE
                }
            }

            viewModel.adapter = viewModel.adapter ?: WidgetOptionsViewAdapter(
                optionList,
                {},
                WidgetType.TEXT_QUIZ,
                resource.correct_option_id,
                resource.text_prediction_id // or image
            )

            textRecyclerView.apply {
                this.layoutManager = viewManager
                this.adapter = viewModel.adapter
                setHasFixedSize(true)
            }

            viewModel.startDismissTimout(resource.timeout, resource.correct_option_id.isNotEmpty())
        }

        if (widget == null) {
            inflated = false
        }
    }

    private fun stateObserver() = Observer<String> {
        when (it) {
            "confirmation" -> {
                confirmationMessage.apply {
                    text = viewModel.data.value?.resource?.confirmation_message ?: ""
                    startAnimation(viewModel.animationPath, viewModel.animationProgress)
                    confirmMessageAnimation.addAnimatorUpdateListener { valueAnimator ->
                        viewModel.animationProgress = valueAnimator.animatedFraction
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
            }
        }
    }
}
