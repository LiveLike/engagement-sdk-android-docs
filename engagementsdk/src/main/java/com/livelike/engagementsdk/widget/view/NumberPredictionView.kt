package com.livelike.engagementsdk.widget.view


import android.content.Context
import android.graphics.drawable.StateListDrawable
import android.text.InputType
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.core.utils.AndroidResource
import com.livelike.engagementsdk.databinding.WidgetNumberPredictionBinding
import com.livelike.engagementsdk.widget.NumberPredictionOptionsTheme
import com.livelike.engagementsdk.widget.SpecifiedWidgetView
import com.livelike.engagementsdk.widget.WidgetsTheme
import com.livelike.engagementsdk.widget.adapters.NumberPredictionOptionAdapter
import com.livelike.engagementsdk.widget.utils.livelikeSharedPrefs.getWidgetNumberPredictionVotedAnswerList
import com.livelike.engagementsdk.widget.viewModel.BaseViewModel
import com.livelike.engagementsdk.widget.viewModel.NumberPredictionViewModel
import com.livelike.engagementsdk.widget.viewModel.NumberPredictionWidget
import com.livelike.engagementsdk.widget.viewModel.WidgetStates
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NumberPredictionView(context: Context, attr: AttributeSet? = null) :
    SpecifiedWidgetView(context, attr),NumberPredictionOptionAdapter.EnableSubmitListener  {

    private var viewModel: NumberPredictionViewModel? = null

    private var inflated = false

    private var isFirstInteraction = false

    private var isFollowUp = false

    private var binding: WidgetNumberPredictionBinding? = null

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
                        timeout, binding?.textEggTimer,
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
                binding?.labelLock?.visibility = View.VISIBLE
                onWidgetInteractionCompleted()

                    if (viewModel?.adapter?.selectedUserVotes != null && viewModel?.adapter?.selectedUserVotes!!.isNotEmpty() &&
                        viewModel?.adapter?.selectedUserVotes!!.size == viewModel?.data?.currentData?.resource?.options?.size && viewModel?.numberPredictionFollowUp == false
                    ) {
                        binding?.labelLock?.visibility = View.VISIBLE
                    }else{
                        binding?.labelLock?.visibility = View.GONE
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
                binding = WidgetNumberPredictionBinding.inflate(LayoutInflater.from(context), this@NumberPredictionView, true)
            }
            isFollowUp = resource.kind.contains("follow-up")
            binding?.apply {
                titleView.title = resource.question
                txtTitleBackground.setBackgroundResource(R.drawable.header_rounded_corner_prediciton)
                layTextRecyclerView.setBackgroundResource(R.drawable.body_rounded_corner_prediction)
                titleView.titleViewBinding.titleTextView.gravity = Gravity.START
            }

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

            binding?.textRecyclerView?.apply {
                viewModel?.adapter?.restoreSelectedVotes(viewModel?.getUserInteraction()?.votes)
                this.adapter = viewModel?.adapter
                setHasFixedSize(true)
            }

            binding?.predictBtn?.setOnClickListener {
                if (viewModel?.adapter?.selectedUserVotes!!.isEmpty() ||
                    viewModel?.adapter?.selectedUserVotes!!.size !=  viewModel?.adapter?.myDataset?.size ) return@setOnClickListener

                lockVote()
            }

            if (viewModel?.getUserInteraction() != null) {
                disableLockButton()
                binding?.labelLock?.visibility = View.VISIBLE
            }else{
                binding?.labelLock?.visibility = View.GONE
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
        if(binding?.predictBtn!=null) {
            binding?.predictBtn?.isEnabled = false
        }
    }


    private fun enableLockButton() {
        if(binding?.predictBtn!=null) {
            binding?.predictBtn?.isEnabled = true
            binding?.labelLock?.visibility = GONE
        }
    }


    private fun hideLockButton(){
        binding?.layLock?.visibility = GONE
    }

    private fun showLockButton(){
        binding?.layLock?.visibility = VISIBLE
    }


    private fun lockInteraction() {
        viewModel?.adapter?.selectionLocked = true
        viewModel?.data?.latest()?.let {
            if (isFollowUp && viewModel?.showTimer == true) {
                binding?.textEggTimer?.showCloseButton { viewModel?.dismissWidget(DismissAction.TIMEOUT) }
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
        if(viewModel?.adapter?.binding?.incrementDecrementLayout?.userInput != null) {
            viewModel?.adapter?.binding?.incrementDecrementLayout?.userInput?.imeOptions = EditorInfo.IME_ACTION_DONE
            viewModel?.adapter?.binding?.incrementDecrementLayout?.userInput?.setRawInputType(InputType.TYPE_CLASS_NUMBER)
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
        binding?.labelLock?.visibility = View.VISIBLE
    }


    override fun applyTheme(theme: WidgetsTheme) {
        super.applyTheme(theme)
        viewModel?.data?.latest()?.let { widget ->
            theme.getThemeLayoutComponent(widget.type)?.let { themeComponent ->
                if (themeComponent is NumberPredictionOptionsTheme) {
                    applyThemeOnTitleView(themeComponent)
                    applyThemeOnTagView(themeComponent)
                    AndroidResource.createDrawable(themeComponent.body)?.let {
                        binding?.layTextRecyclerView?.background = it
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
                    binding?.predictBtn?.background = state

                    //confirmation label theme
                    AndroidResource.updateThemeForView(
                        binding?.labelLock,
                        themeComponent.confirmation,
                        fontFamilyProvider
                    )
                    if (themeComponent.confirmation?.background != null) {
                        binding?.labelLock?.background = AndroidResource.createDrawable(themeComponent.confirmation)
                    }
                    themeComponent.confirmation?.padding?.let {
                        AndroidResource.setPaddingForView(binding?.labelLock, themeComponent.confirmation.padding)
                    }
                }
            }
        }
    }

}