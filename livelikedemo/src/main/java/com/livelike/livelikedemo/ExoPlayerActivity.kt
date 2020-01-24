package com.livelike.livelikedemo

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.support.constraint.Constraints
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.livelike.engagementsdk.LiveLikeContentSession
import com.livelike.engagementsdk.MessageListener
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.publicapis.LiveLikeChatMessage
import com.livelike.engagementsdk.services.messaging.proxies.WidgetInterceptor
import com.livelike.engagementsdk.utils.registerLogsHandler
import com.livelike.livelikedemo.channel.Channel
import com.livelike.livelikedemo.channel.ChannelManager
import com.livelike.livelikedemo.video.PlayerState
import com.livelike.livelikedemo.video.VideoPlayer
import java.util.Calendar
import java.util.Date
import java.util.Timer
import java.util.TimerTask
import kotlin.math.abs
import kotlinx.android.synthetic.main.activity_exo_player.chat_room_button
import kotlinx.android.synthetic.main.activity_exo_player.fullLogs
import kotlinx.android.synthetic.main.activity_exo_player.logsPreview
import kotlinx.android.synthetic.main.activity_exo_player.openLogs
import kotlinx.android.synthetic.main.activity_exo_player.playerView
import kotlinx.android.synthetic.main.activity_exo_player.selectChannelButton
import kotlinx.android.synthetic.main.activity_exo_player.startAd
import kotlinx.android.synthetic.main.activity_exo_player.videoTimestamp
import kotlinx.android.synthetic.main.widget_chat_stacked.chat_view
import kotlinx.android.synthetic.main.widget_chat_stacked.widget_view

class ExoPlayerActivity : AppCompatActivity() {
    companion object {
        const val AD_STATE = "adstate"
        const val SHOWING_DIALOG = "showingDialog"
        const val POSITION = "position"
        const val CHANNEL_NAME = "channelName"
    }

    private var showNotification: Boolean = true
    private var themeCurrent: Int? = null
    private var isChatRoomJoined: Boolean = false
    private lateinit var player: VideoPlayer
    private var session: LiveLikeContentSession? = null
    private var privateGroupChatsession: LiveLikeContentSession? = null
    private var startingState: PlayerState? = null
    private var channelManager: ChannelManager? = null

    private var adsPlaying = false
        set(adsPlaying) {
            field = adsPlaying

            if (adsPlaying) {
                startAd.text = "Stop Ads"
                player.stop()
                session?.pause()
                privateGroupChatsession?.pause()
            } else {
                startAd.text = "Start Ads"
                player.start()
                session?.resume()
                privateGroupChatsession?.resume()
            }
        }
    private val timer = Timer()
    private var chatRoomIds: List<String> = if (BuildConfig.DEBUG) {
        listOf("4d5ecf8d-3012-4ca2-8a56-4b8470c1ec8b", "e50ee571-7679-4efd-ad0b-e5fa00e38384")
    } else {
        listOf("dba595c6-afab-4f73-b22f-c7c0cb317ca9", "f05ee348-b8e5-4107-8019-c66fad7054a8")
    }
    private lateinit var chatRoomLastTimeStampMap: MutableMap<String, Long>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chatRoomLastTimeStampMap = GsonBuilder().create().fromJson(
            getSharedPreferences(PREFERENCES_APP_ID, Context.MODE_PRIVATE).getString(
                PREF_CHAT_ROOM_LAST_TIME,
                null
            ),
            object : TypeToken<MutableMap<String, Long>>() {}.type
        ) ?: mutableMapOf()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        themeCurrent = intent.getIntExtra("theme", R.style.AppTheme)
        this.setTheme(themeCurrent!!)
        setContentView(R.layout.activity_exo_player)
        testKeyboardDismissUseCase(themeCurrent!!)
        playerView.layoutParams.width = Constraints.LayoutParams.MATCH_PARENT

        player = (application as LiveLikeApplication).createPlayer(playerView)
        channelManager = (application as LiveLikeApplication).channelManager
        openLogs.setOnClickListener {
            fullLogs.visibility = if (fullLogs.visibility == View.GONE) View.VISIBLE else View.GONE
        }
        fullLogs.movementMethod = ScrollingMovementMethod()

        showNotification = intent.getBooleanExtra("showNotification", true)

        adsPlaying = savedInstanceState?.getBoolean(AD_STATE) ?: false
        val position = savedInstanceState?.getLong(POSITION) ?: 0
        startingState = PlayerState(0, position, !adsPlaying)

        showingDialog = savedInstanceState?.getBoolean(SHOWING_DIALOG) ?: false
        if (showingDialog) {
            dialog.showDialog(this@ExoPlayerActivity)
        }

        timer.schedule(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    val pdtTime = player.getPDT()
                    videoTimestamp?.text = Date(pdtTime).toString()
                }
            }
        }, 0, 100)

        setUpAdClickListeners()

        channelManager?.let {
            selectChannel(it.selectedChannel)
        }

        selectChannelButton.setOnClickListener {
            channelManager?.let { cm ->
                val channels = cm.getChannels()
                AlertDialog.Builder(this).apply {
                    setTitle("Choose a channel to watch!")
                    setItems(channels.map { it.name }.toTypedArray()) { _, which ->
                        cm.selectedChannel = channels[which]
                        selectChannel(channels[which])
                    }
                    create()
                }.show()
            }
        }

        chat_room_button.setOnClickListener {

            AlertDialog.Builder(this).apply {
                setTitle("Choose a custom Chat Room to join")
                setItems(chatRoomIds.map {
                    "$it[${messageCount[it] ?: 0}]"
                }.toTypedArray()) { _, which ->
                    val enteredChatRoomId = chatRoomIds[which]
                    privateGroupChatsession?.enterChatRoom(enteredChatRoomId)
                    chatRoomLastTimeStampMap[enteredChatRoomId] =
                        Calendar.getInstance().timeInMillis
                    getSharedPreferences(PREFERENCES_APP_ID, Context.MODE_PRIVATE).edit().putString(
                        PREF_CHAT_ROOM_LAST_TIME,
                        GsonBuilder().create().toJson(chatRoomLastTimeStampMap)
                    ).apply()
                    if (!isChatRoomJoined) {
                        val anotherChatRoomId = chatRoomIds[abs(which - 1)]
                        privateGroupChatsession?.joinChatRoom(anotherChatRoomId)
                        isChatRoomJoined = true
                    }
                    chat_view.setSession(privateGroupChatsession!!)
                }
                create()
            }.show()
        }
    }

    private fun testKeyboardDismissUseCase(themeCurrent: Int) {
        if (themeCurrent == R.style.TurnerChatTheme) {
            chat_view.sentMessageListener = {
                chat_view.dismissKeyboard()
            }
        }
    }

    private fun setUpAdClickListeners() {
        startAd.setOnClickListener {
            adsPlaying = !adsPlaying
        }
    }

    private fun selectChannel(channel: Channel) {
        player.stop()
        player.release()
        startingState = PlayerState(0, 0, !adsPlaying)
        initializeLiveLikeSDK(channel)
    }

    private val dialog = object : WidgetInterceptor() {
        override fun widgetWantsToShow() {
            showDialog(this@ExoPlayerActivity)
        }
    }

    private var showingDialog = false

    private fun WidgetInterceptor.showDialog(context: Context) {
        showingDialog = true
        AlertDialog.Builder(context).apply {
            setMessage("You received a Widget, what do you want to do?")
            setPositiveButton("Show") { _, _ ->
                showingDialog = false
                showWidget()
            }
            setNegativeButton("Dismiss") { _, _ ->
                showingDialog = false
                dismissWidget()
            }
            setCancelable(false)
            create()
        }.show()
    }

    var messageCount: MutableMap<String, Long> = mutableMapOf()

    private fun initializeLiveLikeSDK(channel: Channel) {
        registerLogsHandler(object : (String) -> Unit {
            override fun invoke(text: String) {
                Handler(mainLooper).post {
                    logsPreview.text = "$text \n\n ${logsPreview.text}"
                    fullLogs.text = "$text \n\n ${fullLogs.text}"
                }
            }
        })

        if (channel != ChannelManager.NONE_CHANNEL) {
            if (privateGroupChatsession == null) {
                privateGroupChatsession =
                    (application as LiveLikeApplication).sdk.createContentSession(channel.llProgram.toString(), (application as LiveLikeApplication).timecodeGetter)
                for (pair in chatRoomLastTimeStampMap) {
                    val chatRoomId = pair.key
                    val timestamp = ((chatRoomLastTimeStampMap[chatRoomId]
                        ?: Calendar.getInstance().timeInMillis)) + 100
                    //Adding this millisecond so while retrieving count from the pubnub it will not include the last timestamp message,also some extra time while saving data on pubnub servers
                    privateGroupChatsession?.getMessageCount(
                        chatRoomId,
                        timestamp,
                        object :
                            LiveLikeCallback<Long>() {
                            override fun onResponse(result: Long?, error: String?) {
                                result?.let {
                                    messageCount[chatRoomId] =
                                        (messageCount[chatRoomId] ?: 0) + result
                                }
                            }
                        })
                    privateGroupChatsession?.joinChatRoom(chatRoomId)
                }
            }
            chatRoomIds.forEach {
                privateGroupChatsession?.joinChatRoom(it)
            }
            privateGroupChatsession?.setMessageListener(object : MessageListener {
                override fun onNewMessage(chatRoom: String, message: LiveLikeChatMessage) {
                    if (chatRoom == privateGroupChatsession?.getActiveChatRoom?.invoke()) {
                        messageCount[chatRoom] = 0 // reset unread message count
                        chatRoomLastTimeStampMap[chatRoom] = Calendar.getInstance().timeInMillis
                        getSharedPreferences(PREFERENCES_APP_ID, Context.MODE_PRIVATE).edit()
                            .putString(
                                PREF_CHAT_ROOM_LAST_TIME,
                                GsonBuilder().create().toJson(chatRoomLastTimeStampMap)
                            ).apply()
                    } else {
                        if (messageCount[chatRoom] == null) {
                            messageCount[chatRoom] = 1
                        } else {
                            messageCount[chatRoom] = (messageCount[chatRoom] ?: 0) + 1
                        }
                    }
                    messageCount.forEach {
                        logsPreview.text =
                            "channel : ${it.key}, unread : ${it.value} \n\n ${logsPreview.text}"
                        fullLogs.text =
                            "channel : ${it.key}, unread : ${it.value} \n\n ${fullLogs.text}"
                        Log.v("Here", "channel : ${it.key}, unread : ${it.value}")
                    }
                }
            })

            if (themeCurrent == R.style.TurnerChatTheme) {
                val emptyView =
                    LayoutInflater.from(this).inflate(R.layout.empty_chat_data_view, null)
                chat_view.emptyChatBackgroundView = emptyView
                chat_view.allowMediaFromKeyboard = false
            }
            val session = (application as LiveLikeApplication).createSession(
                channel.llProgram.toString(),
                when (showNotification) {
                    true -> dialog
                    else -> null
                }
            )
            chat_view.setSession(session)
            widget_view.setSession(session)
            getSharedPreferences(PREFERENCES_APP_ID, Context.MODE_PRIVATE).apply {
                getString("UserNickname", "").let {
                    if (!it.isNullOrEmpty()) {
                        (application as LiveLikeApplication).sdk.updateChatNickname(it)
                    }
                }
                getString("userPic", null).let {
                    (application as LiveLikeApplication).sdk.updateChatUserPic(it)
                }
            }

            this.session = session

            player.playMedia(Uri.parse(channel.video.toString()), startingState ?: PlayerState())
        }
    }

    override fun onStart() {
        super.onStart()
        if (!adsPlaying)
            player.start()
    }

    override fun onStop() {
        super.onStop()
        player.stop()
    }

    override fun onDestroy() {
        timer.cancel()
        timer.purge()
        player.release()
        session?.widgetInterceptor = null
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onDestroy()
    }

    override fun onPause() {
        session?.widgetInterceptor = null
        session?.pause()
        privateGroupChatsession?.pause()
        super.onPause()
    }

    override fun onResume() {
        channelManager?.let {
            selectChannel(it.selectedChannel)
        }
        session?.resume()
        privateGroupChatsession?.resume()
        super.onResume()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putString(CHANNEL_NAME, channelManager?.selectedChannel?.name ?: "")
        outState?.putBoolean(AD_STATE, adsPlaying)
        outState?.putBoolean(SHOWING_DIALOG, showingDialog)
        outState?.putLong(POSITION, player.position())
    }
}
