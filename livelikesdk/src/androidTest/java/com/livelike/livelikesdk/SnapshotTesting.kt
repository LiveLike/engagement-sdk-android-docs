package com.livelike.livelikesdk

import android.support.test.InstrumentationRegistry
import android.view.LayoutInflater
import com.facebook.testing.screenshot.Screenshot
import com.facebook.testing.screenshot.ViewHelpers
import org.junit.Test


class MyTests {
    @Test
    fun doScreenshot() {
        val targetContext = InstrumentationRegistry.getInstrumentation().context
        val inflater = LayoutInflater.from(targetContext)
        val view = inflater.inflate(com.livelike.livelikesdk.R.layout.widget_view, null, true)
//        val alert = inflater.inflate(com.livelike.livelikesdk.R.layout.alert_widget, null, true)
//
//        ViewHelpers.setupView(alert)
//            .setExactWidthDp(300)
//            .setExactHeightDp(300)
//            .layout()

        ViewHelpers.setupView(view)
            .setExactWidthDp(300)
            .setExactHeightDp(300)
            .layout()

//        view.containerView.addView(alert)

        Screenshot.snap(view).record()
    }
}