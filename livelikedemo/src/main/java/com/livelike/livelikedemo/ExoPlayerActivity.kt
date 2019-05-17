package com.livelike.livelikedemo

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.support.constraint.Constraints
import android.support.v7.app.AppCompatActivity
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.view.WindowManager
import com.livelike.engagementsdkapi.LiveLikeContentSession
import com.livelike.livelikedemo.channel.Channel
import com.livelike.livelikedemo.channel.ChannelManager
import com.livelike.livelikedemo.video.PlayerState
import com.livelike.livelikedemo.video.VideoPlayer
import com.livelike.livelikesdk.LiveLikeSDK
import com.livelike.livelikesdk.utils.registerLogsHandler
import com.livelike.livelikesdk.widget.viewModel.WidgetContainerViewModel
import kotlinx.android.synthetic.main.activity_exo_player.fullLogs
import kotlinx.android.synthetic.main.activity_exo_player.logsPreview
import kotlinx.android.synthetic.main.activity_exo_player.openLogs
import kotlinx.android.synthetic.main.activity_exo_player.playerView
import kotlinx.android.synthetic.main.activity_exo_player.selectChannelButton
import kotlinx.android.synthetic.main.activity_exo_player.startAd
import kotlinx.android.synthetic.main.activity_exo_player.videoTimestamp
import kotlinx.android.synthetic.main.widget_chat_stacked.chat_view
import kotlinx.android.synthetic.main.widget_chat_stacked.widget_view
import java.util.Date
import java.util.Timer
import java.util.TimerTask

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
    lateinit var sdk: LiveLikeSDK

    private var adsPlaying = false
    set(adsPlaying) {
        field = adsPlaying

        if (adsPlaying) {
            startAd.text = "Stop Ads"
            player.stop()
            session?.pause()
        } else {
            startAd.text = "Start Ads"
            player.start()
            session?.resume()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_exo_player)
        sdk = LiveLikeSDK(getString(R.string.app_id), applicationContext)

        playerView.layoutParams.width = Constraints.LayoutParams.MATCH_PARENT

        player = (application as LiveLikeApplication).createPlayer(playerView)
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

        channelManager?.addChannelSelectListener("exoplayer") {
            channelManager?.hide()
            (application as LiveLikeApplication).session = null
            selectChannel(it)
        }

        selectChannelButton.setOnClickListener { channelManager?.show(this) }
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

        session?.close()
        if (channel != ChannelManager.NONE_CHANNEL) {
            val session = (application as LiveLikeApplication).createSession(channel.llProgram.toString(), sdk)

            chat_view.setSession(session)
            WidgetContainerViewModel(widget_view, session)

            this.session = session

            player.playMedia(Uri.parse(channel.video.toString()), startingState ?: PlayerState())
        }
    }

    override fun onStart() {
        super.onStart()
        if (!adsPlaying)
            player.start()
    }

    override fun onStop() {
        super.onStop()
        player.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onPause() {
        super.onPause()
        this.session?.pause()
    }

    override fun onResume() {
        super.onResume()
        this.session?.resume()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putString(CHANNEL_NAME, channelManager?.selectedChannel?.name ?: "")
        outState?.putBoolean(AD_STATE, adsPlaying)
        outState?.putLong(POSITION, player.position())
    }
}
