package com.livelike.livelikedemo

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import kotlin.reflect.KClass
import kotlinx.android.synthetic.main.activity_main.chat_only_button
import kotlinx.android.synthetic.main.activity_main.events_button
import kotlinx.android.synthetic.main.activity_main.events_label
import kotlinx.android.synthetic.main.activity_main.layout_overlay
import kotlinx.android.synthetic.main.activity_main.layout_side_panel
import kotlinx.android.synthetic.main.activity_main.nicknameText
import kotlinx.android.synthetic.main.activity_main.themes_button
import kotlinx.android.synthetic.main.activity_main.themes_label
import kotlinx.android.synthetic.main.activity_main.widgets_only_button

class MainActivity : AppCompatActivity() {

    data class PlayerInfo(val playerName: String, val cls: KClass<out Activity>,var theme:Int)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val channelManager = (application as LiveLikeApplication).channelManager
        setContentView(R.layout.activity_main)

        val player = PlayerInfo("Exo Player", ExoPlayerActivity::class,R.style.AppTheme_NoActionBar)
        val drawerDemoActivity = PlayerInfo("Exo Player", TwoSessionActivity::class,R.style.AppTheme_NoActionBar)

        layout_side_panel.setOnClickListener {
            startActivity(playerDetailIntent(player))
        }

        layout_overlay.setOnClickListener {
            startActivity(playerDetailIntent(drawerDemoActivity))
        }

        events_button.setOnClickListener {
            val channels = channelManager.getChannels()
            AlertDialog.Builder(this).apply {
                setTitle("Choose a channel to watch!")
                setItems(channels.map { it.name }.toTypedArray()) { _, which ->
                    channelManager.selectedChannel = channels[which]
                    events_label.text = channelManager.selectedChannel.name
                }
                create()
            }.show()
        }
        themes_button.setOnClickListener {
            val channels = arrayListOf("Default","Turner")
            AlertDialog.Builder(this).apply {
                setTitle("Choose a theme!")
                setItems(channels.toTypedArray()) { _, which ->
                    themes_label.text = channels[which]
                    player.theme=when(which){
                        0-> R.style.AppTheme_NoActionBar
                        1-> R.style.TurnerChatTheme
                        else -> R.style.AppTheme_NoActionBar
                    }
                }
                create()
            }.show()
        }
        events_label.text = channelManager.selectedChannel.name

        getSharedPreferences("test-app", Context.MODE_PRIVATE).apply {
            getString("UserNickname", "")
            .let {
                nicknameText.setText(it)
                edit().putString("userPic","http://lorempixel.com/200/200/?$it").apply()
            }
            getString("userPic","").let {
                if(it.isNullOrEmpty()){
                    edit().putString("userPic","http://lorempixel.com/200/200/?${java.util.UUID.randomUUID()}").apply()
                } else {
                    edit().putString("userPic","http://lorempixel.com/200/200/?$it").apply()
                }
            }
        }




        nicknameText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                getSharedPreferences("test-app", Context.MODE_PRIVATE).edit().apply {
                    putString("UserNickname", p0?.trim().toString())
                }.apply()
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }
        })

        widgets_only_button.setOnClickListener { startActivity(Intent(this, WidgetOnlyActivity::class.java)) }
        chat_only_button.setOnClickListener { startActivity(Intent(this, ChatOnlyActivity::class.java)) }
    }
}

fun Context.playerDetailIntent(player: MainActivity.PlayerInfo): Intent {
    val intent= Intent(this, player.cls.java)
    intent.putExtra("theme",player.theme)
    return intent
}
