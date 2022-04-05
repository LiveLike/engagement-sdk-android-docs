package com.livelike.livelikedemo

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.Constraints
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.livelike.engagementsdk.LiveLikeContentSession
import com.livelike.engagementsdk.LiveLikeUser
import com.livelike.engagementsdk.LiveLikeWidget
import com.livelike.engagementsdk.WidgetListener
import com.livelike.engagementsdk.chat.ChatViewDelegate
import com.livelike.engagementsdk.chat.ChatViewThemeAttributes
import com.livelike.engagementsdk.chat.data.remote.LiveLikePagination
import com.livelike.engagementsdk.core.services.messaging.proxies.LiveLikeWidgetEntity
import com.livelike.engagementsdk.core.services.messaging.proxies.WidgetInterceptor
import com.livelike.engagementsdk.core.services.messaging.proxies.WidgetLifeCycleEventsListener
import com.livelike.engagementsdk.core.utils.isNetworkConnected
import com.livelike.engagementsdk.core.utils.registerLogsHandler
import com.livelike.engagementsdk.core.utils.unregisterLogsHandler
import com.livelike.engagementsdk.publicapis.ChatMessageType
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.publicapis.LiveLikeChatMessage
import com.livelike.engagementsdk.widget.LiveLikeWidgetViewFactory
import com.livelike.engagementsdk.widget.domain.Reward
import com.livelike.engagementsdk.widget.domain.RewardSource
import com.livelike.engagementsdk.widget.domain.UserProfileDelegate
import com.livelike.engagementsdk.widget.viewModel.WidgetStates
import com.livelike.engagementsdk.widget.widgetModel.*
import com.livelike.livelikedemo.LiveLikeApplication.Companion.PREFERENCES_APP_ID
import com.livelike.livelikedemo.channel.Channel
import com.livelike.livelikedemo.channel.ChannelManager
import com.livelike.livelikedemo.customwidgets.CustomCheerMeter
import com.livelike.livelikedemo.customwidgets.CustomQuizWidget
import com.livelike.livelikedemo.utils.DialogUtils
import com.livelike.livelikedemo.utils.ThemeRandomizer
import com.livelike.livelikedemo.video.PlayerState
import com.livelike.livelikedemo.video.VideoPlayer
import kotlinx.android.synthetic.main.activity_exo_player.*
import kotlinx.android.synthetic.main.custom_msg_item.view.*
import kotlinx.android.synthetic.main.widget_chat_stacked.*
import java.util.*


class ExoPlayerActivity : AppCompatActivity() {

    private var customLink: String? = null
    private var showLink: Boolean = false
    private var enableReplies: Boolean = false
    private val themeRadomizerHandler = Handler(Looper.getMainLooper())
    private var jsonTheme: String? = null
    private var showNotification: Boolean = true
    private var themeCurrent: Int? = null
    private var player: VideoPlayer? = null
    private var session: LiveLikeContentSession? = null
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
            } else {
                startAd.text = "Start Ads"
                player?.start()
                session?.resume()
            }
        }
    private val timer = Timer()
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
            showLink = intent.getBooleanExtra("showLink", false)
            customLink = intent.getStringExtra("customLink")
            enableReplies = intent.getBooleanExtra("enableReplies", false)
            adsPlaying = savedInstanceState?.getBoolean(AD_STATE) ?: false
            val position = savedInstanceState?.getLong(POSITION) ?: 0
            startingState = PlayerState(0, position, !adsPlaying)

            showingDialog = savedInstanceState?.getBoolean(SHOWING_DIALOG) ?: false
            if (showingDialog) {
                dialog.showDialog(this@ExoPlayerActivity)
            }

            timer.schedule(
                object : TimerTask() {
                    override fun run() {
                        runOnUiThread {
                            val pdtTime = player?.getPDT() ?: 0
                            videoTimestamp?.text = Date(pdtTime).toString()
                        }
                    }
                },
                0, 100
            )

            setUpAdClickListeners()

            showChatAvatar = intent.getBooleanExtra("showAvatar", true)
            if (intent.getBooleanExtra("customCheerMeter", false))
                widget_view?.widgetViewFactory = object : LiveLikeWidgetViewFactory {
                    override fun createCheerMeterView(cheerMeterWidgetModel: CheerMeterWidgetmodel): View? {
                        return CustomCheerMeter(this@ExoPlayerActivity).apply {
                            this.cheerMeterWidgetModel = cheerMeterWidgetModel
                        }
                    }

                    override fun createAlertWidgetView(alertWidgetModel: AlertWidgetModel): View? {
                        return null
                    }

                    override fun createQuizWidgetView(
                        quizWidgetModel: QuizWidgetModel,
                        isImage: Boolean
                    ): View? {
                        return CustomQuizWidget(this@ExoPlayerActivity).apply {
                            this.quizWidgetModel = quizWidgetModel
                            this.isImage = isImage
                        }
                    }

                    override fun createPredictionWidgetView(
                        predictionViewModel: PredictionWidgetViewModel,
                        isImage: Boolean
                    ): View? {
                        return null
                    }

                    override fun createPredictionFollowupWidgetView(
                        followUpWidgetViewModel: FollowUpWidgetViewModel,
                        isImage: Boolean
                    ): View? {
                        return null
                    }

                    override fun createPollWidgetView(
                        pollWidgetModel: PollWidgetModel,
                        isImage: Boolean
                    ): View? {
                        return null
                    }

                    override fun createImageSliderWidgetView(imageSliderWidgetModel: ImageSliderWidgetModel): View? {
                        return null
                    }

                    override fun createVideoAlertWidgetView(videoAlertWidgetModel: VideoAlertWidgetModel): View? {
                        return null
                    }

                    override fun createTextAskWidgetView(imageSliderWidgetModel: TextAskWidgetModel): View? {
                        return null
                    }

                    override fun createNumberPredictionWidgetView(
                        numberPredictionWidgetModel: NumberPredictionWidgetModel,
                        isImage: Boolean
                    ): View? {
                        return null
                    }

                    override fun createNumberPredictionFollowupWidgetView(
                        followUpWidgetViewModel: NumberPredictionFollowUpWidgetModel,
                        isImage: Boolean
                    ): View? {
                        return null
                    }

                    override fun createSocialEmbedWidgetView(socialEmbedWidgetModel: SocialEmbedWidgetModel): View? {
                        return null
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
                            ) { dialog, _ ->
                                cm.loadClientConfig(cm.nextUrl)
                                dialog.dismiss()
                            }
                        if (cm.previousUrl?.isNotEmpty() == true)
                            setNeutralButton(
                                "Load Previous"
                            ) { dialog, _ ->
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
                    ),
                    object : TypeToken<List<LiveLikeWidget>>() {}.type
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
                                DialogUtils.showMyWidgetsDialog(
                                    context,
                                    (application as LiveLikeApplication).sdk,
                                    myWidgetsList,
                                    object : LiveLikeCallback<LiveLikeWidget>() {
                                        override fun onResponse(
                                            result: LiveLikeWidget?,
                                            error: String?
                                        ) {
                                            result?.let {
                                                widget_view?.displayWidget(
                                                    (application as LiveLikeApplication).sdk,
                                                    result
                                                )
                                            }
                                        }
                                    }
                                )
                            } else {
                                session?.getPublishedWidgets(
                                    LiveLikePagination.FIRST,
                                    object : LiveLikeCallback<List<LiveLikeWidget>>() {
                                        override fun onResponse(
                                            result: List<LiveLikeWidget>?,
                                            error: String?
                                        ) {
                                            result?.map { it }.let {
                                                DialogUtils.showMyWidgetsDialog(
                                                    context,
                                                    (application as LiveLikeApplication).sdk,
                                                    ArrayList(it ?: emptyList()),
                                                    object : LiveLikeCallback<LiveLikeWidget>() {
                                                        override fun onResponse(
                                                            result: LiveLikeWidget?,
                                                            error: String?
                                                        ) {
                                                            result?.let {
                                                                widget_view?.displayWidget(
                                                                    (application as LiveLikeApplication).sdk,
                                                                    result
                                                                )
                                                            }
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                )
                            }
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
        if (themeCurrent == R.style.MMLChatTheme) {
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

        btn_send_message?.setOnClickListener {
            session?.chatSession?.sendMessage("Sample ${Random().nextInt()}", null, null, null,
                object : LiveLikeCallback<LiveLikeChatMessage>() {
                    override fun onResponse(result: LiveLikeChatMessage?, error: String?) {
                        runOnUiThread {
                            result?.let {
                                Toast.makeText(
                                    applicationContext,
                                    it.toString(),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            error?.let {
                                Toast.makeText(applicationContext, it, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                })
        }

        btn_scroll_position?.setOnClickListener {
            val alert = AlertDialog.Builder(this)
            val edittext = EditText(this)
            alert.setTitle("Enter Message ID")

            alert.setView(edittext)

            alert.setPositiveButton("Done") { dialog, _ -> //What ever you want to do with the value
                val text = edittext.text.toString()
                dialog.dismiss()
                chat_view?.scrollToMessage(text)
            }

            alert.show()
        }

        btn_custom_message.setOnClickListener {
            session?.chatSession?.sendCustomChatMessage("{" +
                    "\"custom_message\": \"${getRandomString((10..50).random())}\"" +
                    "}", object : LiveLikeCallback<LiveLikeChatMessage>() {
                override fun onResponse(result: LiveLikeChatMessage?, error: String?) {
                    runOnUiThread {
                        result?.let {
                            println("ExoPlayerActivity.onResponse> ${it.id}")
                        }
                        error?.let {
                            Toast.makeText(applicationContext, error, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            })
        }
    }

    class MyCustomMsgViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    private fun getRandomString(sizeOfRandomString: Int): String {
        val random = Random()
        val sb = StringBuilder(sizeOfRandomString)
        for (i in 0 until sizeOfRandomString) {
            sb.append(ALLOWED_CHARACTERS[random.nextInt(ALLOWED_CHARACTERS.length)])
            if (random.nextBoolean()) {
                sb.append(" ")
            }
        }
        return sb.toString()
    }

    private val ALLOWED_CHARACTERS = "0123456789qwertyuiopasdfghjklzxcvbnm"

    override fun onBackPressed() {
        if (this.isTaskRoot) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                this.finishAfterTransition()
            }
        } else {
            (application as LiveLikeApplication).removePublicSession()
            (application as LiveLikeApplication).removePrivateSession()
            super.onBackPressed()
        }
    }

    private fun testKeyboardDismissUseCase(themeCurrent: Int) {
        if (themeCurrent == R.style.MMLChatTheme) {
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
            widget_view?.setWidgetListener(object : WidgetListener {
                override fun onNewWidget(liveLikeWidget: LiveLikeWidget) {
                    if (myWidgetsList.find { it.id == liveLikeWidget.id } == null) {
                        myWidgetsList.add(0, liveLikeWidget)
                        getSharedPreferences(PREFERENCES_APP_ID, Context.MODE_PRIVATE)
                            .edit().putString(PREF_MY_WIDGETS, Gson().toJson(myWidgetsList)).apply()
                    }
                }
            })
            widget_view?.allowWidgetSwipeToDismiss = false
            widget_view?.setSession(session)
            widget_view?.widgetLifeCycleEventsListener = object : WidgetLifeCycleEventsListener() {
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
            val allowDiscard = intent.getBooleanExtra("allowDiscard", true)
            session.chatSession.allowDiscardOwnPublishedMessageInSubscription = allowDiscard
            player?.playMedia(Uri.parse(channel.video.toString()), startingState ?: PlayerState())
        }

        if (ThemeRandomizer.themesList.size > 0) {
            widget_view?.applyTheme(ThemeRandomizer.themesList.last())
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
        if (session != null) {
            session?.chatSession?.avatarUrl = avatarUrl
            txt_chat_room_id?.visibility = View.INVISIBLE
            txt_chat_room_title?.visibility = View.INVISIBLE
            println("ExoPlayerActivity.initializeLiveLikeSDK::$showChatAvatar")
            session?.chatSession?.shouldDisplayAvatar = showChatAvatar
            chat_view.enableChatMessageURLs = showLink
            if (showLink) {
                chat_view.chatMessageUrlPatterns = customLink
            }
            chat_view.enableQuoteMessage = enableReplies
//            chat_view.reactionCountFormatter = { count -> prettyCount(count)}

            chat_view.chatViewDelegate = object : ChatViewDelegate {
                override fun onCreateViewHolder(
                    parent: ViewGroup,
                    viewType: ChatMessageType
                ): RecyclerView.ViewHolder {
                    return MyCustomMsgViewHolder(
                        LayoutInflater.from(parent.context)
                            .inflate(R.layout.custom_msg_item, parent, false)
                    )
                }

                override fun onBindViewHolder(
                    holder: RecyclerView.ViewHolder,
                    liveLikeChatMessage: LiveLikeChatMessage,
                    chatViewThemeAttributes: ChatViewThemeAttributes,
                    showChatAvatar: Boolean
                ) {
                    println("ExoPlayerActivity.onBindView>> ${holder is MyCustomMsgViewHolder}")
                    /*
                    chatViewThemeAttributes.chatBubbleBackgroundRes?.let {
                        if (it < 0) {
                            holder.itemView.lay_msg_back.setBackgroundColor(it)
                        } else {
                            val value = TypedValue()
                            try {
                                holder.itemView.lay_msg_back.context.resources.getValue(
                                    it,
                                    value,
                                    true
                                )
                                when (value.type) {
                                    TypedValue.TYPE_REFERENCE, TypedValue.TYPE_STRING -> holder.itemView.lay_msg_back.setBackgroundResource(
                                        it
                                    )
                                    TypedValue.TYPE_NULL -> holder.itemView.lay_msg_back.setBackgroundColor(
                                        Color.TRANSPARENT
                                    )
                                    else -> holder.itemView.lay_msg_back.setBackgroundColor(Color.TRANSPARENT)
                                }
                            } catch (e: Resources.NotFoundException) {
                                holder.itemView.lay_msg_back.setBackgroundColor(it)
                            }
                        }
                    }
                    */
                    if (showChatAvatar) {
                        holder.itemView.img_sender_msg.visibility = View.VISIBLE
                        Glide.with(applicationContext)
                            .load(liveLikeChatMessage.userPic)
                            .error(R.drawable.coin)
                            .placeholder(R.drawable.coin)
                            .into(holder.itemView.img_sender_msg)
                    } else {
                        holder.itemView.img_sender_msg.visibility = View.GONE
                    }
                    holder.itemView.txt_msg.text = liveLikeChatMessage.custom_data
                    holder.itemView.txt_msg_sender_name.text = liveLikeChatMessage.nickname
                    holder.itemView.txt_msg_time.text = liveLikeChatMessage.timestamp
                    if (liveLikeChatMessage.isDeleted) {
                        holder.itemView.txt_msg.text = "This msg is deleted"
                        holder.itemView.txt_msg.setTypeface(null, Typeface.ITALIC)
                    } else {
                        holder.itemView.txt_msg.setTypeface(null, Typeface.NORMAL)
                    }
                }
            }
            chat_view?.setSession(session!!.chatSession)
        }
        player?.playMedia(Uri.parse(channel.video.toString()), startingState ?: PlayerState())
    }

    private fun stopThemeRandomizer() {
        themeRadomizerHandler.removeCallbacksAndMessages(null)
    }

    private fun playThemeRandomizer() {
        themeRadomizerHandler.postDelayed(
            {
                ThemeRandomizer.nextTheme()?.let {
                    widget_view?.applyTheme(it)
                }
                playThemeRandomizer()
            },
            5000
        )
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
        session?.setWidgetContainer(FrameLayout(applicationContext))
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        (applicationContext as LiveLikeApplication).sdk.userProfileDelegate = null
        widget_view?.widgetLifeCycleEventsListener = null
        timer.cancel()
        unregisterLogsHandler()
        chat_view?.clearSession()
        super.onDestroy()
    }

    override fun onPause() {
        session?.pause()
        player?.stop()
        super.onPause()
    }

    override fun onResume() {
        session?.resume()
        if (!adsPlaying) {
            player?.start()
        }
        super.onResume()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(CHANNEL_NAME, channelManager?.selectedChannel?.name ?: "")
        outState.putBoolean(AD_STATE, adsPlaying)
        outState.putBoolean(SHOWING_DIALOG, showingDialog)
        outState.putLong(POSITION, player?.position() ?: 0)
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
