package com.livelike.engagementsdk.services.network

import android.os.Handler
import android.os.Looper
import android.webkit.URLUtil
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.BuildConfig
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.LiveLikeUser
import com.livelike.engagementsdk.chat.ChatMessage
import com.livelike.engagementsdk.core.exceptionhelpers.safeRemoteApiCall
import com.livelike.engagementsdk.data.models.Program
import com.livelike.engagementsdk.data.models.ProgramGamificationProfile
import com.livelike.engagementsdk.data.models.ProgramModel
import com.livelike.engagementsdk.data.models.toProgram
import com.livelike.engagementsdk.utils.addAuthorizationBearer
import com.livelike.engagementsdk.utils.addUserAgent
import com.livelike.engagementsdk.utils.extractBoolean
import com.livelike.engagementsdk.utils.extractStringOrEmpty
import com.livelike.engagementsdk.utils.liveLikeSharedPrefs.addPoints
import com.livelike.engagementsdk.utils.liveLikeSharedPrefs.getSessionId
import com.livelike.engagementsdk.utils.logDebug
import com.livelike.engagementsdk.utils.logError
import com.livelike.engagementsdk.utils.logVerbose
import com.livelike.engagementsdk.utils.logWarn
import com.livelike.engagementsdk.widget.util.SingleRunner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.ByteString
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Suppress("USELESS_ELVIS")
internal class EngagementDataClientImpl : DataClient, EngagementSdkDataClient,
    WidgetDataClient, ChatDataClient {

    override suspend fun reportMessage(
        programId: String,
        message: ChatMessage,
        accessToken: String?
    ) {
        remoteCall<LiveLikeUser>(
            BuildConfig.CONFIG_URL.plus("programs/$programId/report/"),
            RequestType.POST,
            RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"), message.toReportMessageJson()
            ),
            accessToken
        )
    }

    private val MAX_PROGRAM_DATA_REQUESTS = 13

    // TODO better error handling for network calls plus better code organisation for that  we can use retrofit if size is ok to go with or write own annotation processor

    override fun registerImpression(impressionUrl: String) {
        if (impressionUrl.isNullOrEmpty()) {
            return
        }
        val client = OkHttpClient()
        val formBody = FormBody.Builder()
            .add(
                "session_id",
                getSessionId()
            ) // TODO: The session id should come from the parameters
            .build()
        try {
            val request = Request.Builder()
                .url(impressionUrl)
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

    private val client = OkHttpClient()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val gson = GsonBuilder().create()

    private fun newRequest(url: String) = Request.Builder().url(url)

    @Suppress("unused")
    private fun Any?.unit() = Unit

    override fun getProgramData(url: String, responseCallback: (program: Program?) -> Unit) {

        fun respondWith(value: Program?) = mainHandler.post { responseCallback(value) }.unit()

        fun respondOnException(op: () -> Unit) = try {
            op()
        } catch (e: Exception) {
            logError { e }.also { respondWith(null) }
        }

        respondOnException {
            if (!URLUtil.isValidUrl(url)) error("Program Url is invalid.")

            val call = client.newCall(newRequest(url).addUserAgent().build())

            var requestCount = 0

            call.enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) = respondOnException {
                    when (response.code()) {
                        in 400..499 -> error("Program Id is invalid $url")

                        in 500..599 -> if (requestCount++ < MAX_PROGRAM_DATA_REQUESTS) {
                            call.clone().enqueue(this)
                            logWarn { "Failed to fetch program data, trying again." }
                        } else {
                            error("Unable to fetch program data, exceeded max retries.")
                        }

                        else -> {
                            val programJsonString = response.body()?.string()
                            val parsedObject =
                                gson.fromJson(programJsonString, ProgramModel::class.java)
                                    ?: error("Program data was null")

                            if (parsedObject.programUrl == null) {
                                // Program Url is the only required field
                                error("Program Url not present in response.")
                            }
                            respondWith(parsedObject.toProgram())
                        }
                    }
                }

                override fun onFailure(call: Call, e: IOException) = respondOnException { throw e }
            })
        }
    }

    override fun getEngagementSdkConfig(
        url: String,
        responseCallback: (config: EngagementSDK.SdkConfiguration) -> Unit
    ) {
        GlobalScope.launch {
            val result =
                remoteCall<EngagementSDK.SdkConfiguration>(url, RequestType.GET, null, null)
            if (result is Result.Success) {
                responseCallback.invoke(result.data)
            } else {
                logError { "The client id is incorrect. Check your configuration." }
            }
        }
    }

    private fun pareseSdkConfiguration(configData: JsonObject): EngagementSDK.SdkConfiguration {
        return EngagementSDK.SdkConfiguration(
            configData.extractStringOrEmpty("url"),
            configData.extractStringOrEmpty("name"),
            configData.extractStringOrEmpty("client_id"),
            configData.extractStringOrEmpty("media_url"),
            configData.extractStringOrEmpty("pubnub_subscribe_key"),
            configData.extractStringOrEmpty("sendbird_app_id"),
            configData.extractStringOrEmpty("sendbird_api_endpoint"),
            configData.extractStringOrEmpty("programs_url"),
            configData.extractStringOrEmpty("sessions_url"),
            configData.extractStringOrEmpty("sticker_packs_url"),
            configData.extractStringOrEmpty("mixpanel_token"),
            mapOf()
        )
    }

    override fun createUserData(
        clientId: String,
        responseCallback: (livelikeUser: LiveLikeUser) -> Unit
    ) {
        client.newCall(
            Request.Builder().url(BuildConfig.CONFIG_URL.plus("applications/$clientId/profile/")).addUserAgent()
                .post(
                    RequestBody.create(
                        null,
                        ByteArray(0)
                    )
                )
                .build()
        ).enqueue(object : Callback {
            override fun onResponse(call: Call?, response: Response) {
                try {
                    val responseData = JsonParser().parse(response.body()?.string()).asJsonObject
                    val user = LiveLikeUser(
                        responseData.extractStringOrEmpty("id"),
                        responseData.extractStringOrEmpty("nickname"),
                        responseData.extractStringOrEmpty("access_token"),
                        responseData.extractBoolean("widgets_enabled"),
                        responseData.extractBoolean("chat_enabled")
                    )
                    logVerbose { user }
                    mainHandler.post { responseCallback.invoke(user) }
                } catch (e: java.lang.Exception) {
                    logError { e }
                }
            }

            override fun onFailure(call: Call?, e: IOException?) {
                logError { e }
            }
        })
    }

    override fun getUserData(
        clientId: String,
        accessToken: String,
        responseCallback: (livelikeUser: LiveLikeUser?) -> Unit
    ) {
        client.newCall(
            Request.Builder().url(BuildConfig.CONFIG_URL.plus("applications/$clientId/profile/"))
                .addUserAgent()
                .addAuthorizationBearer(accessToken)
                .get()
                .build()
        ).enqueue(object : Callback {
            override fun onResponse(call: Call?, response: Response) {
                try {
                    val responseData = JsonParser().parse(response.body()?.string()).asJsonObject
                    val user = LiveLikeUser(
                        responseData.extractStringOrEmpty("id"),
                        responseData.extractStringOrEmpty("nickname"),
                        accessToken,
                        responseData.extractBoolean("widgets_enabled"),
                        responseData.extractBoolean("chat_enabled")
                    )
                    logVerbose { user }
                    mainHandler.post { responseCallback.invoke(user) }
                } catch (e: java.lang.Exception) {
                    logError { e }
                }
            }

            override fun onFailure(call: Call?, e: IOException?) {
                logError { e }
            }
        })
    }

    override suspend fun patchUser(clientId: String, userJson: JsonObject, accessToken: String?) {
        remoteCall<LiveLikeUser>(
            BuildConfig.CONFIG_URL.plus("applications/$clientId/profile/"),
            RequestType.PATCH,
            RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"), userJson.toString()
            ),
            accessToken
        )
    }

    internal suspend inline fun <reified T : Any> remoteCall(
        url: String,
        requestType: RequestType,
        requestBody: RequestBody? = null,
        accessToken: String?
    ): Result<T> {
        return safeRemoteApiCall({
            withContext(Dispatchers.IO) {
                logDebug { "url : $url" }
                val request = Request.Builder()
                    .url(url)
                    .method(requestType.name, requestBody)
                    .addUserAgent()
                    .addAuthorizationBearer(accessToken)
                    .build()
                val call = client.newCall(request)
                val execute = call.execute()
//               TODO add more network handling cases and remove !!, generic exception
                if (execute.isSuccessful) {
                    val responseString = execute.body()?.string()
                    val data: T = gson.fromJson<T>(
                        responseString,
                        T::class.java
                    )
                    Result.Success(data)
                } else {
                    Result.Error(IOException("response code : {$execute.code()} - ${execute.message()}"))
                }
            }
        })
    }

    // / WIDGET CLIENT BELOW

    var voteUrl = ""
    private val singleRunner = SingleRunner()

    override suspend fun voteAsync(
        widgetVotingUrl: String,
        voteId: String?,
        accessToken: String?,
        body: RequestBody?,
        voteCount: Int,
        ispatch: Boolean,
        ispost: Boolean
    ): String? {
        return singleRunner.afterPrevious {
            when {
                ispatch -> return@afterPrevious patchWithBodyAsync(
                    widgetVotingUrl, body, accessToken
                ).extractStringOrEmpty("url")
                ispost -> return@afterPrevious postWithBodyAsync(
                    widgetVotingUrl, body, accessToken
                ).extractStringOrEmpty("url")
                body != null -> if (voteUrl.isEmpty()) {
                    voteUrl =
                        postAsync(widgetVotingUrl, accessToken, body).extractStringOrEmpty("url")
                } else {
                    putAsync(
                        voteUrl, (body ?: FormBody.Builder()
                            .add("option_id", voteId)
                            .add("choice_id", voteId)
                            .build()), accessToken
                    )
                }
                else -> return@afterPrevious null
            }
            return@afterPrevious null
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

    private suspend fun postAsync(url: String, accessToken: String?, body: RequestBody? = null) =
        suspendCoroutine<JsonObject> {
            val request = Request.Builder()
                .url(url)
                .post(body ?: RequestBody.create(null, ByteString.EMPTY))
                .addUserAgent()
                .addAuthorizationBearer(accessToken)
                .build()
            val call = client.newCall(request)
            call.enqueue(object : Callback {
                override fun onResponse(call: Call?, response: Response) {
                    try {
                        it.resume(JsonParser().parse(response.body()?.string()).asJsonObject)
                    } catch (e: Exception) {
                        logError { e }
                        it.resume(JsonObject())
                    }
                }

                override fun onFailure(call: Call?, e: IOException?) {
                    logError { e }
                }
            })
        }

    private suspend fun postWithBodyAsync(url: String, body: RequestBody?, accessToken: String?) =
        suspendCoroutine<JsonObject> {
            val request = Request.Builder()
                .url(url)
                .post(body)
                .addUserAgent()
                .addAuthorizationBearer(accessToken)
                .build()
            val call = client.newCall(request)
            call.enqueue(object : Callback {
                override fun onResponse(call: Call?, response: Response) {
                    try {
                        it.resume(JsonParser().parse(response.body()?.string()).asJsonObject)
                    } catch (e: Exception) {
                        logError { e }
                        it.resume(JsonObject())
                    }
                }

                override fun onFailure(call: Call?, e: IOException?) {
                    logError { e }
                }
            })
        }

    private suspend fun putAsync(url: String, body: RequestBody, accessToken: String?) =
        suspendCoroutine<JsonObject> {
            val request = Request.Builder()
                .url(url)
                .put(body)
                .addUserAgent()
                .addAuthorizationBearer(accessToken)
                .build()
            val call = client.newCall(request)
            call.enqueue(object : Callback {
                override fun onResponse(call: Call?, response: Response) {
                    try {
                        it.resume(JsonParser().parse(response.body()?.string()).asJsonObject)
                    } catch (e: Exception) {
                        logError { e }
                        it.resume(JsonObject())
                    }
                }

                override fun onFailure(call: Call?, e: IOException?) {
                    logError { e }
                }
            })
        }

    private suspend fun patchWithBodyAsync(url: String, body: RequestBody?, accessToken: String?) =
        suspendCoroutine<JsonObject> {
            val request = Request.Builder()
                .url(url)
                .patch(body)
                .addUserAgent()
                .addAuthorizationBearer(accessToken)
                .build()
            val call = client.newCall(request)
            call.enqueue(object : Callback {
                override fun onResponse(call: Call?, response: Response) {
                    try {
                        it.resume(JsonParser().parse(response.body()?.string()).asJsonObject)
                    } catch (e: Exception) {
                        logError { e }
                        it.resume(JsonObject())
                    }
                }

                override fun onFailure(call: Call?, e: IOException?) {
                    logError { e }
                }
            })
        }
}
