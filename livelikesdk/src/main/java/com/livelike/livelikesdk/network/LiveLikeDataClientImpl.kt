package com.livelike.livelikesdk.network

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.livelike.livelikesdk.LiveLikeDataClient
import com.livelike.livelikesdk.LiveLikeSDK
import com.livelike.livelikesdk.LiveLikeSdkDataClient
import com.livelike.livelikesdk.Program
import com.livelike.livelikesdk.util.extractStringOrEmpty
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class LiveLikeDataClientImpl : LiveLikeDataClient, LiveLikeSdkDataClient {
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
                mainHandler.post { responseCallback.invoke(parseProgramData(JsonParser().parse(responseData).asJsonObject)) }
            }

            override fun onFailure(call: Call?, e: IOException?) {
                //TODO handle error here, or at session level? Currently passing empty Json
                mainHandler.post { responseCallback(parseProgramData(JsonObject())) }
            }
        })
    }

    @SuppressLint("NewApi")
    private fun parseProgramData(programData: JsonObject): Program {
        return Program(
            programData.extractStringOrEmpty("url"),
            programData.extractStringOrEmpty("timeline_url"),
            programData.extractStringOrEmpty("content_id"),
            programData.extractStringOrEmpty("id"),
            programData.extractStringOrEmpty("title"),
            ZonedDateTime.parse(
                programData.extractStringOrEmpty("created_at"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSZZZZZ")
            ).toInstant().toEpochMilli(),
            ZonedDateTime.parse(
                programData.extractStringOrEmpty("started_at"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZZZZZ")
            ).toInstant().toEpochMilli(),
            programData["widgets_enabled"].asBoolean,
            programData["chat_enabled"].asBoolean,
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
                mainHandler.post { responseCallback.invoke(pareseSdkConfiguration(JsonParser().parse(responseData).asJsonObject)) }
            }

            override fun onFailure(call: Call?, e: IOException?) {
                //TODO handle error here, or at session level? Currently passing empty Json
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
}
