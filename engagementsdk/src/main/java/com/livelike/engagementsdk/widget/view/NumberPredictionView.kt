package com.livelike.engagementsdk.widget.view


import android.content.Context
import android.graphics.drawable.StateListDrawable
import android.text.InputType
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.core.utils.AndroidResource
import com.livelike.engagementsdk.widget.NumberPredictionOptionsTheme
import com.livelike.engagementsdk.widget.SpecifiedWidgetView
import com.livelike.engagementsdk.widget.WidgetsTheme
import com.livelike.engagementsdk.widget.adapters.NumberPredictionOptionAdapter
import com.livelike.engagementsdk.widget.utils.livelikeSharedPrefs.getWidgetNumberPredictionVotedAnswerList
import com.livelike.engagementsdk.widget.viewModel.BaseViewModel
import com.livelike.engagementsdk.widget.viewModel.NumberPredictionViewModel
import com.livelike.engagementsdk.widget.viewModel.NumberPredictionWidget
import com.livelike.engagementsdk.widget.viewModel.WidgetStates
import kotlinx.android.synthetic.main.atom_widget_title.view.*
import kotlinx.android.synthetic.main.common_lock_btn_lay.view.*
import kotlinx.android.synthetic.main.livelike_user_input.view.*
import kotlinx.android.synthetic.main.widget_number_prediction.view.*
import kotlinx.android.synthetic.main.widget_number_prediction.view.label_lock
import kotlinx.android.synthetic.main.widget_number_prediction.view.lay_lock
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NumberPredictionView(context: Context, attr: AttributeSet? = null) :
    SpecifiedWidgetView(context, attr),NumberPredictionOptionAdapter.EnableSubmitListener  {

    private var viewModel: NumberPredictionViewModel? = null

    private var inflated = false

    private var isFirstInteraction = false

    private var isFollowUp = false

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
                disableLockButton()
                label_lock.visibility = View.VISIBLE
                onWidgetInteractionCompleted()

                    if (viewModel?.adapter?.selectedUserVotes != null && viewModel?.adapter?.selectedUserVotes!!.isNotEmpty() &&
                        viewModel?.adapter?.selectedUserVotes!!.size == viewModel?.data?.currentData?.resource?.options?.size && viewModel?.numberPredictionFollowUp == false
                    ) {
                        label_lock.visibility = View.VISIBLE
                    }else{
                        label_lock.visibility = View.GONE
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
                inflate(context, R.layout.widget_number_prediction, this@NumberPredictionView)
            }
            isFollowUp = resource.kind.contains("follow-up")
            titleView.title = resource.question
            txtTitleBackground.setBackgroundResource(R.drawable.header_rounded_corner_prediciton)
            lay_textRecyclerView.setBackgroundResource(R.drawable.body_rounded_corner_prediction)
            titleTextView.gravity = Gravity.START

            // added tag for identification of widget (by default will be empty)
            if(isFollowUp){
                setTagViewWithStyleChanges(context.resources.getString(R.string.livelike_number_prediction_follow_up_tag))
                hideLockButton()
            }else{
                setTagViewWithStyleChanges(context.resources.getString(R.string.livelike_number_prediction_tag))
                showLockButton()
            }

            viewModel?.adapter = viewModel?.adapter ?: NumberPredictionOptionAdapter(optionList, type)
            viewModel?.adapter?.apply {
                this.submitListener = this@NumberPredictionView
            }

            disableLockButton()

            textRecyclerView.apply {
                viewModel?.adapter?.restoreSelectedVotes(viewModel?.getUserInteraction()?.votes)
                this.adapter = viewModel?.adapter
                setHasFixedSize(true)
            }

            predictBtn.setOnClickListener {
                if (viewModel?.adapter?.selectedUserVotes!!.isEmpty() ||
                    viewModel?.adapter?.selectedUserVotes!!.size !=  viewModel?.adapter?.myDataset?.size ) return@setOnClickListener

                lockVote()
            }

            if (viewModel?.getUserInteraction() != null) {
                disableLockButton()
                label_lock.visibility = View.VISIBLE
            }else{
                label_lock.visibility = View.GONE
            }

            widgetsTheme?.let {
                applyTheme(it)
            }

            if (isFollowUp) {
                val selectedPredictionVoteList =
                    getWidgetNumberPredictionVotedAnswerList(if (resource.textNumberPredictionId.isNullOrEmpty()) resource.imageNumberPredictionId else resource.textNumberPredictionId)
                    viewModel?.followupState(
                        selectedPredictionVoteList
                    )
            }
            setImeOptionDoneInKeyboard()

            if (widgetViewModel?.widgetState?.latest() == null || widgetViewModel?.widgetState?.latest() == WidgetStates.READY)
                widgetViewModel?.widgetState?.onNext(WidgetStates.READY)

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
                    viewModel?.startDismissTimeout(
                        it.resource.timeout,
                        isFollowUp,
                        widgetViewThemeAttributes
                    )
                }
            }
            WidgetStates.RESULTS -> {
                viewModel?.uiScope?.launch {
                    if(isFollowUp) {
                        viewModel?.data?.latest()?.let {
                            delay(AndroidResource.parseDuration(it.resource.timeout))
                            viewModel?.dismissWidget(DismissAction.TIMEOUT)
                        }
                    }
                }
            }
            WidgetStates.FINISHED -> {
                widgetObserver(null)
            }
        }
    }

    private fun disableLockButton() {
        if(predictBtn!=null) {
            predictBtn.isEnabled = false
        }
    }


    private fun enableLockButton() {
        if(predictBtn!=null) {
            predictBtn.isEnabled = true
            label_lock.visibility = GONE
        }
    }


    private fun hideLockButton(){
        lay_lock.visibility = GONE
    }

    private fun showLockButton(){
        lay_lock.visibility = VISIBLE
    }


    private fun lockInteraction() {
        viewModel?.adapter?.selectionLocked = true
        viewModel?.data?.latest()?.let {
            if (isFollowUp && viewModel?.showTimer == true) {
                textEggTimer.showCloseButton { viewModel?.dismissWidget(DismissAction.TIMEOUT) }
            }
        }
    }

    private fun unLockInteraction() {
        viewModel?.data?.latest()?.let {
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
        viewModel?.run {
             uiScope.launch {
                lockInteractionAndSubmitVote()
            }
        }
        viewModel?.adapter?.selectionLocked = true
        viewModel?.adapter?.notifyDataSetChanged()

        disableLockButton()
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

                    // submit button drawables with state
                    val submitButtonEnabledDrawable = AndroidResource.createDrawable(
                        themeComponent.submitButtonEnabled
                    )
                    val submitButtonDisabledDrawable = AndroidResource.createDrawable(
                        themeComponent.submitButtonDisabled
                    )
                    val state = StateListDrawable()
                    state.addState(intArrayOf(android.R.attr.state_enabled), submitButtonEnabledDrawable)
                    state.addState(intArrayOf(), submitButtonDisabledDrawable)
                    predictBtn?.background = state

                    //confirmation label theme
                    AndroidResource.updateThemeForView(
                        label_lock,
                        themeComponent.confirmation,
                        fontFamilyProvider
                    )
                    if (themeComponent.confirmation?.background != null) {
                        label_lock?.background = AndroidResource.createDrawable(themeComponent.confirmation)
                    }
                    themeComponent.confirmation?.padding?.let {
                        AndroidResource.setPaddingForView(label_lock, themeComponent.confirmation.padding)
                    }
                }
            }
        }
    }

}