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
import com.livelike.livelikesdk.util.liveLikeSharedPrefs.getUserId
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
    override fun registerImpression(impressionUrl: String) {
        if (impressionUrl.isNullOrEmpty()) {
            return
        }
        val client = OkHttpClient()
        val formBody = FormBody.Builder()
            .add("session_id", getUserId())
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
        call.enqueue(object : Callback {
            override fun onResponse(call: Call?, response: Response) {
                val responseData = response.body()?.string()
                try {
                    mainHandler.post { responseCallback.invoke(parseProgramData(JsonParser().parse(responseData).asJsonObject)) }
                } catch (e : MalformedJsonException) {
                    logError { e }
                }
            }

            override fun onFailure(call: Call?, e: IOException?) {
                logError { e }
                mainHandler.post { responseCallback(parseProgramData(JsonObject())) }
            }
        })
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
            programData.extractStringOrEmpty("sendbird_channel"),
            programData.extractStringOrEmpty("stream_url")
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
                } catch ( e: MalformedJsonException) {
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

    override fun vote(url: String) {
        post(url)
    }

    private fun post(url: String) {
        val request = Request.Builder()
            .url(url)
            .post(RequestBody.create(null, ByteString.EMPTY))
            .build()
        val call = client.newCall(request)
        call.enqueue(object : Callback {
            override fun onResponse(call: Call?, response: Response) {}
            override fun onFailure(call: Call?, e: IOException?) { logError { e } }
        })
    }
}

