@file:Suppress("UNNECESSARY_SAFE_CALL", "UNCHECKED_CAST")

package com.livelike.engagementsdk.widget.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.core.utils.AndroidResource
import com.livelike.engagementsdk.widget.SpecifiedWidgetView
import com.livelike.engagementsdk.widget.model.Resource
import com.livelike.engagementsdk.widget.utils.livelikeSharedPrefs.shouldShowPointTutorial
import com.livelike.engagementsdk.widget.view.components.PointView
import com.livelike.engagementsdk.widget.viewModel.BaseViewModel
import com.livelike.engagementsdk.widget.viewModel.WidgetState
import com.livelike.engagementsdk.widget.viewModel.WidgetStates
import com.livelike.engagementsdk.widget.viewModel.WidgetViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    var isFirstInteraction = false

    override var widgetViewModel: BaseViewModel? = null
        get() = viewModel
        set(value) {
            field = value
            viewModel = value as T
//            subscribeCalls()
        }

    protected open fun stateObserver(widgetState: WidgetState) {
        when (widgetState) {
            WidgetState.LOCK_INTERACTION -> confirmInteraction()
            WidgetState.SHOW_RESULTS -> showResults()
            WidgetState.SHOW_GAMIFICATION -> {
                rewardsObserver()
                if (viewModel?.enableDefaultWidgetTransition) {
                    viewModel?.uiScope.launch {
                        delay(2000)
                        viewModel?.dismissWidget(DismissAction.TIMEOUT)
                    }
                }
            }
            WidgetState.DISMISS -> {
                dataModelObserver(null)
            }
        }
    }

    protected abstract fun showResults()

    protected abstract fun confirmInteraction()

    protected open fun dataModelObserver(entity: Entity?) {
        entity?.let { _ ->
            if (!isViewInflated) {
                isViewInflated = true
                if (widgetViewModel?.widgetState?.latest() == null)
                    widgetViewModel?.widgetState?.onNext(WidgetStates.READY)
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
                findViewById<PointView>(R.id.pointView).startAnimation(it.newPoints, true)
                wouldShowProgressionMeter(viewModel?.rewardsType, it, findViewById(R.id.progressionMeterView))
            }
        }
    }

    protected open fun subscribeCalls() {
        viewModel.data.subscribe(this.hashCode()) {
            dataModelObserver(it)
        }
        viewModel.state.subscribe(this.hashCode()) {
            it?.let { stateObserver(it) }
        }
        widgetViewModel?.widgetState?.subscribe(this.hashCode()) {
            when (it) {
                WidgetStates.READY -> {
                    isFirstInteraction = false
                    lockInteraction()
                }
                WidgetStates.INTERACTING -> {
                    unLockInteraction()
                    showResultAnimation = true
                    viewModel?.data?.latest()?.timeout?.let { timeout ->
                        showTimer(
                            timeout, findViewById(R.id.textEggTimer),
                            {
                                viewModel?.animationEggTimerProgress = it
                            },
                            {
                                viewModel?.dismissWidget(it)
                            }
                        )
                    }
                    findViewById<LinearLayout>(R.id.lay_lock).visibility = View.VISIBLE
                    viewModel?.results?.subscribe(this.hashCode()) {
                        if (isFirstInteraction)
                            showResults()
                    }
                }
                WidgetStates.RESULTS, WidgetStates.FINISHED -> {
                    lockInteraction()
                    onWidgetInteractionCompleted()
                    viewModel?.results?.subscribe(this.hashCode()) { showResults() }
                    // showResults()
                    viewModel.confirmInteraction()
                }
            }

            if (viewModel?.enableDefaultWidgetTransition) {
                defaultStateTransitionManager(it)
            }
        }
    }

    internal abstract fun lockInteraction()

    internal abstract fun unLockInteraction()

    private fun defaultStateTransitionManager(widgetStates: WidgetStates?) {
        when (widgetStates) {
            WidgetStates.READY -> {
                moveToNextState()
            }
            WidgetStates.INTERACTING -> {
                viewModel.data.latest()?.let { entity ->
                    val timeout = AndroidResource.parseDuration(entity.timeout)
                    viewModel.startInteractionTimeout(timeout)
                }
//            viewModel?.data?.latest()?.let {
//                viewModel?.startDismissTimout(it.resource.timeout)
//            }
            }
            WidgetStates.RESULTS -> {
//            viewModel?.confirmationState()
            }
            WidgetStates.FINISHED -> {
                dataModelObserver(null)
            }
        }
    }

    protected open fun unsubscribeCalls() {
        viewModel.state.unsubscribe(this.hashCode())
        viewModel.data.unsubscribe(this.hashCode())
        widgetViewModel?.widgetState?.unsubscribe(this.hashCode())
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
