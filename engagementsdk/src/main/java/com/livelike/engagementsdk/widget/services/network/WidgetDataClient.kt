package com.livelike.engagementsdk.widget.services.network

import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.core.services.network.EngagementDataClientImpl
import com.livelike.engagementsdk.core.services.network.RequestType
import com.livelike.engagementsdk.core.utils.addAuthorizationBearer
import com.livelike.engagementsdk.core.utils.addUserAgent
import com.livelike.engagementsdk.core.utils.extractStringOrEmpty
import com.livelike.engagementsdk.core.utils.logError
import com.livelike.engagementsdk.core.utils.logVerbose
import com.livelike.engagementsdk.widget.data.models.ProgramGamificationProfile
import com.livelike.engagementsdk.widget.util.SingleRunner
import com.livelike.engagementsdk.widget.utils.livelikeSharedPrefs.addPoints
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import java.io.IOException

internal interface WidgetDataClient {
    suspend fun voteAsync(
        widgetVotingUrl: String,
        voteId: String? = null,
        accessToken: String? = null,
        body: RequestBody? = null,
        type: RequestType? = null
    ): String?

    fun registerImpression(impressionUrl: String, accessToken: String?)
    suspend fun rewardAsync(
        rewardUrl: String,
        analyticsService: AnalyticsService,
        accessToken: String?
    ): ProgramGamificationProfile?
}

internal class WidgetDataClientImpl : EngagementDataClientImpl(), WidgetDataClient {
    var voteUrl = ""
    private val singleRunner = SingleRunner()

    override suspend fun voteAsync(
        widgetVotingUrl: String,
        voteId: String?,
        accessToken: String?,
        body: RequestBody?,
        type: RequestType?
    ): String? {
        return singleRunner.afterPrevious {
            if (voteUrl.isEmpty()) {
                voteUrl =
                    postAsync(
                        widgetVotingUrl,
                        accessToken,
                        body,
                        type ?: RequestType.POST
                    ).extractStringOrEmpty("url")
            } else {
                postAsync(
                    voteUrl, accessToken, (body ?: FormBody.Builder()
                        .add("option_id", voteId)
                        .add("choice_id", voteId)
                        .build()), type ?: RequestType.PUT
                )
            }
            return@afterPrevious voteUrl
        }
    }

    override suspend fun rewardAsync(
        rewardUrl: String,
        analyticsService: AnalyticsService,
        accessToken: String?
    ): ProgramGamificationProfile? {
        return gson.fromJson(
            postAsync(rewardUrl, accessToken),
            ProgramGamificationProfile::class.java
        )?.also {
            addPoints(it.newPoints ?: 0)
            analyticsService.registerSuperAndPeopleProperty(
                "Lifetime Points" to (it.points.toString() ?: "0")
            )
        }
    }

    override fun registerImpression(impressionUrl: String, accessToken: String?) {
        if (impressionUrl.isNullOrEmpty()) {
            return
        }
        val client = OkHttpClient()
        val formBody = FormBody.Builder()
            .build()
        try {
            val request = Request.Builder()
                .url(impressionUrl)
                .addAuthorizationBearer(accessToken)
                .post(formBody)
                .addUserAgent()
                .build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    logError { "failed to register impression" }
                }

                override fun onResponse(call: Call, response: Response) {
                    logVerbose { "impression registered " + response.message() }
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}