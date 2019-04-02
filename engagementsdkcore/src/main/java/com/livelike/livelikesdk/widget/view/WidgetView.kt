package com.livelike.livelikesdk.widget.view

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import com.google.gson.JsonObject
import com.livelike.engagementsdkapi.LiveLikeContentSession
import com.livelike.engagementsdkapi.WidgetEvent
import com.livelike.engagementsdkapi.WidgetEventListener
import com.livelike.engagementsdkapi.WidgetRenderer
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.analytics.analyticService
import com.livelike.livelikesdk.animation.ViewAnimation
import com.livelike.livelikesdk.parser.WidgetParser
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
import com.livelike.livelikesdk.widget.view.prediction.image.PredictionImageFollowupWidget
import com.livelike.livelikesdk.widget.view.prediction.image.PredictionImageQuestionWidget
import com.livelike.livelikesdk.widget.view.prediction.text.PredictionTextFollowUpWidgetView
import com.livelike.livelikesdk.widget.view.prediction.text.PredictionTextQuestionWidgetView
import com.livelike.livelikesdk.widget.view.quiz.QuizImageWidget
import com.livelike.livelikesdk.widget.view.quiz.QuizTextWidget
import kotlinx.android.synthetic.main.widget_view.view.*


class WidgetView(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs), WidgetRenderer {
    override var widgetListener: WidgetEventListener? = null
    private var currentWidget: Widget? = null
    private var viewRoot: View = LayoutInflater.from(context).inflate(R.layout.widget_view, this, true)

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
    }

    override fun displayWidget(
        type: String,
        payload: JsonObject
    ) {
        logDebug { "NOW - Show Widget $type on screen: $payload" }
        val widget = Widget()
        val parser = WidgetParser()
        val widgetResource = gson.fromJson(payload, Resource::class.java)
        val parentWidth = this.width

        when (WidgetType.fromString(type)) {
            WidgetType.TEXT_PREDICTION -> {
                parser.parseTextPredictionCommon(widget, widgetResource)
                val predictionWidget =
                    PredictionTextQuestionWidgetView(
                        context,
                        null,
                        0
                    ).apply { initialize({ dismissCurrentWidget() }, widget.timeout) }

                widget.registerObserver(predictionWidget)
                widget.notifyDataSetChange()

                predictionWidget.userTappedCallback { emitWidgetOptionSelected(widget.id, widgetResource.kind) }
                widget_view.addView(predictionWidget)
                widgetShown(widgetResource)
                currentWidget = widget
            }

            WidgetType.TEXT_PREDICTION_RESULTS -> {
                parser.parsePredictionFollowup(widget, widgetResource)
                val predictionWidget = PredictionTextFollowUpWidgetView(context, null, 0)
                    .apply { initialize({ dismissCurrentWidget() }, widget.timeout) }

                widget.registerObserver(predictionWidget)
                widget.notifyDataSetChange()
                if (widget.optionSelected.id.isNullOrEmpty()) {
                    //user did not interact with previous widget, mark dismissed and don't show followup
                    widgetListener?.onWidgetEvent(WidgetEvent.WIDGET_DISMISS)
                    return
                }
                widget_view.addView(predictionWidget)
                widgetShown(widgetResource)
                currentWidget = widget
            }

            WidgetType.IMAGE_PREDICTION -> {
                parser.parseTextPredictionCommon(widget, widgetResource)
                val predictionWidget = PredictionImageQuestionWidget(context, null, 0)
                    .apply { initialize({ dismissCurrentWidget() }, widget.timeout, parentWidth) }
                predictionWidget.layoutParams = layoutParams
                widget.registerObserver(predictionWidget)
                widget.notifyDataSetChange()
                predictionWidget.userTappedCallback {
                    emitWidgetOptionSelected(widget.id, widgetResource.kind)
                }
                widget_view.addView(predictionWidget)
                widgetShown(widgetResource)
                currentWidget = widget
            }

            WidgetType.IMAGE_PREDICTION_RESULTS -> {
                parser.parsePredictionFollowup(widget, widgetResource)
                val predictionWidget = PredictionImageFollowupWidget(context, null, 0)
                    .apply { initialize({ dismissCurrentWidget() }, widget.timeout, parentWidth) }

                widget.registerObserver(predictionWidget)
                widget.notifyDataSetChange()
                if (widget.optionSelected.id.isNullOrEmpty()) {
                    //user did not interact with previous widget, mark dismissed and don't show followup
                    widgetListener?.onWidgetEvent(WidgetEvent.WIDGET_DISMISS)
                    return
                }
                widget_view.addView(predictionWidget)
                widgetShown(widgetResource)
                currentWidget = widget
            }

            WidgetType.TEXT_QUIZ -> {
                val quizTextWidget = QuizTextWidget(context,
                    null,
                    0)
                    .apply { initialize({dismissCurrentWidget()}, widget.timeout, { optionSelectionEvents() }) }

                parser.parseQuiz(widget, widgetResource)

                quizTextWidget.userTappedCallback {
                    emitWidgetOptionSelected(widget.id, widgetResource.kind)
                }

                widget.registerObserver(quizTextWidget)
                widget.notifyDataSetChange()
                widget_view.addView(quizTextWidget)
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
                    .apply { initialize({dismissCurrentWidget()}, widget.timeout, { optionSelectionEvents() }, parentWidth) }

                quizWidget.layoutParams = layoutParams
                parser.parseQuiz(widget, widgetResource)
                widget.registerObserver(quizWidget)
                widget.notifyDataSetChange()
                quizWidget.userTappedCallback {
                    emitWidgetOptionSelected(widget.id, widgetResource.kind)
                }

                widget_view.addView(quizWidget)
                widgetShown(widgetResource)
                currentWidget = widget
            }

            WidgetType.IMAGE_QUIZ_RESULT -> {
                currentWidget?.let {
                    parser.parseQuizResult(it, widgetResource)
                    it.notifyDataSetChange()
                }
            }

            WidgetType.ALERT -> {
                val alertWidget = AlertWidget(context, null).apply {
                    val alertResource = gson.fromJson(payload, Alert::class.java)
                    initialize({ dismissCurrentWidget() }, alertResource)
                    currentWidget = Widget().apply { id = alertResource.id }
                    emitWidgetShown(alertResource.id, alertResource.kind)
                    widgetListener?.onWidgetDisplayed(alertResource.impression_url)
                }
                widget_view.addView(alertWidget)
            }

            else -> {
                logDebug { "Received Widget is not Implemented." }
            }
        }
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
        ViewAnimation(widget_view.parent as View).triggerTransitionOutAnimation {
            removeView()
            (widget_view.parent as View).translationY = 0f
            currentWidget?.apply {
                emitWidgetDismissEvents(this)
                optionSelectionEvents()
            }
        }
    }

    private fun emitWidgetDismissEvents(widget: Widget) {
        emitWidgetDismissed(widget.id, widget.kind ?: "unknown")
        widgetListener?.onWidgetEvent(WidgetEvent.WIDGET_DISMISS)
    }

    private fun removeView() {
        logVerbose { "Dismissing the widget: ${currentWidget?.id ?: "empty ID"}" }
        widget_view.removeAllViews()
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
        currentWidget?.subscribeChannel?.let { widgetListener?.onOptionVote(optionSelected?.voteUrl.toString(), it) }
        widgetListener?.onFetchingQuizResult(optionSelected?.answerUrl.toString())
    }
}
