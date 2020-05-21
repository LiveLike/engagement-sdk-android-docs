package com.livelike.livelikedemo

import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkInfo
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.chat.ChatRoom
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.livelikedemo.channel.ChannelManager
import kotlinx.android.synthetic.main.activity_main.btn_create
import kotlinx.android.synthetic.main.activity_main.chat_only_button
import kotlinx.android.synthetic.main.activity_main.chatroomText
import kotlinx.android.synthetic.main.activity_main.chk_show_dismiss
import kotlinx.android.synthetic.main.activity_main.events_button
import kotlinx.android.synthetic.main.activity_main.events_label
import kotlinx.android.synthetic.main.activity_main.layout_overlay
import kotlinx.android.synthetic.main.activity_main.layout_side_panel
import kotlinx.android.synthetic.main.activity_main.nicknameText
import kotlinx.android.synthetic.main.activity_main.private_group_button
import kotlinx.android.synthetic.main.activity_main.private_group_label
import kotlinx.android.synthetic.main.activity_main.progressBar
import kotlinx.android.synthetic.main.activity_main.textView2
import kotlinx.android.synthetic.main.activity_main.themes_button
import kotlinx.android.synthetic.main.activity_main.themes_json_button
import kotlinx.android.synthetic.main.activity_main.themes_json_label
import kotlinx.android.synthetic.main.activity_main.themes_label
import kotlinx.android.synthetic.main.activity_main.toggle_auto_keyboard_hide
import kotlinx.android.synthetic.main.activity_main.widgets_framework_button
import kotlinx.android.synthetic.main.activity_main.widgets_only_button
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import kotlin.reflect.KClass

class MainActivity : AppCompatActivity() {

    data class PlayerInfo(
        val playerName: String,
        val cls: KClass<out Activity>,
        var theme: Int,
        var keyboardClose: Boolean = true,
        var showNotification: Boolean = true,
        var jsonTheme: String? = null
    )

    private lateinit var channelManager: ChannelManager
    private val mConnReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val currentNetworkInfo =
                intent.getParcelableExtra<NetworkInfo>(ConnectivityManager.EXTRA_NETWORK_INFO)
            if (currentNetworkInfo.isConnected) {
//                (application as LiveLikeApplication).initSDK()
                channelManager.loadClientConfig()
            }
        }
    }
    private var chatRoomIds: MutableSet<String> = mutableSetOf()

    override fun onDestroy() {
        super.onDestroy()
        ExoPlayerActivity.privateGroupRoomId = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        channelManager = (application as LiveLikeApplication).channelManager

        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network?) {
                    super.onAvailable(network)
//                    (application as LiveLikeApplication).initSDK()
                    channelManager.loadClientConfig()
                }

                override fun onLost(network: Network?) {
                    super.onLost(network)
                }

                override fun onUnavailable() {
                    super.onUnavailable()
                }
            })
        } else {
            registerReceiver(mConnReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        }

        setContentView(R.layout.activity_main)

        val player = PlayerInfo(
            "Exo Player",
            ExoPlayerActivity::class,
            R.style.Default,
            true

        )

        val onlyWidget = PlayerInfo(
            "Widget Only",
            WidgetOnlyActivity::class,
            R.style.Default,
            true
        )
        val drawerDemoActivity =
            PlayerInfo("Exo Player", TwoSessionActivity::class, R.style.Default, false)

        layout_side_panel.setOnClickListener {
            startActivity(playerDetailIntent(player))
        }

        layout_overlay.setOnClickListener {
            startActivity(playerDetailIntent(drawerDemoActivity))
        }

        chk_show_dismiss.isChecked = player.showNotification
        chk_show_dismiss.setOnCheckedChangeListener { buttonView, isChecked ->
            player.showNotification = isChecked
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

        private_group_button.setOnClickListener {
            AlertDialog.Builder(this).apply {
                setTitle("Select a private group")
                setItems(chatRoomIds.toTypedArray()) { _, which ->
                    // On change of theme we need to create the session in order to pass new attribute of theme to widgets and chat
                    (application as LiveLikeApplication).removePrivateSession()
                    private_group_label.text = chatRoomIds.elementAt(which)
                    ExoPlayerActivity.privateGroupRoomId = chatRoomIds.elementAt(which)
                }
                create()
            }.show()
        }

        themes_button.setOnClickListener {
            val channels = arrayListOf("Default", "Turner", "Custom Chat Reaction")
            AlertDialog.Builder(this).apply {
                setTitle("Choose a theme!")
                setItems(channels.toTypedArray()) { _, which ->
                    // On change of theme we need to create the session in order to pass new attribute of theme to widgets and chat
                    (application as LiveLikeApplication).removePublicSession()
                    themes_label.text = channels[which]
                    EngagementSDK.enableDebug = false
                    player.theme = when (which) {
                        0 -> R.style.Default
                        1 -> {
                            EngagementSDK.enableDebug = true
                            R.style.TurnerChatTheme
                        }
                        2 -> {
                            EngagementSDK.enableDebug = false
                            R.style.CustomChatReactionTheme
                        }
                        else -> R.style.Default
                    }
                    onlyWidget.theme = when (which) {
                        0 -> R.style.Default
                        1 -> {
                            EngagementSDK.enableDebug = true
                            R.style.TurnerChatTheme
                        }
                        2 -> {
                            EngagementSDK.enableDebug = false
                            R.style.CustomChatReactionTheme
                        }
                        else -> R.style.Default
                    }
                }
                create()
            }.show()
        }

        themes_json_button.setOnClickListener {
            val files: ArrayList<String> =
                ArrayList(assets.list("themes")?.toList() ?: arrayListOf())
            files.add(0, "None")
            AlertDialog.Builder(this).apply {
                setTitle("Choose a theme!")
                setItems(files.toTypedArray()) { _, which ->
                    // On change of theme we need to create the session in order to pass new attribute of theme to widgets and chat
                    themes_json_label.text = files[which]
                    EngagementSDK.enableDebug = false
                    files.let {
                        when (which) {
                            0 -> {
                                player.jsonTheme = null
                            }
                            else -> {
                                val path = files[which]
                                val theme = getFileFromAsset(context, "themes/$path")
                                if (theme != null) {
                                    player.jsonTheme = theme
                                    onlyWidget.jsonTheme = theme
                                } else
                                    Toast.makeText(
                                        applicationContext,
                                        "Unable to get the theme json",
                                        Toast.LENGTH_LONG
                                    ).show()
                            }
                        }
                    }
                }
                create()
            }.show()
        }

        events_label.text = channelManager.selectedChannel.name

        getSharedPreferences(PREFERENCES_APP_ID, Context.MODE_PRIVATE).apply {
            getString("UserNickname", "")
                .let {
                    nicknameText.setText(it)
//                edit().putString("userPic","http://lorempixel.com/200/200/?$it").apply()
                }
            getString("userPic", "").let {
                if (it.isNullOrEmpty()) {
                    edit().putString(
                        "userPic",
                        "https://loremflickr.com/200/200?lock=${java.util.UUID.randomUUID()}"
                    ).apply()
                } else {
                    edit().putString("userPic", it).apply()
                }
            }
            chatRoomIds = getStringSet("chatRoomList", mutableSetOf())
        }

        btn_create.setOnClickListener {
            val title = chatroomText.text.toString()
            progressBar.visibility = View.VISIBLE
            (application as LiveLikeApplication).sdk.createChatRoom(
                title,
                object : LiveLikeCallback<ChatRoom>() {
                    override fun onResponse(result: ChatRoom?, error: String?) {
                        runOnUiThread {
                            textView2.text = when {
                                result != null -> "${result.title ?: "No Title"}(${result.id})"
                                else -> error
                            }
                            result?.let {
                                chatRoomIds.add(it.id)
                                getSharedPreferences(PREFERENCES_APP_ID, Context.MODE_PRIVATE)
                                    .edit().putStringSet("chatRoomList", chatRoomIds).apply()
                            }
                            progressBar.visibility = View.GONE
                        }
                    }
                })
        }

        toggle_auto_keyboard_hide.setOnCheckedChangeListener { buttonView, isChecked ->
            player.keyboardClose = isChecked
        }
        toggle_auto_keyboard_hide.isChecked = player.keyboardClose

        nicknameText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                getSharedPreferences(PREFERENCES_APP_ID, Context.MODE_PRIVATE).edit().apply {
                    putString("UserNickname", p0?.trim().toString())
                }.apply()
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }
        })

        widgets_framework_button.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    WidgetFrameworkTestActivity::class.java
                )
            )
        }

        widgets_only_button.setOnClickListener {
            startActivity(playerDetailIntent(onlyWidget))
        }
        chat_only_button.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    ChatOnlyActivity::class.java
                )
            )
        }
    }
}

fun Context.playerDetailIntent(player: MainActivity.PlayerInfo): Intent {
    val intent = Intent(this, player.cls.java)
    intent.putExtra("theme", player.theme)
    intent.putExtra("jsonTheme", player.jsonTheme)
    intent.putExtra("showNotification", player.showNotification)
    intent.putExtra(
        "keyboardClose", when (player.theme) {
            R.style.TurnerChatTheme -> player.keyboardClose
            else -> true
        }
    )
    return intent
}

fun getFileFromAsset(context: Context, path: String): String? {
    try {
        val asset = context.assets
        val br = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            BufferedReader(InputStreamReader(asset.open(path), StandardCharsets.UTF_8))
        } else {
            BufferedReader(InputStreamReader(asset.open(path)))
        }
        val sb = StringBuilder()
        var str: String?
        while (br.readLine().also { str = it } != null) {
            sb.append(str)
        }
        br.close()
        return sb.toString()
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}
