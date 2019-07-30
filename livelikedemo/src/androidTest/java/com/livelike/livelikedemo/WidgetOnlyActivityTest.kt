package com.livelike.livelikedemo

import android.Manifest
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import android.support.test.runner.screenshot.Screenshot
import android.view.View
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import android.graphics.Bitmap.CompressFormat
import android.support.test.runner.screenshot.CustomScreenCaptureProcessor
import android.support.v4.app.ActivityCompat

@RunWith(AndroidJUnit4::class)
class WidgetOnlyActivityTest {
    @get:Rule
    val activityRule = ActivityTestRule(WidgetOnlyActivity::class.java, false, false)

    @Test
    fun appLaunchesSuccessfully() {
        activityRule.launchActivity(null)
        onView(withId(R.id.buttonRefresh)).perform(click())
        takeScreenshot("widgetOnly1")
        onView(withIndex(withId(R.id.text_button), 0)).perform(click())
        onView(withIndex(withId(R.id.text_button), 2)).perform(click())
        onView(withIndex(withId(R.id.text_button), 5)).perform(click())
        takeScreenshot("widgetOnly2")
        Thread.sleep(1000)
        ActivityCompat.requestPermissions(activityRule.activity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
        Thread.sleep(5000)
        takeScreenshot("widgetOnly2")
    }

    private fun takeScreenshot(filename: String) {
        Screenshot.capture().apply {
            name = filename
            format = CompressFormat.PNG
            process(setOf(CustomScreenCaptureProcessor()))
        }
    }
}

fun withIndex(matcher: Matcher<View>, index: Int): Matcher<View> {
    return object : TypeSafeMatcher<View>() {
        var currentIndex = 0

        override fun describeTo(description: Description) {
            description.appendText("with index: ")
            description.appendValue(index)
            matcher.describeTo(description)
        }

        override fun matchesSafely(view: View): Boolean {
            return matcher.matches(view) && currentIndex++ == index
        }
    }
}
