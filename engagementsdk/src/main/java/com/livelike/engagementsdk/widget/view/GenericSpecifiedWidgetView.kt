package com.livelike.engagementsdk.widget.view

import android.content.Context
import android.util.AttributeSet
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.data.models.ProgramGamificationProfile
import com.livelike.engagementsdk.utils.liveLikeSharedPrefs.shouldShowPointTutorial
import com.livelike.engagementsdk.widget.SpecifiedWidgetView
import com.livelike.engagementsdk.widget.viewModel.ViewModel
import com.livelike.engagementsdk.widget.viewModel.WidgetViewModel
import kotlinx.android.synthetic.main.widget_text_option_selection.view.pointView
import kotlinx.android.synthetic.main.widget_text_option_selection.view.progressionMeterView

/**
 * For now creating separate class, will mere it with specified widget view after assessment of all widget views and then move all widget views to inherit this
 * Also For now Doing minimal refactor to expedite image slider delivery.
 */

internal abstract class GenericSpecifiedWidgetView<T : WidgetViewModel>(context: Context, attr: AttributeSet? = null) : SpecifiedWidgetView(context, attr) {

    var viewModel: T? = null

    override var dismissFunc: ((DismissAction) -> Unit)? = { viewModel?.dismissWidget(it) }

    var isViewInflated = false

    override var widgetViewModel: ViewModel? = null
        get() = super.widgetViewModel
        set(value) {
            field = value
            viewModel = value as T
            subscribeCalls()
        }

    fun stateObserver() {
        TODO("handling states like confirm, results, results + followup etc ")
    }

    protected fun rewardsObserver(gamificationProfile: ProgramGamificationProfile?) {
        gamificationProfile?.let {
            if (!shouldShowPointTutorial()) {
                pointView.startAnimation(it.newPoints, true)
                wouldShowProgressionMeter(viewModel?.rewardsType, it, progressionMeterView)
            }
        }
    }

    protected abstract fun subscribeCalls()

    protected abstract fun unSubscribeCalls()

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        subscribeCalls()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        unSubscribeCalls()
    }
}
