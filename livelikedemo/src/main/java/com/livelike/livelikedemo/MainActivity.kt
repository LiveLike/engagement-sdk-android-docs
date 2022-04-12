package com.livelike.livelikedemo

import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.graphics.Typeface
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.angads25.filepicker.controller.DialogSelectionListener
import com.google.gson.JsonParser
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.FontFamilyProvider
import com.livelike.engagementsdk.LiveLikeEngagementTheme
import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.chat.ChatRoomInfo
import com.livelike.engagementsdk.core.services.network.Result
import com.livelike.engagementsdk.core.utils.isNetworkConnected
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.publicapis.LiveLikeUserApi
import com.livelike.livelikedemo.LiveLikeApplication.Companion.CHAT_ROOM_LIST
import com.livelike.livelikedemo.LiveLikeApplication.Companion.PREFERENCES_APP_ID
import com.livelike.livelikedemo.utils.DialogUtils
import com.livelike.livelikedemo.utils.ThemeRandomizer
import kotlinx.android.synthetic.main.activity_main.*
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
        var customCheerMeter: Boolean = false,
        var showLink: Boolean = false,
        var customLink: String? = null,
        var allowDiscard: Boolean = true,
        var allowDefaultChatRoom: Boolean = true,
        var quoteMsg: Boolean=false

    ) {
    }

    private lateinit var userStream: Stream<LiveLikeUserApi>
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private val selectedEnvironment: String = "Selected App Environment"
    private val mConnReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (context.isNetworkConnected()) {
                (application as LiveLikeApplication).channelManager.loadClientConfig()
            }
        }
    }
    private var chatRoomIds: MutableSet<String> = mutableSetOf()

    override fun onDestroy() {
        ExoPlayerActivity.privateGroupRoomId = null

        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            cm.unregisterNetworkCallback(networkCallback!!)
        } else {
            unregisterReceiver(mConnReceiver)
        }
        userStream.unsubscribe(this)
        (application as LiveLikeApplication).removePublicSession()
        (application as LiveLikeApplication).removePrivateSession()
        super.onDestroy()
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

    private fun registerNetWorkCallback() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
//                    (application as LiveLikeApplication).initSDK()
                    (application as LiveLikeApplication).channelManager.loadClientConfig()
                }

                override fun onLost(network: Network) {
                    super.onLost(network)
                }

                override fun onUnavailable() {
                    super.onUnavailable()
                }
            }
            cm.registerDefaultNetworkCallback(networkCallback!!)
        } else {
            @Suppress("DEPRECATION") //kept due to pre M support
            registerReceiver(mConnReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        }
    }

    val player = PlayerInfo(
        "Exo Player",
        ExoPlayerActivity::class,
        R.style.Default,
        true,
    )

    val onlyWidget = PlayerInfo(
        "Widget Only",
        WidgetOnlyActivity::class,
        R.style.Default,
        true,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        (application as LiveLikeApplication).selectEnvironment("QA")
        env_label.text = "QA"
        events_label.text = "Select Channel"
        registerNetWorkCallback()
        sdk_version.text = "SDK Version : ${com.livelike.engagementsdk.BuildConfig.SDK_VERSION}"
        if (BuildConfig.VERSION_CODE > 1) {
            build_no.text = "Bitrise build : ${BuildConfig.VERSION_CODE}"
        }

        val drawerDemoActivity =
            PlayerInfo("Exo Player", TwoSessionActivity::class, R.style.Default, false)

        layout_side_panel.setOnClickListener {
            player.customLink = ed_link_custom.text.toString()
            player.quoteMsg = chk_enable_quote_msg.isChecked
            startActivity(playerDetailIntent(player))
        }

        env_button.setOnClickListener {
            AlertDialog.Builder(this).apply {
                setTitle("Select Environment")
                setItems(LiveLikeApplication.environmentMap.map { it.key }
                    .toTypedArray()) { _, which ->
                    (application as LiveLikeApplication).selectEnvironment(LiveLikeApplication.environmentMap.keys.toList()[which])
                    events_label.text = "Select Channel"
                    LiveLikeApplication.environmentMap.keys.toList()[which].let {
                        env_label.text = it
                        getSharedPreferences(PREFERENCES_APP_ID, Context.MODE_PRIVATE).apply {
                            edit().putString(selectedEnvironment, it).apply()
                        }
                    }
                }
                create()
            }.show()
        }

        layout_overlay.setOnClickListener {
            startActivity(playerDetailIntent(drawerDemoActivity))
        }

        // chk_show_dismiss.isChecked = player.showNotification
        chk_show_dismiss.isChecked = false
        chk_show_dismiss.setOnCheckedChangeListener { _, isChecked ->
            player.showNotification = isChecked
        }
        chk_show_avatar.isChecked = player.showAvatar
        chk_show_avatar.setOnCheckedChangeListener { _, isChecked ->
            player.showAvatar = isChecked
        }

        chk_custom_widgets_ui.setOnCheckedChangeListener { _, isChecked ->
            player.customCheerMeter = isChecked
            onlyWidget.customCheerMeter = isChecked
            LiveLikeApplication.showCustomWidgetsUI = isChecked
        }
        chk_allow_discard_own_msg.isChecked = true
        chk_allow_discard_own_msg.setOnCheckedChangeListener { btn, isChecked ->
            player.allowDiscard = isChecked
        }

        chk_enable_debug.isChecked = EngagementSDK.enableDebug
        chk_enable_debug.setOnCheckedChangeListener { _, isChecked ->
            EngagementSDK.enableDebug = isChecked
        }
        chk_show_links.setOnCheckedChangeListener { _, isChecked ->
            player.showLink = isChecked
        }
        chk_enable_quote_msg.setOnCheckedChangeListener { _, isChecked ->
            player.quoteMsg = isChecked
        }
        chk_allow_default_load_chat_room.isChecked = true
        chk_allow_default_load_chat_room.setOnCheckedChangeListener { _, isChecked ->
            player.allowDefaultChatRoom = isChecked
        }


        sample_app.setOnClickListener {
            startActivity(Intent(this, SampleAppActivity::class.java))
        }

        events_button.setOnClickListener {
            (application as LiveLikeApplication).channelManager.let { channelManager ->
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
                        ) { dialog, _ ->
                            channelManager.loadClientConfig(channelManager.nextUrl)
                            dialog.dismiss()
                        }
                    if (channelManager.previousUrl?.isNotEmpty() == true)
                        setNeutralButton(
                            "Load Previous"
                        ) { dialog, _ ->
                            channelManager.loadClientConfig(channelManager.previousUrl)
                            dialog.dismiss()
                        }
                    create()
                }.show()
            }
        }

        live_blog.setOnClickListener {
            startActivity(Intent(this, LiveBlogActivity::class.java))
        }

        timeline_two.setOnClickListener {
            startActivity(Intent(this, IntractableTimelineActivity::class.java))
        }

        leaderboard_button.setOnClickListener {
            startActivity(Intent(this, LeaderBoardActivity::class.java))
        }

        unclaimed_interaction.setOnClickListener {
            startActivity(Intent(this, UnclaimedInteractionActivity::class.java))
        }

        widgets_json_button.setOnClickListener {
            startActivity(Intent(this, WidgetJsonActivity::class.java))
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
            val channels = arrayListOf("Default", "Turner", "Custom Chat Reaction", "None")
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
                        else -> R.style.AppTheme
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
                        else -> R.style.AppTheme
                    }
                }
                create()
            }.show()
        }

        themes_json_button.setOnClickListener {
            DialogUtils.showFilePicker(
                this,
                DialogSelectionListener { files ->
                    setupJsonThemesFilePath(files)
                }
            )
        }

        events_label.text = (application as LiveLikeApplication).channelManager.selectedChannel.name

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
            getString(selectedEnvironment, null)?.let {
                (application as LiveLikeApplication).selectEnvironment(it)
                events_label.text = "Select Channel"
                env_label.text = it
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
                }
            )
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

        toggle_auto_keyboard_hide.setOnCheckedChangeListener { _, isChecked ->
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
        userStream = (application as LiveLikeApplication).sdk.userStream
        userStream.subscribe(this.hashCode()) {
            runOnUiThread {
                txt_nickname_server.text = it?.nickname
                it?.let {
                    Toast.makeText(
                        applicationContext,
                        "CustomData: ${it.custom_data}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        btn_user_details.setOnClickListener {
            (application as LiveLikeApplication).sdk.getCurrentUserDetails(object :
                LiveLikeCallback<LiveLikeUserApi>() {
                override fun onResponse(result: LiveLikeUserApi?, error: String?) {
                    result?.let {
                        Toast.makeText(
                            applicationContext,
                            "API CustomData: ${it.custom_data}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    error?.let {
                        Toast.makeText(
                            applicationContext,
                            "$it",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            })
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
            if ((application as LiveLikeApplication).channelManager.getChannels().isNotEmpty())
                startActivity(Intent(this, CustomChatActivity::class.java))
            else
                Toast.makeText(this, "Please wait for events loading", Toast.LENGTH_SHORT).show()
        }
        sponsor_test.setOnClickListener {
            startActivity(Intent(this, SponsorTestActivity::class.java))
        }
        badges_collection.setOnClickListener {
            startActivity(Intent(this, BadgesCollectionActivity::class.java))
        }

        rewards_client_test.setOnClickListener {
            startActivity(Intent(this, RewardsClientTestActivity::class.java))
        }

        get_widget_filter.setOnClickListener {
            startActivity(Intent(this, GetWidgetTestActivity::class.java))
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
    var line: String?
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
    intent.putExtra("showNotification", false)
    intent.putExtra("avatarUrl", player.avatarUrl)
    intent.putExtra("showAvatar", player.showAvatar)
    intent.putExtra("customCheerMeter", player.customCheerMeter)
    intent.putExtra("showLink", player.showLink)
    intent.putExtra("customLink", player.customLink)
    intent.putExtra("enableReplies", player.quoteMsg)
    intent.putExtra("allowDiscard", player.allowDiscard)
    intent.putExtra("allowDefaultLoadChatRoom", player.allowDefaultChatRoom)
    intent.putExtra(
        "keyboardClose",
        when (player.theme) {
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
