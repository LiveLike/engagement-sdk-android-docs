package com.livelike.livelikesdk.network

import android.os.Handler
import android.os.Looper
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.livelike.livelikesdk.LiveLikeDataClient
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class LiveLikeDataClientImpl : LiveLikeDataClient {
    private val client = OkHttpClient()
    private val mainHandler = Handler(Looper.getMainLooper())
    override fun getLiveLikeProgramData(url: String, responseCallback: (response: JsonObject) -> Unit) {
        val request = Request.Builder()
            .url(url)
            .build()

        val call = client.newCall(request)
        call.enqueue(object: Callback {
            override fun onResponse(call: Call?, response: Response) {

                val responseData = response.body()?.string()
                //TODO validate this is actually json?
                mainHandler.post { responseCallback.invoke(JsonParser().parse(responseData).asJsonObject) }
            }

            override fun onFailure(call: Call?, e: IOException?) {
                //TODO handle error here, or at session level? Currently passing empty Json
                mainHandler.post { responseCallback(JsonObject()) }
            }
        })
    }
}