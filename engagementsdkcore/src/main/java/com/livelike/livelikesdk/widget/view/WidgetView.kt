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
import com.livelike.livelikesdk.widget.view.poll.PollImageWidget
import com.livelike.livelikesdk.widget.view.poll.PollTextWidget
import com.livelike.livelikesdk.widget.view.prediction.image.PredictionImageFollowupWidget
import com.livelike.livelikesdk.widget.view.prediction.image.PredictionImageQuestionWidget
import com.livelike.livelikesdk.widget.view.prediction.text.PredictionTextFollowUpWidgetView
import com.livelike.livelikesdk.widget.view.prediction.text.PredictionTextQuestionWidgetView
import com.livelike.livelikesdk.widget.view.quiz.QuizImageWidget
import com.livelike.livelikesdk.widget.view.quiz.QuizTextWidget
import kotlinx.android.synthetic.main.widget_view.view.widgetContainerView

/**
 * The WidgetView is the container where widgets are being displayed.
 * Make sure to set the session to this view to get the data flowing from the LiveLike CMS.
 *
 * This view can be used directly in your layout.
 */
class WidgetView(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs), WidgetRenderer {
    override var widgetListener: WidgetEventListener? = null
    override var widgetStateProcessor: WidgetStateProcessor? = null
    private var currentWidget: Widget? = null
    private var viewRoot: View = LayoutInflater.from(context).inflate(R.layout.widget_view, this, true)
    private var containerView = widgetContainerView as FrameLayout
    private var marginSize = dpToPx(40)
    private var timeout = 0L
    private var widgetType = WidgetType.NONE.value

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

    /**
     * Sets the Session on the widget view.
     * The widget events coming from the LiveLike CMS will flow through the session
     * and display widget where this view is being drawn.
     *
     * @param session The session used on the widget view.
     */
    fun setSession(session: LiveLikeContentSession) {
        session.widgetRenderer = this
        post { requestLayout() }
        val currentWidgetId = widgetStateProcessor?.currentWidgetId ?: return
        val widgetState = widgetStateProcessor?.getWidgetState(currentWidgetId)
        post {
            widgetState?.payload?.let {
                displayWidget(widgetState.type.toString(), it, widgetState)
            }
        }
    }

    override fun displayWidget(
        type: String,
        payload: JsonObject,
        initialState: WidgetTransientState
    ) {
        logDebug { "NOW - Show Widget $type on screen: $payload" }
        val widget = Widget()
        val parser = WidgetParser()
        val widgetResource = gson.fromJson(payload, Resource::class.java)
        val parentWidth = this.width - marginSize
        widgetType = type
        val progressedState = WidgetTransientState()
        when (WidgetType.fromString(widgetType)) {
            WidgetType.TEXT_PREDICTION -> {
                parser.parseTextOptionCommon(widget, widgetResource)
                val predictionWidget = PredictionTextQuestionWidgetView(context, null, 0)
                widget.registerObserver(predictionWidget)

                initialState.timeout = widget.timeout

                predictionWidget.initialize(
                    { dismissCurrentWidget() },
                    initialState,
                    progressedState,
                    parentWidth,
                    ViewAnimationManager(predictionWidget),
                    { saveState(widget.id.toString(), payload, type, it) }
                )

                if (initialState.userSelection != null)
                    widget.optionSelectedUpdated(initialState.userSelection)

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
                    // user did not interact with previous widget, mark dismissed and don't show followup
                    widgetListener?.onWidgetEvent(WidgetEvent.WIDGET_DISMISS)
                    return
                }

                timeout = widget.timeout
                initialState.timeout = widget.timeout

                predictionWidget.initialize({
                    dismissCurrentWidget()
                },
                    initialState,
                    progressedState,
                    parentWidth,
                    ViewAnimationManager(predictionWidget),
                    { saveState(widget.id.toString(), payload, type, it) }
                )
                widget.notifyDataSetChange()

                containerView.addView(predictionWidget)
                widgetShown(widgetResource)
                currentWidget = widget
            }

            WidgetType.IMAGE_PREDICTION -> {
                parser.parseTextOptionCommon(widget, widgetResource)
                val predictionWidget = PredictionImageQuestionWidget(context, null, 0)
                widget.registerObserver(predictionWidget)

                initialState.timeout = widget.timeout

                predictionWidget.initialize(
                    { dismissCurrentWidget() },
                    widget.timeout,
                    initialState,
                    progressedState,
                    parentWidth,
                    ViewAnimationManager(predictionWidget),
                    { saveState(widget.id.toString(), payload, type, it) })

                if (initialState.userSelection != null)
                    widget.optionSelectedUpdated(initialState.userSelection)

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
                widget.registerObserver(predictionWidget)

                if (widget.optionSelected.id.isNullOrEmpty()) {
                    // user did not interact with previous widget, mark dismissed and don't show followup
                    widgetListener?.onWidgetEvent(WidgetEvent.WIDGET_DISMISS)
                    return
                }

                initialState.timeout = widget.timeout

                predictionWidget.initialize({
                    dismissCurrentWidget()
                },
                    initialState,
                    progressedState,
                    parentWidth,
                    ViewAnimationManager(predictionWidget),
                    { saveState(widget.id.toString(), payload, type, it) }
                )
                widget.notifyDataSetChange()

                containerView.addView(predictionWidget)
                widgetShown(widgetResource)
                currentWidget = widget
            }

            WidgetType.TEXT_QUIZ -> {
                displayQuizWidget(parser, widget, widgetResource, initialState, parentWidth, progressedState, payload)
            }

            WidgetType.TEXT_QUIZ_RESULT -> {
                if (initialState.timerAnimatorStartPhase != 0f && initialState.resultAnimatorStartPhase != 0f) {
                    displayQuizWidget(
                        parser,
                        widget,
                        widgetResource,
                        initialState,
                        parentWidth,
                        progressedState,
                        payload
                    )
                } else {
                    progressedState.resultPayload = payload
                    progressedState.type = widgetType
                    progressedState.let { ns ->
                        widgetStateProcessor?.updateWidgetState(
                            currentWidget?.id.toString(),
                            ns
                        )
                    }
                }

                currentWidget?.let {
                    if (initialState.resultPayload != null) {
                        parser.parseQuizResult(it, gson.fromJson(initialState.resultPayload, Resource::class.java))
                    } else parser.parseQuizResult(it, widgetResource)
                    it.notifyDataSetChange()
                }
            }

            WidgetType.IMAGE_QUIZ -> {
                displayQuizImageWidget(
                    parser,
                    widget,
                    widgetResource,
                    initialState,
                    parentWidth,
                    progressedState,
                    payload
                )
            }

            WidgetType.IMAGE_QUIZ_RESULT -> {
                if (initialState.timerAnimatorStartPhase != 0f && initialState.resultAnimatorStartPhase != 0f) {
                    displayQuizImageWidget(
                        parser,
                        widget,
                        widgetResource,
                        initialState,
                        parentWidth,
                        progressedState,
                        payload
                    )
                } else {
                    progressedState.resultPayload = payload
                    progressedState.type = widgetType
                    progressedState.let { ps ->
                        widgetStateProcessor?.updateWidgetState(
                            currentWidget?.id.toString(),
                            ps
                        )
                    }
                }

                currentWidget?.let {
                    if (initialState.resultPayload != null) {
                        parser.parseQuizResult(it, gson.fromJson(initialState.resultPayload, Resource::class.java))
                    } else parser.parseQuizResult(it, widgetResource)
                    it.notifyDataSetChange()
                }
            }

            WidgetType.TEXT_POLL -> {
                val pollTextWidget = PollTextWidget(context, null, 0)

                initialState.timeout = widget.timeout

                pollTextWidget.initialize(
                    { dismissCurrentWidget() },
                    initialState,
                    progressedState,
                    { optionSelectionEvents() },
                    parentWidth,
                    ViewAnimationManager(pollTextWidget),
                    { saveState(widget.id.toString(), payload, widgetType, it) })

                parser.parsePoll(widget, widgetResource)

                pollTextWidget.userTappedCallback {
                    emitWidgetOptionSelected(widget.id, widgetResource.kind)
                }

                widget.registerObserver(pollTextWidget)
                widget.notifyDataSetChange()
                widgetListener?.subscribeForResults(widget.subscribeChannel)
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

            WidgetType.IMAGE_POLL -> {
                val pollImageWidget = PollImageWidget(context, null, 0)

                initialState.timeout = widget.timeout

                pollImageWidget.initialize(
                    { dismissCurrentWidget() },
                    initialState,
                    progressedState,
                    { optionSelectionEvents() },
                    parentWidth,
                    ViewAnimationManager(pollImageWidget),
                    { saveState(widget.id.toString(), payload, widgetType, it) })

                parser.parsePoll(widget, widgetResource)

                pollImageWidget.userTappedCallback {
                    emitWidgetOptionSelected(widget.id, widgetResource.kind)
                }

                widget.registerObserver(pollImageWidget)
                widget.notifyDataSetChange()
                widgetListener?.subscribeForResults(widget.subscribeChannel)
                containerView.addView(pollImageWidget)
                widgetShown(widgetResource)
                currentWidget = widget
            }

            WidgetType.IMAGE_POLL_RESULT -> {
                currentWidget?.let {
                    parser.parsePollResult(it, widgetResource)
                    it.notifyDataSetChange()
                }
            }

            WidgetType.ALERT -> {
                val alertWidget = AlertWidget(context, null)
                val alertResource = gson.fromJson(payload, Alert::class.java)

                currentWidget = Widget().apply {
                    id = alertResource.id
                    alertWidget.initialize({
                        dismissCurrentWidget()
                    },
                        alertResource,
                        progressedState,
                        ViewAnimationManager(alertWidget), {
                            saveState(id.toString(), payload, widgetType, it)
                        })
                }

                emitWidgetShown(alertResource.id, alertResource.kind)
                widgetListener?.onWidgetDisplayed(alertResource.impression_url)
                containerView.addView(alertWidget)
            }

            else -> {
                logDebug { "Received Widget is not Implemented." }
            }
        }
    }

    private fun displayQuizWidget(
        parser: WidgetParser,
        widget: Widget,
        widgetResource: Resource,
        initialState: WidgetTransientState,
        parentWidth: Int,
        progressedState: WidgetTransientState,
        payload: JsonObject
    ) {
        parser.parseQuiz(widget, widgetResource)
        val quizTextWidget = QuizTextWidget(
            context,
            null,
            0
        )
        widget.registerObserver(quizTextWidget)
        progressedState.payload = payload
        progressedState.let { state -> widgetStateProcessor?.updateWidgetState(widget.id.toString(), state) }
        initialState.timeout = widget.timeout

        quizTextWidget.initialize(
            { dismissCurrentWidget() },
            initialState,
            progressedState,
            { optionSelectionEvents() },
            parentWidth,
            ViewAnimationManager(quizTextWidget),
            { saveState(widget.id.toString(), payload, widgetType, it) })

        if (initialState.userSelection != null) {
            widget.optionSelectedUpdated(initialState.userSelection)
        }

        widget.notifyDataSetChange()

        quizTextWidget.userTappedCallback {
            emitWidgetOptionSelected(widget.id, widgetResource.kind)
        }

        containerView.addView(quizTextWidget)
        widgetShown(widgetResource)
        currentWidget = widget
    }

    private fun displayQuizImageWidget(
        parser: WidgetParser,
        widget: Widget,
        widgetResource: Resource,
        initialState: WidgetTransientState,
        parentWidth: Int,
        progressedState: WidgetTransientState,
        payload: JsonObject
    ) {
        parser.parseQuiz(widget, widgetResource)
        val quizTextWidget = QuizImageWidget(
            context,
            null,
            0
        )
        widget.registerObserver(quizTextWidget)
        progressedState.payload = payload
        progressedState.let { state -> widgetStateProcessor?.updateWidgetState(widget.id.toString(), state) }
        initialState.timeout = widget.timeout

        quizTextWidget.initialize(
            { dismissCurrentWidget() },
            initialState,
            progressedState,
            { optionSelectionEvents() },
            parentWidth,
            ViewAnimationManager(quizTextWidget),
            { saveState(widget.id.toString(), payload, widgetType, it) })

        if (initialState.userSelection != null) {
            widget.optionSelectedUpdated(initialState.userSelection)
        }

        widget.notifyDataSetChange()

        quizTextWidget.userTappedCallback {
            emitWidgetOptionSelected(widget.id, widgetResource.kind)
        }

        containerView.addView(quizTextWidget)
        widgetShown(widgetResource)
        currentWidget = widget
    }

    private fun saveState(
        id: String,
        payload: JsonObject,
        type: String,
        progressesState: WidgetTransientState
    ) {
        widgetStateProcessor?.currentWidgetId = id

        progressesState.type = type
        progressesState.timeStamp = System.currentTimeMillis()
        progressesState.let { state -> widgetStateProcessor?.updateWidgetState(id, state) }
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
        currentWidget?.id?.let {
            optionSelected?.id?.let { optionId -> addWidgetPredictionVoted(it, optionId) }
        }

        currentWidget?.subscribeChannel?.let {
            if (currentWidget?.selectedVoteChangeUrl.isNullOrEmpty())
                widgetListener?.onOptionVote(
                    optionSelected?.voteUrl.toString(),
                    it
                ) { changeUrl -> currentWidget?.selectedVoteChangeUrl = changeUrl }
            else
                widgetListener?.onOptionVoteUpdate(
                    currentWidget?.selectedVoteChangeUrl.orEmpty(),
                    optionSelected?.id.orEmpty(),
                    it
                ) { changeUrl -> currentWidget?.selectedVoteChangeUrl = changeUrl }
        }
        widgetListener?.onFetchingQuizResult(optionSelected?.answerUrl.toString())
    }
}
