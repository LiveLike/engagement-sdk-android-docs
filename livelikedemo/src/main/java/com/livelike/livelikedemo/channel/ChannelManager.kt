package com.livelike.livelikedemo.channel

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.support.annotation.NonNull
import android.util.Log
import com.livelike.livelikedemo.PREFERENCES_APP_ID
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.URL

class ChannelManager(private val channelConfigUrl: String, val appContext: Context) {


    private val client: OkHttpClient = OkHttpClient()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val channelSelectListeners = mutableListOf<(Channel) -> Unit>()
    val channelList: MutableList<Channel> = mutableListOf()
    @NonNull
    var selectedChannel: Channel = NONE_CHANNEL
        set(channel) {
            field = channel
            persistChannel(channel.name)
            for (listener in channelSelectListeners)
                listener.invoke(channel)
        }

    init {
        loadClientConfig()
    }

    fun loadClientConfig() {
        val request = Request.Builder()
            .url(channelConfigUrl)
            .build()

        val call = client.newCall(request)
        call.enqueue(object : Callback {
            override fun onResponse(call: okhttp3.Call, response: Response) {
                val responseData = response.body()?.string()
                mainHandler.post {
                    val savedChannel =
                        appContext.getSharedPreferences(PREFERENCES_APP_ID, Context.MODE_PRIVATE)
                            .getString(PREFERENCE_CHANNEL_ID, "")
                    try {
                        val json = JSONObject(responseData)
                        val results = json.getJSONArray("results")
                        if (results.length() > 0) {
                            channelList.clear()
                        }
                        for (i in 0 until results.length()) {
                            val channel = getChannelFor(results.getJSONObject(i))
                            channel?.let {
                                channelList.add(channel)
                                if (savedChannel == channel.name) {
                                    selectedChannel = channel
                                }
                            }
                        }
                    } catch (e: JSONException) {
                        Log.e("ChannelMger", e.message)
                    }
                }
            }

            override fun onFailure(call: okhttp3.Call, e: IOException?) {
                Log.e("ChannelMgr", e?.message)
            }
        })
    }

    private fun getChannelFor(channelData: JSONObject): Channel? {
        val streamUrl = channelData.getString("stream_url")
        val url: URL? = when {
            streamUrl.isNullOrEmpty() || streamUrl.equals("null") -> null
            else -> URL(streamUrl)
        }
        return Channel(
            channelData.getString("title"),
            url,
            null,
            channelData.getString("id")
        )
    }

    private fun persistChannel(channelName: String) {
        appContext.getSharedPreferences(PREFERENCES_APP_ID, Context.MODE_PRIVATE)
            .edit()
            .putString(PREFERENCE_CHANNEL_ID, channelName)
            .apply()
    }

    fun getChannels(): ArrayList<Channel> {
        return ArrayList(channelList)
    }

    companion object {
        private val PREFERENCE_CHANNEL_ID = "ChannelId"
        val NONE_CHANNEL = Channel("None - Clear Session")
    }
}

data class Channel(
    val name: String,
    val video: URL? = null,
    val thumbnail: URL? = null,
    val llProgram: String? = null
)
