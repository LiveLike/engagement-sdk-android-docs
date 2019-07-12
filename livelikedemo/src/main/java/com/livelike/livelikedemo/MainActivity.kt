package com.livelike.livelikedemo

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.chat_only_button
import kotlinx.android.synthetic.main.activity_main.events_button
import kotlinx.android.synthetic.main.activity_main.events_label
import kotlinx.android.synthetic.main.activity_main.layout_overlay
import kotlinx.android.synthetic.main.activity_main.layout_side_panel
import kotlinx.android.synthetic.main.activity_main.widgets_only_button
import kotlin.reflect.KClass

class MainActivity : AppCompatActivity() {

    data class PlayerInfo(val playerName: String, val cls: KClass<out Activity>)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val channelManager = (application as LiveLikeApplication).channelManager
        setContentView(R.layout.activity_main)

        val player = PlayerInfo("Exo Player", ExoPlayerActivity::class)
        val drawerDemoActivity = PlayerInfo("Exo Player", TwoSessionActivity::class)

        layout_side_panel.setOnClickListener {
            startActivity(playerDetailIntent(player))
        }

        layout_overlay.setOnClickListener {
            startActivity(playerDetailIntent(drawerDemoActivity))
        }

        events_button.setOnClickListener {
            channelManager.show(this)
        }

        channelManager.addChannelSelectListener("mainActivity") {
            channelManager.hide()
            events_label.text = it.name
        }

        widgets_only_button.setOnClickListener { startActivity(Intent(this, WidgetOnlyActivity::class.java)) }
        chat_only_button.setOnClickListener { startActivity(Intent(this, ChatOnlyActivity::class.java)) }
    }
}

fun Context.playerDetailIntent(player: MainActivity.PlayerInfo): Intent {
    return Intent(this, player.cls.java)
}
