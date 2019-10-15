package com.livelike.engagementsdk.widget.view

import android.content.Context
import android.util.AttributeSet
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.utils.liveLikeSharedPrefs.shouldShowPointTutorial
import com.livelike.engagementsdk.widget.SpecifiedWidgetView
import com.livelike.engagementsdk.widget.model.Resource
import com.livelike.engagementsdk.widget.viewModel.ViewModel
import com.livelike.engagementsdk.widget.viewModel.WidgetState
import com.livelike.engagementsdk.widget.viewModel.WidgetViewModel
import kotlinx.android.synthetic.main.widget_text_option_selection.view.pointView
import kotlinx.android.synthetic.main.widget_text_option_selection.view.progressionMeterView

/**
 * For now creating separate class, will mere it with specified widget view after full assessment of other widget views and then move all widget views to inherit this
 * Also For now Doing minimal refactor to expedite image slider delivery.
 */

internal abstract class GenericSpecifiedWidgetView<T : WidgetViewModel<Resource>>(
    context: Context,
    attr: AttributeSet? = null
) : SpecifiedWidgetView(context, attr) {

    // Move viewmodel to constructor
    lateinit var viewModel: T

    override var dismissFunc: ((DismissAction) -> Unit)? = { viewModel?.dismissWidget(it) }

    var isViewInflated = false

    override var widgetViewModel: ViewModel? = null
        get() = super.widgetViewModel
        set(value) {
            field = value
            viewModel = value as T
            subscribeCalls()
        }

    protected open fun stateObserver(widgetState: WidgetState) {
        when (widgetState) {
            WidgetState.CONFIRM_INTERACTION -> confirmInteraction()
            WidgetState.SHOW_RESULTS -> showResults()
            WidgetState.SHOW_GAMIFICATION -> rewardsObserver()
        }
    }

    protected abstract fun showResults()

    protected abstract fun confirmInteraction()

    protected fun rewardsObserver() {
        viewModel.gamificationProfile?.latest()?.let {
            if (!shouldShowPointTutorial()) {
                pointView.startAnimation(it.newPoints, true)
                wouldShowProgressionMeter(viewModel?.rewardsType, it, progressionMeterView)
            }
        }
    }

    protected open fun subscribeCalls() {
        viewModel.state.subscribe(javaClass.name) {
            it?.let { stateObserver(it) }
        }
    }

    protected open fun unsubscribeCalls() {
        viewModel.state.unsubscribe(javaClass.name)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        subscribeCalls()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        unsubscribeCalls()
    }
}
