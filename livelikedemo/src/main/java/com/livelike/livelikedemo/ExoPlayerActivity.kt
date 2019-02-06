package com.livelike.livelikedemo

import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.constraint.Constraints
import android.view.View
import com.livelike.livelikedemo.video.ExoPlayerImpl
import com.livelike.livelikedemo.video.VideoPlayer
import com.livelike.livelikesdk.LiveLikeSDK
import com.livelike.livelikesdk.chat.*
import com.livelike.livelikesdk.messaging.EpochTime
import kotlinx.android.synthetic.main.activity_exo_player.*
import kotlinx.android.synthetic.main.widget_chat_stacked.*
import java.util.Date

class ExoPlayerActivity : AppCompatActivity() {

    lateinit var player: VideoPlayer
    var useDrawerLayout: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exo_player)
        initializeLiveLikeSDK()
        useDrawerLayout = intent.getBooleanExtra(USE_DRAWER_LAYOUT, false)
        if(!useDrawerLayout)
            playerView.layoutParams.width = Constraints.LayoutParams.MATCH_PARENT
        player = ExoPlayerImpl(this, playerView)
        player.playMedia(Uri.parse("https://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4"))

        startAd.setOnClickListener {
            player.stop()
            //Pause call to sdk here
            adOverlay.visibility = View.VISIBLE
            stopAd.visibility = View.VISIBLE
            startAd.visibility = View.GONE
        }

        stopAd.setOnClickListener {
            player.start()
            //Resume call to sdk here
            adOverlay.visibility = View.GONE
            stopAd.visibility = View.GONE
            startAd.visibility = View.VISIBLE
        }
    }

    private fun initializeLiveLikeSDK() {
        val sdk = LiveLikeSDK("app_Id")
        val session = sdk.createContentSession("someContentId") { currentPlayheadPosition() }

        // Bind the chatView object here with the session.
        val chatTheme = ChatTheme.Builder()
                            .backgroundColor(Color.RED)
                            .cellFont(Typeface.SANS_SERIF)
                        .build()
        val chatAdapter = ChatAdapter(session, chatTheme, DefaultChatCellFactory(applicationContext, null))
        chat_view.setDataSource(chatAdapter)
        widget_view.setSession(session)
    }

    private fun currentPlayheadPosition() =  EpochTime(System.currentTimeMillis())

    override fun onStart() {
        super.onStart()
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
}
