package com.livelike.livelikesdk.widget.view

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.gson.JsonObject
import com.livelike.engagementsdkapi.LiveLikeContentSession
import com.livelike.engagementsdkapi.WidgetEvent
import com.livelike.engagementsdkapi.WidgetEventListener
import com.livelike.engagementsdkapi.WidgetRenderer
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.analytics.analyticService
import com.livelike.livelikesdk.parser.WidgetParser
import com.livelike.livelikesdk.util.AndroidResource.Companion.pxToDp
import com.livelike.livelikesdk.util.gson
import com.livelike.livelikesdk.util.liveLikeSharedPrefs.addWidgetPredictionVoted
import com.livelike.livelikesdk.util.logDebug
import com.livelike.livelikesdk.util.logError
import com.livelike.livelikesdk.util.logVerbose
import com.livelike.livelikesdk.widget.WidgetType
import com.livelike.livelikesdk.widget.model.*
import com.livelike.livelikesdk.widget.view.prediction.image.PredictionImageFollowupWidget
import com.livelike.livelikesdk.widget.view.prediction.image.PredictionImageQuestionWidget
import com.livelike.livelikesdk.widget.view.prediction.text.PredictionTextFollowUpWidgetView
import com.livelike.livelikesdk.widget.view.prediction.text.PredictionTextQuestionWidgetView
import kotlinx.android.synthetic.main.widget_view.view.*
import com.livelike.livelikesdk.widget.view.quiz.QuizImageWidget


class WidgetView(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs), WidgetRenderer {
    override var widgetListener: WidgetEventListener? = null
    private var currentWidget: Widget? = null
    private var viewRoot: View = LayoutInflater.from(context).inflate(R.layout.widget_view, this, true)
    private lateinit var quizWidget : QuizImageWidget

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
        val layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        layoutParams.topMargin = 0
        val widget = Widget()
        when (WidgetType.fromString(type)) {
            WidgetType.TEXT_PREDICTION -> {
                val parser = WidgetParser()
                val widgetResource = gson.fromJson(payload, Resource::class.java)
                parser.parseTextPredictionCommon(widget, widgetResource)
                val predictionWidget =
                    PredictionTextQuestionWidgetView(
                        context,
                        null,
                        0
                    ).apply { initialize({ dismissCurrentWidget() }, widget.timeout) }

                predictionWidget.layoutParams = layoutParams

                val widgetData = PredictionWidgetQuestion(widget)
                widget.registerObserver(predictionWidget)
                widgetData.notifyDataSetChange()
                predictionWidget.userTappedCallback {
                    emitWidgetOptionSelected(widgetData.id, widgetResource.kind)
                }
                containerView.addView(predictionWidget)
                emitWidgetShown(widgetData.id, widgetResource.kind)
                widgetListener?.onWidgetDisplayed(widgetResource.impression_url)
                currentWidget = widget
            }

            WidgetType.TEXT_PREDICTION_RESULTS -> {
                val parser = WidgetParser()
                val widgetResource = gson.fromJson(payload, Resource::class.java)
                parser.parsePredictionFollowup(widget, widgetResource)
                val predictionWidget = PredictionTextFollowUpWidgetView(context, null, 0)
                    .apply { initialize({ dismissCurrentWidget() }, widget.timeout) }

                predictionWidget.layoutParams = layoutParams

                val followupWidgetData = PredictionWidgetFollowUp(widget)
                widget.registerObserver(predictionWidget)
                followupWidgetData.notifyDataSetChange()
                if (widget.optionSelected.id.isNullOrEmpty()) {
                    //user did not interact with previous widget, mark dismissed and don't show followup
                    widgetListener?.onWidgetEvent(WidgetEvent.WIDGET_DISMISS)
                    return
                }
                containerView.addView(predictionWidget)
                emitWidgetShown(widget.id, widgetResource.kind)
                widgetListener?.onWidgetDisplayed(widgetResource.impression_url)
                currentWidget = widget
            }

            WidgetType.IMAGE_PREDICTION -> {
                val parser = WidgetParser()
                val widgetResource = gson.fromJson(payload, Resource::class.java)
                parser.parseTextPredictionCommon(widget, widgetResource)
                val predictionWidget = PredictionImageQuestionWidget(context, null, 0)
                    .apply { initialize({ dismissCurrentWidget() }, widget.timeout) }
                predictionWidget.layoutParams = layoutParams
                val widgetData = PredictionWidgetQuestion(widget)
                widget.registerObserver(predictionWidget)
                widgetData.notifyDataSetChange()
                predictionWidget.userTappedCallback {
                    emitWidgetOptionSelected(widgetData.id, widgetResource.kind)
                }
                containerView.addView(predictionWidget)
                emitWidgetShown(widgetData.id, widgetResource.kind)
                widgetListener?.onWidgetDisplayed(widgetResource.impression_url)
                currentWidget = widget
            }

            WidgetType.IMAGE_PREDICTION_RESULTS -> {
                val parser = WidgetParser()
                val widgetResource = gson.fromJson(payload, Resource::class.java)
                parser.parsePredictionFollowup(widget, widgetResource)
                val predictionWidget = PredictionImageFollowupWidget(context, null, 0)
                    .apply { initialize({ dismissCurrentWidget() }, widget.timeout) }

                predictionWidget.layoutParams = layoutParams

                val followupWidgetData = PredictionWidgetFollowUp(widget)
                widget.registerObserver(predictionWidget)
                followupWidgetData.notifyDataSetChange()
                if (widget.optionSelected.id.isNullOrEmpty()) {
                    //user did not interact with previous widget, mark dismissed and don't show followup
                    widgetListener?.onWidgetEvent(WidgetEvent.WIDGET_DISMISS)
                    return
                }
                containerView.addView(predictionWidget)
                emitWidgetShown(widget.id, widgetResource.kind)
                widgetListener?.onWidgetDisplayed(widgetResource.impression_url)
                currentWidget = widget
            }

            WidgetType.IMAGE_QUIZ -> {
                val parser = WidgetParser()
                val widgetResource = gson.fromJson(payload, Resource::class.java)
                quizWidget = QuizImageWidget(context,
                        null,
                        0)
                        .apply { initialize({dismissCurrentWidget()}, widget.timeout, { optionSelectionActions() }) }

                quizWidget.layoutParams = layoutParams
                parser.parseQuiz(widget, widgetResource)
                val followupWidgetData = PredictionWidgetQuestion(widget)
                widget.registerObserver(quizWidget)
                followupWidgetData.notifyDataSetChange()
                quizWidget.userTappedCallback {
                    emitWidgetOptionSelected(widget.id, widgetResource.kind)
                }

                containerView.addView(predictionWidget)
                container.addView(quizWidget)
                emitWidgetShown(widget.id, widgetResource.kind)
                currentWidget = widget
            }

            WidgetType.IMAGE_QUIZ_RESULT -> {
                val parser = WidgetParser()
                val widgetResource = gson.fromJson(payload, Resource::class.java)
                currentWidget?.let {
                    parser.parseQuizResult(it, widgetResource)
                    val followupWidgetData = QuizWidgetResult(currentWidget!!)
                    followupWidgetData.registerObserver(quizWidget)
                    followupWidgetData.notifyDataSetChange()
                    emitWidgetShown(widget.id, widgetResource.kind)
                    currentWidget = widget
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
                containerView.addView(alertWidget)
            }

            else -> {
                logDebug { "Received Widget is not Implemented." }
            }
        }
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
<<<<<<< HEAD
        logVerbose { "Dismissing the widget: ${currentWidget?.id ?: "empty ID"}" }
        containerView.removeAllViews()
        if (currentWidget?.kind != "image-quiz") {
            logVerbose { "Dismissing the widget: ${currentWidget?.id ?: "empty ID"}" }
            containerView.removeAllViews()
        }
        container.removeAllViews()
=======
        removeView()
>>>>>>> 83ed36f... SDK-223: Some PR fixup
        val widget = currentWidget ?: return
        emitWidgetDismissEvents(widget)
        optionSelectionActions()
    }

    private fun emitWidgetDismissEvents(widget: Widget) {
        emitWidgetDismissed(widget.id, widget.kind ?: "unknown")
        widgetListener?.onWidgetEvent(WidgetEvent.WIDGET_DISMISS)
    }

    private fun removeView() {
        logVerbose { "Dismissing the widget: ${currentWidget?.id ?: "empty ID"}" }
        container.removeAllViews()
    }

    private fun optionSelectionActions() {
        val optionSelected = currentWidget?.optionSelected
        if (optionSelected?.id == "") {
            removeView()
            currentWidget?.let { emitWidgetDismissEvents(it) }
        }

        currentWidget?.id?.let {
            optionSelected?.id?.let { optionId -> addWidgetPredictionVoted(it, optionId) }
        }
        currentWidget?.subscribeChannel?.let { widgetListener?.onOptionVote(optionSelected?.voteUrl.toString(), it) }
        widgetListener?.onFetchingQuizResult(optionSelected?.answerUrl.toString())
    }
}
    private fun emitWidgetEvents(widget: Widget) {


    }
private fun performPostWidgetDismissalActions(widget: Widget) {
    val optionSelected = widget.optionSelected
    widget.id?.let { optionSelected.id?.let { optionId -> addWidgetPredictionVoted(it, optionId) } }
    widgetListener?.onOptionVote(optionSelected.voteUrl.toString(), widget.subscribeChannel)
    widgetListener?.onFetchingQuizResult(optionSelected.answerUrl.toString())
}