@file:Suppress("UselessCallOnNotNull", "USELESS_ELVIS")

package com.livelike.engagementsdk.widget.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.core.utils.AndroidResource
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.widget.SpecifiedWidgetView
import com.livelike.engagementsdk.widget.adapters.WidgetOptionsViewAdapter
import com.livelike.engagementsdk.widget.model.Resource
import com.livelike.engagementsdk.widget.utils.livelikeSharedPrefs.getWidgetPredictionVotedAnswerIdOrEmpty
import com.livelike.engagementsdk.widget.utils.livelikeSharedPrefs.shouldShowPointTutorial
import com.livelike.engagementsdk.widget.viewModel.PredictionViewModel
import com.livelike.engagementsdk.widget.viewModel.PredictionWidget
import com.livelike.engagementsdk.widget.viewModel.ViewModel
import kotlinx.android.synthetic.main.atom_widget_title.view.titleTextView
import kotlinx.android.synthetic.main.widget_text_option_selection.view.confirmationMessage
import kotlinx.android.synthetic.main.widget_text_option_selection.view.followupAnimation
import kotlinx.android.synthetic.main.widget_text_option_selection.view.pointView
import kotlinx.android.synthetic.main.widget_text_option_selection.view.progressionMeterView
import kotlinx.android.synthetic.main.widget_text_option_selection.view.textEggTimer
import kotlinx.android.synthetic.main.widget_text_option_selection.view.textRecyclerView
import kotlinx.android.synthetic.main.widget_text_option_selection.view.titleView
import kotlinx.android.synthetic.main.widget_text_option_selection.view.txtTitleBackground

class PredictionView(context: Context, attr: AttributeSet? = null) : SpecifiedWidgetView(context, attr) {

    private var viewModel: PredictionViewModel? = null

    private var inflated = false

    override var dismissFunc: ((action: DismissAction) -> Unit)? = { viewModel?.dismissWidget(it) }

    override var widgetViewModel: ViewModel? = null
        get() = super.widgetViewModel
        set(value) {
            field = value
            viewModel = value as PredictionViewModel
        }

    // Refresh the view when re-attached to the activity
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewModel?.data?.subscribe(javaClass) { widgetObserver(it) }
        viewModel?.state?.subscribe(javaClass) { stateObserver(it) }
    }

    private fun resultsObserver(resource: Resource?) {
        resource?.apply {
            val optionResults = resource.getMergedOptions() ?: return
            val totalVotes = optionResults.sumBy { it.getMergedVoteCount().toInt() }
            val options = viewModel?.data?.currentData?.resource?.getMergedOptions() ?: return
            options.forEach { opt ->
                optionResults.find {
                    it.id == opt.id
                }?.apply {
                    opt.updateCount(this)
                    opt.percentage = opt.getPercent(totalVotes.toFloat())
                }
            }
            logDebug { "PredictionWidget Showing result total:$totalVotes" }
            viewModel?.adapter?.myDataset = options
            textRecyclerView.swapAdapter(viewModel?.adapter, false)
        }
    }

    private fun widgetObserver(widget: PredictionWidget?) {
        widget?.apply {
            val optionList = resource.getMergedOptions() ?: return
            if (!inflated) {
                inflated = true
                inflate(context, R.layout.widget_text_option_selection, this@PredictionView)
            }

            titleView.title = resource.question
            txtTitleBackground.setBackgroundResource(R.drawable.header_rounded_corner_prediciton)
            titleTextView.gravity = Gravity.START
            viewModel?.adapter = viewModel?.adapter ?: WidgetOptionsViewAdapter(
                optionList,
                {
                    viewModel?.adapter?.showPercentage = true
                    viewModel?.adapter?.notifyDataSetChanged()
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
            viewModel?.startDismissTimout(resource.timeout, isFollowUp, widgetViewThemeAttributes)
            if (isFollowUp) {
                val selectedPredictionId = getWidgetPredictionVotedAnswerIdOrEmpty(if (resource.text_prediction_id.isNullOrEmpty()) resource.image_prediction_id else resource.text_prediction_id)
                viewModel?.followupState(
                    selectedPredictionId,
                    resource.correct_option_id,
                    widgetViewThemeAttributes
                )
            }

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
            logDebug { "showing PredictionView Widget" }
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
                resultsObserver(viewModel?.results?.latest())
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
                viewModel?.points?.let {
                    if (!shouldShowPointTutorial() && it > 0) {
                        pointView.startAnimation(it, true)
                        wouldShowProgressionMeter(viewModel?.rewardsType, viewModel?.gamificationProfile?.latest(), progressionMeterView)
                    }
                }
            }
            "followup" -> {
                followupAnimation?.apply {
                    setAnimation(viewModel?.
                    animationPath)
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

                viewModel?.points?.let {
                    if (!shouldShowPointTutorial() && it > 0) {
                        pointView.startAnimation(it, true)
                        wouldShowProgressionMeter(viewModel?.rewardsType, viewModel?.gamificationProfile?.latest(), progressionMeterView)
                    }
                }
            }
        }
    }

}
