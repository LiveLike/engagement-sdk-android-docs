package com.livelike.engagementsdk.widget.view

import android.animation.Animator
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.widget.SpecifiedWidgetView
import com.livelike.engagementsdk.widget.adapters.NumberPredictionOptionAdapter
import com.livelike.engagementsdk.widget.utils.livelikeSharedPrefs.getWidgetNumberPredictionVotedAnswerList
import com.livelike.engagementsdk.widget.utils.livelikeSharedPrefs.getWidgetPredictionVotedAnswerIdOrEmpty
import com.livelike.engagementsdk.widget.utils.livelikeSharedPrefs.shouldShowPointTutorial
import com.livelike.engagementsdk.widget.viewModel.BaseViewModel
import com.livelike.engagementsdk.widget.viewModel.NumberPredictionViewModel
import com.livelike.engagementsdk.widget.viewModel.NumberPredictionWidget
import com.livelike.engagementsdk.widget.viewModel.WidgetStates
import kotlinx.android.synthetic.main.atom_widget_title.view.titleTextView
import kotlinx.android.synthetic.main.common_lock_btn_lay.view.btn_lock
import kotlinx.android.synthetic.main.common_lock_btn_lay.view.label_lock
import kotlinx.android.synthetic.main.common_lock_btn_lay.view.lay_lock
import kotlinx.android.synthetic.main.widget_text_option_selection.view.confirmationMessage
import kotlinx.android.synthetic.main.widget_text_option_selection.view.followupAnimation
import kotlinx.android.synthetic.main.widget_text_option_selection.view.lay_textRecyclerView
import kotlinx.android.synthetic.main.widget_text_option_selection.view.pointView
import kotlinx.android.synthetic.main.widget_text_option_selection.view.progressionMeterView
import kotlinx.android.synthetic.main.widget_text_option_selection.view.textEggTimer
import kotlinx.android.synthetic.main.widget_text_option_selection.view.textRecyclerView
import kotlinx.android.synthetic.main.widget_text_option_selection.view.titleView
import kotlinx.android.synthetic.main.widget_text_option_selection.view.txtTitleBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NumberPredictionView(context: Context, attr: AttributeSet? = null) : SpecifiedWidgetView(context, attr)  {

    private var viewModel: NumberPredictionViewModel? = null

    private var inflated = false

    private var isFirstInteraction = false

    override var dismissFunc: ((action: DismissAction) -> Unit)? = { viewModel?.dismissWidget(it) }

    override var widgetViewModel: BaseViewModel? = null
        get() = viewModel
        set(value) {
            field = value
            viewModel = value as NumberPredictionViewModel
        }

    init {
        isFirstInteraction = viewModel?.getUserInteraction() != null
    }


    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        widgetObserver(viewModel?.data?.latest())
        viewModel?.widgetState?.subscribe(javaClass.simpleName) { widgetStateObserver(it) }
    }


    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
    }


    private fun widgetStateObserver(widgetStates: WidgetStates?){
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
                        timeout, textEggTimer,
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
                        followupAnimation?.apply {
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

                    }
                }
            }
        }
        if (viewModel?.enableDefaultWidgetTransition == true) {
            defaultStateTransitionManager(widgetStates)
        }
    }


    private fun widgetObserver(widget: NumberPredictionWidget?) {
        widget?.apply {
            val optionList = resource.getMergedOptions() ?: return
            if (!inflated) {
                inflated = true
                inflate(context, R.layout.widget_text_option_selection, this@NumberPredictionView)
            }
            val isFollowUp = resource.kind.contains("follow-up")
            titleView.title = resource.question
            txtTitleBackground.setBackgroundResource(R.drawable.header_rounded_corner_prediciton)
            lay_textRecyclerView.setBackgroundResource(R.drawable.body_rounded_corner_prediction)
            titleTextView.gravity = Gravity.START


            btn_lock.text = context.resources.getString(R.string.livelike_predict_label)
            label_lock.text = context.resources.getString(R.string.livelike_predicted_label)

            viewModel?.adapter = viewModel?.adapter ?: NumberPredictionOptionAdapter(optionList, type)

            disableLockButton()
            textRecyclerView.apply {
                this.adapter = viewModel?.adapter
                setHasFixedSize(true)
            }


            if (isFollowUp) {
                val selectedPredictionVoteList =
                    getWidgetNumberPredictionVotedAnswerList(if (resource.text_prediction_id.isNullOrEmpty()) resource.image_prediction_id else resource.text_prediction_id)
               /* viewModel?.followupState(
                    selectedPredictionVoteList,
                    resource.correct_option_id,
                    widgetViewThemeAttributes
                )*/
            }


            if (widgetViewModel?.widgetState?.latest() == null || widgetViewModel?.widgetState?.latest() == WidgetStates.READY)
                widgetViewModel?.widgetState?.onNext(WidgetStates.READY)
            logDebug { "showing NumberPredictionWidget" }
        }

        if (widget == null) {
            inflated = false
            removeAllViews()
            parent?.let { (it as ViewGroup).removeAllViews() }
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
                    viewModel?.startDismissTimeout(
                        it.resource.timeout,
                        isFollowUp,
                        widgetViewThemeAttributes
                    )
                }
            }
            WidgetStates.RESULTS -> {
                if (!isFirstInteraction) {
                    viewModel?.dismissWidget(DismissAction.TIMEOUT)
                }
                followupAnimation.apply {
                    addAnimatorListener(object : Animator.AnimatorListener {
                        override fun onAnimationRepeat(animation: Animator?) {
                        }

                        override fun onAnimationEnd(animation: Animator?) {
                            viewModel?.uiScope?.launch {
                                delay(11000)
                                viewModel?.dismissWidget(DismissAction.TIMEOUT)
                            }
                        }

                        override fun onAnimationCancel(animation: Animator?) {
                        }

                        override fun onAnimationStart(animation: Animator?) {
                        }
                    })
                }
            }
            WidgetStates.FINISHED -> {
                widgetObserver(null)
            }
        }
    }

    private fun disableLockButton() {
        lay_lock.visibility = VISIBLE
        btn_lock.isEnabled = false
        btn_lock.alpha = 0.5f
    }


    private fun enableLockButton() {
        btn_lock.isEnabled = true
        btn_lock.alpha = 1f
    }


    private fun lockInteraction() {
        viewModel?.adapter?.selectionLocked = true
        viewModel?.data?.latest()?.let {
            val isFollowUp = it.resource.kind.contains("follow-up")
            if (isFollowUp) {
                textEggTimer.showCloseButton { viewModel?.dismissWidget(DismissAction.TIMEOUT) }

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
}