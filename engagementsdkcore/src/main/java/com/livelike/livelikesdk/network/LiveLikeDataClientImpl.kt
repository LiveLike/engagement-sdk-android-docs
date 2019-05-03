package com.livelike.livelikesdk.network

import android.os.Handler
import android.os.Looper
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.stream.MalformedJsonException
import com.livelike.engagementsdkapi.LiveLikeUser
import com.livelike.livelikesdk.LiveLikeDataClient
import com.livelike.livelikesdk.LiveLikeSDK
import com.livelike.livelikesdk.LiveLikeSdkDataClient
import com.livelike.livelikesdk.Program
import com.livelike.livelikesdk.util.extractStringOrEmpty
import com.livelike.livelikesdk.util.liveLikeSharedPrefs.getSessionId
import com.livelike.livelikesdk.util.logError
import com.livelike.livelikesdk.util.logVerbose
import com.livelike.livelikesdk.widget.WidgetDataClient
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

internal class LiveLikeDataClientImpl : LiveLikeDataClient, LiveLikeSdkDataClient, WidgetDataClient {

    private val MAX_PROGRAM_DATA_REQUESTS = 13

    override fun registerImpression(impressionUrl: String) {
        if (impressionUrl.isNullOrEmpty()) {
            return
        }
        val client = OkHttpClient()
        val formBody = FormBody.Builder()
            .add("session_id", getSessionId())
            .build()
        try {
            val request = Request.Builder()
                .url(impressionUrl)
                .post(formBody)
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

    override fun getLiveLikeProgramData(url: String, responseCallback: (program: Program) -> Unit) {
        val request = Request.Builder()
            .url(url)
            .build()

        val call = client.newCall(request)
        var requestCount = 0;
        var callback = object : Callback {
            override fun onResponse(call: Call?, response: Response) {
                val responseCode = response.code()
                if(responseCode in 400..499) {
                    logError { "Program Id is invalid "}
                    return
                }

                if(responseCode >= 500) {
                    if(requestCount < MAX_PROGRAM_DATA_REQUESTS) {
                        call?.clone()?.enqueue(this)
                        requestCount += 1
                        logError { "Failed to fetch program data, trying again."}
                    }
                    else {
                        logError { "Unable to fetch program data, exceeded max retries."}
                    }
                    return
                }

                val responseData = response.body()?.string()
                try {
                    mainHandler.post { responseCallback.invoke(parseProgramData(JsonParser().parse(responseData).asJsonObject)) }
                } catch (e: MalformedJsonException) {
                    logError { e }
                }
            }

            override fun onFailure(call: Call?, e: IOException?) {
                logError { e }
            }
        }
        call.enqueue(callback);
    }

    private fun parseProgramData(programData: JsonObject): Program {
        return Program(
            programData.extractStringOrEmpty("url"),
            programData.extractStringOrEmpty("timeline_url"),
            programData.extractStringOrEmpty("content_id"),
            programData.extractStringOrEmpty("id"),
            programData.extractStringOrEmpty("title"),
            programData["widgets_enabled"]?.asBoolean ?: true,
            programData["chat_enabled"]?.asBoolean ?: true,
            programData.extractStringOrEmpty("subscribe_channel"),
            programData.extractStringOrEmpty("sendbird_channel")
        )
    }

    override fun getLiveLikeSdkConfig(url: String, responseCallback: (config: LiveLikeSDK.SdkConfiguration) -> Unit) {
        val request = Request.Builder()
            .url(url)
            .build()

        val call = client.newCall(request)
        call.enqueue(object : Callback {
            override fun onResponse(call: Call?, response: Response) {
                val responseData = response.body()?.string()
                try {
                    mainHandler.post { responseCallback.invoke(pareseSdkConfiguration(JsonParser().parse(responseData).asJsonObject)) }
                } catch (e: MalformedJsonException) {
                    logError { e }
                }
            }

            override fun onFailure(call: Call?, e: IOException?) {
                logError { e }
                mainHandler.post { responseCallback(pareseSdkConfiguration(JsonObject())) }
            }
        })
    }

    private fun pareseSdkConfiguration(configData: JsonObject): LiveLikeSDK.SdkConfiguration {
        return LiveLikeSDK.SdkConfiguration(
            configData.extractStringOrEmpty("url"),
            configData.extractStringOrEmpty("name"),
            configData.extractStringOrEmpty("client_id"),
            configData.extractStringOrEmpty("media_url"),
            configData.extractStringOrEmpty("pubnub_subscribe_key"),
            configData.extractStringOrEmpty("sendbird_app_id"),
            configData.extractStringOrEmpty("sendbird_api_endpoint"),
            configData.extractStringOrEmpty("programs_url"),
            configData.extractStringOrEmpty("sessions_url"),
            configData.extractStringOrEmpty("sticker_packs_url")
        )
    }

    override fun getLiveLikeUserData(url: String, responseCallback: (livelikeUser: LiveLikeUser) -> Unit) {
        val requestString = "{}"
        client.newCall(
            Request.Builder().url(url).post(
                RequestBody.create(
                    MediaType.parse(requestString),
                    requestString
                )
            ).build()
        ).enqueue(object : Callback {
            override fun onResponse(call: Call?, response: Response) {

                val responseData = JsonParser().parse(response.body()?.string()).asJsonObject
                val user = LiveLikeUser(
                    responseData.extractStringOrEmpty("id"),
                    responseData.extractStringOrEmpty("nickname")
                )
                logVerbose { user }
                mainHandler.post { responseCallback.invoke(user) }
            }

            override fun onFailure(call: Call?, e: IOException?) {
                logError { e }
            }
        })
    }

    override fun vote(voteUrl: String, voteUpdateCallback: ((String) -> Unit)?) {
        if (voteUrl == "null" || voteUrl.isEmpty()) {
            logError { "Voting failed as voteUrl is empty" }
            return
        }
        logVerbose { "Voting for $voteUrl" }
        post(voteUrl) { responseJson ->
            voteUpdateCallback?.invoke(responseJson.extractStringOrEmpty("url"))
        }
    }

    override fun changeVote(voteUrl: String, newVoteId: String, voteUpdateCallback: ((String) -> Unit)?) {
        if (voteUrl == "null" || voteUrl.isEmpty()) {
            logError { "Vote Change failed as voteUrl is empty" }
            return
        }
        put(
            voteUrl, FormBody.Builder()
                .add("option_id", newVoteId)
                .build()
        ) { responseJson ->
            voteUpdateCallback?.invoke(responseJson.extractStringOrEmpty("url"))
        }
    }

    override fun fetchQuizResult(answerUrl: String) {
        if (answerUrl == "null" || answerUrl.isEmpty()) {
            logError { "Cannot make a post request to answerUrl as it is empty" }
            return
        }
        logVerbose { "Sending post request for $answerUrl" }
        post(answerUrl)
    }

    private fun post(url: String, responseCallback: ((JsonObject) -> Unit)? = null) {
        val request = Request.Builder()
            .url(url)
            .post(RequestBody.create(null, ByteString.EMPTY))
            .build()
        val call = client.newCall(request)
        call.enqueue(object : Callback {
            override fun onResponse(call: Call?, response: Response) {
                mainHandler.post {
                    responseCallback?.invoke(JsonParser().parse(response.body()?.string()).asJsonObject)
                }
            }

            override fun onFailure(call: Call?, e: IOException?) {
                logError { e }
            }
        })
    }

    private fun put(url: String, body: FormBody, responseCallback: ((JsonObject) -> Unit)? = null) {
        val request = Request.Builder()
            .url(url)
            .put(body)
            .build()
        val call = client.newCall(request)
        call.enqueue(object : Callback {
            override fun onResponse(call: Call?, response: Response) {
                mainHandler.post {
                    responseCallback?.invoke(JsonParser().parse(response.body()?.string()).asJsonObject)
                }
            }

            override fun onFailure(call: Call?, e: IOException?) {
                logError { e }
            }
        })
    }
}
