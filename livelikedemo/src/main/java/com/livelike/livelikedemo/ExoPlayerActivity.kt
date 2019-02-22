package com.livelike.livelikedemo

import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.support.constraint.Constraints
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.livelike.livelikedemo.video.ExoPlayerImpl
import com.livelike.livelikedemo.video.VideoPlayer
import com.livelike.livelikesdk.LiveLikeContentSession
import com.livelike.livelikesdk.LiveLikeSDK
import com.livelike.livelikesdk.chat.ChatAdapter
import com.livelike.livelikesdk.chat.ChatTheme
import com.livelike.livelikesdk.chat.DefaultChatCellFactory
import com.livelike.livelikesdk.messaging.EpochTime
import kotlinx.android.synthetic.main.activity_exo_player.*
import kotlinx.android.synthetic.main.widget_chat_stacked.*

class ExoPlayerActivity : AppCompatActivity() {

    companion object {
        const val AD_STATE = "adstate"
        const val POSITION = "position"
    }

    private var player: VideoPlayer? = null
    var useDrawerLayout: Boolean = false
    private lateinit var session: LiveLikeContentSession
    private var adsPlaying = false
    set(adsPlaying) {
        field = adsPlaying
        adOverlay.visibleOrGone(adsPlaying)
        stopAd.visibleOrGone(adsPlaying)
        startAd.visibleOrGone(!adsPlaying)

        if(adsPlaying){
            player?.stop()
            session.pause()
        }
        else{
            player?.start()
            session.resume()
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exo_player)
        initializeLiveLikeSDK()
        useDrawerLayout = intent.getBooleanExtra(USE_DRAWER_LAYOUT, false)
        if(!useDrawerLayout)
            playerView.layoutParams.width = Constraints.LayoutParams.MATCH_PARENT

        player = ExoPlayerImpl(this, playerView)

        adsPlaying = savedInstanceState?.getBoolean(AD_STATE) == true
        val position = savedInstanceState?.getLong(POSITION) ?: 0

        player?.playMedia(
            Uri.parse("http://livecut-streams.livelikecdn.com/live/colorbars-angle1/index.m3u8"),
            position,
            !adsPlaying
        )
        setUpAdClickListeners()
    }

    private fun setUpAdClickListeners() {
        startAd.setOnClickListener {
            adsPlaying = true
        }

        stopAd.setOnClickListener {
            adsPlaying = false

        }
    }

    private fun initializeLiveLikeSDK() {
        val sdk = LiveLikeSDK("app_Id")
        session = sdk.createContentSession("someContentId") {
            EpochTime(player?.getCurrentDate()!!)
        }

        // Bind the chatView object here with the session.
        val chatTheme = ChatTheme.Builder()
                            .backgroundColor(Color.RED)
                            .cellFont(Typeface.SANS_SERIF)
                        .build()
        val chatAdapter = ChatAdapter(session, chatTheme, DefaultChatCellFactory(applicationContext, null))
        chat_view.setDataSource(chatAdapter)
        chat_view.setSession(session)
        widget_view.setSession(session)
    }

    private fun currentPlayheadPosition() = EpochTime(player?.position() ?: 0)

    override fun onStart() {
        super.onStart()
        if(!adsPlaying)
            player?.start()
    }

    override fun onStop() {
        super.onStop()
        player?.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)

        outState?.putBoolean(AD_STATE, adOverlay.visibility == View.VISIBLE)
        outState?.putLong(POSITION, player?.position() ?: 0)
    }
}

fun View.visibleOrGone(visible: Boolean) {
    visibility = if(visible) View.VISIBLE else View.GONE
}
