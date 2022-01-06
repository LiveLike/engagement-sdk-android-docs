@file:Suppress("UselessCallOnNotNull", "USELESS_ELVIS")

package com.livelike.engagementsdk.widget.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.livelike.engagementsdk.widget.utils.livelikeSharedPrefs.getWidgetPredictionVotedAnswerIdOrEmpty
import com.livelike.engagementsdk.widget.utils.livelikeSharedPrefs.shouldShowPointTutorial
import com.livelike.engagementsdk.widget.viewModel.BaseViewModel
import com.livelike.engagementsdk.widget.viewModel.PredictionViewModel
import com.livelike.engagementsdk.widget.viewModel.PredictionWidget
import com.livelike.engagementsdk.widget.viewModel.WidgetStates


class PredictionView(context: Context, attr: AttributeSet? = null) :
    SpecifiedWidgetView(context, attr) {

    private var viewModel: PredictionViewModel? = null

    private var inflated = false

    private var isFirstInteraction = false
    private lateinit var binding: WidgetTextOptionSelectionBinding

    override var dismissFunc: ((action: DismissAction) -> Unit)? = { viewModel?.dismissWidget(it) }

    override var widgetViewModel: BaseViewModel? = null
        get() = viewModel
        set(value) {
            field = value
            viewModel = value as PredictionViewModel
        }

    init {
        isFirstInteraction = viewModel?.getUserInteraction() != null
    }

    // Refresh the view when re-attached to the activity
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        widgetObserver(viewModel?.data?.latest())
        viewModel?.widgetState?.subscribe(javaClass.simpleName) { widgetStateObserver(it) }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        viewModel?.data?.unsubscribe(javaClass.simpleName)
        viewModel?.widgetState?.unsubscribe(javaClass.simpleName)
        viewModel?.results?.unsubscribe(javaClass.simpleName)
    }

    private fun widgetStateObserver(widgetStates: WidgetStates?) {
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
                        timeout, binding.textEggTimer,
                        {
                            viewModel?.animationEggTimerProgress = it
                        },
                        {
                            viewModel?.dismissWidget(it)
                        }
                    )
                }
            }
            WidgetStates.RESULTS, WidgetStates.FINISHED -> {
                lockInteraction()
                onWidgetInteractionCompleted()
                viewModel?.apply {
                    if (followUp) {
                        binding.followupAnimation?.apply {
                            if (viewModel?.animationPath?.isNotEmpty() == true)
                                setAnimation(
                                    viewModel?.animationPath
                                )
                            progress = viewModel?.animationProgress!!
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
                        }
                        viewModel?.points?.let {
                            if (!shouldShowPointTutorial() && it > 0) {
                                binding.pointView.startAnimation(it, true)
                                wouldShowProgressionMeter(
                                    viewModel?.rewardsType,
                                    viewModel?.gamificationProfile?.latest(),
                                    binding.progressionMeterView
                                )
                            }
                        }
                    } else {
                        viewModel?.results?.subscribe(javaClass.simpleName) {
                            if (isFirstInteraction)
                                resultsObserver(it)
                        }

                        binding.confirmationMessage?.apply {
                            if (isFirstInteraction) {
                                text =
                                    viewModel?.data?.currentData?.resource?.confirmation_message
                                    ?: ""
                                viewModel?.animationPath?.let {
                                    viewModel?.animationProgress?.let { it1 ->
                                        startAnimation(
                                            it,
                                            it1
                                        )
                                    }
                                }
                                subscribeToAnimationUpdates { value ->
                                    viewModel?.animationProgress = value
                                }
                                visibility = if (showResultAnimation) {
                                    View.VISIBLE
                                } else {
                                    View.GONE
                                }
                            }
                        }

                        viewModel?.points?.let {
                            if (!shouldShowPointTutorial() && it > 0) {
                                binding.pointView.startAnimation(it, true)
                                wouldShowProgressionMeter(
                                    viewModel?.rewardsType,
                                    viewModel?.gamificationProfile?.latest(),
                                    binding.progressionMeterView
                                )
                            }
                        }
                    }
                }
            }
        }
        if (viewModel?.enableDefaultWidgetTransition == true) {
            defaultStateTransitionManager(widgetStates)
        }
    }

    private fun lockInteraction() {
        viewModel?.adapter?.selectionLocked = true
        viewModel?.data?.latest()?.let {
            val isFollowUp = it.resource.kind.contains("follow-up")
            if (isFollowUp) {
                binding.textEggTimer.showCloseButton { viewModel?.dismissWidget(DismissAction.TIMEOUT) }

            }
        }
    }

    private fun unLockInteraction() {
        viewModel?.data?.latest()?.let {
            val isFollowUp = it.resource.kind.contains("follow-up")
            if (!isFollowUp) {
                viewModel?.adapter?.selectionLocked = false
                // marked widget as interactive
                viewModel?.markAsInteractive()
            }
        }
    }

    private fun defaultStateTransitionManager(widgetStates: WidgetStates?) {
        when (widgetStates) {
            WidgetStates.READY -> {
                moveToNextState()
            }
            WidgetStates.INTERACTING -> {
                viewModel?.data?.latest()?.let {
                    val isFollowUp = it.resource.kind.contains("follow-up")
                    viewModel?.startDismissTimout(
                        it.resource.timeout,
                        isFollowUp,
                        widgetViewThemeAttributes
                    )
                }
            }
            WidgetStates.RESULTS -> {
//                viewModel?.confirmationState()
            }
            WidgetStates.FINISHED -> {
                widgetObserver(null)
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
            logDebug { "PredictionWidget Showing result total:$totalVotes" }
            viewModel?.adapter?.myDataset = options
            viewModel?.adapter?.showPercentage = true
            binding.textRecyclerView?.swapAdapter(viewModel?.adapter, false)
        }
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
                        binding.layTextRecyclerView?.background = it
                    }
                }
            }
        }
    }

    private fun widgetObserver(widget: PredictionWidget?) {
        widget?.apply {
            val optionList = resource.getMergedOptions() ?: return
            if (!inflated) {
                inflated = true
                binding = WidgetTextOptionSelectionBinding.inflate(LayoutInflater.from(context), this@PredictionView, true)
            }

            val isFollowUp = resource.kind.contains("follow-up")

            // added tag for identification of widget (by default will be empty)
            if(isFollowUp){
                setTagViewWithStyleChanges(context.resources.getString(R.string.livelike_follow_up_tag))
            }else{
                setTagViewWithStyleChanges(context.resources.getString(R.string.livelike_prediction_tag))
            }

            binding.titleView.title = resource.question
            binding.txtTitleBackground.setBackgroundResource(R.drawable.header_rounded_corner_prediciton)
            binding.layTextRecyclerView?.setBackgroundResource(R.drawable.body_rounded_corner_prediction)
            binding.titleView.titleViewBinding.titleTextView.gravity = Gravity.START
            viewModel?.adapter = viewModel?.adapter ?: WidgetOptionsViewAdapter(
                optionList,
                widget.type,
                resource.correct_option_id,
                (if (resource.text_prediction_id.isNullOrEmpty()) resource.image_prediction_id else resource.text_prediction_id)
                    ?: ""
            )

            // set on click
            viewModel?.adapter?.onClick = {
                viewModel?.adapter?.notifyDataSetChanged()
                viewModel?.onOptionClicked()
                isFirstInteraction = true
                viewModel?.saveInteraction(it)
            }

            widgetsTheme?.let {
                applyTheme(it)
            }

            if (!isFollowUp)
                viewModel?.apply {
                    val rootPath = widgetViewThemeAttributes.stayTunedAnimation
                    animationPath = AndroidResource.selectRandomLottieAnimation(
                        rootPath,
                        context.applicationContext
                    ) ?: ""
                }

            binding.textRecyclerView.apply {
                isFirstInteraction = viewModel?.getUserInteraction() != null
                viewModel?.adapter?.restoreSelectedPosition(viewModel?.getUserInteraction()?.optionId)
                this.adapter = viewModel?.adapter
                setHasFixedSize(true)
            }

            if (isFollowUp) {
                val selectedPredictionId =
                    getWidgetPredictionVotedAnswerIdOrEmpty(if (resource.text_prediction_id.isNullOrEmpty()) resource.image_prediction_id else resource.text_prediction_id)
                viewModel?.followupState(
                    selectedPredictionId,
                    resource.correct_option_id,
                    widgetViewThemeAttributes
                )
            }

            logDebug { "showing PredictionView Widget" }
            if (widgetViewModel?.widgetState?.latest() == null || widgetViewModel?.widgetState?.latest() == WidgetStates.READY)
                widgetViewModel?.widgetState?.onNext(WidgetStates.READY)
        }

        if (widget == null) {
            inflated = false
            removeAllViews()
            parent?.let { (it as ViewGroup).removeAllViews() }
        }
    }
}
