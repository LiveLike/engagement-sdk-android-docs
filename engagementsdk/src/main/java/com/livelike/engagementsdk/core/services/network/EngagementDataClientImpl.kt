package com.livelike.engagementsdk.core.services.network

import android.os.Handler
import android.os.Looper
import android.webkit.URLUtil
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.LiveLikeUser
import com.livelike.engagementsdk.core.data.models.Program
import com.livelike.engagementsdk.core.data.models.ProgramModel
import com.livelike.engagementsdk.core.data.models.toProgram
import com.livelike.engagementsdk.core.exceptionhelpers.safeRemoteApiCall
import com.livelike.engagementsdk.core.utils.addAuthorizationBearer
import com.livelike.engagementsdk.core.utils.addUserAgent
import com.livelike.engagementsdk.core.utils.extractBoolean
import com.livelike.engagementsdk.core.utils.extractStringOrEmpty
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.core.utils.logError
import com.livelike.engagementsdk.core.utils.logVerbose
import com.livelike.engagementsdk.core.utils.logWarn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
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
internal open class EngagementDataClientImpl : DataClient,
    EngagementSdkDataClient {

    // TODO better error handling for network calls plus better code organisation for that  we can use retrofit if size is ok to go with or write own annotation processor

    private val client = OkHttpClient()
    private val mainHandler = Handler(Looper.getMainLooper())
    internal val gson = GsonBuilder().create()

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
        responseCallback: (config: Result<EngagementSDK.SdkConfiguration>) -> Unit
    ) {
        GlobalScope.launch {
            val result =
                remoteCall<EngagementSDK.SdkConfiguration>(
                    url,
                    RequestType.GET, null, null
                )
            responseCallback.invoke(result)
            if (result is Result.Error) {
                logError { "The client id is incorrect. Check your configuration." }
            }
        }
    }

    override fun createUserData(
        profileUrl: String,
        responseCallback: (livelikeUser: LiveLikeUser) -> Unit
    ) {
        client.newCall(
            Request.Builder().url(profileUrl).addUserAgent()
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
                        responseData.extractBoolean("chat_enabled"),
                        null,
                        responseData.extractStringOrEmpty("url"),
                        responseData.extractStringOrEmpty("chat_room_memberships_url")
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
        profileUrl: String,
        accessToken: String,
        responseCallback: (livelikeUser: LiveLikeUser?) -> Unit
    ) {
        client.newCall(
            Request.Builder().url(profileUrl)
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
                        responseData.extractBoolean("chat_enabled"),
                        null,
                        responseData.extractStringOrEmpty("url"),
                        responseData.extractStringOrEmpty("chat_room_memberships_url")
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

    override suspend fun patchUser(profileUrl: String, userJson: JsonObject, accessToken: String?) {
        remoteCall<LiveLikeUser>(
            profileUrl,
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
                    Result.Success(
                        data
                    )
                } else {
                    Result.Error(
                        IOException("response code : ${execute.code()} - ${execute.message()}")
                    )
                }
            }
        })
    }

    // / WIDGET CLIENT BELOW

    internal suspend fun postAsync(
        url: String,
        accessToken: String?,
        body: RequestBody? = null,
        requestType: RequestType = RequestType.POST
    ) =
        suspendCoroutine<JsonObject> {
            val request = Request.Builder()
                .url(url)
                .method(requestType.name, body ?: RequestBody.create(null, ByteString.EMPTY))
                .addUserAgent()
                .addAuthorizationBearer(accessToken)
                .build()
            val call = client.newCall(request)
            call.enqueue(object : Callback {
                override fun onResponse(call: Call?, response: Response) {
                    try {
                        val s = response.body()?.string()
                        it.resume(JsonParser().parse(s).asJsonObject)
                    } catch (e: Exception) {
                        logError { e }
                        it.resume(JsonObject())
                    }
                }

                override fun onFailure(call: Call?, e: IOException?) {
                    it.resume(JsonObject())
                    logError { e }
                }
            })
        }

    companion object {
        private const val MAX_PROGRAM_DATA_REQUESTS = 13
    }
}
