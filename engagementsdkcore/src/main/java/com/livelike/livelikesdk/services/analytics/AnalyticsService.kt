package com.livelike.livelikesdk.services.analytics

import android.content.Context
import com.google.gson.JsonObject
import com.mixpanel.android.mpmetrics.MixpanelAPI
import org.json.JSONObject
import com.livelike.engagementsdkapi.LiveLikeContentSession
import com.livelike.livelikesdk.widget.DismissAction
import com.livelike.livelikesdk.widget.WidgetType
import java.text.SimpleDateFormat
import java.util.Date

internal interface AnalyticsService {
    fun trackWidgetInteraction(
        kind: WidgetType,
        id: String,
        interactionInfo: AnalyticsWidgetInteractionInfo
    )
    fun trackAppOpen()
    fun trackMessageSent(msgId: String, msgLength: Int)
    fun trackWidgetReceived(kind: WidgetType, id: String)
    fun trackWidgetDisplayed(kind: String, id: String)
    fun trackWidgetDismiss(
        kind: WidgetType,
        id: String,
        interactionInfo: AnalyticsWidgetInteractionInfo,
        interactable: Boolean?,
        action: DismissAction
    )
    fun trackInteraction(kind: String, id: String, interactionType: String, interactionCount: Int = 1)
    fun trackOrientationChange(isPortrait: Boolean)
    fun trackSession(sessionId: String)
    fun trackButtonTap(buttonName: String, extra: JsonObject)
    fun trackUsername(username: String)
}

internal class AnalyticsWidgetInteractionInfo {
    var interactionCount: Int = 0
    var timeOfFirstInteraction: Long = -1
    var timeOfLastInteraction: Long = 0
    var timeOfFirstDisplay: Long = -1

    fun incrementInteraction() {
        interactionCount += 1

        val timeNow = System.currentTimeMillis()
        if (timeOfFirstInteraction < 0) {
            timeOfFirstInteraction = timeNow
        }
        timeOfLastInteraction = timeNow
    }

    fun widgetDisplayed() {
        timeOfFirstDisplay = System.currentTimeMillis()
    }

    fun reset() {
        interactionCount = 0
        timeOfFirstInteraction = -1
        timeOfLastInteraction = -1
    }
}

internal class AnalyticsWidgetSpecificInfo {
    var responseChanges: Int = 0
    var finalAnswerIndex: Int = -1
    var totalOptions: Int = 0
    var userVotePercentage: Int = 0
    var votePosition: Int = 0
    var widgetResult: String = ""

    fun reset() {
        responseChanges = 0
        finalAnswerIndex = -1
        totalOptions = 0
        userVotePercentage = 0
        votePosition = 0
        widgetResult = ""
    }
}

internal val analyticService = MixpanelAnalytics()

internal class MixpanelAnalytics : AnalyticsService {

    private lateinit var mixpanel: MixpanelAPI

    fun initialize(context: Context, token: String) {
        mixpanel = MixpanelAPI.getInstance(context, token)
        trackAppOpen()
    }

    fun setSession(session: LiveLikeContentSession) {
        trackWidgets(session)
    }

    companion object {
        const val KEY_CHAT_MESSAGE_SENT = "Chat Message Sent"
        const val KEY_WIDGET_RECEIVED = "Widget_Received"
        const val KEY_WIDGET_DISPLAYED = "Widget Displayed"
        const val KEY_WIDGET_INTERACTION = "Widget Interaction"
        const val KEY_WIDGET_USER_DISMISS = "Widget Dismissed"
        const val KEY_ORIENTATION_CHANGED = "Orientation_Changed"
        const val KEY_ACTION_TAP = "Action_Tap"
    }

    private var parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")

    private fun getWidgetType(type: WidgetType): String {
        return when (type) {
            WidgetType.TEXT_POLL -> "Text Poll"
            WidgetType.IMAGE_POLL -> "Image Poll"
            WidgetType.IMAGE_PREDICTION -> "Image Prediction"
            WidgetType.IMAGE_PREDICTION_FOLLOW_UP -> "Image Prediction Follow-up"
            WidgetType.TEXT_PREDICTION -> "Text Prediction"
            WidgetType.TEXT_PREDICTION_FOLLOW_UP -> "Text Prediction Follow-up"
            else -> ""
        }
    }

    override fun trackWidgetInteraction(
        kind: WidgetType,
        id: String,
        interactionInfo: AnalyticsWidgetInteractionInfo
    ) {
        val properties = JSONObject()
        properties.put("Widget Type", getWidgetType(kind))
        properties.put("Widget ID", id)
        properties.put("First Tap Time", parser.format(Date(interactionInfo.timeOfFirstInteraction)))
        properties.put("Last Tap Time", parser.format(Date(interactionInfo.timeOfLastInteraction)))

        mixpanel.track(KEY_WIDGET_INTERACTION, properties)
    }

    private fun trackWidgets(session: LiveLikeContentSession) {
        session.currentWidgetInfosStream.subscribe(KEY_WIDGET_DISPLAYED) {
            if (it != null) {
                trackWidgetDisplayed(it.type, it.payload["id"].toString())
            }
        }
    }

    override fun trackAppOpen() {
        val firstTimeProperties = JSONObject()
        val timeNow = parser.format(Date(System.currentTimeMillis()))
        firstTimeProperties.put("First App Open", timeNow)
        mixpanel.registerSuperPropertiesOnce(firstTimeProperties)

        var properties = JSONObject()
        properties.put("Last App Open", timeNow)
        mixpanel.registerSuperProperties(properties)
    }

    override fun trackMessageSent(msgId: String, msgLength: Int) {
        val properties = JSONObject()
        properties.put("Chat Message ID", msgId)
        properties.put("Character Length", msgLength)
        mixpanel.track(KEY_CHAT_MESSAGE_SENT, properties)
    }

    override fun trackWidgetDisplayed(kind: String, id: String) {
        val properties = JSONObject()
        properties.put("widgetType", getWidgetType(WidgetType.fromString(kind)))
        properties.put("widgetId", id)
        mixpanel.track(KEY_WIDGET_DISPLAYED, properties)
    }

    override fun trackWidgetReceived(kind: WidgetType, id: String) {
        val properties = JSONObject()
        properties.put("Time Of Last Widget Receipt", parser.format(Date(System.currentTimeMillis())))
        properties.put("Last Widget Type", getWidgetType(kind))
        properties.put("Last Widget Id", id)
        mixpanel.registerSuperProperties(properties)
    }

    override fun trackWidgetDismiss(
        kind: WidgetType,
        id: String,
        interactionInfo: AnalyticsWidgetInteractionInfo,
        canInteract: Boolean?,
        action: DismissAction
    ) {

        if (action == DismissAction.TIMEOUT) {
            return
        }
        val dismissAction = when (action) {
            DismissAction.TAP_X -> "Tap X"
            DismissAction.SWIPE -> "Swipe"
            else -> ""
        }

        val timeNow = System.currentTimeMillis()
        val timeSinceLastTap = (timeNow - interactionInfo.timeOfLastInteraction).toFloat() / 1000
        val timeSinceStart = (timeNow - interactionInfo.timeOfFirstDisplay).toFloat() / 1000

        val interactionState = if (canInteract != null && canInteract) "Open To Interaction" else "Closed To Interaction"

        val properties = JSONObject()
        properties.put("Widget Type", getWidgetType(kind))
        properties.put("Widget ID", id)
        properties.put("Number Of Taps", interactionInfo.interactionCount)
        properties.put("Dismiss Action", dismissAction)
        properties.put("Dismiss Seconds Since Last Tap", timeSinceLastTap)
        properties.put("Dismiss Seconds Since Start", timeSinceStart)
        properties.put("Interactable State", interactionState)

        mixpanel.track(KEY_WIDGET_USER_DISMISS, properties)
    }

    override fun trackInteraction(kind: String, id: String, interactionType: String, interactionCount: Int) {
        val properties = JSONObject()
        properties.put("kind", kind)
        properties.put("id", id)
        properties.put("interactionType", interactionType)
        properties.put("interactionCount", interactionCount)
        mixpanel.track(KEY_WIDGET_INTERACTION, properties)
    }

    override fun trackOrientationChange(isPortrait: Boolean) {
        val properties = JSONObject()
        properties.put("isPortrait", isPortrait)
        mixpanel.track(KEY_ORIENTATION_CHANGED, properties)
    }

    override fun trackButtonTap(buttonName: String, extra: JsonObject) {
        val properties = JSONObject()
        properties.put("buttonName", buttonName)
        properties.put("extra", extra)
        mixpanel.track(KEY_ACTION_TAP, properties)
    }

    override fun trackSession(sessionId: String) {
        mixpanel.identify(sessionId)
        mixpanel.people.identify(sessionId)
    }

    override fun trackUsername(username: String) {
        mixpanel.people.set("username", username)
    }
}