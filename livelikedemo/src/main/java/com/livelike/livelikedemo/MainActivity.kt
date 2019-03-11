package com.livelike.livelikedemo

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

import android.support.design.widget.BottomSheetDialog
import android.support.v7.view.ContextThemeWrapper
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout

import kotlinx.android.synthetic.main.activity_main.*
import kotlin.reflect.KClass

import com.livelike.livelikedemo.channel.ChannelSelectionView
import com.livelike.livelikedemo.video.Channel
import com.livelike.livelikesdk.util.logDebug


const val USE_DRAWER_LAYOUT = "use_drawer"

class MainActivity : AppCompatActivity() {

    data class PlayerInfo(val playerName: String, val cls: KClass<out Activity>)
    lateinit var  application : LiveLikeApplication
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        application = getApplication() as LiveLikeApplication
        setContentView(R.layout.activity_main)

        val player = PlayerInfo("Exo Player", ExoPlayerActivity::class);

        layout_overlay.setOnClickListener {
            startActivity(playerDetailIntent(player))
        }

        layout_side_panel.setOnClickListener {
            startActivity(playerDetailIntent(player, true))
        }

        events_button.setOnClickListener {
            val mBottomSheetDialog = BottomSheetDialog(this)
            val sheetView = ChannelSelectionView(this)//this.layoutInflater.inflate(R.layout.channel_select_bottom, null) as LinearLayout
            sheetView.channelList = application.channelManager.channelList
            sheetView.channelSelectListener = { channel ->
                logDebug { "Channel Select " + channel.name }
            }
            mBottomSheetDialog.setContentView(sheetView)
            mBottomSheetDialog.show()
        }
    }
}

fun Context.playerDetailIntent(player: MainActivity.PlayerInfo, useDrawer : Boolean = false): Intent {
    return Intent(this, player.cls.java).apply {
        putExtra(USE_DRAWER_LAYOUT, useDrawer )
    }
}
