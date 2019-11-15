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
import android.view.View
import android.view.WindowManager
import com.livelike.engagementsdk.LiveLikeContentSession
import com.livelike.engagementsdk.MessageListener
import com.livelike.engagementsdk.publicapis.LiveLikeChatMessage
import com.livelike.engagementsdk.services.messaging.proxies.WidgetInterceptor
import com.livelike.engagementsdk.utils.registerLogsHandler
import com.livelike.livelikedemo.channel.Channel
import com.livelike.livelikedemo.channel.ChannelManager
import com.livelike.livelikedemo.video.PlayerState
import com.livelike.livelikedemo.video.VideoPlayer
import java.util.Date
import java.util.Timer
import java.util.TimerTask
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
        } else {
            startAd.text = "Start Ads"
            player.start()
            session?.resume()
        }
    }
    val timer = Timer()
    val chatChannelNames = listOf("qatest1", "qatest3", "test2")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_exo_player)

        playerView.layoutParams.width = Constraints.LayoutParams.MATCH_PARENT

        player = (application as LiveLikeApplication).createPlayer(playerView)
        channelManager = (application as LiveLikeApplication).channelManager
        openLogs.setOnClickListener {
            fullLogs.visibility = if (fullLogs.visibility == View.GONE) View.VISIBLE else View.GONE
        }
        fullLogs.movementMethod = ScrollingMovementMethod()

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
                setItems(chatChannelNames.map {
                    "$it[${messageCount[it]?.size ?: 0}]"
                }.toTypedArray()) { _, which ->
                    privateGroupChatsession!!.enterChatRoom(chatChannelNames[which])
                    chat_view.setSession(privateGroupChatsession!!)
                }
                create()
            }.show()
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

    val messageCount: MutableMap<String, MutableList<String>> = mutableMapOf()

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
            val session = (application as LiveLikeApplication).createSession(channel.llProgram.toString(),
                dialog)
            privateGroupChatsession = (application as LiveLikeApplication).sdk.createContentSession("50feace1-37d0-4bbb-afbb-3c3799188520")

            privateGroupChatsession?.setMessageListener(object : MessageListener {
                override fun onNewMessage(chatRoom: String, message: LiveLikeChatMessage) {
                    if (chatRoom == privateGroupChatsession?.getActiveChatRoom?.invoke()) {
                        messageCount[chatRoom] = mutableListOf() // reset unread message count
                    } else {
                        if (messageCount[chatRoom] == null) {
                            messageCount[chatRoom] = mutableListOf(message.id.toString())
                        } else {
                            messageCount[chatRoom]?.add(message.id.toString())
                        }
                    }
                    messageCount.forEach {
                        logsPreview.text = "channel : ${it.key}, unread : ${it.value.size} \n\n ${logsPreview.text}"
                        fullLogs.text = "channel : ${it.key}, unread : ${it.value.size} \n\n ${fullLogs.text}"
                        Log.e("Here", "channel : ${it.key}, unread : ${it.value.size}")
                    }
                }
            })

            chat_view.setSession(session)
            widget_view.setSession(session)
            getSharedPreferences("test-app", Context.MODE_PRIVATE)
                .getString("UserNickname", "")
                .let {
                    if (!it.isNullOrEmpty()) {
                        (application as LiveLikeApplication).sdk.updateChatNickname(it)
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
        super.onPause()
    }

    override fun onResume() {
        channelManager?.let {
            selectChannel(it.selectedChannel)
        }
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
