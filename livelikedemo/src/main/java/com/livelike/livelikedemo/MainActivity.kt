package com.livelike.livelikedemo

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.reflect.KClass

const val USE_DRAWER_LAYOUT = "use_drawer"

class MainActivity : AppCompatActivity() {

    data class PlayerInfo(val playerName: String, val cls: KClass<out Activity>)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
/*
        //Add player activities by adding them to the list below.
        val  players = listOf(
            PlayerInfo("Exo Player", ExoPlayerActivity::class)
            //PlayerInfo("notExoPlauyer", ExoPlayerActivity::class)
        )

        players.forEach { createPlayerButton(it) }
        */
    }
/*
    fun createPlayerButton(player : PlayerInfo) {
        val pButton = Button(this)
        pButton.layoutParams = LinearLayout.LayoutParams(400, 200)
        pButton.text = player.playerName
        pButton.setOnClickListener {
            startActivity(PlayerDetailIntent(player))
        }
        main_layout.addView(pButton ,0)
    }
}

fun Context.PlayerDetailIntent(player: MainActivity.PlayerInfo, useDrawer : Boolean): Intent {
    return Intent(this, player.cls.java).apply {
        putExtra(USE_DRAWER_LAYOUT, useDrawer )
    }
}*/
