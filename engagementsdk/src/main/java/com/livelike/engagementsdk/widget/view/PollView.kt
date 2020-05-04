package com.livelike.engagementsdk.widget.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.widget.SpecifiedWidgetView
import com.livelike.engagementsdk.widget.adapters.WidgetOptionsViewAdapter
import com.livelike.engagementsdk.widget.model.Resource
import com.livelike.engagementsdk.widget.utils.livelikeSharedPrefs.shouldShowPointTutorial
import com.livelike.engagementsdk.widget.viewModel.BaseViewModel
import com.livelike.engagementsdk.widget.viewModel.PollViewModel
import com.livelike.engagementsdk.widget.viewModel.PollWidget
import com.livelike.engagementsdk.widget.viewModel.WidgetStates
import kotlinx.android.synthetic.main.atom_widget_title.view.titleTextView
import kotlinx.android.synthetic.main.widget_text_option_selection.view.pointView
import kotlinx.android.synthetic.main.widget_text_option_selection.view.progressionMeterView
import kotlinx.android.synthetic.main.widget_text_option_selection.view.textEggTimer
import kotlinx.android.synthetic.main.widget_text_option_selection.view.textRecyclerView
import kotlinx.android.synthetic.main.widget_text_option_selection.view.titleView
import kotlinx.android.synthetic.main.widget_text_option_selection.view.txtTitleBackground

class PollView(context: Context, attr: AttributeSet? = null) : SpecifiedWidgetView(context, attr) {

    private var viewModel: PollViewModel? = null

    private var inflated = false

    override var dismissFunc: ((DismissAction) -> Unit)? = { viewModel?.dismissWidget(it) }

    override var widgetViewModel: BaseViewModel? = null
        set(value) {
            field = value
            viewModel = value as PollViewModel
//            viewModel?.data?.subscribe(javaClass.simpleName) { resourceObserver(it) }
            viewModel?.widgetState?.subscribe(javaClass.simpleName) { stateObserver(it) }
//            viewModel?.results?.subscribe(javaClass.simpleName) { resultsObserver(it) }
            viewModel?.currentVoteId?.subscribe(javaClass.simpleName) { clickedOptionObserver(it) }
            viewModel?.points?.subscribe(javaClass.simpleName) { rewardsObserver(it) }
        }

    private fun clickedOptionObserver(id: String?) {
        id?.let {
            viewModel?.adapter?.showPercentage = true
            viewModel?.adapter?.notifyDataSetChanged()
            viewModel?.onOptionClicked()
        }
    }

    override fun moveToNextState() {
        super.moveToNextState()
    }

    // Refresh the view when re-attached to the activity
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewModel?.data?.subscribe(javaClass.simpleName) { resourceObserver(it) }
        viewModel?.widgetState?.subscribe(javaClass.simpleName) { stateObserver(it) }
//        viewModel?.results?.subscribe(javaClass.simpleName) { resultsObserver(it) }
        viewModel?.currentVoteId?.subscribe(javaClass.simpleName) { clickedOptionObserver(it) }
        viewModel?.points?.subscribe(javaClass.simpleName) { rewardsObserver(it) }
    }

    private fun stateObserver(widgetStates: WidgetStates?) {
        when (widgetStates) {
            WidgetStates.READY -> {
                lockInteraction()
            }
            WidgetStates.INTERACTING -> {
                unLockInteraction()
//                viewModel?.data?.latest()?.let {
//                    viewModel?.startDismissTimout(it.resource.timeout)
//                }
            }
            WidgetStates.RESULTS -> {
                lockInteraction()
                onWidgetInteractionCompleted()
                viewModel?.results?.subscribe(javaClass.simpleName) { resultsObserver(it) }
            }
            WidgetStates.FINISHED -> {
//                resourceObserver(null)
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
                pointView.startAnimation(it, true)
                wouldShowProgressionMeter(
                    viewModel?.rewardsType,
                    viewModel?.gamificationProfile?.latest(),
                    progressionMeterView
                )
            }
        }
    }

    private fun resourceObserver(widget: PollWidget?) {
        widget?.apply {
            val optionList = resource.getMergedOptions() ?: return
            if (!inflated) {
                inflated = true
                inflate(context, R.layout.widget_text_option_selection, this@PollView)
            }

            titleView.title = resource.question
            txtTitleBackground.setBackgroundResource(R.drawable.header_rounded_corner_poll)
            titleTextView.gravity = Gravity.START

            viewModel?.adapter = viewModel?.adapter ?: WidgetOptionsViewAdapter(optionList, {
                val selectedId = viewModel?.adapter?.myDataset?.get(
                    viewModel?.adapter?.selectedPosition ?: -1
                )?.id ?: ""
                viewModel?.currentVoteId?.onNext(selectedId)
            }, type)
            viewModel?.onWidgetInteractionCompleted = { onWidgetInteractionCompleted() }

            textRecyclerView.apply {
                this.adapter = viewModel?.adapter
            }
            showTimer()
            logDebug { "showing PollWidget" }
            widgetViewModel?.widgetState?.onNext(WidgetStates.READY)
        }
        if (widget == null) {
            inflated = false
            removeAllViews()
            parent?.let { (it as ViewGroup).removeAllViews() }
        }
    }

    private fun PollWidget.showTimer() {
        showTimer(resource.timeout, viewModel?.animationEggTimerProgress, textEggTimer, {
            viewModel?.animationEggTimerProgress = it
        }, {
            viewModel?.dismissWidget(it)
        })
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
            logDebug { "PollWidget Showing result total:$totalVotes" }
        }
    }
}
