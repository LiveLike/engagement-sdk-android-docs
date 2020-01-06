package com.livelike.engagementsdk.core.exceptionhelpers

import android.app.Application
import android.content.Context
import android.util.JsonReader
import com.bugsnag.android.BugsnagException
import com.bugsnag.android.Client
import com.bugsnag.android.Configuration
import com.bugsnag.android.JsonStream
import com.bugsnag.android.Report
import com.livelike.engagementsdk.BUGSNAG_ENGAGEMENT_SDK_KEY
import com.livelike.engagementsdk.utils.gson
import com.livelike.engagementsdk.utils.logError
import java.io.StringReader
import java.io.StringWriter
import java.io.Writer

internal object BugsnagClient {

    var client: Client? = null

    fun wouldInitializeBugsnagClient(applicationContext: Context) {
        if (applicationContext is Application) {
            if (client == null) {
                val config = Configuration(BUGSNAG_ENGAGEMENT_SDK_KEY).apply {
                    projectPackages = arrayOf(LIVELIKE_ENGAGEMENTSDK_PACKAGE)

                    beforeSend {
                        it.isPartOfProject()
                    }
                }

                client =
                    Client(applicationContext, config)
            }
        } else {
            throw IllegalArgumentException("Pass application context in SDK initialization")
        }
    }

    /**
     * Determine if the Bug Report has been raised from the project.
     *
     * The strategy used here is to check if the stracktrace includes any mention of the package name.
     *
     * @return true if the bug has been raised from project's code.
     */
    private fun Report.isPartOfProject() : Boolean{
        val cause = error.exception.cause as? BugsnagException? ?: return true
        val writer = StringWriter()
        cause.toStream(JsonStream(writer))
        writer.use {
            return it.buffer.contains(LIVELIKE_ENGAGEMENTSDK_PACKAGE)
        }
    }
}

private const val LIVELIKE_ENGAGEMENTSDK_PACKAGE = "com.livelike.engagementsdk"
