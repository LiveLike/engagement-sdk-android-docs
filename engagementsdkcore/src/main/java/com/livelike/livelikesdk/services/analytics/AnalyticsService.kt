package com.livelike.livelikesdk.services.analytics

import android.content.Context
import com.google.gson.JsonObject
import com.livelike.livelikesdk.chat.KeyboardHideReason
import com.livelike.livelikesdk.chat.KeyboardType
import com.livelike.livelikesdk.widget.DismissAction
import com.livelike.livelikesdk.widget.WidgetType
import com.mixpanel.android.mpmetrics.MixpanelAPI
import com.mixpanel.android.mpmetrics.MixpanelExtension
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal interface AnalyticsService {
    fun trackConfiguration(internalAppName: String) // add more info if required in the future
    fun trackWidgetInteraction(
        kind: WidgetType,
        id: String,
        interactionInfo: AnalyticsWidgetInteractionInfo
    )
    fun trackSessionStarted()
    fun trackMessageSent(msgId: String, msgLength: Int)
    fun trackWidgetReceived(kind: WidgetType, id: String)
    fun trackWidgetDisplayed(kind: WidgetType, id: String)
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
    fun trackKeyboardOpen(keyboardType: KeyboardType)
    fun trackKeyboardClose(keyboardType: KeyboardType, hideMethod: KeyboardHideReason, chatMessageId: String? = null)
}

internal class MockAnalyticsService : AnalyticsService{
    override fun trackConfiguration(internalAppName: String) {
    }

    override fun trackWidgetInteraction(kind: WidgetType, id: String, interactionInfo: AnalyticsWidgetInteractionInfo) {
    }

    override fun trackSessionStarted() {
    }

    override fun trackMessageSent(msgId: String, msgLength: Int) {
    }

    override fun trackWidgetReceived(kind: WidgetType, id: String) {
    }

    override fun trackWidgetDisplayed(kind: WidgetType, id: String) {
    }

    override fun trackWidgetDismiss(
        kind: WidgetType,
        id: String,
        interactionInfo: AnalyticsWidgetInteractionInfo,
        interactable: Boolean?,
        action: DismissAction
    ) {
    }

    override fun trackInteraction(kind: String, id: String, interactionType: String, interactionCount: Int) {
    }

    override fun trackOrientationChange(isPortrait: Boolean) {
    }

    override fun trackSession(sessionId: String) {
    }

    override fun trackButtonTap(buttonName: String, extra: JsonObject) {
    }

    override fun trackUsername(username: String) {
    }

    override fun trackKeyboardOpen(keyboardType: KeyboardType) {
    }

    override fun trackKeyboardClose(
        keyboardType: KeyboardType,
        hideMethod: KeyboardHideReason,
        chatMessageId: String?
    ) {
    }

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

internal class MixpanelAnalytics(context: Context, token: String, programId: String) : AnalyticsService {

    private var mixpanel: MixpanelAPI = MixpanelExtension.getUniqueInstance(context, token, programId)

    companion object {
        const val KEY_CHAT_MESSAGE_SENT = "Chat Message Sent"
        const val KEY_WIDGET_RECEIVED = "Widget_Received"
        const val KEY_WIDGET_DISPLAYED = "Widget Displayed"
        const val KEY_WIDGET_INTERACTION = "Widget Interaction"
        const val KEY_WIDGET_USER_DISMISS = "Widget Dismissed"
        const val KEY_ORIENTATION_CHANGED = "Orientation_Changed"
        const val KEY_ACTION_TAP = "Action_Tap"
        const val KEY_KEYBOARD_SELECTED = "Keyboard Selected"
        const val KEY_KEYBOARD_HIDDEN = "Keyboard Hidden"
    }

    private var parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

    init {
        trackSessionStarted()
        val properties = JSONObject()
        properties.put("Program ID", programId)
        mixpanel.registerSuperProperties(properties)
    }

    private fun getWidgetType(type: WidgetType): String {
        return when (type) {
            WidgetType.TEXT_POLL -> "Text Poll"
            WidgetType.IMAGE_POLL -> "Image Poll"
            WidgetType.IMAGE_PREDICTION -> "Image Prediction"
            WidgetType.IMAGE_PREDICTION_FOLLOW_UP -> "Image Prediction Follow-up"
            WidgetType.TEXT_PREDICTION -> "Text Prediction"
            WidgetType.TEXT_PREDICTION_FOLLOW_UP -> "Text Prediction Follow-up"
            WidgetType.IMAGE_QUIZ -> "Image Quiz"
            WidgetType.TEXT_QUIZ -> "Text Quiz"
            WidgetType.ALERT -> "Alert"
        }
    }

    override fun trackConfiguration(internalAppName: String) {
        val properties = JSONObject()
        properties.put("Internal App Name", internalAppName)
        mixpanel.registerSuperPropertiesOnce(properties)
    }

    private fun getKeyboardType(kType: KeyboardType): String {
        return when (kType) {
            KeyboardType.STANDARD -> "Standard"
            KeyboardType.EMOJI -> "Emoji"
        }
    }

    override fun trackKeyboardClose(keyboardType: KeyboardType, hideMethod: KeyboardHideReason, chatMessageId: String?) {
        val properties = JSONObject()
        properties.put("Keyboard Type", getKeyboardType(keyboardType))

        val hideReason = when (hideMethod) {
            KeyboardHideReason.TAP_OUTSIDE -> "Dismissed Via Tap Outside"
            KeyboardHideReason.MESSAGE_SENT -> "Sent Message"
        }
        properties.put("Keyboard Hide Method", hideReason)
        chatMessageId?.apply {
            properties.put("Chat Message ID", chatMessageId)
        }
        mixpanel.track(KEY_KEYBOARD_HIDDEN, properties)
    }

    override fun trackKeyboardOpen(keyboardType: KeyboardType) {
        val properties = JSONObject()
        properties.put("Keyboard Type", getKeyboardType(keyboardType))
        mixpanel.track(KEY_KEYBOARD_SELECTED, properties)
    }

    override fun trackWidgetInteraction(
        kind: WidgetType,
        id: String,
        interactionInfo: AnalyticsWidgetInteractionInfo
    ) {
        val properties = JSONObject()
        val timeOfLastInteraction = parser.format(Date(interactionInfo.timeOfLastInteraction))
        properties.put("Widget Type", getWidgetType(kind))
        properties.put("Widget ID", id)
        properties.put("First Tap Time", parser.format(Date(interactionInfo.timeOfFirstInteraction)))
        properties.put("Last Tap Time", timeOfLastInteraction)
        properties.put("No of Taps", interactionInfo.interactionCount)

        mixpanel.track(KEY_WIDGET_INTERACTION, properties)

        val superProp = JSONObject()
        superProp.put("Time of Last Widget Interaction", timeOfLastInteraction)
        mixpanel.registerSuperProperties(superProp)
    }

    override fun trackSessionStarted() {
        val firstTimeProperties = JSONObject()
        val timeNow = parser.format(Date(System.currentTimeMillis()))
        firstTimeProperties.put("Session started", timeNow)
        mixpanel.registerSuperPropertiesOnce(firstTimeProperties)

        val properties = JSONObject()
        properties.put("Last Session started", timeNow)
        mixpanel.registerSuperProperties(properties)
    }

    override fun trackMessageSent(msgId: String, msgLength: Int) {
        val properties = JSONObject()
        properties.put("Chat Message ID", msgId)
        properties.put("Character Length", msgLength)
        mixpanel.track(KEY_CHAT_MESSAGE_SENT, properties)

        val superProp = JSONObject()
        val timeNow = parser.format(Date(System.currentTimeMillis()))
        superProp.put("Time of Last Chat Message", timeNow)
        mixpanel.registerSuperProperties(superProp)
    }

    override fun trackWidgetDisplayed(kind: WidgetType, id: String) {
        val properties = JSONObject()
        properties.put("Widget Type", getWidgetType(kind))
        properties.put("Widget ID", id)
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
        interactable: Boolean?,
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

        val interactionState =
            if (interactable != null && interactable) "Open To Interaction" else "Closed To Interaction"

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