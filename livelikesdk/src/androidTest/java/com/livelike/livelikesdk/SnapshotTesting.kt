package com.livelike.livelikesdk

import android.support.test.InstrumentationRegistry
import android.support.test.annotation.UiThreadTest
import android.view.LayoutInflater
import com.facebook.testing.screenshot.Screenshot
import com.facebook.testing.screenshot.ViewHelpers
import com.livelike.livelikesdk.widget.model.Alert
import com.livelike.livelikesdk.widget.view.AlertWidget
import kotlinx.android.synthetic.main.widget_view.view.*
import org.junit.Test


/*
*
* adb shell pm grant com.livelike.livelikesdk android.permission.SET_ANIMATION_SCALE
* adb shell settings put global window_animation_scale 0.0
* adb shell settings put global transition_animation_scale 0.0
* adb shell settings put global animator_duration_scale 0.0
*
* */

class MyTests {
    @Test
    @UiThreadTest
    fun doScreenshot() {
        val targetContext = InstrumentationRegistry.getInstrumentation().context
        val inflater = LayoutInflater.from(targetContext)
        val view = inflater.inflate(com.livelike.livelikesdk.R.layout.widget_view, null, true)
        val alertWidget = AlertWidget(targetContext)
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
                "This is the text bodyyyyy",
                "https://facebook.github.io/screenshot-tests-for-android/static/logo.png",
                "https://facebook.github.io/screenshot-tests-for-android/static/logo.png",
                "Click here please"
            )
        )

//
//        ViewHelpers.setupView(alertWidget)
//            .setExactWidthDp(300)
//            .setExactHeightDp(200)
//            .layout()


        ViewHelpers.setupView(view)
            .setExactWidthDp(300)
            .setExactHeightDp(200)
            .layout()

        view.containerView.addView(alertWidget)
        Screenshot
            .snap(view)
            .record()


    }
}