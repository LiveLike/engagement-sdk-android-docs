package com.livelike.livelikesdk

import android.content.Context
import android.support.test.InstrumentationRegistry
import android.support.test.annotation.UiThreadTest
import android.view.LayoutInflater
import android.view.View
import com.facebook.testing.screenshot.Screenshot
import com.facebook.testing.screenshot.ViewHelpers
import com.livelike.engagementsdkapi.WidgetTransientState
import com.livelike.livelikesdk.animation.ViewAnimationManager
import com.livelike.livelikesdk.widget.model.Alert
import com.livelike.livelikesdk.widget.view.AlertWidget
import kotlinx.android.synthetic.main.widget_view.view.widgetContainerView
import org.junit.Before
import org.junit.Test

/* Need to diable the animations on the device.
*
* adb shell pm grant com.livelike.livelikesdk android.permission.SET_ANIMATION_SCALE
* adb shell settings put global window_animation_scale 0.0
* adb shell settings put global transition_animation_scale 0.0
* adb shell settings put global animator_duration_scale 0.0
*
* */

class AlertWidgetImage {
    private lateinit var context: Context
    private lateinit var imageOnly: View
    private lateinit var imageAndLabel: View
    private lateinit var imageAndLabelAndLink: View
    private lateinit var textOnly: View
    private lateinit var textAndLabel: View
    private lateinit var textAndLabelAndLink: View

    val bodyText =
        "This is the body This is the body This is the body This is the body This is the body This is the body This is the body"
    val labelTitle = "DEAL"
    val imageUrl = "https://cf-blast-storage.livelikecdn.com/assets/8569d8c0-3fe5-47e9-b852-751ee18383ff.png"
    val linkText = "Click on me, I'm a link"
    val linkUrl = "https://www.google.com/"

    private fun setupView(
        title: String = "",
        bodyText: String = "",
        imageUrl: String = "",
        linkUrl: String = "",
        linkText: String = ""
    ): View {
        context = InstrumentationRegistry.getInstrumentation().context
        val inflater = LayoutInflater.from(context)
        val widgetView = inflater.inflate(com.livelike.livelikesdk.R.layout.widget_view, null, true)
        val alertWidget = AlertWidget(context)
        alertWidget.initialize(
            {}, Alert(
                "an-id",
                "https://facebook.github.io/screenshot-tests-for-android/static/logo.png",
                "alert",
                "nothing",
                "something",
                "something",
                "time",
                "time",
                title,
                bodyText,
                imageUrl,
                linkUrl,
                linkText
            ),
            WidgetTransientState(),
            ViewAnimationManager(alertWidget),
            {}
        )

        ViewHelpers.setupView(alertWidget)
            .setExactWidthDp(300)
            .setExactHeightDp(200)
            .layout()

        widgetView.widgetContainerView.addView(alertWidget)

        ViewHelpers.setupView(widgetView)
            .setExactWidthDp(300)
            .setExactHeightDp(200)
            .layout()

        return widgetView ?: View(context)
    }

    private fun takeScreenshot(view: View, name: String) {
        Screenshot
            .snap(view)
            .setName(name)
            .record()
    }

    @Before
    @UiThreadTest
    fun setup() {
        imageOnly = setupView(imageUrl = imageUrl)
        imageAndLabel = setupView(labelTitle, imageUrl = imageUrl)
        imageAndLabelAndLink = setupView(labelTitle, "", imageUrl, linkUrl, linkText)
        textOnly = setupView(bodyText = bodyText)
        textAndLabel = setupView(labelTitle, bodyText = bodyText)
        textAndLabelAndLink = setupView(labelTitle, bodyText, "", linkUrl, linkText)
    }

    @Test
    fun doScreenshot() {
        takeScreenshot(imageOnly, "Alert-Widget-Image-Only")
        takeScreenshot(imageAndLabel, "Alert-Widget-Image-Label")
        takeScreenshot(imageAndLabelAndLink, "Alert-Widget-Image-Label-Link")
        takeScreenshot(textOnly, "Alert-Widget-Body-Only")
        takeScreenshot(textAndLabel, "Alert-Widget-Body-Label")
        takeScreenshot(textAndLabelAndLink, "Alert-Widget-Body-Label-Link")
    }
}
