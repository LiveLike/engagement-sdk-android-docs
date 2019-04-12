package com.livelike.livelikedemo

import android.os.Bundle
import android.support.constraint.Constraints
import android.support.v7.app.AppCompatActivity
import android.view.WindowManager
import com.livelike.engagementsdkapi.LiveLikeContentSession
import com.livelike.livelikedemo.channel.Channel
import com.livelike.livelikedemo.channel.ChannelManager
import com.livelike.livelikedemo.video.ExoPlayerImpl
import com.livelike.livelikedemo.video.PlayerState
import com.livelike.livelikedemo.video.VideoPlayer
import com.livelike.livelikesdk.LiveLikeSDK
import kotlinx.android.synthetic.main.activity_drawer_demo.*
import java.util.*


class DrawerDemoActivity : AppCompatActivity() {
    companion object {
        const val POSITION = "position"
        const val CHANNEL_NAME = "channelName"
    }

    private lateinit var player: VideoPlayer
    private var session: LiveLikeContentSession? = null
    private var startingState: PlayerState? = null
    private var channelManager: ChannelManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_drawer_demo)

        playerView.layoutParams.width = Constraints.LayoutParams.MATCH_PARENT

        player = ExoPlayerImpl(this, playerView)
        channelManager = (application as LiveLikeApplication).channelManager

        Timer().schedule(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    val pdtTime = player.getPDT()
                }
            }
        }, 0, 100)

        channelManager?.let {
            selectChannel(it.selectedChannel)
        }

        channelManager?.addChannelSelectListener {
            channelManager?.hide()
            selectChannel(it)
        }
    }

    private fun selectChannel(channel: Channel) {
        player.stop()
        player.release()
        startingState = PlayerState(0, 0, true)
        initializeLiveLikeSDK(channel)
    }

    private fun initializeLiveLikeSDK(channel: Channel) {
        val sdk = LiveLikeSDK(getString(R.string.app_id), applicationContext)
        if (channel == ChannelManager.NONE_CHANNEL) {
            session?.close()
        } else {
//            player.createSession(channel.llProgram.toString(), sdk) {
//                this.session = it
//
//                chatWidget.chat.setSession(it)
//                chatWidget.widgets.setSession(it)
//
//                player.playMedia(Uri.parse(channel.video.toString()), startingState ?: PlayerState())
//            }
        }
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
        outState?.putLong(POSITION, player.position())
    }
}
