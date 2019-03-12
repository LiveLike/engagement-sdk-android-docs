package com.livelike.livelikedemo


import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.reflect.KClass


const val USE_DRAWER_LAYOUT = "use_drawer"

class MainActivity : AppCompatActivity() {

    data class PlayerInfo(val playerName: String, val cls: KClass<out Activity>)
    lateinit var  application : LiveLikeApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        application = getApplication() as LiveLikeApplication
        val channelManager = application.channelManager
        setContentView(R.layout.activity_main)

        val player = PlayerInfo("Exo Player", ExoPlayerActivity::class);

        layout_overlay.setOnClickListener {
            startActivity(playerDetailIntent(player))
        }

        layout_side_panel.setOnClickListener {
            startActivity(playerDetailIntent(player, true))
        }

        events_button.setOnClickListener {
            channelManager.show(this)
        }

        channelManager.addChannelSelectListener {
            channelManager.hide()
            events_label.text = it.name
        }
    }
}

fun Context.playerDetailIntent(player: MainActivity.PlayerInfo, useDrawer : Boolean = false): Intent {
    return Intent(this, player.cls.java).apply {
        putExtra(USE_DRAWER_LAYOUT, useDrawer )
    }
}
