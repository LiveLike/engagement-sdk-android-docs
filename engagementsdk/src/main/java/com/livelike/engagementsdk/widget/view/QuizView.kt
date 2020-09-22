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
import com.livelike.engagementsdk.widget.OptionsWidgetThemeComponent
import com.livelike.engagementsdk.widget.SpecifiedWidgetView
import com.livelike.engagementsdk.widget.WidgetsTheme
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

    private var viewModel: QuizViewModel? = null

    override var dismissFunc: ((action: DismissAction) -> Unit)? = { viewModel?.dismissWidget(it) }

    override var widgetViewModel: BaseViewModel? = null
        set(value) {
            field = value
            viewModel = value as QuizViewModel
            viewModel?.widgetState?.subscribe(javaClass) { stateWidgetObserver(it) }
            viewModel?.currentVoteId?.subscribe(javaClass) { onClickObserver() }
        }

    // Refresh the view when re-attached to the activity
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewModel?.data?.subscribe(javaClass.simpleName) { resourceObserver(it) }
        viewModel?.results?.subscribe(javaClass) { resultsObserver(it) }
    }

    private fun stateWidgetObserver(widgetStates: WidgetStates?) {
        when (widgetStates) {
            WidgetStates.READY -> {
                lockInteraction()
            }
            WidgetStates.INTERACTING -> {
                unLockInteraction()
            }
            WidgetStates.RESULTS, WidgetStates.FINISHED -> {
                lockInteraction()
                onWidgetInteractionCompleted()
                viewModel?.apply {
                    val isUserCorrect =
                        adapter?.selectedPosition?.let {
                            if (it > -1) {
                                adapter?.myDataset?.get(it)?.is_correct
                            }
                            false
                        }
                            ?: false
                    val rootPath =
                        if (isUserCorrect) widgetViewThemeAttributes.widgetWinAnimation else widgetViewThemeAttributes.widgetLoseAnimation
                    animationPath =
                        AndroidResource.selectRandomLottieAnimation(rootPath, context) ?: ""
                }
                resultsObserver(viewModel?.results?.latest())
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
        if (viewModel?.enableDefaultWidgetTransition == true) {
            defaultStateTransitionManager(widgetStates)
        }
    }

    private var inflated = false

    private fun onClickObserver() {
        viewModel?.onOptionClicked()
    }

    override fun applyTheme(theme: WidgetsTheme) {
        super.applyTheme(theme)
        viewModel?.data?.latest()?.let { widget ->
            theme.getThemeLayoutComponent(widget.type)?.let { themeComponent ->
                if (themeComponent is OptionsWidgetThemeComponent) {
                    applyThemeOnTitleView(themeComponent)
                    viewModel?.adapter?.component = themeComponent
                    viewModel?.adapter?.notifyDataSetChanged()
                }
            }
        }
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

            widgetsTheme?.let {
                applyTheme(it)
            }

            textRecyclerView.apply {
                this.adapter = viewModel?.adapter
                setHasFixedSize(true)
            }
            showTimer(resource.timeout, viewModel?.animationEggTimerProgress, textEggTimer, {
                viewModel?.animationEggTimerProgress = it
            }, {
                viewModel?.dismissWidget(it)
            })
            widgetViewModel?.widgetState?.onNext(WidgetStates.READY)
            logDebug { "showing QuizWidget" }
        }
        if (widget == null) {
            inflated = false
            removeAllViews()
            parent?.let { (it as ViewGroup).removeAllViews() }
        }
    }

    private fun lockInteraction() {
        viewModel?.adapter?.selectionLocked = true
    }

    private fun unLockInteraction() {
        viewModel?.adapter?.selectionLocked = false
    }

    private fun defaultStateTransitionManager(widgetStates: WidgetStates?) {
        when (widgetStates) {
            WidgetStates.READY -> {
                moveToNextState()
            }
            WidgetStates.INTERACTING -> {
                viewModel?.data?.latest()?.let {
                    viewModel?.startDismissTimout(
                        it.resource.timeout,
                        widgetViewThemeAttributes
                    )
                }
            }
            WidgetStates.RESULTS -> {
                followupAnimation.apply {
                    addAnimatorListener(object : Animator.AnimatorListener {
                        override fun onAnimationRepeat(animation: Animator?) {
                        }

                        override fun onAnimationEnd(animation: Animator?) {
                            viewModel?.dismissWidget(DismissAction.TIMEOUT)
                        }

                        override fun onAnimationCancel(animation: Animator?) {
                        }

                        override fun onAnimationStart(animation: Animator?) {
                        }
                    })
                }
            }
            WidgetStates.FINISHED -> {
                resourceObserver(null)
            }
        }
    }

    private fun resultsObserver(resource: Resource?) {
        (resource ?: viewModel?.data?.currentData?.resource)?.apply {
            val optionResults = this.getMergedOptions() ?: return
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
