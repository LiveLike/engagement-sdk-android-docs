package com.livelike.livelikedemo

import android.app.Application
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.support.constraint.Constraints
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.PopupMenu
import com.livelike.livelikedemo.video.Channel
import com.livelike.livelikedemo.video.ExoPlayerImpl
import com.livelike.livelikedemo.video.PlayerState
import com.livelike.livelikedemo.video.VideoPlayer
import com.livelike.livelikesdk.LiveLikeContentSession
import com.livelike.livelikesdk.LiveLikeSDK
import com.livelike.livelikesdk.chat.ChatAdapter
import com.livelike.livelikesdk.chat.ChatTheme
import com.livelike.livelikesdk.chat.DefaultChatCellFactory
import kotlinx.android.synthetic.main.activity_exo_player.*
import kotlinx.android.synthetic.main.widget_chat_stacked.*
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.URL

class ExoPlayerActivity : AppCompatActivity() {

    companion object {
        const val AD_STATE = "adstate"
        const val POSITION = "position"
        const val CHANNEL_NAME = "channelName"
        const val TEST_CONFIG_URL =
            "https://livelike-webs.s3.amazonaws.com/mobile-pilot/video-backend-sdk-android-demo.json"
    }

    private val client: OkHttpClient = OkHttpClient()
    private val channelList: MutableList<Channel> = mutableListOf()

    private lateinit var player: VideoPlayer
    var useDrawerLayout: Boolean = false
    private var session: LiveLikeContentSession? = null
    private var startingState: PlayerState? = null
    private var selectedChannel: Channel? = null

    private var adsPlaying = false
    set(adsPlaying) {
        field = adsPlaying
        adOverlay.visibleOrGone(adsPlaying)
        stopAd.visibleOrGone(adsPlaying)
        startAd.visibleOrGone(!adsPlaying)

        if(adsPlaying){
            player.stop()
            session?.pause()
        }
        else{
            player.start()
            session?.resume()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exo_player)

        useDrawerLayout = intent.getBooleanExtra(USE_DRAWER_LAYOUT, false)
        if(!useDrawerLayout)
            playerView.layoutParams.width = Constraints.LayoutParams.MATCH_PARENT

        player = ExoPlayerImpl(this, playerView)

        adsPlaying = savedInstanceState?.getBoolean(AD_STATE) == true
        val position = savedInstanceState?.getLong(POSITION) ?: 0
        startingState = PlayerState(0, position, !adsPlaying)


        loadClientConfig(savedInstanceState?.getString(CHANNEL_NAME) ?: "")
        setUpAdClickListeners()
    }

    private fun loadClientConfig(channelName: String) {
        val request = Request.Builder()
            .url(TEST_CONFIG_URL)
            .build()

        val call = client.newCall(request)
        call.enqueue(object : Callback {
            override fun onResponse(call: okhttp3.Call, response: Response) {
                val responseData = response.body()?.string()
                runOnUiThread {
                    try {
                        val json = JSONObject(responseData)
                        val results = json.getJSONArray("results")
                        for (i in 0..(results.length() - 1)) {
                            val channel = getChannelFor(results.getJSONObject(i))
                            channelList.add(channel)
                            if (channelName.isNotEmpty() && channel.name.equals(channelName))
                                selectChannel(channel)
                        }
                        displayChannelList(channelList)
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            }

            override fun onFailure(call: okhttp3.Call, e: IOException?) {
                println("Request Failure.")
            }
        })
    }
    private fun setUpAdClickListeners() {
        startAd.setOnClickListener {
            adsPlaying = true
        }

        stopAd.setOnClickListener {
            adsPlaying = false
        }
    }

    private fun displayChannelList(channels: List<Channel>) {
        if (channels.size > 1) {
            selectChannelButton.visibility = View.VISIBLE
            selectChannelButton.setOnClickListener {
                val popupMenu = PopupMenu(this, selectChannelButton)
                popupMenu.setOnMenuItemClickListener {
                    for (channel: Channel in channels) {
                        if (it.title.equals(channel.name)) {
                            selectChannel(channel)
                        }
                    }
                    true
                }
                for (channel: Channel in channels)
                    popupMenu.menu.add(channel.name)
                popupMenu.show()
            }
        } else if (channels.size == 1) {
            selectChannel(channels[0])
        }
    }

    private fun selectChannel(channel: Channel) {
        player.stop()
        player.release()
        startingState = PlayerState(0, 0, !adsPlaying)
        initializeLiveLikeSDK(channel)
    }

    private fun initializeLiveLikeSDK(channel: Channel) {
        selectedChannel = channel

        val sdk = LiveLikeSDK(getString(R.string.app_id), applicationContext)
        player.createSession(channel.llProgram.toString(), sdk) {
            this.session = it
            // Bind the chatView object here with the session.
            val chatTheme = ChatTheme.Builder()
                .backgroundColor(Color.RED)
                .cellFont(Typeface.SANS_SERIF)
                .build()
            val chatAdapter = ChatAdapter(it, chatTheme, DefaultChatCellFactory(applicationContext, null))
            chat_view.setDataSource(chatAdapter)

            chat_view.setSession(it)
            widget_view.setSession(it)

            player.playMedia(Uri.parse(channel.video.toString()), startingState ?: PlayerState())
        }
    }

    override fun onStart() {
        super.onStart()
        if(!adsPlaying)
            player.start()
    }

    override fun onStop() {
        super.onStop()
        player.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putString(CHANNEL_NAME, selectedChannel?.name ?: "")
        outState?.putBoolean(AD_STATE, adOverlay.visibility == View.VISIBLE)
        outState?.putLong(POSITION, player.position())
    }

    //TODO move this to common ground
    private fun getChannelFor(channelData: JSONObject): Channel {
        return Channel(
            channelData.getString("name"),
            URL(channelData.getString("video_url")),
            URL(channelData.getString("video_thumbnail_url")),
            URL(channelData.getString("livelike_program_url"))
        )
    }
}

fun View.visibleOrGone(visible: Boolean) {
    visibility = if(visible) View.VISIBLE else View.GONE
}
