package com.livelike.livelikedemo.channel

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.support.annotation.NonNull
import android.support.design.widget.BottomSheetDialog
import android.util.Log
import android.view.ViewGroup
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
    private val channelSelectListeners = mutableListOf<(Channel) -> Unit>()
    private val channelListenersKeys = mutableListOf<String>()
    private val view: ChannelSelectionView = ChannelSelectionView(appContext)
    private var channelBottomSheetDialog: BottomSheetDialog? = null
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
                            if (savedChannel == channel.name) {
                                selectedChannel = channel
                            }
                        }
                        view.channelList = channelList
                        view.channelSelectListener = { channel -> selectedChannel = channel }
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

    private fun getChannelFor(channelData: JSONObject): Channel {
        return Channel(
            channelData.getString("name"),
            URL(channelData.getString("video_url")),
            URL(channelData.getString("video_thumbnail_url")),
            URL(channelData.getString("livelike_program_url"))
        )
    }

    private fun persistChannel(channelName: String) {
        appContext.getSharedPreferences(PREFERENCE_APP_ID, Context.MODE_PRIVATE)
            .edit()
            .putString(PREFERENCE_CHANNEL_ID, channelName)
            .apply()
    }

    fun addChannelSelectListener(key: String, listener: (Channel) -> Unit) {
        if (!channelListenersKeys.contains(key)) {
            channelSelectListeners.add(listener)
        }
    }

    fun removeChannelSelectListener(listener: (Channel) -> Unit) {
        channelSelectListeners.remove(listener)
    }

    fun show(context: Context) {
        channelBottomSheetDialog = BottomSheetDialog(context)
        removeViewParentIfExists()
        channelBottomSheetDialog?.setContentView(view)
        channelBottomSheetDialog?.show()
    }

    fun hide() {
        channelBottomSheetDialog?.hide()
    }

    private fun removeViewParentIfExists() {
        if (view.parent != null)
            (view.parent as ViewGroup).removeView(view)
    }
}

data class Channel(val name: String, val video: URL? = null, val thumbnail: URL? = null, val llProgram: URL? = null)