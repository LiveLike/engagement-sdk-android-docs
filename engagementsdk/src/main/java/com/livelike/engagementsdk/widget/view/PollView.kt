package com.livelike.engagementsdk.widget.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
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
import com.livelike.engagementsdk.widget.utils.livelikeSharedPrefs.shouldShowPointTutorial
import com.livelike.engagementsdk.widget.viewModel.BaseViewModel
import com.livelike.engagementsdk.widget.viewModel.PollViewModel
import com.livelike.engagementsdk.widget.viewModel.PollWidget
import com.livelike.engagementsdk.widget.viewModel.WidgetStates

class PollView(context: Context, attr: AttributeSet? = null) : SpecifiedWidgetView(context, attr) {

    private var viewModel: PollViewModel? = null

    private var inflated = false
    private var binding: WidgetTextOptionSelectionBinding? = null

    override var dismissFunc: ((DismissAction) -> Unit)? = { viewModel?.dismissWidget(it) }

    override var widgetViewModel: BaseViewModel? = null
        set(value) {
            field = value
            viewModel = value as PollViewModel
//            viewModel?.data?.subscribe(this.hashCode()) { resourceObserver(it) }
//            viewModel?.widgetState?.subscribe(this.hashCode()) { stateObserver(it) }
//            viewModel?.results?.subscribe(this.hashCode()) { resultsObserver(it) }
            viewModel?.currentVoteId?.subscribe(this.hashCode()) { clickedOptionObserver(it) }
            viewModel?.points?.subscribe(this.hashCode()) { rewardsObserver(it) }
        }

    private fun clickedOptionObserver(id: String?) {
        id?.let {
            viewModel?.adapter?.showPercentage = true
            viewModel?.adapter?.notifyDataSetChanged()
            viewModel?.onOptionClicked()
        }
    }

    // Refresh the view when re-attached to the activity
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewModel?.data?.subscribe(this.hashCode()) { resourceObserver(it) }
        viewModel?.widgetState?.subscribe(this.hashCode()) {
            stateObserver(it)
        }
//        viewModel?.results?.subscribe(this.hashCode()) { resultsObserver(it) }
        viewModel?.currentVoteId?.subscribe(this.hashCode()) { clickedOptionObserver(it) }
        viewModel?.points?.subscribe(this.hashCode()) { rewardsObserver(it) }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        viewModel?.data?.unsubscribe(this.hashCode())
        viewModel?.widgetState?.unsubscribe(this.hashCode())
        viewModel?.currentVoteId?.unsubscribe(this.hashCode())
        viewModel?.points?.unsubscribe(this.hashCode())
        viewModel?.results?.unsubscribe(this.hashCode())
    }

    private fun stateObserver(widgetStates: WidgetStates?) {
        when (widgetStates) {
            WidgetStates.READY -> {
                lockInteraction()
            }
            WidgetStates.INTERACTING -> {
                unLockInteraction()
                showResultAnimation = true

                // show timer while widget interaction mode
                viewModel?.data?.latest()?.showTimer()

                viewModel?.results?.subscribe(this.hashCode()) {
                    if (isFirstInteraction)
                        resultsObserver(it)
                }
//                viewModel?.data?.latest()?.let {
//                    viewModel?.startDismissTimout(it.resource.timeout)
//                }
            }
            WidgetStates.RESULTS, WidgetStates.FINISHED -> {
                lockInteraction()
                onWidgetInteractionCompleted()
                viewModel?.results?.subscribe(this.hashCode()) { resultsObserver(it) }
            }
        }
        if (viewModel?.enableDefaultWidgetTransition == true) {
            defaultStateTransitionManager(widgetStates)
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
                    viewModel?.startDismissTimout(it.resource.timeout)
                }
            }
            WidgetStates.RESULTS -> {
                viewModel?.confirmationState()
            }
            WidgetStates.FINISHED -> {
                resourceObserver(null)
            }
        }
    }

    private fun rewardsObserver(points: Int?) {
        points?.let {
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

    override fun applyTheme(theme: WidgetsTheme) {
        super.applyTheme(theme)
        viewModel?.data?.latest()?.let { widget ->
            theme.getThemeLayoutComponent(widget.type)?.let { themeComponent ->
                if (themeComponent is OptionsWidgetThemeComponent) {
                    applyThemeOnTitleView(themeComponent)
                    applyThemeOnTagView(themeComponent)
                    viewModel?.adapter?.component = themeComponent
                    viewModel?.adapter?.fontFamilyProvider = fontFamilyProvider
                    viewModel?.adapter?.notifyDataSetChanged()
                    AndroidResource.createDrawable(themeComponent.body)?.let {
                        binding?.layTextRecyclerView?.background = it
                    }
                }
            }
        }
    }

    private var isFirstInteraction = false

    init {
        isFirstInteraction = viewModel?.getUserInteraction() != null
    }

    private fun resourceObserver(widget: PollWidget?) {
        widget?.apply {
            val optionList = resource.getMergedOptions() ?: return
            if (!inflated) {
                inflated = true
                binding = WidgetTextOptionSelectionBinding.inflate(LayoutInflater.from(context), this@PollView, true)
                wouldInflateSponsorUi()
            }
            binding?.txtTitleBackground?.setBackgroundResource(R.drawable.header_rounded_corner_poll)
            binding?.layTextRecyclerView?.setBackgroundResource(R.drawable.body_rounded_corner_poll)

            // added tag as label for identification of widget (by default tag will be empty)
            setTagViewWithStyleChanges(context.resources.getString(R.string.livelike_poll_tag))
            binding?.titleView?.title = resource.question
            // TODO: update header background with margin or padding
            binding?.titleView?.titleViewBinding?.titleTextView?.gravity = Gravity.START
            viewModel?.adapter = viewModel?.adapter ?: WidgetOptionsViewAdapter(optionList, type)

            // set on click
            viewModel?.adapter?.onClick = {
                val selectedId = viewModel?.adapter?.myDataset?.get(
                    viewModel?.adapter?.selectedPosition ?: -1
                )?.id ?: ""
                viewModel?.currentVoteId?.onNext(selectedId)
                widgetLifeCycleEventsListener?.onUserInteract(widgetData)
                isFirstInteraction = true
                viewModel?.saveInteraction(it)
            }

            widgetsTheme?.let {
                applyTheme(it)
            }

            viewModel?.onWidgetInteractionCompleted = { onWidgetInteractionCompleted() }

            binding?.textRecyclerView.apply {
                isFirstInteraction = viewModel?.getUserInteraction() != null
                viewModel?.adapter?.restoreSelectedPosition(viewModel?.getUserInteraction()?.optionId)
                this?.adapter = viewModel?.adapter
            }

            logDebug { "showing PollWidget" }
            if (widgetViewModel?.widgetState?.latest() == null || widgetViewModel?.widgetState?.latest() == WidgetStates.READY)
                widgetViewModel?.widgetState?.onNext(WidgetStates.READY)
        }
        if (widget == null) {
            inflated = false
            removeAllViews()
            parent?.let { (it as ViewGroup).removeAllViews() }
        }
    }

    private fun PollWidget.showTimer() {
        showTimer(
            resource.timeout, binding?.textEggTimer,
            {
                viewModel?.animationEggTimerProgress = it
            },
            {
                viewModel?.dismissWidget(it)
            }
        )
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
            if (this.getMergedTotal() > 0) {
                viewModel?.adapter?.showPercentage = true
            }
            binding?.textRecyclerView?.swapAdapter(viewModel?.adapter, false)
            logDebug { "PollWidget Showing result total:$totalVotes" }
        }
    }
}
