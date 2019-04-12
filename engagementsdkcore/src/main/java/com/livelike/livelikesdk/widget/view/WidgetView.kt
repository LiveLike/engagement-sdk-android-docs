package com.livelike.livelikesdk.widget.view

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.google.gson.JsonObject
import com.livelike.engagementsdkapi.LiveLikeContentSession
import com.livelike.engagementsdkapi.WidgetEvent
import com.livelike.engagementsdkapi.WidgetEventListener
import com.livelike.engagementsdkapi.WidgetRenderer
import com.livelike.engagementsdkapi.WidgetStateProcessor
import com.livelike.engagementsdkapi.WidgetTransientState
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.analytics.analyticService
import com.livelike.livelikesdk.animation.AnimationProperties
import com.livelike.livelikesdk.animation.ViewAnimationManager
import com.livelike.livelikesdk.parser.WidgetParser
import com.livelike.livelikesdk.util.AndroidResource.Companion.dpToPx
import com.livelike.livelikesdk.util.AndroidResource.Companion.pxToDp
import com.livelike.livelikesdk.util.gson
import com.livelike.livelikesdk.util.liveLikeSharedPrefs.addWidgetPredictionVoted
import com.livelike.livelikesdk.util.logDebug
import com.livelike.livelikesdk.util.logError
import com.livelike.livelikesdk.util.logVerbose
import com.livelike.livelikesdk.widget.WidgetType
import com.livelike.livelikesdk.widget.model.Alert
import com.livelike.livelikesdk.widget.model.Resource
import com.livelike.livelikesdk.widget.model.Widget
import com.livelike.livelikesdk.widget.view.poll.PollTextWidget
import com.livelike.livelikesdk.widget.view.prediction.image.PredictionImageFollowupWidget
import com.livelike.livelikesdk.widget.view.prediction.image.PredictionImageQuestionWidget
import com.livelike.livelikesdk.widget.view.prediction.text.PredictionTextFollowUpWidgetView
import com.livelike.livelikesdk.widget.view.prediction.text.PredictionTextQuestionWidgetView
import com.livelike.livelikesdk.widget.view.quiz.QuizImageWidget
import com.livelike.livelikesdk.widget.view.quiz.QuizTextWidget
import kotlinx.android.synthetic.main.widget_view.view.*


class WidgetView(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs), WidgetRenderer {
    override var widgetListener: WidgetEventListener? = null
    override var widgetStateProcessor: WidgetStateProcessor? = null
    private var currentWidget: Widget? = null
    private var viewRoot: View = LayoutInflater.from(context).inflate(R.layout.widget_view, this, true)
    private var containerView = widgetContainerView as FrameLayout
    private var marginSize = dpToPx(40)
    private var pieTimerAnimatorStartValue = 0f
    private var timeout = 0L

    companion object {
        private const val WIDGET_MINIMUM_SIZE_DP = 260
    }

    private fun verifyViewMinWidth(view: View) {
        visibility = View.VISIBLE
        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val width = pxToDp(view.width)
        if (width < WIDGET_MINIMUM_SIZE_DP) {
            visibility = View.GONE
            logError { "The Widget zone is too small to be displayed. Minimum size is $WIDGET_MINIMUM_SIZE_DP dp. Measured size here is: $width dp" }
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        verifyViewMinWidth(viewRoot)
    }

    fun setSession(session: LiveLikeContentSession) {
        session.widgetRenderer = this
        val currentWidget = widgetStateProcessor?.currentWidget ?: return
        val widgetState = widgetStateProcessor?.getWidgetState(currentWidget)
        widgetState?.payload?.let { displayWidget(widgetState.type.toString(), it, widgetState) }
    }

    override fun displayWidget(
        type: String,
        payload: JsonObject,
        previousState: WidgetTransientState?
    ) {
        logDebug { "NOW - Show Widget $type on screen: $payload" }
        val widget = Widget()
        val parser = WidgetParser()
        val widgetResource = gson.fromJson(payload, Resource::class.java)
        val parentWidth = this.width - marginSize

        val newState = WidgetTransientState()
        when (WidgetType.fromString(type)) {
            WidgetType.TEXT_PREDICTION -> {
                parser.parseTextOptionCommon(widget, widgetResource)
                val predictionWidget = PredictionTextQuestionWidgetView(context, null, 0)
                widget.registerObserver(predictionWidget)

                val id = widget.id.toString()
                timeout = widget.timeout

                if (previousState != null) {
                    restoreState(previousState)
                    widget.optionSelectedUpdated(previousState.userSelection)
                }

                predictionWidget.initialize(
                    { dismissCurrentWidget() },
                    timeout,
                    parentWidth,
                    ViewAnimationManager(predictionWidget).apply {
                        initializeTimerProperties(AnimationProperties(pieTimerAnimatorStartValue))
                    },
                    { saveState(id, newState, payload, type, it) }
                )

                widget.notifyDataSetChange()
                predictionWidget.userTappedCallback { emitWidgetOptionSelected(widget.id, widgetResource.kind) }
                containerView.addView(predictionWidget)

                widgetShown(widgetResource)
                currentWidget = widget
            }

            WidgetType.TEXT_PREDICTION_RESULTS -> {
                parser.parsePredictionFollowup(widget, widgetResource)
                val predictionWidget = PredictionTextFollowUpWidgetView(context, null, 0)
                widget.registerObserver(predictionWidget)
                if (widget.optionSelected.id.isNullOrEmpty()) {
                    //user did not interact with previous widget, mark dismissed and don't show followup
                    widgetListener?.onWidgetEvent(WidgetEvent.WIDGET_DISMISS)
                    return
                }

                timeout = widget.timeout

                if (previousState != null) {
                    restoreState(previousState)
                }

                predictionWidget.initialize({
                    dismissCurrentWidget() },
                    timeout,
                    parentWidth,
                    ViewAnimationManager(predictionWidget).apply {
                        initializeResultProperties(AnimationProperties(pieTimerAnimatorStartValue, 1f, previousState?.resultAnimationPath))
                    },
                    { saveState(widget.id.toString(), newState, payload, type, it) }
                )
                widget.notifyDataSetChange()

                containerView.addView(predictionWidget)
                widgetShown(widgetResource)
                currentWidget = widget
            }

            WidgetType.IMAGE_PREDICTION -> {
                parser.parseTextOptionCommon(widget, widgetResource)
                val predictionWidget = PredictionImageQuestionWidget(context, null, 0)

                newState?.timeout = widget.timeout
                predictionWidget.initialize({ dismissCurrentWidget() }, widget.timeout, parentWidth, ViewAnimationManager(predictionWidget))
                widget.id?.let { newState?.let { it1 -> widgetStateProcessor?.updateWidgetState(it, it1) } }
                predictionWidget.layoutParams = layoutParams
                widget.registerObserver(predictionWidget)
                widget.notifyDataSetChange()
                predictionWidget.userTappedCallback {
                    emitWidgetOptionSelected(widget.id, widgetResource.kind)
                }
                containerView.addView(predictionWidget)
                widgetShown(widgetResource)
                currentWidget = widget
            }

            WidgetType.IMAGE_PREDICTION_RESULTS -> {
                parser.parsePredictionFollowup(widget, widgetResource)
                val predictionWidget = PredictionImageFollowupWidget(context, null, 0)
                newState?.timeout = widget.timeout
                predictionWidget.initialize({ dismissCurrentWidget() }, widget.timeout, parentWidth, ViewAnimationManager(predictionWidget))

                predictionWidget.layoutParams = layoutParams

                widget.registerObserver(predictionWidget)
                widget.notifyDataSetChange()
                if (widget.optionSelected.id.isNullOrEmpty()) {
                    //user did not interact with previous widget, mark dismissed and don't show followup
                    widgetListener?.onWidgetEvent(WidgetEvent.WIDGET_DISMISS)
                    return
                }
                containerView.addView(predictionWidget)
                widgetShown(widgetResource)
                currentWidget = widget
            }

            WidgetType.TEXT_QUIZ -> {
                val quizTextWidget = QuizTextWidget(context,
                    null,
                    0)
                quizTextWidget.initialize({dismissCurrentWidget()},
                    widget.timeout, { optionSelectionEvents() }, parentWidth, ViewAnimationManager(quizTextWidget), {})

                parser.parseQuiz(widget, widgetResource)

                quizTextWidget.userTappedCallback {
                    emitWidgetOptionSelected(widget.id, widgetResource.kind)
                }

                widget.registerObserver(quizTextWidget)
                widget.notifyDataSetChange()
                containerView.addView(quizTextWidget)
                widgetShown(widgetResource)
                currentWidget = widget
            }

            WidgetType.TEXT_QUIZ_RESULT -> {
                currentWidget?.let {
                    parser.parseQuizResult(it, widgetResource)
                    it.notifyDataSetChange()
                }
            }

            WidgetType.IMAGE_QUIZ -> {
                val quizWidget = QuizImageWidget(context,
                    null,
                    0)
                newState?.timeout = widget.timeout
                quizWidget.initialize({dismissCurrentWidget()}, widget.timeout, { optionSelectionEvents() }, parentWidth, ViewAnimationManager(quizWidget))

                parser.parseQuiz(widget, widgetResource)
                widget.registerObserver(quizWidget)
                widget.notifyDataSetChange()
                quizWidget.userTappedCallback {
                    emitWidgetOptionSelected(widget.id, widgetResource.kind)
                }

                containerView.addView(quizWidget)
                widgetShown(widgetResource)
                currentWidget = widget
            }

            WidgetType.IMAGE_QUIZ_RESULT -> {
                currentWidget?.let {
                    parser.parseQuizResult(it, widgetResource)
                    it.notifyDataSetChange()
                }
            }

            WidgetType.TEXT_POLL -> {
                val pollTextWidget = PollTextWidget(context,
                    null,
                    0)
                    .apply {
                        initialize(
                            { dismissCurrentWidget() },
                            widget.timeout,
                            { optionSelectionEvents() },
                            parentWidth
                        )
                    }

                parser.parsePoll(widget, widgetResource)

                pollTextWidget.userTappedCallback {
                    emitWidgetOptionSelected(widget.id, widgetResource.kind)
                }

                widget.registerObserver(pollTextWidget)
                widget.notifyDataSetChange()
                containerView.addView(pollTextWidget)
                widgetShown(widgetResource)
                currentWidget = widget
            }

            WidgetType.TEXT_POLL_RESULT -> {
                currentWidget?.let {
                    parser.parsePollResult(it, widgetResource)
                    it.notifyDataSetChange()
                }
            }

            WidgetType.ALERT -> {
                val alertWidget = AlertWidget(context, null)
                val alertResource = gson.fromJson(payload, Alert::class.java)
                newState?.timeout = alertResource.timeout.toLong()
                alertWidget.initialize({ dismissCurrentWidget() }, alertResource, ViewAnimationManager(alertWidget))
                currentWidget = Widget().apply { id = alertResource.id }
                emitWidgetShown(alertResource.id, alertResource.kind)
                widgetListener?.onWidgetDisplayed(alertResource.impression_url)
                containerView.addView(alertWidget)
            }

            else -> {
                logDebug { "Received Widget is not Implemented." }
            }
        }
    }

    private fun restoreState(previousState: WidgetTransientState) {
        pieTimerAnimatorStartValue = previousState.remainingTime
        timeout = previousState.timeout
    }

    private fun saveState(
        id: String,
        newState: WidgetTransientState,
        payload: JsonObject,
        type: String,
        previousState: WidgetTransientState
    ) {
        widgetStateProcessor?.currentWidget = id
        newState.payload = payload
        newState.type = type
        newState.timeout = timeout
        newState.remainingTime = previousState.remainingTime
        newState.userSelection = previousState.userSelection
        newState.resultAnimationPath = previousState.resultAnimationPath
        newState.resultAnimationTimeRemaining = previousState.resultAnimationTimeRemaining
        newState.let { state -> widgetStateProcessor?.updateWidgetState(id, state) }
    }

    private fun widgetShown(widget: Resource) {
        emitWidgetShown(widget.id, widget.kind)
        widgetListener?.onWidgetDisplayed(widget.impression_url)
    }

    private fun emitWidgetOptionSelected(widgetId: String?, kind: String) {
        analyticService.trackInteraction(widgetId ?: "", kind, "OptionSelected")
    }

    private fun emitWidgetDismissed(widgetId: String?, kind: String) {
        analyticService.trackWidgetDismiss(widgetId ?: "", kind)
    }

    private fun emitWidgetShown(widgetId: String?, kind: String) {
        analyticService.trackWidgetReceived(widgetId ?: "", kind)
    }

    override fun dismissCurrentWidget() {
        ViewAnimationManager(containerView.parent as View).triggerTransitionOutAnimation {
            removeView()
            resetState()

            (containerView.parent as View).translationY = 0f
            currentWidget?.apply {
                emitWidgetDismissEvents(this)
                optionSelectionEvents()
            }
        }
    }

    private fun resetState() {
        pieTimerAnimatorStartValue = 0f
        timeout = 0L
        widgetStateProcessor?.release(currentWidget?.id.toString())
    }

    private fun emitWidgetDismissEvents(widget: Widget) {
        emitWidgetDismissed(widget.id, widget.kind ?: "unknown")
        widgetListener?.onWidgetEvent(WidgetEvent.WIDGET_DISMISS)
    }

    private fun removeView() {
        logVerbose { "Dismissing the widget: ${currentWidget?.id ?: "empty ID"}" }
        containerView.removeAllViews()
    }

    private fun optionSelectionEvents() {
        val optionSelected = currentWidget?.optionSelected
        if (optionSelected?.id == "") {
            removeView()
            currentWidget?.let { emitWidgetDismissEvents(it) }
            return
        }

        currentWidget?.id?.let {
            optionSelected?.id?.let { optionId -> addWidgetPredictionVoted(it, optionId) }
        }
        currentWidget?.subscribeChannel?.let {
            if(currentWidget?.selectedVoteChangeUrl.isNullOrEmpty())
                widgetListener?.onOptionVote(optionSelected?.voteUrl.toString(), it) {changeUrl -> currentWidget?.selectedVoteChangeUrl = changeUrl}
            else
                widgetListener?.onOptionVoteUpdate(currentWidget?.selectedVoteChangeUrl.orEmpty(), optionSelected?.id.orEmpty(), it) {changeUrl -> currentWidget?.selectedVoteChangeUrl = changeUrl}
        }
        widgetListener?.onFetchingQuizResult(optionSelected?.answerUrl.toString())
    }
}
