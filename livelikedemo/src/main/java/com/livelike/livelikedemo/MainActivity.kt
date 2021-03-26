package com.livelike.livelikedemo

import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Typeface
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkInfo
import android.os.Build
import android.os.Bundle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.appcompat.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import com.github.angads25.filepicker.controller.DialogSelectionListener
import com.google.gson.JsonParser
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.FontFamilyProvider
import com.livelike.engagementsdk.LiveLikeEngagementTheme
import com.livelike.engagementsdk.chat.ChatRoomInfo
import com.livelike.engagementsdk.core.services.network.Result
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.livelikedemo.channel.ChannelManager
import com.livelike.livelikedemo.utils.DialogUtils
import com.livelike.livelikedemo.utils.ThemeRandomizer
import kotlinx.android.synthetic.main.activity_main.btn_avatar
import kotlinx.android.synthetic.main.activity_main.btn_avatar_remove
import kotlinx.android.synthetic.main.activity_main.btn_create
import kotlinx.android.synthetic.main.activity_main.btn_join
import kotlinx.android.synthetic.main.activity_main.btn_nick_name
import kotlinx.android.synthetic.main.activity_main.build_no
import kotlinx.android.synthetic.main.activity_main.chat_input_visibility_switch
import kotlinx.android.synthetic.main.activity_main.chat_only_button
import kotlinx.android.synthetic.main.activity_main.chatroomText
import kotlinx.android.synthetic.main.activity_main.chatroomText1
import kotlinx.android.synthetic.main.activity_main.chk_custom_widgets_ui
import kotlinx.android.synthetic.main.activity_main.chk_show_avatar
import kotlinx.android.synthetic.main.activity_main.chk_show_dismiss
import kotlinx.android.synthetic.main.activity_main.custom_chat
import kotlinx.android.synthetic.main.activity_main.ed_avatar
import kotlinx.android.synthetic.main.activity_main.events_button
import kotlinx.android.synthetic.main.activity_main.events_label
import kotlinx.android.synthetic.main.activity_main.layout_overlay
import kotlinx.android.synthetic.main.activity_main.layout_side_panel
import kotlinx.android.synthetic.main.activity_main.leaderboard_button
import kotlinx.android.synthetic.main.activity_main.leaderboard_rank
import kotlinx.android.synthetic.main.activity_main.live_blog
import kotlinx.android.synthetic.main.activity_main.nicknameText
import kotlinx.android.synthetic.main.activity_main.private_group_button
import kotlinx.android.synthetic.main.activity_main.private_group_label
import kotlinx.android.synthetic.main.activity_main.progressBar
import kotlinx.android.synthetic.main.activity_main.sample_app
import kotlinx.android.synthetic.main.activity_main.sdk_version
import kotlinx.android.synthetic.main.activity_main.textView2
import kotlinx.android.synthetic.main.activity_main.themes_button
import kotlinx.android.synthetic.main.activity_main.themes_json_button
import kotlinx.android.synthetic.main.activity_main.themes_json_label
import kotlinx.android.synthetic.main.activity_main.themes_label
import kotlinx.android.synthetic.main.activity_main.toggle_auto_keyboard_hide
import kotlinx.android.synthetic.main.activity_main.txt_nickname_server
import kotlinx.android.synthetic.main.activity_main.view_pager_sample
import kotlinx.android.synthetic.main.activity_main.widget_viewModel
import kotlinx.android.synthetic.main.activity_main.widgets_framework_button
import kotlinx.android.synthetic.main.activity_main.widgets_only_button
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStream
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
        var jsonTheme: String? = null,
        var avatarUrl: String? = null,
        var showAvatar: Boolean = true,
        var customCheerMeter: Boolean = false
    )

    private lateinit var channelManager: ChannelManager
    private val mConnReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val currentNetworkInfo =
                intent.getParcelableExtra<NetworkInfo>(ConnectivityManager.EXTRA_NETWORK_INFO)
            if (currentNetworkInfo!!.isConnected) {
//                (application as LiveLikeApplication).initSDK()
                channelManager.loadClientConfig()
            }
        }
    }
    private var chatRoomIds: MutableSet<String> = mutableSetOf()

    override fun onDestroy() {
        super.onDestroy()
        ExoPlayerActivity.privateGroupRoomId = null
        LocalBroadcastManager.getInstance(this)
            .unregisterReceiver(mConnReceiver)
    }

    override fun onBackPressed() {
        if (this.isTaskRoot) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                this.finishAfterTransition()
            }
        } else {
            super.onBackPressed()
        }
    }

    fun registerNetWorkCallback() {
        channelManager = (application as LiveLikeApplication).channelManager
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
//                    (application as LiveLikeApplication).initSDK()
                    channelManager.loadClientConfig()
                }

                override fun onLost(network: Network) {
                    super.onLost(network)
                }

                override fun onUnavailable() {
                    super.onUnavailable()
                }
            })
        } else {
            registerReceiver(mConnReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        }
    }

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        registerNetWorkCallback()
        sdk_version.text = "SDK Version : ${com.livelike.engagementsdk.BuildConfig.VERSION_NAME}"
        if (BuildConfig.VERSION_CODE > 1) {
            build_no.text = "Bitrise build : ${BuildConfig.VERSION_CODE}"
        }

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
        chk_show_avatar.isChecked = player.showAvatar
        chk_show_avatar.setOnCheckedChangeListener { buttonView, isChecked ->
            player.showAvatar = isChecked;
        }

        chk_custom_widgets_ui.setOnCheckedChangeListener { buttonView, isChecked ->
            player.customCheerMeter = isChecked
            onlyWidget.customCheerMeter = isChecked

            LiveLikeApplication.showCustomWidgetsUI = isChecked
        }
        sample_app.setOnClickListener {
            startActivity(Intent(this, SampleAppActivity::class.java))
        }

        events_button.setOnClickListener {
            val channels = channelManager.getChannels()
            AlertDialog.Builder(this).apply {
                setTitle("Choose a channel to watch!")
                setItems(channels.map { it.name }.toTypedArray()) { _, which ->
                    channelManager.selectedChannel = channels[which]
                    events_label.text = channelManager.selectedChannel.name
                }
                if (channelManager.nextUrl?.isNotEmpty() == true)
                    setPositiveButton(
                        "Load Next"
                    ) { dialog, which ->
                        channelManager.loadClientConfig(channelManager.nextUrl)
                        dialog.dismiss()
                    }
                if (channelManager.previousUrl?.isNotEmpty() == true)
                    setNeutralButton(
                        "Load Previous"
                    ) { dialog, which ->
                        channelManager.loadClientConfig(channelManager.previousUrl)
                        dialog.dismiss()
                    }
                create()
            }.show()
        }

        live_blog.setOnClickListener {
            startActivity(Intent(this, LiveBlogActivity::class.java))
        }

        leaderboard_button.setOnClickListener {
            startActivity(Intent(this, LeaderBoardActivity::class.java))
        }
        private_group_button.setOnClickListener {
            AlertDialog.Builder(this).apply {
                setTitle("Select a private group")
                setItems(chatRoomIds.toTypedArray()) { _, which ->
                    // On change of theme we need to create the session in order to pass new attribute of theme to widgets and chat
                    (application as LiveLikeApplication).removePrivateSession()
                    private_group_label.text = chatRoomIds.elementAt(which)
                    ExoPlayerActivity.privateGroupRoomId = chatRoomIds.elementAt(which)

                    // Copy to clipboard
                    var clipboard: ClipboardManager =
                        getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    var clip = ClipData.newPlainText("label", chatRoomIds.elementAt(which))
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(
                        applicationContext,
                        "Room Id Copy To Clipboard",
                        Toast.LENGTH_LONG
                    )
                        .show()
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
                            EngagementSDK.enableDebug = false
                            R.style.MMLChatTheme
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
                            EngagementSDK.enableDebug = false
                            R.style.MMLChatTheme
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
            DialogUtils.showFilePicker(this,
                DialogSelectionListener { files ->
                    setupJsonThemesFilePath(files)
                })
        }

        events_label.text = channelManager.selectedChannel.name

        getSharedPreferences(PREFERENCES_APP_ID, Context.MODE_PRIVATE).apply {
            getString("UserNickname", "")
                .let {
                    nicknameText.setText(it)
//                edit().putString("userPic","http://lorempixel.com/200/200/?$it").commit()
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

            chatRoomIds = getStringSet(CHAT_ROOM_LIST, mutableSetOf()) ?: mutableSetOf()
        }

        view_pager_sample.setOnClickListener {
            startActivity(Intent(this, MainActivity2::class.java))
        }

        btn_create.setOnClickListener {
            val title = chatroomText.text.toString()
            progressBar.visibility = View.VISIBLE
            (application as LiveLikeApplication).sdk.createChatRoom(
                title,
                null,
                object : LiveLikeCallback<ChatRoomInfo>() {
                    override fun onResponse(result: ChatRoomInfo?, error: String?) {
                        textView2.text = when {
                            result != null -> "${result.title ?: "No Title"}(${result.id})"
                            else -> error
                        }
                        result?.let {
                            chatRoomIds.add(it.id)
                            getSharedPreferences(PREFERENCES_APP_ID, Context.MODE_PRIVATE)
                                .edit().apply {
                                    putStringSet(CHAT_ROOM_LIST, chatRoomIds).apply()
                                }
                        }
                        progressBar.visibility = View.GONE
                    }
                })
        }

        btn_join.setOnClickListener {
            val chatRoomId = chatroomText1.text.toString()
            if (chatRoomId.isEmpty().not()) {
                chatRoomIds.add(chatRoomId)
                getSharedPreferences(PREFERENCES_APP_ID, Context.MODE_PRIVATE)
                    .edit().putStringSet(CHAT_ROOM_LIST, chatRoomIds).apply()
                chatroomText1.setText("")
            }
        }

        toggle_auto_keyboard_hide.setOnCheckedChangeListener { buttonView, isChecked ->
            player.keyboardClose = isChecked
        }
        toggle_auto_keyboard_hide.isChecked = player.keyboardClose

        chat_input_visibility_switch.setOnCheckedChangeListener { _, isChecked ->
            ExoPlayerActivity.isHideChatInput = isChecked
        }

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
        (application as LiveLikeApplication).sdk.userStream.subscribe(this) {
            runOnUiThread {
                txt_nickname_server.text = it?.nickname
            }
        }
        btn_nick_name.setOnClickListener {
            if (nicknameText.text.toString().isEmpty().not())
                (application as LiveLikeApplication).sdk.updateChatNickname(nicknameText.text.toString())
        }

        widgets_framework_button.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    WidgetFrameworkTestActivity::class.java
                )
            )
        }

        widget_viewModel.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    WidgetActivity::class.java
                )
            )
        }

        leaderboard_rank.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    LeaderBoardPositionActitivty::class.java
                )
            )
        }

        ed_avatar.setText("https://encrypted-tbn0.gstatic.com/images?q=tbn%3AANd9GcSmyhNs7yHwEYgBHE0MNK3H5YeDbCcf3BDF9A&usqp=CAU")

        btn_avatar.setOnClickListener {
            val url = ed_avatar.text.toString()
            player.avatarUrl = url
        }

        btn_avatar_remove.setOnClickListener {
            player.avatarUrl = null
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
        custom_chat.setOnClickListener {
            startActivity(Intent(this, CustomChatActivity::class.java))
        }

        (application as LiveLikeApplication).removePublicSession()
        (application as LiveLikeApplication).removePrivateSession()
    }

    fun setupJsonThemesFilePath(files: Array<out String>?) {
        if (files?.isNotEmpty() == true) {
            ThemeRandomizer.themesList.clear()
            themes_json_label.post {
                themes_json_label.text = "${files.size} selected"
            }
        } else {
            themes_json_label.post {
                themes_json_label.text = "None"
            }
        }
        files?.forEach { file ->
            val fin = FileInputStream(file)
            val theme: String? = convertStreamToString(fin)
            // Make sure you close all streams.
            fin.close()
            val element =
                LiveLikeEngagementTheme.instanceFrom(JsonParser.parseString(theme).asJsonObject)
            if (element is Result.Success) {
                element.data.fontFamilyProvider = object : FontFamilyProvider {
                    override fun getTypeFace(fontFamilyName: String): Typeface? {
                        if (fontFamilyName.contains("Pangolin"))
                            return Typeface.createFromAsset(
                                resources.assets,
                                "fonts/Pangolin-Regular.ttf"
                            )
                        else if (fontFamilyName.contains("Raleway")) {
                            return Typeface.createFromAsset(
                                resources.assets,
                                "fonts/Raleway-Regular.ttf"
                            )
                        }
                        return null
                    }
                }
                ThemeRandomizer.themesList.add(element.data)
            }
            if (theme != null) {
                player.jsonTheme = theme
                onlyWidget.jsonTheme = theme
            } else
                themes_json_label.post {
                    Toast.makeText(
                        applicationContext,
                        "Unable to get the theme json",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }
}

@Throws(java.lang.Exception::class)
fun convertStreamToString(`is`: InputStream?): String? {
    val reader = BufferedReader(InputStreamReader(`is`))
    val sb = java.lang.StringBuilder()
    var line: String? = null
    while (reader.readLine().also { line = it } != null) {
        sb.append(line).append("\n")
    }
    reader.close()
    return sb.toString()
}

fun Context.playerDetailIntent(player: MainActivity.PlayerInfo): Intent {
    val intent = Intent(this, player.cls.java)
    intent.putExtra("theme", player.theme)
    intent.putExtra("jsonTheme", player.jsonTheme)
    intent.putExtra("showNotification", player.showNotification)
    intent.putExtra("avatarUrl", player.avatarUrl)
    intent.putExtra("showAvatar", player.showAvatar)
    intent.putExtra("customCheerMeter", player.customCheerMeter)
    intent.putExtra(
        "keyboardClose", when (player.theme) {
            R.style.MMLChatTheme -> player.keyboardClose
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
