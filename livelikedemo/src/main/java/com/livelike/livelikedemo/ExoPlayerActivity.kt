package com.livelike.livelikedemo

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.constraint.Constraints
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.livelike.engagementsdk.LiveLikeContentSession
import com.livelike.engagementsdk.LiveLikeUser
import com.livelike.engagementsdk.LiveLikeWidget
import com.livelike.engagementsdk.MessageListener
import com.livelike.engagementsdk.WidgetListener
import com.livelike.engagementsdk.chat.ChatRoomInfo
import com.livelike.engagementsdk.chat.LiveLikeChatSession
import com.livelike.engagementsdk.chat.data.remote.LiveLikePagination
import com.livelike.engagementsdk.core.services.messaging.proxies.LiveLikeWidgetEntity
import com.livelike.engagementsdk.core.services.messaging.proxies.WidgetInterceptor
import com.livelike.engagementsdk.core.services.messaging.proxies.WidgetLifeCycleEventsListener
import com.livelike.engagementsdk.core.utils.isNetworkConnected
import com.livelike.engagementsdk.core.utils.registerLogsHandler
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.publicapis.LiveLikeChatMessage
import com.livelike.engagementsdk.widget.LiveLikeWidgetViewFactory
import com.livelike.engagementsdk.widget.domain.Reward
import com.livelike.engagementsdk.widget.domain.RewardSource
import com.livelike.engagementsdk.widget.domain.UserProfileDelegate
import com.livelike.engagementsdk.widget.viewModel.CheerMeterWidgetmodel
import com.livelike.engagementsdk.widget.viewModel.WidgetStates
import com.livelike.livelikedemo.channel.Channel
import com.livelike.livelikedemo.channel.ChannelManager
import com.livelike.livelikedemo.customwidgets.CustomCheerMeter
import com.livelike.livelikedemo.utils.DialogUtils
import com.livelike.livelikedemo.utils.ThemeRandomizer
import com.livelike.livelikedemo.video.PlayerState
import com.livelike.livelikedemo.video.VideoPlayer
import kotlinx.android.synthetic.main.activity_exo_player.btn_my_widgets
import kotlinx.android.synthetic.main.activity_exo_player.chat_room_button
import kotlinx.android.synthetic.main.activity_exo_player.fullLogs
import kotlinx.android.synthetic.main.activity_exo_player.live_blog
import kotlinx.android.synthetic.main.activity_exo_player.logsPreview
import kotlinx.android.synthetic.main.activity_exo_player.openLogs
import kotlinx.android.synthetic.main.activity_exo_player.playerView
import kotlinx.android.synthetic.main.activity_exo_player.selectChannelButton
import kotlinx.android.synthetic.main.activity_exo_player.startAd
import kotlinx.android.synthetic.main.activity_exo_player.videoTimestamp
import kotlinx.android.synthetic.main.widget_chat_stacked.chat_view
import kotlinx.android.synthetic.main.widget_chat_stacked.txt_chat_room_id
import kotlinx.android.synthetic.main.widget_chat_stacked.txt_chat_room_title
import kotlinx.android.synthetic.main.widget_chat_stacked.widget_view
import java.util.Calendar
import java.util.Date
import java.util.Timer
import java.util.TimerTask

class ExoPlayerActivity : AppCompatActivity() {

    private val themeRadomizerHandler = Handler(Looper.getMainLooper())
    private var jsonTheme: String? = null
    private var showNotification: Boolean = true
    private var themeCurrent: Int? = null
    private var isChatRoomJoined: Boolean = false
    private var player: VideoPlayer? = null
    private var session: LiveLikeContentSession? = null
    private var privateGroupChatsession: LiveLikeChatSession? = null
    private var startingState: PlayerState? = null
    private var channelManager: ChannelManager? = null
    private var myWidgetsList: ArrayList<LiveLikeWidget> = arrayListOf()

    private var adsPlaying = false
        set(adsPlaying) {
            field = adsPlaying

            if (adsPlaying) {
                startAd.text = "Stop Ads"
                player?.stop()
                session?.pause()
                privateGroupChatsession?.pause()
            } else {
                startAd.text = "Start Ads"
                player?.start()
                session?.resume()
                privateGroupChatsession?.resume()
            }
        }
    private val timer = Timer()
    private var chatRoomIds: List<String> = when (BuildConfig.FLAVOR) {
        "staging" -> {
            listOf("4d5ecf8d-3012-4ca2-8a56-4b8470c1ec8b", "e50ee571-7679-4efd-ad0b-e5fa00e38384")
        }
        "qatesting" -> {
            listOf("db177f26-2715-4b9f-9559-83fa05e58bfc", "73a19566-a855-432f-9a70-65266e79a81f")
        }
        "production" -> {
            listOf("dba595c6-afab-4f73-b22f-c7c0cb317ca9", "f05ee348-b8e5-4107-8019-c66fad7054a8")
        }
        else -> listOf()
    }
    private lateinit var chatRoomLastTimeStampMap: MutableMap<String, Long>
    private var showChatAvatar = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        themeCurrent = intent.getIntExtra("theme", R.style.AppTheme)
        this.setTheme(themeCurrent!!)
        setContentView(R.layout.activity_exo_player)
        jsonTheme = intent.getStringExtra("jsonTheme")
        if (isNetworkConnected()) {
            chatRoomLastTimeStampMap = GsonBuilder().create().fromJson(
                getSharedPreferences(PREFERENCES_APP_ID, Context.MODE_PRIVATE).getString(
                    PREF_CHAT_ROOM_LAST_TIME,
                    null
                ),
                object : TypeToken<MutableMap<String, Long>>() {}.type
            ) ?: mutableMapOf()
            testKeyboardDismissUseCase(themeCurrent!!)
            playerView.layoutParams.width = Constraints.LayoutParams.MATCH_PARENT

            player = (application as LiveLikeApplication).createPlayer(playerView)
            channelManager = (application as LiveLikeApplication).channelManager
            openLogs.setOnClickListener {
                fullLogs.visibility =
                    if (fullLogs.visibility == View.GONE) View.VISIBLE else View.GONE
            }
            fullLogs.movementMethod = ScrollingMovementMethod()

            live_blog.setOnClickListener {
                startActivity(Intent(this, LiveBlogActivity::class.java))
            }

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
                        val pdtTime = player?.getPDT() ?: 0
                        videoTimestamp?.text = Date(pdtTime).toString()
                    }
                }
            }, 0, 100)

            setUpAdClickListeners()

            showChatAvatar = intent.getBooleanExtra("showAvatar", true)
            if (intent.getBooleanExtra("customCheerMeter", false)) {
                widget_view.widgetViewFactory = object : LiveLikeWidgetViewFactory {
                    override fun createCheerMeterView(viewModel: CheerMeterWidgetmodel): View? {
                        println("WidgetOnlyActivity.createCheerMeterView")
                        return CustomCheerMeter(this@ExoPlayerActivity).apply {
                            cheerMeterWidgetModel = viewModel
                        }
                    }
                }
            }

            selectChannelButton.setOnClickListener {
                channelManager?.let { cm ->
                    val channels = cm.getChannels()
                    AlertDialog.Builder(this).apply {
                        setTitle("Choose a channel to watch!")
                        if (cm.nextUrl?.isNotEmpty() == true)
                            setPositiveButton(
                                "Load Next"
                            ) { dialog, which ->
                                cm.loadClientConfig(cm.nextUrl)
                                dialog.dismiss()
                            }
                        if (cm.previousUrl?.isNotEmpty() == true)
                            setNeutralButton(
                                "Load Previous"
                            ) { dialog, which ->
                                cm.loadClientConfig(cm.previousUrl)
                                dialog.dismiss()
                            }
                        setItems(channels.map { it.name }.toTypedArray()) { _, which ->
                            cm.selectedChannel = channels[which]
                            privateGroupRoomId = null
                            selectChannel(channels[which])
                        }
                        create()
                    }.show()
                }
            }
            myWidgetsList = GsonBuilder().create()
                .fromJson(
                    getSharedPreferences(PREFERENCES_APP_ID, Context.MODE_PRIVATE).getString(
                        PREF_MY_WIDGETS,
                        null
                    ), object : TypeToken<List<LiveLikeWidget>>() {}.type
                ) ?: arrayListOf()

            btn_my_widgets.setOnClickListener {

                AlertDialog.Builder(this).apply {
                    setTitle("Choose Option")
                        .setItems(
                            arrayListOf(
                                "Live Widgets",
                                "Published Widgets"
                            ).toTypedArray()
                        ) { _, which ->
                            if (which == 0) {
                                DialogUtils.showMyWidgetsDialog(context,
                                    (application as LiveLikeApplication).sdk,
                                    myWidgetsList,
                                    object : LiveLikeCallback<LiveLikeWidget>() {
                                        override fun onResponse(
                                            result: LiveLikeWidget?,
                                            error: String?
                                        ) {
                                            result?.let {
                                                widget_view.displayWidget(
                                                    (application as LiveLikeApplication).sdk,
                                                    result
                                                )
                                            }
                                        }
                                    })
                            } else {
                                session?.getPublishedWidgets(LiveLikePagination.FIRST,
                                    object : LiveLikeCallback<List<LiveLikeWidget?>>() {
                                        override fun onResponse(
                                            result: List<LiveLikeWidget?>?,
                                            error: String?
                                        ) {
                                            result?.map { it!! }.let {
                                                DialogUtils.showMyWidgetsDialog(context,
                                                    (application as LiveLikeApplication).sdk,
                                                    ArrayList(it),
                                                    object : LiveLikeCallback<LiveLikeWidget>() {
                                                        override fun onResponse(
                                                            result: LiveLikeWidget?,
                                                            error: String?
                                                        ) {
                                                            result?.let {
                                                                widget_view.displayWidget(
                                                                    (application as LiveLikeApplication).sdk,
                                                                    result
                                                                )
                                                            }
                                                        }
                                                    })
                                            }
                                        }
                                    })
                            }
                        }
                    create()
                }.show()


            }

            chat_room_button.setOnClickListener {

                AlertDialog.Builder(this).apply {
                    setTitle("Choose a custom Chat Room to join")
                    setItems(chatRoomIds.map {
                        "$it[${messageCount[it] ?: 0}]"
                    }.toTypedArray()) { _, which ->
                        val enteredChatRoomId = chatRoomIds[which]
                        privateGroupRoomId = enteredChatRoomId
                        privateGroupChatsession?.enterChatRoom(enteredChatRoomId)
//                    getSharedPreferences(PREFERENCES_APP_ID, Context.MODE_PRIVATE).edit().putString(
//                        PREF_CHAT_ROOM_LAST_TIME,
//                        GsonBuilder().create().toJson(chatRoomLastTimeStampMap)
//                    ).apply()
                        if (!isChatRoomJoined) {
                            val anotherChatRoomId = chatRoomIds[kotlin.math.abs(which - 1)]
                            privateGroupChatsession?.joinChatRoom(anotherChatRoomId)
                            isChatRoomJoined = true
                        }
                        chat_view.setSession(privateGroupChatsession!!)
                    }
                    create()
                }.show()
            }
            channelManager?.let {
                selectChannel(it.selectedChannel)
            }
        } else {
            // checkForNetworkToRecreateActivity()
        }
        if (themeCurrent == R.style.TurnerChatTheme) {
            val emptyView =
                LayoutInflater.from(this).inflate(R.layout.empty_chat_data_view, null)
            chat_view.emptyChatBackgroundView = emptyView
            chat_view.allowMediaFromKeyboard = false
        }
        if (isHideChatInput) {
            chat_view.isChatInputVisible = false
        }

        (applicationContext as LiveLikeApplication).sdk.userProfileDelegate =
            object : UserProfileDelegate {
                override fun userProfile(
                    userProfile: LiveLikeUser,
                    reward: Reward,
                    rewardSource: RewardSource
                ) {
                    val text =
                        "rewards recieved from ${rewardSource.name} : id is ${reward.rewardItem}, amount is ${reward.amount}"
                    logsPreview.text = "$text \n\n ${logsPreview.text}"
                    fullLogs.text = "$text \n\n ${fullLogs.text}"
                    println(text)
                }
            }
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
        player?.stop()
        player?.release()
        startingState = PlayerState(0, 0, !adsPlaying)
        initializeLiveLikeSDK(channel)
    }

    private val dialog = object : WidgetInterceptor() {
        override fun widgetWantsToShow(widgetData: LiveLikeWidgetEntity) {
            val widgetDataJson = GsonBuilder().create().toJson(widgetData)
            addLogs("widgetWantsToShow : $widgetDataJson")
            showDialog(this@ExoPlayerActivity)
        }
    }

    private var showingDialog = false

    private fun WidgetInterceptor.showDialog(context: Context) {
        if ((context as ExoPlayerActivity).isFinishing.not()) {
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
    }

    var messageCount: MutableMap<String, Long> = mutableMapOf()

    private fun initializeLiveLikeSDK(channel: Channel) {
        registerLogsHandler(object :
                (String) -> Unit {
            override fun invoke(text: String) {
                Handler(mainLooper).post {
                    logsPreview.text = "$text \n\n ${logsPreview.text}"
                    fullLogs.text = "$text \n\n ${fullLogs.text}"
                }
            }
        })

        if (channel != ChannelManager.NONE_CHANNEL) {
            val session = (application as LiveLikeApplication).createPublicSession(
                channel.llProgram.toString(),
                when (showNotification) {
                    true -> dialog
                    else -> null
                }
            )
            widget_view.setWidgetListener(object : WidgetListener {
                override fun onNewWidget(liveLikeWidget: LiveLikeWidget) {
                    if (myWidgetsList.find { it.id == liveLikeWidget.id } == null) {
                        myWidgetsList.add(0, liveLikeWidget)
                        getSharedPreferences(PREFERENCES_APP_ID, Context.MODE_PRIVATE)
                            .edit().putString(PREF_MY_WIDGETS, Gson().toJson(myWidgetsList)).apply()
                    }
                }
            })
            widget_view.setSession(session)

            widget_view.widgetLifeCycleEventsListener = object : WidgetLifeCycleEventsListener() {
                override fun onWidgetStateChange(
                    state: WidgetStates,
                    widgetData: LiveLikeWidgetEntity
                ) {
                }

                override fun onUserInteract(widgetData: LiveLikeWidgetEntity) {

                }

                override fun onWidgetPresented(widgetData: LiveLikeWidgetEntity) {
                    val widgetDataJson = GsonBuilder().create().toJson(widgetData)
                    addLogs("onWidgetPresented : $widgetDataJson")
                    playThemeRandomizer()
                }

                override fun onWidgetInteractionCompleted(widgetData: LiveLikeWidgetEntity) {
                    val widgetDataJson = GsonBuilder().create().toJson(widgetData)
                    addLogs("onWidgetInteractionCompleted : $widgetDataJson")
                }

                override fun onWidgetDismissed(widgetData: LiveLikeWidgetEntity) {
                    val widgetDataJson = GsonBuilder().create().toJson(widgetData)
                    addLogs("onWidgetDismissed : $widgetDataJson")
                    stopThemeRandomizer()
                }
            }
            this.session = session
            player?.playMedia(Uri.parse(channel.video.toString()), startingState ?: PlayerState())
        }

        if (privateGroupChatsession == null) {
            privateGroupChatsession =
                (application as LiveLikeApplication).createPrivateSession(
//                    errorDelegate = object : ErrorDelegate() {
//                        override fun onError(error: String) {
//                            checkForNetworkToRecreateActivity()
//                        }
//                    }

                )
            privateGroupChatsession?.setMessageListener(object : MessageListener {
                override fun onNewMessage(chatRoom: String, message: LiveLikeChatMessage) {
                    Log.v(
                        "Here$chatRoom",
                        "onNewMessage: ${message.message}  timestamp:${message.timestamp}"
                    )
                    logsPreview.text =
                        "New Message :${message.message} timestamp:${message.timestamp} \n\n ${logsPreview.text}"
                    fullLogs.text =
                        "New Message :${message.message} timestamp:${message.timestamp} \n\n ${fullLogs.text}"
                    if (chatRoom == privateGroupChatsession?.getActiveChatRoom?.invoke()) {
                        messageCount[chatRoom] = 0 // reset unread message count
                        // Adding the timetoken of the message from pubnub to get the count,if not time token then current timestamp in microseconds
                        if (message.timestamp.isEmpty()) {
                            chatRoomLastTimeStampMap[chatRoom] =
                                Calendar.getInstance().timeInMillis
                        } else {
                            // Added 1 into time //this is done only for those cases when user is watching the chatroom
                            // if it is not watching the chatroom then no need to add the 1 in the time
                            if (chatRoomLastTimeStampMap[chatRoom] == null || chatRoomLastTimeStampMap[chatRoom]!! < message.timestamp.toLong())
                                chatRoomLastTimeStampMap[chatRoom] =
                                    (message.timestamp.toLong() + 1)
                        }
                        Log.v(
                            "Here$chatRoom",
                            "onNewMessage2: ${message.message}  timestamp:${message.timestamp} lastTimeStamp:${chatRoomLastTimeStampMap[chatRoom]}"
                        )
                        getSharedPreferences(
                            PREFERENCES_APP_ID,
                            Context.MODE_PRIVATE
                        ).edit()
                            .putString(
                                PREF_CHAT_ROOM_LAST_TIME,
                                GsonBuilder().create().toJson(chatRoomLastTimeStampMap)
                            ).apply()
                    } else {
                        if (chatRoomLastTimeStampMap[chatRoom] == 0L) {
                            chatRoomLastTimeStampMap[chatRoom] = message.timestamp.toLong()
                            if (messageCount[chatRoom] == null) {
                                messageCount[chatRoom] = 1
                            }
                            Log.v(
                                "Here$chatRoom",
                                "onNewMessage3: ${message.message}  timestamp:${message.timestamp} lastTimeStamp:${chatRoomLastTimeStampMap[chatRoom]}"
                            )
                            getSharedPreferences(
                                PREFERENCES_APP_ID,
                                Context.MODE_PRIVATE
                            ).edit()
                                .putString(
                                    PREF_CHAT_ROOM_LAST_TIME,
                                    GsonBuilder().create().toJson(chatRoomLastTimeStampMap)
                                ).apply()
                        }
                        Log.v(
                            "Here$chatRoom",
                            "onNewMessage4: ${message.message}  timestamp:${message.timestamp} lastTimeStamp:${chatRoomLastTimeStampMap[chatRoom]}"
                        )
                        if (chatRoomLastTimeStampMap[chatRoom] == null || chatRoomLastTimeStampMap[chatRoom]!! < message.timestamp.toLong())
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
                        Log.v(
                            "Here$chatRoom",
                            "channel : ${it.key}, unread : ${it.value} lasttimestamp:${chatRoomLastTimeStampMap[chatRoom]}"
                        )
                    }
                }
            })
            chatRoomIds.forEach {
                privateGroupChatsession?.joinChatRoom(it)
            }
            for (pair in chatRoomLastTimeStampMap) {
                val chatRoomId = pair.key
                var timestamp = ((chatRoomLastTimeStampMap[chatRoomId]
                    ?: Calendar.getInstance().timeInMillis))
                if (timestamp == 0L) timestamp = Calendar.getInstance().timeInMillis
                logsPreview.text =
                    "Get Count: $timestamp roomId: $chatRoomId \n\n ${logsPreview.text}"
                fullLogs.text =
                    "Get Count: $timestamp roomId: $chatRoomId \n\n ${fullLogs.text}"
                Log.v("Here", "Getting Count Read channel : $chatRoomId timestamp: $timestamp")
                privateGroupChatsession?.getMessageCount(
                    chatRoomId,
                    timestamp,
                    object :
                        LiveLikeCallback<Byte>() {
                        override fun onResponse(result: Byte?, error: String?) {
                            logsPreview.text =
                                "Count Result: $timestamp roomId: $chatRoomId count: $result \n\n ${logsPreview.text}"
                            fullLogs.text =
                                "Count Result: $timestamp roomId: $chatRoomId count: $result \n\n ${fullLogs.text}"
                            Log.v(
                                "Here",
                                "Count Read channel : $chatRoomId lasttimestamp:$timestamp count: $result"
                            )
                            result?.let {
                                messageCount[chatRoomId] =
                                    (messageCount[chatRoomId] ?: 0) + result
                            }
                        }
                    })
            }
            if (chatRoomLastTimeStampMap.isEmpty()) {
                chatRoomIds.forEach {
                    chatRoomLastTimeStampMap[it] = 0L
                }
            }
        }

        if (ThemeRandomizer.themesList.size > 0) {
            widget_view.applyTheme(ThemeRandomizer.themesList.last())
        }

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
        val avatarUrl = intent.getStringExtra("avatarUrl")
        if (privateGroupRoomId != null) {
            privateGroupChatsession?.shouldDisplayAvatar = showChatAvatar
            privateGroupChatsession?.enterChatRoom(privateGroupRoomId!!)
            privateGroupChatsession?.avatarUrl = avatarUrl
            txt_chat_room_id.visibility = View.VISIBLE
            txt_chat_room_title.visibility = View.VISIBLE
            (application as LiveLikeApplication).sdk.getChatRoom(privateGroupRoomId!!,
                object : LiveLikeCallback<ChatRoomInfo>() {
                    override fun onResponse(result: ChatRoomInfo?, error: String?) {
                        result?.let {
                            txt_chat_room_title.text = it.title ?: "No Title"
                            txt_chat_room_id.text = it.id
                        }
                    }
                })
            chat_view.setSession(privateGroupChatsession!!)
        } else if (session != null) {
            session?.chatSession?.avatarUrl = avatarUrl
            txt_chat_room_id.visibility = View.INVISIBLE
            txt_chat_room_title.visibility = View.INVISIBLE
            session?.chatSession?.shouldDisplayAvatar = showChatAvatar
            chat_view.setSession(session!!.chatSession)
        }
        this.session = session
        player?.playMedia(Uri.parse(channel.video.toString()), startingState ?: PlayerState())
    }

    private fun stopThemeRandomizer() {
        themeRadomizerHandler.removeCallbacksAndMessages(null)
    }

    private fun playThemeRandomizer() {
        themeRadomizerHandler.postDelayed({
            ThemeRandomizer.nextTheme()?.let {
                widget_view.applyTheme(it)
            }
            playThemeRandomizer()
        }, 5000)
    }

    private fun addLogs(logs: String?) {
        logsPreview.text = "$logs \n\n ${logsPreview.text}"
        fullLogs.text = "$logs \n\n ${fullLogs.text}"
    }

//    private fun checkForNetworkToRecreateActivity() {
//        //removing this method implementation as it is causing multiple instances on same activity in a task
// //        playerView.postDelayed({
// //            if (isNetworkConnected()) {
// //                playerView.post {
// //                    startActivity(intent)
// //                    finish()
// //                }
// //            } else {
// //                checkForNetworkToRecreateActivity()
// //            }
// //        }, 1000)
//    }

    override fun onStart() {
        super.onStart()
        if (!adsPlaying)
            player?.start()
    }

    override fun onStop() {
        super.onStop()
        player?.stop()
    }

    override fun onDestroy() {
        (application as LiveLikeApplication).player = null
        timer.cancel()
        timer.purge()
        player?.release()
        session?.widgetInterceptor = null
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onDestroy()
    }

    override fun onPause() {
        session?.pause()
        privateGroupChatsession?.pause()
        super.onPause()
    }

    override fun onResume() {
        session?.resume()
        privateGroupChatsession?.resume()
        super.onResume()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putString(CHANNEL_NAME, channelManager?.selectedChannel?.name ?: "")
        outState?.putBoolean(AD_STATE, adsPlaying)
        outState?.putBoolean(SHOWING_DIALOG, showingDialog)
        outState?.putLong(POSITION, player?.position() ?: 0)
    }

    companion object {
        const val AD_STATE = "adstate"
        const val SHOWING_DIALOG = "showingDialog"
        const val POSITION = "position"
        const val CHANNEL_NAME = "channelName"
        var privateGroupRoomId: String? = null
        var isHideChatInput: Boolean = false
    }
}
