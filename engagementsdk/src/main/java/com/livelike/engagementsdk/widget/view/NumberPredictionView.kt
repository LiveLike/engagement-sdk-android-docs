package com.livelike.engagementsdk.widget.view

import android.animation.Animator
import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.LinearLayout
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.core.utils.AndroidResource
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.widget.NumberPredictionOptionsTheme
import com.livelike.engagementsdk.widget.SpecifiedWidgetView
import com.livelike.engagementsdk.widget.WidgetsTheme
import com.livelike.engagementsdk.widget.adapters.NumberPredictionOptionAdapter
import com.livelike.engagementsdk.widget.utils.livelikeSharedPrefs.getWidgetNumberPredictionVotedAnswerList
import com.livelike.engagementsdk.widget.viewModel.BaseViewModel
import com.livelike.engagementsdk.widget.viewModel.NumberPredictionViewModel
import com.livelike.engagementsdk.widget.viewModel.NumberPredictionWidget
import com.livelike.engagementsdk.widget.viewModel.WidgetStates
import kotlinx.android.synthetic.main.atom_widget_title.view.titleTextView
import kotlinx.android.synthetic.main.common_lock_btn_lay.view.btn_lock
import kotlinx.android.synthetic.main.common_lock_btn_lay.view.label_lock
import kotlinx.android.synthetic.main.livelike_user_input.view.userInput
import kotlinx.android.synthetic.main.widget_text_option_selection.view.confirmationMessage
import kotlinx.android.synthetic.main.widget_text_option_selection.view.followupAnimation
import kotlinx.android.synthetic.main.widget_text_option_selection.view.lay_textRecyclerView
import kotlinx.android.synthetic.main.widget_text_option_selection.view.textEggTimer
import kotlinx.android.synthetic.main.widget_text_option_selection.view.textRecyclerView
import kotlinx.android.synthetic.main.widget_text_option_selection.view.titleView
import kotlinx.android.synthetic.main.widget_text_option_selection.view.txtTitleBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NumberPredictionView(context: Context, attr: AttributeSet? = null) :
    SpecifiedWidgetView(context, attr),NumberPredictionOptionAdapter.EnableSubmitListener  {

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
        viewModel?.data?.unsubscribe(javaClass.simpleName)
        viewModel?.widgetState?.unsubscribe(javaClass.simpleName)
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
                showResultAnimation = true
                disableLockButton()
                if(viewModel?.adapter?.selectedUserVotes != null) {
                    if (viewModel?.adapter?.selectedUserVotes!!.isNotEmpty() &&
                        viewModel?.adapter?.selectedUserVotes!!.size == viewModel?.data?.currentData?.resource?.options?.size
                    ) {
                        label_lock.visibility = View.VISIBLE
                    }
                }
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
                    }else{
                        confirmationMessage?.apply {
                           // if (isFirstInteraction) {
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
                           // }
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

            // added tag for identification of widget (by default will be empty)
            if(isFollowUp){
                setTagViewWithStyleChanges(context.resources.getString(R.string.livelike_number_prediction_follow_up_tag))
            }else{
                setTagViewWithStyleChanges(context.resources.getString(R.string.livelike_number_prediction_tag))
            }

            viewModel?.adapter = viewModel?.adapter ?: NumberPredictionOptionAdapter(optionList, type)
            viewModel?.adapter?.apply {
                this.submitListener = this@NumberPredictionView
            }

            disableLockButton()
            if (!isFollowUp)
                viewModel?.apply {
                    val rootPath = widgetViewThemeAttributes.stayTunedAnimation
                    animationPath = AndroidResource.selectRandomLottieAnimation(
                        rootPath,
                        context.applicationContext
                    ) ?: ""
                }

            textRecyclerView.apply {
                viewModel?.adapter?.restoreSelectedVotes(viewModel?.getUserInteraction()?.votes)
                this.adapter = viewModel?.adapter
                setHasFixedSize(true)
            }

            btn_lock.setOnClickListener {
                if (viewModel?.adapter?.selectedUserVotes!!.isEmpty() ||
                    viewModel?.adapter?.selectedUserVotes!!.size !=  viewModel?.adapter?.myDataset?.size ) return@setOnClickListener

                lockVote()
                textEggTimer.showCloseButton { viewModel?.dismissWidget(DismissAction.TIMEOUT) }
               // viewModel?.adapter?.notifyDataSetChanged()
            }

            if (viewModel?.getUserInteraction() != null) {
                disableLockButton()
            }

            widgetsTheme?.let {
                applyTheme(it)
            }

            if (isFollowUp) {
                val selectedPredictionVoteList =
                    getWidgetNumberPredictionVotedAnswerList(if (resource.textNumberPredictionId.isNullOrEmpty()) resource.imageNumberPredictionId else resource.textNumberPredictionId)
                if(selectedPredictionVoteList.isNotEmpty()) {
                    viewModel?.followupState(
                        selectedPredictionVoteList,
                        widgetViewThemeAttributes
                    )
                }
            }
            setImeOptionDoneInKeyboard()

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
               // nothing needed here
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
        val lockBtn = findViewById<Button>(R.id.btn_lock)
        findViewById<LinearLayout>(R.id.lay_lock)?.visibility = VISIBLE
        lockBtn?.isEnabled = false
        lockBtn?.alpha = 0.5f
    }


    private fun enableLockButton() {
        btn_lock.isEnabled = true
        btn_lock.alpha = 1f
    }


    private fun lockInteraction() {
        viewModel?.adapter?.selectionLocked = true
        viewModel?.data?.latest()?.let {
            val isFollowUp = it.resource.kind.contains("follow-up")
            if (isFollowUp && viewModel?.showTimer == true) {
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

    /** changes the return key as done in keyboard */
    private fun setImeOptionDoneInKeyboard() {
        if(userInput != null) {
            userInput.imeOptions = EditorInfo.IME_ACTION_DONE
            userInput.setRawInputType(InputType.TYPE_CLASS_NUMBER)
        }
    }

    override fun onSubmitEnabled(isSubmitBtnEnabled: Boolean) {
       if(isSubmitBtnEnabled) {
           enableLockButton()
       }else{
           disableLockButton()
       }
    }

    /** submits user's vote */
    private fun lockVote() {
        isFirstInteraction = true
        disableLockButton()
        viewModel?.run {
            timeOutJob?.cancel()
             uiScope.launch {
                lockInteractionAndSubmitVote()
            }
        }
        label_lock.visibility = View.VISIBLE
    }


    override fun applyTheme(theme: WidgetsTheme) {
        super.applyTheme(theme)
        viewModel?.data?.latest()?.let { widget ->
            theme.getThemeLayoutComponent(widget.type)?.let { themeComponent ->
                if (themeComponent is NumberPredictionOptionsTheme) {
                    applyThemeOnTitleView(themeComponent)
                    applyThemeOnTagView(themeComponent)
                    AndroidResource.createDrawable(themeComponent.body)?.let {
                        lay_textRecyclerView.background = it
                    }
                   viewModel?.adapter?.component = themeComponent
                    viewModel?.adapter?.notifyDataSetChanged()
                }

            }
        }
    }

}