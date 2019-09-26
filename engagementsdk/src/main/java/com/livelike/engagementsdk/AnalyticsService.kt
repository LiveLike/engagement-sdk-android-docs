package com.livelike.engagementsdk

import android.content.Context
import android.util.Log
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.livelike.engagementsdk.analytics.AnalyticsSuperProperties
import com.mixpanel.android.mpmetrics.MixpanelAPI
import com.mixpanel.android.mpmetrics.MixpanelExtension
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONObject

interface AnalyticsService {
    fun registerEventObserver(eventObserver: (String, JSONObject) -> Unit)

    fun registerSuperProperty(analyticsSuperProperties: AnalyticsSuperProperties, value: Any?)
    fun registerSuperAndPeopleProperty(event: Pair<String, String>)

    fun trackConfiguration(internalAppName: String) // add more info if required in the future
    fun trackWidgetInteraction(
        kind: String,
        id: String,
        interactionInfo: AnalyticsWidgetInteractionInfo
    )
    fun trackSessionStarted()
    fun trackMessageSent(msgId: String, msgLength: Int)
    fun trackLastChatStatus(status: Boolean)
    fun trackLastWidgetStatus(status: Boolean)
    fun trackWidgetReceived(kind: String, id: String)
    fun trackWidgetDisplayed(kind: String, id: String)
    fun trackWidgetDismiss(
        kind: String,
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
    fun trackFlagButtonPressed()
    fun trackReportingMessage()
    fun trackBlockingUser()
    fun trackCancelFlagUi()
    fun trackPointTutorialSeen(completionType: String, secondsSinceStart: Long)
    fun trackPointThisProgram(points: Int)
    fun trackBadgeCollectedButtonPressed(badgeId: String, badgeLevel: Int)
}

class MockAnalyticsService : AnalyticsService {

    override fun trackBadgeCollectedButtonPressed(badgeId: String, badgeLevel: Int) {
        Log.d("[Analytics]", "[${object {}.javaClass.enclosingMethod?.name}]$badgeId $badgeLevel")
    }

    override fun registerSuperProperty(
        analyticsSuperProperties: AnalyticsSuperProperties,
        value: Any?
    ) {
        Log.d("[Analytics]", "[${object {}.javaClass.enclosingMethod?.name}]$value")
    }

    override fun trackPointThisProgram(points: Int) {
        Log.d("[Analytics]", "[${object{}.javaClass.enclosingMethod?.name}]$points")
    }

    override fun trackPointTutorialSeen(completionType: String, secondsSinceStart: Long) {
        Log.d("[Analytics]", "[${object{}.javaClass.enclosingMethod?.name}]")
    }

    override fun trackFlagButtonPressed() {
        Log.d("[Analytics]", "[${object{}.javaClass.enclosingMethod?.name}]")
    }

    override fun trackReportingMessage() {
        Log.d("[Analytics]", "[${object{}.javaClass.enclosingMethod?.name}]")
    }

    override fun trackCancelFlagUi() {
        Log.d("[Analytics]", "[${object{}.javaClass.enclosingMethod?.name}]")
    }

    override fun trackBlockingUser() {
        Log.d("[Analytics]", "[${object{}.javaClass.enclosingMethod?.name}]")
    }

    override fun registerSuperAndPeopleProperty(event: Pair<String, String>) {
        Log.d("[Analytics]", "[${object{}.javaClass.enclosingMethod?.name}] $event")
    }

    override fun registerEventObserver(eventObserver: (String, JSONObject) -> Unit) {
    }

    override fun trackLastChatStatus(status: Boolean) {
        Log.d("[Analytics]", "[${object{}.javaClass.enclosingMethod?.name}] $status")
    }

    override fun trackLastWidgetStatus(status: Boolean) {
        Log.d("[Analytics]", "[${object{}.javaClass.enclosingMethod?.name}] $status")
    }

    override fun trackConfiguration(internalAppName: String) {
        Log.d("[Analytics]", "[${object{}.javaClass.enclosingMethod?.name}] $internalAppName")
    }

    override fun trackWidgetInteraction(kind: String, id: String, interactionInfo: AnalyticsWidgetInteractionInfo) {
        Log.d("[Analytics]", "[${object{}.javaClass.enclosingMethod?.name}] $kind $interactionInfo")
    }

    override fun trackSessionStarted() {
        Log.d("[Analytics]", "[${object{}.javaClass.enclosingMethod?.name}]")
    }

    override fun trackMessageSent(msgId: String, msgLength: Int) {
        Log.d("[Analytics]", "[${object{}.javaClass.enclosingMethod?.name}] $msgId")
    }

    override fun trackWidgetReceived(kind: String, id: String) {
        Log.d("[Analytics]", "[${object{}.javaClass.enclosingMethod?.name}] $kind")
    }

    override fun trackWidgetDisplayed(kind: String, id: String) {
        Log.d("[Analytics]", "[${object{}.javaClass.enclosingMethod?.name}] $kind")
    }

    override fun trackWidgetDismiss(
        kind: String,
        id: String,
        interactionInfo: AnalyticsWidgetInteractionInfo,
        interactable: Boolean?,
        action: DismissAction
    ) {
        Log.d("[Analytics]", "[${object{}.javaClass.enclosingMethod?.name}] $kind $action $interactionInfo")
    }

    override fun trackInteraction(kind: String, id: String, interactionType: String, interactionCount: Int) {
        Log.d("[Analytics]", "[${object{}.javaClass.enclosingMethod?.name}] $kind $interactionType")
    }

    override fun trackOrientationChange(isPortrait: Boolean) {
        Log.d("[Analytics]", "[${object{}.javaClass.enclosingMethod?.name}] $isPortrait")
    }

    override fun trackSession(sessionId: String) {
        Log.d("[Analytics]", "[${object{}.javaClass.enclosingMethod?.name}] $sessionId")
    }

    override fun trackButtonTap(buttonName: String, extra: JsonObject) {
        Log.d("[Analytics]", "[${object{}.javaClass.enclosingMethod?.name}] $buttonName")
    }

    override fun trackUsername(username: String) {
        Log.d("[Analytics]", "[${object{}.javaClass.enclosingMethod?.name}] $username")
    }

    override fun trackKeyboardOpen(keyboardType: KeyboardType) {
        Log.d("[Analytics]", "[${object{}.javaClass.enclosingMethod?.name}] $keyboardType")
    }

    override fun trackKeyboardClose(
        keyboardType: KeyboardType,
        hideMethod: KeyboardHideReason,
        chatMessageId: String?
    ) {
        Log.d("[Analytics]", "[${object{}.javaClass.enclosingMethod?.name}] $keyboardType $hideMethod")
    }
}

class AnalyticsWidgetInteractionInfo {
    var interactionCount: Int = 0
    var timeOfFirstInteraction: Long = -1
    var timeOfLastInteraction: Long = 0
    var timeOfFirstDisplay: Long = -1

    // gamification
    var pointEarned: Int = 0
    var badgeEarned: String? = null
    var badgeLevelEarned: Int? = null
    var pointsInCurrentLevel: Int? = null
    var pointsToNextLevel: Int? = null

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

    override fun toString(): String {
        return "interactionCount: $interactionCount, timeOfFirstInteraction:$timeOfFirstInteraction, timeOfLastInteraction: $timeOfLastInteraction"
    }
}

class AnalyticsWidgetSpecificInfo {
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

class MixpanelAnalytics(val context: Context, token: String?, programId: String) :
    AnalyticsService {

    private var mixpanel: MixpanelAPI = MixpanelExtension.getUniqueInstance(context, token ?: "5c82369365be76b28b3716f260fbd2f5", programId)

    companion object {
        const val KEY_CHAT_MESSAGE_SENT = "Chat Message Sent"
        const val KEY_WIDGET_RECEIVED = "Widget_Received"
        const val KEY_WIDGET_DISPLAYED = "Widget Displayed"
        const val KEY_WIDGET_INTERACTION = "Widget Interacted"
        const val KEY_WIDGET_USER_DISMISS = "Widget Dismissed"
        const val KEY_ORIENTATION_CHANGED = "Orientation_Changed"
        const val KEY_ACTION_TAP = "Action_Tap"
        const val KEY_KEYBOARD_SELECTED = "Keyboard Selected"
        const val KEY_KEYBOARD_HIDDEN = "Keyboard Hidden"
        const val KEY_FLAG_BUTTON_PRESSED = "Chat Flag Button Pressed"
        const val KEY_FLAG_ACTION_SELECTED = "Chat Flag Action Selected"
        const val KEY_POINT_TUTORIAL_COMPLETED = "Points Tutorial Completed"
        const val KEY_REASON = "Reason"
        const val KEY_EVENT_BADGE_COLLECTED_BUTTON_PRESSED = "Badge Collected Button Pressed"
    }

    private var parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

    init {
        trackSessionStarted()
        JSONObject().apply {
            put("Program ID", programId)
            put("SDK Version", BuildConfig.SDK_VERSION)
            put("Official App Name", getApplicationName(context))
            put("Bundle Id", context.packageName)
            put("Operating System", "Android")
            mixpanel.registerSuperProperties(this)
            mixpanel.people.set(this)
        }
        context.getSharedPreferences("analytics", Context.MODE_PRIVATE).apply {
            if (getBoolean("firstSdkOpen", true)) {
                edit().putBoolean("firstSdkOpen", false).apply()
                JSONObject().apply {
                    val currentDate = parser.format(Date())
                    put("First SDK Open", currentDate)
                    mixpanel.registerSuperPropertiesOnce(this)
                    mixpanel.people.set(this)
                }
            }
        }
    }

    var eventObserver: ((String, JSONObject) -> Unit)? = null

    override fun registerEventObserver(eventObserver: (String, JSONObject) -> Unit) {
        this.eventObserver = eventObserver
    }

    override fun trackFlagButtonPressed() {
        mixpanel.track(KEY_FLAG_BUTTON_PRESSED)
        eventObserver?.invoke(KEY_KEYBOARD_SELECTED, JSONObject())
    }

    override fun trackReportingMessage() {
        val properties = JSONObject()
        properties.put(KEY_REASON, "Reporting Message")
        mixpanel.track(KEY_FLAG_ACTION_SELECTED, properties)
        eventObserver?.invoke(KEY_FLAG_ACTION_SELECTED, properties)
    }

    override fun trackBlockingUser() {
        val properties = JSONObject()
        properties.put(KEY_REASON, "Blocking User")
        mixpanel.track(KEY_FLAG_ACTION_SELECTED, properties)
        eventObserver?.invoke(KEY_FLAG_ACTION_SELECTED, properties)
    }

    override fun trackCancelFlagUi() {
        val properties = JSONObject()
        properties.put(KEY_REASON, "Blocking User")
        mixpanel.track(KEY_FLAG_ACTION_SELECTED, properties)
        eventObserver?.invoke(KEY_FLAG_ACTION_SELECTED, properties)
    }

    override fun registerSuperAndPeopleProperty(event: Pair<String, String>) {
        JSONObject().apply {
            put(event.first, event.second)
            mixpanel.registerSuperProperties(this)
            mixpanel.people.set(this)
            eventObserver?.invoke(event.first, this)
        }
    }

    private fun getApplicationName(context: Context): String {
        val applicationInfo = context.applicationInfo
        val stringId = applicationInfo.labelRes
        return if (stringId == 0) applicationInfo.nonLocalizedLabel.toString() else context.getString(stringId)
    }

    override fun trackLastChatStatus(status: Boolean) {
        JSONObject().apply {
            put("Last Chat Status", if (status) "Enabled" else "Disabled")
            mixpanel.registerSuperProperties(this)
            mixpanel.people.set(this)
            eventObserver?.invoke("Last Chat Status", this)
        }
    }

    override fun trackLastWidgetStatus(status: Boolean) {
        JSONObject().apply {
            put("Last Widget Status", if (status) "Enabled" else "Disabled")
            mixpanel.registerSuperProperties(this)
            mixpanel.people.set(this)
            eventObserver?.invoke("Last Widget Status", this)
        }
    }

    override fun trackConfiguration(internalAppName: String) {
        JSONObject().apply {
            put("Internal App Name", internalAppName)
            mixpanel.registerSuperPropertiesOnce(this)
            mixpanel.people.set(this)
        }
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
        eventObserver?.invoke(KEY_KEYBOARD_HIDDEN, properties)
    }

    override fun trackKeyboardOpen(keyboardType: KeyboardType) {
        val properties = JSONObject()
        properties.put("Keyboard Type", getKeyboardType(keyboardType))
        mixpanel.track(KEY_KEYBOARD_SELECTED, properties)
        eventObserver?.invoke(KEY_KEYBOARD_SELECTED, properties)
    }

    override fun trackWidgetInteraction(
        kind: String,
        id: String,
        interactionInfo: AnalyticsWidgetInteractionInfo
    ) {
        val properties = JSONObject()
        val timeOfLastInteraction = parser.format(Date(interactionInfo.timeOfLastInteraction))
        properties.put("Widget Type", kind)
        properties.put("Widget ID", id)
        properties.put("First Tap Time", parser.format(Date(interactionInfo.timeOfFirstInteraction)))
        properties.put("Last Tap Time", timeOfLastInteraction)
        properties.put("No of Taps", interactionInfo.interactionCount)
        properties.put("Points Earned", interactionInfo.pointEarned)

        interactionInfo.badgeEarned?.let {
            properties.put("Badge Earned", interactionInfo.badgeEarned)
            properties.put("Badge Level Earned", interactionInfo.badgeLevelEarned)
        }
        interactionInfo.pointsInCurrentLevel?.let { properties.put("Points In Current Level", it) }
        interactionInfo.pointsToNextLevel?.let { properties.put("Points To Next Level", it) }

        mixpanel.track(KEY_WIDGET_INTERACTION, properties)
        eventObserver?.invoke(KEY_WIDGET_INTERACTION, properties)

        val superProp = JSONObject()
        superProp.put("Time of Last Widget Interaction", timeOfLastInteraction)
        mixpanel.registerSuperProperties(superProp)
    }

    override fun trackSessionStarted() {
        val firstTimeProperties = JSONObject()
        val timeNow = parser.format(Date(System.currentTimeMillis()))
        firstTimeProperties.put("Session started", timeNow)
        mixpanel.registerSuperPropertiesOnce(firstTimeProperties)
        eventObserver?.invoke("Session started", firstTimeProperties)

        val properties = JSONObject()
        properties.put("Last Session started", timeNow)
        mixpanel.registerSuperProperties(properties)
    }

    override fun trackPointTutorialSeen(completionType: String, secondsSinceStart: Long) {
        val properties = JSONObject()
        properties.put("Completion Type", completionType)
        properties.put("Dismiss Seconds Since Start", secondsSinceStart)
        mixpanel.track(KEY_POINT_TUTORIAL_COMPLETED, properties)
        eventObserver?.invoke(KEY_POINT_TUTORIAL_COMPLETED, properties)
    }

    override fun trackPointThisProgram(points: Int) {
        JSONObject().apply {
            put(AnalyticsSuperProperties.POINTS_THIS_PROGRAM.key, points)
            mixpanel.registerSuperProperties(this)
            eventObserver?.invoke(AnalyticsSuperProperties.POINTS_THIS_PROGRAM.key, this)
        }
    }

    override fun trackBadgeCollectedButtonPressed(badgeId: String, badgeLevel: Int) {
        val properties = JSONObject()
        properties.put("Badge ID", badgeId)
        properties.put("Level", badgeLevel)
        mixpanel.track(KEY_EVENT_BADGE_COLLECTED_BUTTON_PRESSED, properties)
        eventObserver?.invoke(KEY_EVENT_BADGE_COLLECTED_BUTTON_PRESSED, properties)
    }

    override fun registerSuperProperty(analyticsSuperProperties: AnalyticsSuperProperties, value: Any?) {
        JSONObject().apply {
            put(analyticsSuperProperties.key, value ?: JsonNull.INSTANCE)
            mixpanel.registerSuperProperties(this)
            if (analyticsSuperProperties.isPeopleProperty) {
                mixpanel.people.set(this)
            }
            eventObserver?.invoke(analyticsSuperProperties.key, this)
        }
    }

    override fun trackMessageSent(msgId: String, msgLength: Int) {
        val properties = JSONObject()
        properties.put("Chat Message ID", msgId)
        properties.put("Character Length", msgLength)
        mixpanel.track(KEY_CHAT_MESSAGE_SENT, properties)
        eventObserver?.invoke(KEY_CHAT_MESSAGE_SENT, properties)

        val superProp = JSONObject()
        val timeNow = parser.format(Date(System.currentTimeMillis()))
        superProp.put("Time of Last Chat Message", timeNow)
        mixpanel.registerSuperProperties(superProp)
    }

    override fun trackWidgetDisplayed(kind: String, id: String) {
        val properties = JSONObject()
        properties.put("Widget Type", kind)
        properties.put("Widget ID", id)
        mixpanel.track(KEY_WIDGET_DISPLAYED, properties)
        eventObserver?.invoke(KEY_WIDGET_DISPLAYED, properties)
    }

    override fun trackWidgetReceived(kind: String, id: String) {
        val properties = JSONObject()
        properties.put("Time Of Last Widget Receipt", parser.format(Date(System.currentTimeMillis())))
        properties.put("Widget Type", kind)
        properties.put("Widget Id", id)
        mixpanel.track(KEY_WIDGET_RECEIVED, properties)
        mixpanel.registerSuperProperties(properties)
        eventObserver?.invoke(KEY_WIDGET_RECEIVED, properties)
    }

    override fun trackWidgetDismiss(
        kind: String,
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

        context.getSharedPreferences("analytics", Context.MODE_PRIVATE).apply {
            val properties = JSONObject()
            properties.put("Widget Type", kind)
            properties.put("Widget ID", id)
            properties.put("Number Of Taps", interactionInfo.interactionCount)
            properties.put("Dismiss Action", dismissAction)
            properties.put("Dismiss Seconds Since Last Tap", timeSinceLastTap)
            properties.put("Dismiss Seconds Since Start", timeSinceStart)
            properties.put("Interactable State", interactionState)
            properties.put("Last Widget Type", getString("lastWidgetType", ""))
            mixpanel.track(KEY_WIDGET_USER_DISMISS, properties)
            eventObserver?.invoke(KEY_WIDGET_USER_DISMISS, properties)

            edit().putString("lastWidgetType", kind).apply()
        }
    }

    private var lastOrientation: Boolean? = null

    override fun trackInteraction(kind: String, id: String, interactionType: String, interactionCount: Int) {
        val properties = JSONObject()
        properties.put("kind", kind)
        properties.put("id", id)
        properties.put("interactionType", interactionType)
        properties.put("interactionCount", interactionCount)
        mixpanel.track(KEY_WIDGET_INTERACTION, properties)
        eventObserver?.invoke(KEY_WIDGET_INTERACTION, properties)
    }

    override fun trackOrientationChange(isPortrait: Boolean) {
        if (lastOrientation == isPortrait) return // return if the orientation stays the same
        lastOrientation = isPortrait
        JSONObject().apply {
            put("Device Orientation", if (isPortrait)"PORTRAIT" else "LANDSCAPE")
            mixpanel.track(KEY_ORIENTATION_CHANGED, this)
            mixpanel.registerSuperProperties(this)
            eventObserver?.invoke(KEY_ORIENTATION_CHANGED, this)
        }
        JSONObject().apply {
            put("Last Device Orientation", if (isPortrait)"PORTRAIT" else "LANDSCAPE")
            mixpanel.people.set(this)
        }
    }

    override fun trackButtonTap(buttonName: String, extra: JsonObject) {
        val properties = JSONObject()
        properties.put("buttonName", buttonName)
        properties.put("extra", extra)
        mixpanel.track(KEY_ACTION_TAP, properties)
        eventObserver?.invoke(KEY_ACTION_TAP, properties)
    }

    override fun trackSession(sessionId: String) {
        mixpanel.identify(sessionId)
        mixpanel.people.identify(sessionId)
    }

    override fun trackUsername(username: String) {
        mixpanel.people.set("Nickname", username)
        val properties = JSONObject()
        properties.put("Nickname", username)
        mixpanel.registerSuperProperties(properties)
        eventObserver?.invoke("Nickname", properties)
    }
}

enum class KeyboardHideReason {
    MESSAGE_SENT,
    TAP_OUTSIDE
}

enum class KeyboardType {
    STANDARD,
    EMOJI
}

enum class DismissAction {
    TIMEOUT,
    SWIPE,
    TAP_X
}
