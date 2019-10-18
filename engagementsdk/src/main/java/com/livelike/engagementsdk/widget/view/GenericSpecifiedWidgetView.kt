@file:Suppress("UNNECESSARY_SAFE_CALL", "UNCHECKED_CAST")

package com.livelike.engagementsdk.widget.view

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.utils.AndroidResource
import com.livelike.engagementsdk.utils.liveLikeSharedPrefs.shouldShowPointTutorial
import com.livelike.engagementsdk.widget.SpecifiedWidgetView
import com.livelike.engagementsdk.widget.model.Resource
import com.livelike.engagementsdk.widget.viewModel.ViewModel
import com.livelike.engagementsdk.widget.viewModel.WidgetState
import com.livelike.engagementsdk.widget.viewModel.WidgetViewModel
import kotlinx.android.synthetic.main.widget_text_option_selection.view.pointView
import kotlinx.android.synthetic.main.widget_text_option_selection.view.progressionMeterView
import kotlinx.android.synthetic.main.widget_text_option_selection.view.textEggTimer

/**
 * For now creating separate class, will mere it with specified widget view after full assessment of other widget views and then move all widget views to inherit this
 * Also For now Doing minimal refactor to expedite image slider delivery.
 */

internal abstract class GenericSpecifiedWidgetView<Entity : Resource, T : WidgetViewModel<Entity>>(
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
            WidgetState.LOCK_INTERACTION -> confirmInteraction()
            WidgetState.SHOW_RESULTS -> showResults()
            WidgetState.SHOW_GAMIFICATION -> rewardsObserver()
            WidgetState.DISMISS -> {}
        }
    }

    protected abstract fun showResults()

    protected abstract fun confirmInteraction()

    protected open fun dataModelObserver(entity: Entity?) {
        entity?.let { _ ->
            val timeout = AndroidResource.parseDuration(entity.timeout)
            if (!isViewInflated) {
                isViewInflated = true
                viewModel.startInteractionTimeout(timeout)
            }
            val animationLength = timeout.toFloat()
            if (viewModel.animationEggTimerProgress < 1f) {
                viewModel.animationEggTimerProgress.let {
                    textEggTimer?.startAnimationFrom(it, animationLength, { animationTimerProgress ->
                        viewModel.animationEggTimerProgress = animationTimerProgress
                    }, { dismissAction ->
                        viewModel.dismissWidget(dismissAction)
                    })
                }
            }
        }
        if (entity == null) {
            isViewInflated = false
            removeAllViews()
            parent?.let { (it as ViewGroup).removeAllViews() }
        }
    }

    private fun rewardsObserver() {
        viewModel.gamificationProfile?.latest()?.let {
            if (!shouldShowPointTutorial() && it.newPoints > 0) {
                pointView.startAnimation(it.newPoints, true)
                wouldShowProgressionMeter(viewModel?.rewardsType, it, progressionMeterView)
            }
        }
    }

    protected open fun subscribeCalls() {
        viewModel.state.subscribe(javaClass.name) {
            it?.let { stateObserver(it) }
        }
        viewModel.data.subscribe(javaClass.simpleName) {
            dataModelObserver(it)
        }
    }

    protected open fun unsubscribeCalls() {
        viewModel.state.unsubscribe(javaClass.name)
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
