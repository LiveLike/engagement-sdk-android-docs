package com.livelike.livelikedemo

import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.constraint.Constraints
import android.util.Log
import android.view.View
import com.livelike.livelikedemo.video.ExoPlayerImpl
import com.livelike.livelikedemo.video.VideoPlayer
import kotlinx.android.synthetic.main.activity_exo_player.*

class ExoPlayerActivity : AppCompatActivity() {

    lateinit var player: VideoPlayer
    var useDrawerLayout: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exo_player)

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
