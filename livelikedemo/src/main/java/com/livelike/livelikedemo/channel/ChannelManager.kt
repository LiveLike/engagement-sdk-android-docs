package com.livelike.livelikedemo.channel

import android.os.Handler
import android.os.Looper
import com.livelike.livelikedemo.video.Channel
import com.livelike.livelikesdk.util.logError
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.URL

class ChannelManager(private val channelConfigUrl: String) {

    val channelList: MutableList<Channel> = mutableListOf()
    private val client: OkHttpClient = OkHttpClient()
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        loadClientConfig()
    }

    private fun loadClientConfig() {
        val request = Request.Builder()
            .url(channelConfigUrl)
            .build()

        val call = client.newCall(request)
        call.enqueue(object : Callback {
            override fun onResponse(call: okhttp3.Call, response: Response) {
                val responseData = response.body()?.string()
                mainHandler.post {
                    try {
                        val json = JSONObject(responseData)
                        val results = json.getJSONArray("results")
                        for (i in 0..(results.length() - 1)) {
                            val channel = getChannelFor(results.getJSONObject(i))
                            channelList.add(channel)
                        }
                    } catch (e: JSONException) {
                        logError { e }
                    }
                }
            }

            override fun onFailure(call: okhttp3.Call, e: IOException?) {
                logError { "Request Failure: $e" }
            }
        })
    }

    private fun getChannelFor(channelData: JSONObject): Channel {
        return Channel(
            channelData.getString("name"),
            URL(channelData.getString("video_url")),
            URL(channelData.getString("video_thumbnail_url")),
            URL(channelData.getString("livelike_program_url"))
        )
    }
}