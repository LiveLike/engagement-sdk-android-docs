package com.livelike.livelikesdk

import android.content.Context
import android.support.test.InstrumentationRegistry
import android.support.test.annotation.UiThreadTest
import android.view.LayoutInflater
import android.view.View
import com.facebook.testing.screenshot.Screenshot
import com.facebook.testing.screenshot.ViewHelpers
import com.livelike.livelikesdk.widget.model.Alert
import com.livelike.livelikesdk.widget.view.AlertWidget
import kotlinx.android.synthetic.main.widget_view.view.*
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
    private lateinit var widgetView: View
    private lateinit var context: Context

    @Before
    @UiThreadTest
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context
        val inflater = LayoutInflater.from(context)
        widgetView = inflater.inflate(com.livelike.livelikesdk.R.layout.widget_view, null, true)
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
                "DEAL",
                "",
                "https://facebook.github.io/screenshot-tests-for-android/static/logo.png",
                "https://facebook.github.io/screenshot-tests-for-android/static/logo.png",
                "Click here please"
            )
        )

        ViewHelpers.setupView(alertWidget)
            .setExactWidthDp(300)
            .setExactHeightDp(200)
            .layout()

        widgetView.containerView.addView(alertWidget)

        ViewHelpers.setupView(widgetView)
            .setExactWidthDp(300)
            .setExactHeightDp(200)
            .layout()
    }

    @Test
    fun doScreenshot() {
        Screenshot
            .snap(widgetView).setName("Alert-Widget-Image")
            .record()
    }
}

class AlertWidgetBody {
    private lateinit var widgetView: View
    private lateinit var context: Context

    @Before
    @UiThreadTest
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context
        val inflater = LayoutInflater.from(context)
        widgetView = inflater.inflate(com.livelike.livelikesdk.R.layout.widget_view, null, true)
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
                "DEAL",
                "This is the body This is the body This is the body This is the body This is the body This is the body This is the body",
                "",
                "https://facebook.github.io/screenshot-tests-for-android/static/logo.png",
                "Click here please"
            )
        )

        ViewHelpers.setupView(alertWidget)
            .setExactWidthDp(300)
            .setExactHeightDp(200)
            .layout()

        widgetView.containerView.addView(alertWidget)

        ViewHelpers.setupView(widgetView)
            .setExactWidthDp(300)
            .setExactHeightDp(200)
            .layout()
    }

    @Test
    fun doScreenshot() {
        Screenshot
            .snap(widgetView)
            .setName("Alert-Widget-Body")
            .record()
    }
}

class AlertWidgetBodyNoLinkNoLabel {
    private lateinit var widgetView: View
    private lateinit var context: Context

    @Before
    @UiThreadTest
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context
        val inflater = LayoutInflater.from(context)
        widgetView = inflater.inflate(com.livelike.livelikesdk.R.layout.widget_view, null, true)
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
                "",
                "This is the body This is the body This is the body This is the body This is the body This is the body This is the body",
                "",
                "",
                ""
            )
        )

        ViewHelpers.setupView(alertWidget)
            .setExactWidthDp(300)
            .setExactHeightDp(200)
            .layout()

        widgetView.containerView.addView(alertWidget)

        ViewHelpers.setupView(widgetView)
            .setExactWidthDp(300)
            .setExactHeightDp(200)
            .layout()
    }

    @Test
    fun doScreenshot() {
        Screenshot
            .snap(widgetView)
            .setName("Alert-Widget-Body-NoLink-NoLabel")
            .record()
    }
}

class AlertWidgetImageNoLinkNoLabel {
    private lateinit var widgetView: View
    private lateinit var context: Context

    @Before
    @UiThreadTest
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context
        val inflater = LayoutInflater.from(context)
        widgetView = inflater.inflate(com.livelike.livelikesdk.R.layout.widget_view, null, true)
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
                "",
                "",
                "https://facebook.github.io/screenshot-tests-for-android/static/logo.png",
                "",
                ""
            )
        )

        ViewHelpers.setupView(alertWidget)
            .setExactWidthDp(300)
            .setExactHeightDp(200)
            .layout()

        widgetView.containerView.addView(alertWidget)

        ViewHelpers.setupView(widgetView)
            .setExactWidthDp(300)
            .setExactHeightDp(200)
            .layout()
    }

    @Test
    fun doScreenshot() {
        Screenshot
            .snap(widgetView)
            .setName("Alert-Widget-Image-NoLink-NoLabel")
            .record()
    }
}