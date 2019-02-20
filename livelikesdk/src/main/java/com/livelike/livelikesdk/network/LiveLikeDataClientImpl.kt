package com.livelike.livelikesdk.network

import android.util.Log
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.livelike.livelikesdk.LiveLikeDataClient
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class LiveLikeDataClientImpl : LiveLikeDataClient {
    private val client = OkHttpClient()

    override fun getLiveLikeProgramData(url: String, responseCallback: (response: JsonObject) -> Unit) {
        val request = Request.Builder()
            .url(url)
            .build()

        val call = client.newCall(request)
        call.enqueue(object: Callback {
            override fun onResponse(call: Call?, response: Response) {
                val responseData = response.body()?.string()
                //TODO validate this is actually json?
                responseCallback.invoke(JsonParser().parse(responseData).asJsonObject)
            }

            override fun onFailure(call: Call?, e: IOException?) {
                //TODO handle error here, or at session level? Currently passing empty Json
                responseCallback(JsonObject())
            }
        })

    }
}