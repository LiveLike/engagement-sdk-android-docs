package com.livelike.engagementsdk.widget.view

import android.animation.Animator
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
import com.livelike.engagementsdk.widget.utils.livelikeSharedPrefs.shouldShowPointTutorial
import com.livelike.engagementsdk.widget.viewModel.BaseViewModel
import com.livelike.engagementsdk.widget.viewModel.QuizViewModel
import com.livelike.engagementsdk.widget.viewModel.QuizWidget
import com.livelike.engagementsdk.widget.viewModel.WidgetStates
import kotlinx.android.synthetic.main.atom_widget_title.view.titleTextView
import kotlinx.android.synthetic.main.widget_text_option_selection.view.followupAnimation
import kotlinx.android.synthetic.main.widget_text_option_selection.view.pointView
import kotlinx.android.synthetic.main.widget_text_option_selection.view.progressionMeterView
import kotlinx.android.synthetic.main.widget_text_option_selection.view.textEggTimer
import kotlinx.android.synthetic.main.widget_text_option_selection.view.textRecyclerView
import kotlinx.android.synthetic.main.widget_text_option_selection.view.titleView
import kotlinx.android.synthetic.main.widget_text_option_selection.view.txtTitleBackground

class QuizView(context: Context, attr: AttributeSet? = null) : SpecifiedWidgetView(context, attr) {

    private var isInteracting: Boolean = false
    private var viewModel: QuizViewModel? = null

    override var dismissFunc: ((action: DismissAction) -> Unit)? = { viewModel?.dismissWidget(it) }

    override var widgetViewModel: BaseViewModel? = null
        get() = super.widgetViewModel
        set(value) {
            field = value
            viewModel = value as QuizViewModel
            viewModel?.widgetState?.subscribe(javaClass) { stateWidgetObserver(it) }
            viewModel?.results?.subscribe(javaClass) { resultsObserver(it) }
            viewModel?.currentVoteId?.subscribe(javaClass) { onClickObserver() }
        }

    private fun stateWidgetObserver(widgetStates: WidgetStates?) {
        when (widgetStates) {
            WidgetStates.READY -> {
                resourceObserver(viewModel?.data?.latest())
            }
            WidgetStates.INTERACTING -> {
                if (!isInteracting) {
                    viewModel?.data?.latest()?.let {
                        viewModel?.startDismissTimout(
                            it.resource.timeout,
                            widgetViewThemeAttributes
                        )
                    }
                    isInteracting = true
                } else {
                    onWidgetInteractionCompleted()
                }
            }
            WidgetStates.FINISHED -> {
                resourceObserver(null)
            }
            WidgetStates.RESULTS -> {
                listOf(textEggTimer).forEach { v ->
                    v?.showCloseButton() {
                        viewModel?.dismissWidget(it)
                    }
                }
                viewModel?.adapter?.correctOptionId =
                    viewModel?.adapter?.myDataset?.find { it.is_correct }?.id ?: ""
                viewModel?.adapter?.userSelectedOptionId =
                    viewModel?.adapter?.selectedPosition?.let { it1 ->
                        viewModel?.adapter?.myDataset?.get(it1)?.id
                    } ?: ""

                textRecyclerView.swapAdapter(viewModel?.adapter, false)
                textRecyclerView.adapter?.notifyItemChanged(0)

                followupAnimation.apply {
                    setAnimation(viewModel?.animationPath)
                    progress = viewModel?.animationProgress ?: 0f
                    logDebug { "Animation: ${viewModel?.animationPath}" }
                    addAnimatorUpdateListener { valueAnimator ->
                        viewModel?.animationProgress = valueAnimator.animatedFraction
                    }
                    addAnimatorListener(object : Animator.AnimatorListener {
                        override fun onAnimationRepeat(animation: Animator?) {
                        }

                        override fun onAnimationEnd(animation: Animator?) {
                            viewModel?.dismissWidget(DismissAction.TAP_X)
                        }

                        override fun onAnimationCancel(animation: Animator?) {
                        }

                        override fun onAnimationStart(animation: Animator?) {
                        }
                    })
                    if (progress != 1f) {
                        resumeAnimation()
                    }
                    visibility = View.VISIBLE
                }
                viewModel?.points?.let {
                    if (!shouldShowPointTutorial() && it > 0) {
                        pointView.startAnimation(it, true)
                        wouldShowProgressionMeter(
                            viewModel?.rewardsType,
                            viewModel?.gamificationProfile?.latest(),
                            progressionMeterView
                        )
                    }
                }
            }
        }
    }

    private var inflated = false

    private fun onClickObserver() {
        viewModel?.onOptionClicked()
    }

    // Refresh the view when re-attached to the activity
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewModel?.widgetState?.subscribe(javaClass) { stateWidgetObserver(it) }
        viewModel?.results?.subscribe(javaClass) { resultsObserver(it) }
        viewModel?.currentVoteId?.subscribe(javaClass) { onClickObserver() }
    }

    private fun resourceObserver(widget: QuizWidget?) {
        widget?.apply {
            val optionList = resource.getMergedOptions() ?: return
            if (!inflated) {
                inflated = true
                inflate(context, R.layout.widget_text_option_selection, this@QuizView)
            }

            titleView.title = resource.question
            txtTitleBackground.setBackgroundResource(R.drawable.header_rounded_corner_quiz)

            titleTextView.gravity = Gravity.START
            viewModel?.adapter = viewModel?.adapter ?: WidgetOptionsViewAdapter(optionList, {
                viewModel?.adapter?.apply {
                    val currentSelectionId = myDataset[selectedPosition]
                    viewModel?.currentVoteId?.onNext(currentSelectionId.id)
                }
            }, type)

            textRecyclerView.apply {
                this.adapter = viewModel?.adapter
                setHasFixedSize(true)
            }

            viewModel?.widgetState?.onNext(WidgetStates.INTERACTING)

            val animationLength = AndroidResource.parseDuration(resource.timeout).toFloat()
            if (viewModel?.animationEggTimerProgress!! < 1f) {
                listOf(textEggTimer).forEach { v ->
                    v?.startAnimationFrom(
                        viewModel?.animationEggTimerProgress ?: 0f,
                        animationLength,
                        {
                            viewModel?.animationEggTimerProgress = it
                        }) {
                        viewModel?.dismissWidget(it)
                    }
                }
            }
        }
        logDebug { "showing QuizWidget" }
        if (widget == null) {
            inflated = false
            removeAllViews()
            parent?.let { (it as ViewGroup).removeAllViews() }
        }
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
            viewModel?.adapter?.myDataset = options
            textRecyclerView.swapAdapter(viewModel?.adapter, false)
            viewModel?.adapter?.showPercentage = false
            logDebug { "QuizWidget Showing result total:$totalVotes" }
        }
    }
}
