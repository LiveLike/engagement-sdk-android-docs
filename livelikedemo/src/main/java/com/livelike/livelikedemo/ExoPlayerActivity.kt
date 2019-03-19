package com.livelike.livelikedemo

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.support.constraint.Constraints
import android.support.v7.app.AppCompatActivity
import android.text.method.ScrollingMovementMethod
import android.view.View
import com.livelike.livelikedemo.channel.Channel
import com.livelike.livelikedemo.channel.ChannelManager
import com.livelike.livelikedemo.video.ExoPlayerImpl
import com.livelike.livelikedemo.video.PlayerState
import com.livelike.livelikedemo.video.VideoPlayer
import com.livelike.livelikesdk.LiveLikeContentSession
import com.livelike.livelikesdk.LiveLikeSDK
import com.livelike.livelikesdk.util.registerLogsHandler
import kotlinx.android.synthetic.main.activity_exo_player.*
import kotlinx.android.synthetic.main.widget_chat_stacked.*
import java.util.*


class ExoPlayerActivity : AppCompatActivity() {
    companion object {
        const val AD_STATE = "adstate"
        const val POSITION = "position"
        const val CHANNEL_NAME = "channelName"
    }

    private lateinit var player: VideoPlayer
    private var session: LiveLikeContentSession? = null
    private var startingState: PlayerState? = null
    private var channelManager: ChannelManager? = null

    private var adsPlaying = false
    set(adsPlaying) {
        field = adsPlaying

        if(adsPlaying){
            startAd.text = "Stop Ads"
            player.stop()
            session?.pause()
        }
        else{
            startAd.text = "Start Ads"
            player.start()
            session?.resume()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exo_player)

        playerView.layoutParams.width = Constraints.LayoutParams.MATCH_PARENT

        player = ExoPlayerImpl(this, playerView)
        channelManager = (application as LiveLikeApplication).channelManager
        openLogs.setOnClickListener {
            fullLogs.visibility = if (fullLogs.visibility == View.GONE) View.VISIBLE else View.GONE
        }
        fullLogs.movementMethod = ScrollingMovementMethod()

        adsPlaying = savedInstanceState?.getBoolean(AD_STATE) ?: false
        val position = savedInstanceState?.getLong(POSITION) ?: 0
        startingState = PlayerState(0, position, !adsPlaying)

        Timer().schedule(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    val pdtTime = player.getPDT()
                    videoTimestamp?.text = Date(pdtTime).toString()
                }
            }
        }, 0, 100)

        setUpAdClickListeners()
        
        channelManager?.let {
            selectChannel(it.selectedChannel)
        }

        channelManager?.addChannelSelectListener {
            channelManager?.hide()
            selectChannel(it)
        }

        selectChannelButton.setOnClickListener {  channelManager?.show(this) }
    }

    private fun setUpAdClickListeners() {
        startAd.setOnClickListener {
            adsPlaying = !adsPlaying
        }
    }

    private fun selectChannel(channel: Channel) {
        player.stop()
        player.release()
        startingState = PlayerState(0, 0, !adsPlaying)
        initializeLiveLikeSDK(channel)
    }

    private fun initializeLiveLikeSDK(channel: Channel) {
        registerLogsHandler(object : (String) -> Unit {
            override fun invoke(text: String) {
                Handler(mainLooper).post {
                    logsPreview.text = "$text \n\n ${logsPreview.text}"
                    fullLogs.text = "$text \n\n ${fullLogs.text}"
                }
            }
        })

        val sdk = LiveLikeSDK(getString(R.string.app_id), applicationContext)
        if(channel == ChannelManager.NONE_CHANNEL) {
            session?.close()
        } else {
            player.createSession(channel.llProgram.toString(), sdk) {
                this.session = it

//                val chatAdapter = ChatAdapter(it, chatTheme, DefaultChatCellFactory(applicationContext, null))
//                chat_view.setDataSource(chatAdapter)

                chat_view.setSession(it)
                widget_view.setSession(it)

                player.playMedia(Uri.parse(channel.video.toString()), startingState ?: PlayerState())
            }
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
        outState?.putString(CHANNEL_NAME, channelManager?.selectedChannel?.name ?: "")
        outState?.putBoolean(AD_STATE, adsPlaying)
        outState?.putLong(POSITION, player.position())
    }
}
