package com.livelike.engagementsdk.widget.view

import android.animation.Animator
import android.content.Context
import android.graphics.drawable.StateListDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.core.utils.AndroidResource
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.databinding.WidgetTextOptionSelectionBinding
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class QuizView(context: Context, attr: AttributeSet? = null) : SpecifiedWidgetView(context, attr) {

    private var viewModel: QuizViewModel? = null

    override var dismissFunc: ((action: DismissAction) -> Unit)? = { viewModel?.dismissWidget(it) }

    override var widgetViewModel: BaseViewModel? = null
        set(value) {
            field = value
            viewModel = value as QuizViewModel
        }

    private var isFirstInteraction = false
    private var binding: WidgetTextOptionSelectionBinding? = null


    init {
        isFirstInteraction = viewModel?.getUserInteraction() != null
    }

    // Refresh the view when re-attached to the activity
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewModel?.data?.subscribe(javaClass.simpleName) { resourceObserver(it) }
        viewModel?.widgetState?.subscribe(javaClass) { stateWidgetObserver(it) }
        viewModel?.currentVoteId?.subscribe(javaClass) { onClickObserver() }
        // viewModel?.results?.subscribe(javaClass) { resultsObserver(it) }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        viewModel?.data?.unsubscribe(javaClass.simpleName)
        viewModel?.widgetState?.unsubscribe(javaClass.simpleName)
        viewModel?.currentVoteId?.unsubscribe(javaClass.simpleName)
        viewModel?.results?.unsubscribe(javaClass.simpleName)
    }

    private fun stateWidgetObserver(widgetStates: WidgetStates?) {
        when (widgetStates) {
            WidgetStates.READY -> {
                lockInteraction()
            }
            WidgetStates.INTERACTING -> {
                unLockInteraction()
                showResultAnimation = true
                // show timer while widget interaction mode
                viewModel?.data?.latest()?.resource?.timeout?.let { timeout ->
                    showTimer(
                        timeout, binding?.textEggTimer,
                        {
                            viewModel?.animationEggTimerProgress = it
                        },
                        {
                            viewModel?.dismissWidget(it)
                        }
                    )
                }
                binding?.layLock?.layLock?.visibility = View.VISIBLE
            }
            WidgetStates.RESULTS, WidgetStates.FINISHED -> {
                lockInteraction()
                onWidgetInteractionCompleted()
                disableLockButton()
                binding?.layLock?.labelLock?.visibility = View.VISIBLE
                viewModel?.results?.subscribe(javaClass.simpleName) {
                    if (isFirstInteraction) {
                        resultsObserver(viewModel?.results?.latest())
                    }
                }

                if (isFirstInteraction) {
                    viewModel?.apply {
                        val isUserCorrect =
                            adapter?.selectedPosition?.let {
                                if (it > -1) {
                                    return@let adapter?.myDataset?.get(it)?.is_correct
                                }
                                return@let false
                            }
                                ?: false
                        val rootPath =
                            if (isUserCorrect) widgetViewThemeAttributes.widgetWinAnimation else widgetViewThemeAttributes.widgetLoseAnimation
                        animationPath =
                            AndroidResource.selectRandomLottieAnimation(rootPath, context) ?: ""
                    }

                    viewModel?.adapter?.correctOptionId =
                        viewModel?.adapter?.myDataset?.find { it.is_correct }?.id ?: ""
                    viewModel?.adapter?.userSelectedOptionId =
                        viewModel?.adapter?.selectedPosition?.let { it1 ->
                        if (it1 > -1)
                            return@let viewModel?.adapter?.myDataset?.get(it1)?.id
                        return@let null
                    } ?: ""

                    binding?.textRecyclerView?.swapAdapter(viewModel?.adapter, false)
                    binding?.textRecyclerView?.adapter?.notifyItemChanged(0)
                }

                binding?.followupAnimation?.apply {
                    if (isFirstInteraction) {
                        setAnimation(viewModel?.animationPath)
                        progress = viewModel?.animationProgress ?: 0f
                        logDebug { "Animation: ${viewModel?.animationPath}" }
                        addAnimatorUpdateListener { valueAnimator ->
                            viewModel?.animationProgress = valueAnimator.animatedFraction
                        }
                        if (progress != 1f) {
                            resumeAnimation()
                        }
                        visibility = if (showResultAnimation) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }
                    } else {
                        visibility = View.GONE
                    }
                }

                viewModel?.points?.let {
                    if (!shouldShowPointTutorial() && it > 0) {
                        binding?.pointView?.startAnimation(it, true)
                        wouldShowProgressionMeter(
                            viewModel?.rewardsType,
                            viewModel?.gamificationProfile?.latest(),
                            binding?.progressionMeterView!!
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
                    applyThemeOnTagView(themeComponent)
                    viewModel?.adapter?.component = themeComponent
                    viewModel?.adapter?.notifyDataSetChanged()
                    AndroidResource.createDrawable(themeComponent.body)?.let {
                        binding?.layTextRecyclerView?.background = it
                    }

                    // submit button drawables theme
                    val submitButtonEnabledDrawable = AndroidResource.createDrawable(
                        themeComponent.submitButtonEnabled
                    )
                    val submitButtonDisabledDrawable = AndroidResource.createDrawable(
                        themeComponent.submitButtonDisabled
                    )
                    val state = StateListDrawable()
                    state.addState(intArrayOf(android.R.attr.state_enabled), submitButtonEnabledDrawable)
                    state.addState(intArrayOf(), submitButtonDisabledDrawable)
                    binding?.layLock?.btnLock?.background = state

                    //confirmation label theme
                    AndroidResource.updateThemeForView(
                        binding?.layLock?.labelLock,
                        themeComponent.confirmation,
                        fontFamilyProvider
                    )
                    if (themeComponent.confirmation?.background != null) {
                        binding?.layLock?.labelLock?.background = AndroidResource.createDrawable(themeComponent.confirmation)
                    }
                      themeComponent.confirmation?.padding?.let {
                        AndroidResource.setPaddingForView(binding?.layLock?.labelLock, themeComponent.confirmation.padding)
                    }
                }
            }
        }
    }

    private fun resourceObserver(widget: QuizWidget?) {
        widget?.apply {
            val optionList = resource.getMergedOptions() ?: return
            if (!inflated) {
                inflated = true
                binding = WidgetTextOptionSelectionBinding.inflate(LayoutInflater.from(context), this@QuizView, true)
            }

            // added tag for identification of widget (by default will be empty)
            setTagViewWithStyleChanges(context.resources.getString(R.string.livelike_quiz_tag))
            binding?.apply {
                titleView.title = resource.question
                txtTitleBackground.setBackgroundResource(R.drawable.header_rounded_corner_quiz)
                layTextRecyclerView?.setBackgroundResource(R.drawable.body_rounded_corner_quiz)
                titleView.titleViewBinding.titleTextView.gravity = Gravity.START
                layLock.btnLock.text = context.resources.getString(R.string.livelike_answer_label)
                layLock.labelLock.text = context.resources.getString(R.string.livelike_answered_label)
            }
            viewModel?.adapter = viewModel?.adapter ?: WidgetOptionsViewAdapter(optionList, type)

            // set on click
            viewModel?.adapter?.onClick = {
                viewModel?.adapter?.apply {
                    val currentSelectionId = myDataset[selectedPosition]
                    viewModel?.currentVoteId?.onNext(currentSelectionId.id)
                    widgetLifeCycleEventsListener?.onUserInteract(widgetData)
                    isFirstInteraction = true
                }
                enableLockButton()
            }

            widgetsTheme?.let {
                applyTheme(it)
            }
            disableLockButton()
            binding?.textRecyclerView?.apply {
                isFirstInteraction = viewModel?.getUserInteraction() != null
                this.adapter = viewModel?.adapter
                viewModel?.adapter?.restoreSelectedPosition(viewModel?.getUserInteraction()?.choiceId)
                setHasFixedSize(true)
            }
            binding?.layLock?.btnLock?.setOnClickListener {
                if (viewModel?.adapter?.selectedPosition != RecyclerView.NO_POSITION) {
                    lockVote()
                    binding?.textEggTimer?.visibility = GONE
                }
            }
            if (viewModel?.getUserInteraction() != null) {
                findViewById<TextView>(R.id.label_lock)?.visibility = VISIBLE
            } else if (viewModel?.adapter?.selectedPosition != RecyclerView.NO_POSITION) {
                enableLockButton()
            }

            if (widgetViewModel?.widgetState?.latest() == null || widgetViewModel?.widgetState?.latest() == WidgetStates.READY)
                widgetViewModel?.widgetState?.onNext(WidgetStates.READY)
            logDebug { "showing QuizWidget" }
        }
        if (widget == null) {
            inflated = false
            removeAllViews()
            parent?.let { (it as ViewGroup).removeAllViews() }
        }
    }

    private fun lockVote() {
        disableLockButton()
        viewModel?.currentVoteId?.currentData?.let { id ->
            viewModel?.adapter?.myDataset?.find { it.id == id }?.let { option ->
                viewModel?.saveInteraction(option)
            }
        }
        binding?.layLock?.labelLock?.visibility = View.VISIBLE
        viewModel?.run {
            timeOutJob?.cancel()
            uiScope.launch {
                lockInteractionAndSubmitVote()
            }
        }
    }

    private fun enableLockButton() {
        binding?.layLock?.btnLock?.apply{
            isEnabled = true
            alpha = 1f
        }
    }

    private fun disableLockButton() {
        binding?.layLock?.layLock?.visibility = VISIBLE
        binding?.layLock?.btnLock?.apply {
            isEnabled = false
            alpha = 0.5f
        }
    }

    private fun lockInteraction() {
        viewModel?.adapter?.selectionLocked = true
    }

    private fun unLockInteraction() {
        viewModel?.adapter?.selectionLocked = false
        // marked widget as interactive
        viewModel?.markAsInteractive()
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
                if (!isFirstInteraction) {
                    viewModel?.dismissWidget(DismissAction.TIMEOUT)
                }
                binding?.followupAnimation?.apply {
                    addAnimatorListener(object : Animator.AnimatorListener {
                        override fun onAnimationRepeat(animation: Animator?) {
                            // nothing needed here
                        }

                        override fun onAnimationEnd(animation: Animator?) {
                            viewModel?.uiScope?.launch {
                                delay(11000)
                                viewModel?.dismissWidget(DismissAction.TIMEOUT)
                            }
                        }

                        override fun onAnimationCancel(animation: Animator?) {
                            // nothing needed here
                        }

                        override fun onAnimationStart(animation: Animator?) {
                            // nothing needed here
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
            val totalVotes = optionResults.sumOf { it.getMergedVoteCount().toInt() }
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
            binding?.textRecyclerView?.swapAdapter(viewModel?.adapter, false)
            viewModel?.adapter?.showPercentage = false
            logDebug { "QuizWidget Showing result total:$totalVotes" }
        }
    }
}
