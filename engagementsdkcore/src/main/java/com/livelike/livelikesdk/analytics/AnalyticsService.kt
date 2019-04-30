package com.livelike.livelikesdk.analytics

import android.content.Context
import com.google.gson.JsonObject
import com.mixpanel.android.mpmetrics.MixpanelAPI
import org.json.JSONObject

internal interface AnalyticsService {
    fun trackMessageSent(containEmoji: Boolean)
    fun trackWidgetReceived(kind: String, id: String)
    fun trackWidgetDismiss(kind: String, id: String)
    fun trackInteraction(kind: String, id: String, interactionType: String, interactionCount: Int = 1)
    fun trackOrientationChange(isPortrait: Boolean)
    fun trackSession(sessionId: String)
    fun trackButtonTap(buttonName: String, extra: JsonObject)
    fun trackUsername(username: String)
}

internal val analyticService = MixpanelAnalytics()

internal class MixpanelAnalytics : AnalyticsService {

    private lateinit var mixpanel: MixpanelAPI

    fun initialize(context: Context, token: String) {
        mixpanel = MixpanelAPI.getInstance(context, token)
    }

    companion object {
        const val KEY_CHAT_MESSAGE_SENT = "Chat_Sent_Message"
        const val KEY_WIDGET_RECEIVED = "Widget_Received"
        const val KEY_WIDGET_INTERACTION = "Widget_Interaction"
        const val KEY_WIDGET_USER_DISMISS = "Widget_User_Dismiss"
        const val KEY_ORIENTATION_CHANGED = "Orientation_Changed"
        const val KEY_ACTION_TAP = "Action_Tap"
    }


    override fun trackMessageSent(containEmoji: Boolean) {
        val properties = JSONObject()
        properties.put("containEmoji", containEmoji)
        mixpanel.track(KEY_CHAT_MESSAGE_SENT, properties)
    }

    override fun trackWidgetReceived(kind: String, id: String) {
        val properties = JSONObject()
        properties.put("kind", kind)
        properties.put("id", id)
        mixpanel.track(KEY_WIDGET_RECEIVED, properties)
    }

    override fun trackWidgetDismiss(kind: String, id: String) {
        val properties = JSONObject()
        properties.put("kind", kind)
        properties.put("id", id)
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