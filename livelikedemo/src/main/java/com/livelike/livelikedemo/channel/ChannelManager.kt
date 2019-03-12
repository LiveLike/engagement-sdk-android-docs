package com.livelike.livelikedemo.channel

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.support.annotation.NonNull
import com.livelike.livelikesdk.util.logError
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.URL

class ChannelManager(private val channelConfigUrl: String, val appContext: Context) {

    companion object {
        private val PREFERENCE_CHANNEL_ID = "ChannelId"
        private val PREFERENCE_APP_ID = "livelike-testapp"
        val NONE_CHANNEL = Channel("None - Clear Session")
    }

    private val client: OkHttpClient = OkHttpClient()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val channelSelectListeners  = mutableListOf<(Channel) -> Unit>()
    val channelList: MutableList<Channel> = mutableListOf()
    @NonNull
    var selectedChannel : Channel = NONE_CHANNEL
    set(channel) {
        field = channel
        persistChannel(channel.name)
        for (listener in channelSelectListeners)
            listener.invoke(channel)
    }

    var view: ChannelSelectionView = ChannelSelectionView(appContext)
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
                    val savedChannel = appContext.getSharedPreferences(PREFERENCE_APP_ID, Context.MODE_PRIVATE)
                        .getString(PREFERENCE_CHANNEL_ID, "")
                    try {
                        val json = JSONObject(responseData)
                        val results = json.getJSONArray("results")
                        for (i in 0..(results.length() - 1)) {
                            val channel = getChannelFor(results.getJSONObject(i))
                            channelList.add(channel)
                            if(savedChannel == channel.name) {
                                selectedChannel = channel
                            }
                        }
                        view.channelList = channelList
                        view.channelSelectListener = { channel -> selectedChannel = channel }
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

    private fun persistChannel(channelName : String) {
        appContext.getSharedPreferences(PREFERENCE_APP_ID, Context.MODE_PRIVATE)
            .edit()
            .putString(PREFERENCE_CHANNEL_ID, channelName)
            .apply()
    }

    fun addChannelSelectListener(listener: (Channel) -> Unit) {
        channelSelectListeners.add(listener)
    }

    fun removeChannelSelectListener(listener: (Channel) -> Unit) {
        channelSelectListeners.remove(listener)
    }
}

/*
Class Representing a Demo App channel which comes from service via json like:
	{
      "name": "Android Demo Channel 1",
      "video_url": "http://livecut-streams.livelikecdn.com/live/colorbars-angle1/index.m3u8",
      "video_thumbnail_url": "http://lorempixel.com/200/200/?2",
      "livelike_program_url": "https://livelike-blast.herokuapp.com/api/v1/programs/00f4cdfd-6a19-4853-9c21-51aa46d070a0/"
    }
}*/

data class Channel(val name: String, val video: URL? = null, val thumbnail: URL? = null, val llProgram: URL? = null)