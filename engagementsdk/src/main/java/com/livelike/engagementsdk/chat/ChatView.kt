package com.livelike.engagementsdk.chat

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.support.constraint.ConstraintLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.Spannable
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import com.livelike.engagementsdk.CHAT_PROVIDER
import com.livelike.engagementsdk.DEFAULT_CHAT_MESSAGE_DATE_TIIME_FROMATTER
import com.livelike.engagementsdk.EpochTime
import com.livelike.engagementsdk.KeyboardHideReason
import com.livelike.engagementsdk.KeyboardType
import com.livelike.engagementsdk.LiveLikeUser
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.ViewAnimationEvents
import com.livelike.engagementsdk.chat.data.remote.PubnubChatEventType
import com.livelike.engagementsdk.chat.stickerKeyboard.FragmentClickListener
import com.livelike.engagementsdk.chat.stickerKeyboard.Sticker
import com.livelike.engagementsdk.chat.stickerKeyboard.StickerKeyboardView
import com.livelike.engagementsdk.chat.stickerKeyboard.countMatches
import com.livelike.engagementsdk.chat.stickerKeyboard.findImages
import com.livelike.engagementsdk.chat.stickerKeyboard.replaceWithImages
import com.livelike.engagementsdk.chat.stickerKeyboard.replaceWithStickers
import com.livelike.engagementsdk.core.utils.AndroidResource
import com.livelike.engagementsdk.core.utils.AndroidResource.Companion.dpToPx
import com.livelike.engagementsdk.core.utils.animators.buildScaleAnimator
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.core.utils.logError
import com.livelike.engagementsdk.core.utils.scanForActivity
import com.livelike.engagementsdk.publicapis.LiveLikeChatMessage
import com.livelike.engagementsdk.publicapis.toLiveLikeChatMessage
import com.livelike.engagementsdk.widget.data.models.ProgramGamificationProfile
import com.livelike.engagementsdk.widget.view.loadImage
import java.util.Date
import kotlin.math.max
import kotlin.math.min
import kotlinx.android.synthetic.main.chat_input.view.button_chat_send
import kotlinx.android.synthetic.main.chat_input.view.button_emoji
import kotlinx.android.synthetic.main.chat_input.view.chat_input_background
import kotlinx.android.synthetic.main.chat_input.view.chat_input_border
import kotlinx.android.synthetic.main.chat_input.view.edittext_chat_message
import kotlinx.android.synthetic.main.chat_input.view.user_profile_display_LL
import kotlinx.android.synthetic.main.chat_user_profile_bar.view.gamification_badge_iv
import kotlinx.android.synthetic.main.chat_user_profile_bar.view.pointView
import kotlinx.android.synthetic.main.chat_user_profile_bar.view.rank_label
import kotlinx.android.synthetic.main.chat_user_profile_bar.view.rank_value
import kotlinx.android.synthetic.main.chat_user_profile_bar.view.user_profile_tv
import kotlinx.android.synthetic.main.chat_view.view.chatInput
import kotlinx.android.synthetic.main.chat_view.view.chat_view
import kotlinx.android.synthetic.main.chat_view.view.chatdisplay
import kotlinx.android.synthetic.main.chat_view.view.chatdisplayBack
import kotlinx.android.synthetic.main.chat_view.view.loadingSpinner
import kotlinx.android.synthetic.main.chat_view.view.snap_live
import kotlinx.android.synthetic.main.chat_view.view.sticker_keyboard
import kotlinx.android.synthetic.main.chat_view.view.swipeToRefresh
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import pl.droidsonroids.gif.MultiCallback

/**
 *  This view will load and display a chat component. To use chat view
 *  ```
 *  <com.livelike.sdk.chat.ChatView
 *      android:id="@+id/chatView"
 *      android:layout_width="wrap_content"
 *      android:layout_height="wrap_content">
 *   </com.livelike.sdk.chat.ChatView>
 *  ```
 *
 */
open class ChatView(context: Context, private val attrs: AttributeSet?) :
    ConstraintLayout(context, attrs) {
    companion object {
        const val SNAP_TO_LIVE_ANIMATION_DURATION = 400F
        const val SNAP_TO_LIVE_ALPHA_ANIMATION_DURATION = 320F
        const val SNAP_TO_LIVE_ANIMATION_DESTINATION = 50
        private const val CHAT_MINIMUM_SIZE_DP = 292
        private const val SMOOTH_SCROLL_MESSAGE_COUNT_LIMIT = 100
    }

    private val chatAttribute = ChatViewThemeAttributes()
    private val uiScope = CoroutineScope(Dispatchers.Main)

    private var session: LiveLikeChatSession? = null
    private var snapToLiveAnimation: AnimatorSet? = null
    private var showingSnapToLive: Boolean = false
    private var currentUser: LiveLikeUser? = null

    var allowMediaFromKeyboard: Boolean = true
        set(value) {
            field = value
            edittext_chat_message.allowMediaFromKeyboard = value
        }

    var emptyChatBackgroundView: View? = null
        set(view) {
            field = view
            if (chatdisplayBack.childCount > 1)
                chatdisplayBack.removeViewAt(1)
            initEmptyView()
        }

    /** Boolean option to enable / disable the profile display inside chat view */
    var displayUserProfile: Boolean = false
        set(value) {
            field = value
            user_profile_display_LL?.apply {
                visibility = if (value) View.VISIBLE else View.GONE
            }
        }

    private val viewModel: ChatViewModel?
        get() = (session as ChatSession?)?.chatViewModel

    val callback = MultiCallback(true)

    init {
        context.scanForActivity()?.window?.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
                    or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        ) // INFO: Adjustresize doesn't work with Fullscreen app.. See issue https://stackoverflow.com/questions/7417123/android-how-to-adjust-layout-in-full-screen-mode-when-softkeyboard-is-visible
        context.obtainStyledAttributes(
            attrs,
            R.styleable.ChatView,
            0, 0
        ).apply {
            try {
                displayUserProfile =
                    getBoolean(R.styleable.ChatView_displayUserProfile, false)
                chatAttribute.initAttributes(context, this)
            } finally {
                recycle()
            }
        }
        initView(context)
    }

    private fun setBackButtonInterceptor(v: View) {
        v.isFocusableInTouchMode = true
        v.requestFocus()
        v.setOnKeyListener(object : OnKeyListener {
            override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
                if (event?.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK) {
                    if (sticker_keyboard.visibility == View.VISIBLE) {
                        hideStickerKeyboard(KeyboardHideReason.BACK_BUTTON)
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun initView(context: Context) {
        LayoutInflater.from(context).inflate(R.layout.chat_view, this, true)
        user_profile_display_LL.visibility = if (displayUserProfile) View.VISIBLE else View.GONE
        chatAttribute.apply {
            rank_value.setTextColor(rankValueTextColor)
            chat_view.background = chatViewBackgroundRes
            chatDisplayBackgroundRes?.let {
                chatdisplay.background = it
            }
            chat_input_background.background = chatInputViewBackgroundRes
            chat_input_border.background = chatInputBackgroundRes
            edittext_chat_message.setTextColor(chatInputTextColor)
            edittext_chat_message.setHintTextColor(chatInputHintTextColor)
            edittext_chat_message.setTextSize(
                TypedValue.COMPLEX_UNIT_PX,
                chatInputTextSize.toFloat()
            )
            button_emoji.setImageDrawable(chatStickerSendDrawable)
            button_emoji.setColorFilter(
                sendStickerTintColor,
                android.graphics.PorterDuff.Mode.MULTIPLY
            )
            button_emoji.visibility = when {
                showStickerSend -> View.VISIBLE
                else -> View.GONE
            }

            val layoutParams = button_chat_send.layoutParams
            layoutParams.width = sendIconWidth
            layoutParams.height = sendIconHeight
            button_chat_send.layoutParams = layoutParams
            button_chat_send.setImageDrawable(chatSendDrawable)
            button_chat_send.background = chatSendBackgroundDrawable
            button_chat_send.setPadding(
                chatSendPaddingLeft,
                chatSendPaddingTop,
                chatSendPaddingRight,
                chatSendPaddingBottom
            )
            button_chat_send.setColorFilter(
                sendImageTintColor,
                android.graphics.PorterDuff.Mode.MULTIPLY
            )
            initEmptyView()
        }
        callback.addView(edittext_chat_message)

        swipeToRefresh.setOnRefreshListener {
            if (viewModel?.chatLoaded == true)
                viewModel?.loadPreviousMessages()
            else
                swipeToRefresh.isRefreshing = false
        }
    }

    private fun initEmptyView() {
        emptyChatBackgroundView?.let {
            if (chatdisplayBack.childCount == 1) {
                val layoutParam = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                layoutParam.gravity = Gravity.CENTER
                chatdisplayBack.addView(it, layoutParam)
            }
            it.visibility = View.GONE
        }
    }

    /**
     * unix timestamp is passed as param
     * returns the formatted string to display
     */
    open fun formatMessageDateTime(messageTimeStamp: Long?): String {
        if (messageTimeStamp == null || messageTimeStamp == 0L) {
            return ""
        }
        val dateTime = Date()
        dateTime.time = messageTimeStamp
        return DEFAULT_CHAT_MESSAGE_DATE_TIIME_FROMATTER.format(dateTime)
    }

    fun setSession(session: LiveLikeChatSession) {
        if (this.session === session) return // setting it multiple times same view with same session have a weird behaviour will debug later.
        hideGamification()
        this.session = session.apply {
            analyticService.trackOrientationChange(resources.configuration.orientation == 1)
        }

        viewModel?.apply {
            chatAdapter.chatViewThemeAttribute = chatAttribute
            chatAdapter.messageTimeFormatter = { time ->
                formatMessageDateTime(time)
            }
            initStickerKeyboard(sticker_keyboard, this)
            refreshWithDeletedMessage()
            setDataSource(chatAdapter)
            if (chatLoaded)
                checkEmptyChat()
            eventStream.subscribe(javaClass.simpleName) {
                logDebug { "Chat event stream : $it" }
                when (it) {
                    ChatViewModel.EVENT_NEW_MESSAGE -> {
                        // Auto scroll if user is looking at the latest messages
                        autoScroll = true
                        checkEmptyChat()
                        if (isLastItemVisible && !swipeToRefresh.isRefreshing) {
                            snapToLive()
                        }
                    }
                    ChatViewModel.EVENT_LOADING_COMPLETE -> {
                        uiScope.launch {
                            // Add delay to avoid UI glitch when recycler view is loading
                            delay(500)
                            hideLoadingSpinner()
                            checkEmptyChat()
                            if (!swipeToRefresh.isRefreshing)
                                snapToLive()
                            swipeToRefresh.isRefreshing = false
                        }
                    }
                    ChatViewModel.EVENT_LOADING_STARTED -> {
                        uiScope.launch {
                            hideKeyboard(KeyboardHideReason.EXPLICIT_CALL)
                            hideStickerKeyboard(KeyboardHideReason.EXPLICIT_CALL)
                            initEmptyView()
                            delay(400)
                            showLoadingSpinner()
                        }
                    }
                }
            }
            userStream.subscribe(javaClass.simpleName) {
                currentUser = it
                it?.let {
                    uiScope.launch {
                        user_profile_tv.visibility = View.VISIBLE
                        user_profile_tv.text = it.nickname
                    }
                }
            }
            programRepository?.programGamificationProfileStream?.subscribe(javaClass.simpleName) {
                it?.let { programRank ->
                    if (programRank.newPoints == 0 || pointView.visibility == View.GONE) {
                        pointView.showPoints(programRank.points)
                        wouldShowBadge(programRank)
                        showUserRank(programRank)
                    } else if (programRank.points == programRank.newPoints) {
                        pointView.apply {
                            postDelayed(
                                {
                                    startAnimationFromTop(programRank.points)
                                    showUserRank(programRank)
                                },
                                6300
                            )
                        }
                    } else {
                        pointView.apply {
                            postDelayed(
                                {
                                    startAnimationFromTop(programRank.points)
                                    showUserRank(programRank)
                                },
                                1000
                            )
                        }
                    }
                }
            }
            animationEventsStream?.subscribe(javaClass.simpleName) {
                if (it == ViewAnimationEvents.BADGE_COLLECTED) {
                    programRepository?.programGamificationProfileStream?.latest()
                        ?.let { programGamificationProfile ->
                            wouldShowBadge(programGamificationProfile, true)
                        }
                }
            }

            chatAdapter.checkListIsAtTop = lambda@{
                val lm: LinearLayoutManager = chatdisplay.layoutManager as LinearLayoutManager
                if (lm.findFirstVisibleItemPosition() == it) {
                    return@lambda true
                }
                return@lambda false
            }

            edittext_chat_message.addTextChangedListener(object : TextWatcher {
                var containsImage = false
                override fun afterTextChanged(s: Editable?) {
                    val matcher = s.toString().findImages()
                    if (matcher.find()) {
                        containsImage = true
                        replaceWithImages(
                            s as Spannable,
                            this@ChatView.context,
                            callback,
                            true
                        )
                        // cleanup before the image
                        if (matcher.start()> 0) edittext_chat_message.text?.delete(0, matcher.start())

                        // cleanup after the image
                        if (matcher.end() <s.length) edittext_chat_message.text?.delete(matcher.end(), s.length)
                        // Move to end of line
                        edittext_chat_message.setSelection(edittext_chat_message.text?.length ?: 0)
                        if (edittext_chat_message.text?.isNotEmpty() == true)
                            wouldUpdateChatInputAccessibiltyFocus(100)
                    } else if (containsImage) {
                        containsImage = false
                        s?.length?.let { edittext_chat_message.text?.delete(0, it) }
                    } else {
                        containsImage = false
                        stickerPackRepository?.let { stickerPackRepository ->
                            replaceWithStickers(
                                s as Spannable,
                                this@ChatView.context,
                                stickerPackRepository,
                                edittext_chat_message, null
                            )
                        }
                    }
                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) = Unit

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) = Unit
            })

            button_emoji.setOnClickListener {
                if (sticker_keyboard.visibility == View.GONE) showStickerKeyboard() else hideStickerKeyboard(
                    KeyboardHideReason.CHANGING_KEYBOARD_TYPE
                )
            }
        }
    }

    private fun checkEmptyChat() {
        emptyChatBackgroundView?.let {
            it.visibility = if ((viewModel?.messageList?.size ?: 0) == 0)
                View.VISIBLE
            else
                View.GONE
        }
    }

    private fun initStickerKeyboard(
        stickerKeyboardView: StickerKeyboardView,
        chatViewModel: ChatViewModel
    ) {
        stickerKeyboardView.initTheme(chatAttribute)
        uiScope.launch {
            chatViewModel.stickerPackRepositoryFlow.collect { stickerPackRepository ->
                stickerKeyboardView.setProgram(stickerPackRepository) {
                    if (it.isNullOrEmpty()) {
                        button_emoji?.visibility = View.GONE
                        sticker_keyboard?.visibility = View.GONE
                    } else {
                        button_emoji?.visibility = View.VISIBLE
                    }
                    viewModel?.chatAdapter?.notifyDataSetChanged()
                }
                // used to pass the shortcode to the keyboard
                stickerKeyboardView.setOnClickListener(object : FragmentClickListener {
                    override fun onClick(sticker: Sticker) {
                        val textToInsert = ":${sticker.shortcode}:"
                        val start = max(edittext_chat_message.selectionStart, 0)
                        val end = max(edittext_chat_message.selectionEnd, 0)
                        if (edittext_chat_message.text!!.length + textToInsert.length < 250) {
                            // replace selected text or start where the cursor is
                            edittext_chat_message.text?.replace(
                                min(start, end), max(start, end),
                                textToInsert, 0, textToInsert.length
                            )
                        }
                    }
                })
            }
        }
    }

    private fun wouldShowBadge(programRank: ProgramGamificationProfile, animate: Boolean = false) {
        var currentBadge = programRank.newBadges?.max()
        if (currentBadge == null) {
            currentBadge = programRank.currentBadge
        }
        currentBadge?.let {
            gamification_badge_iv.visibility = View.VISIBLE
            gamification_badge_iv.loadImage(it.imageFile, dpToPx(14))
            if (animate) {
                gamification_badge_iv.buildScaleAnimator(0f, 1f, 1000).start()
            }
        }
    }

    private fun hideGamification() {
        pointView?.visibility = View.GONE
        rank_label?.visibility = View.GONE
        rank_value?.visibility = View.GONE
        gamification_badge_iv?.visibility = View.GONE
    }

    private fun showUserRank(programGamificationProfile: ProgramGamificationProfile) {
        if (programGamificationProfile.points > 0) {
            rank_label.visibility = View.VISIBLE
            rank_value.visibility = View.VISIBLE
            uiScope.async {
                delay(1000)
                rank_value.text = "#${programGamificationProfile.rank}"
            }
        }
    }

    override fun onViewRemoved(view: View?) {
        viewModel?.apply {
            eventStream.unsubscribe(javaClass.simpleName)
            userStream.unsubscribe(javaClass.simpleName)
        }
        super.onViewRemoved(view)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthDp = AndroidResource.pxToDp(width)
        if (widthDp < CHAT_MINIMUM_SIZE_DP && widthDp != 0) {
            logError { "[CONFIG ERROR] Current ChatView Width is $widthDp, it must be more than 292dp or won't display on the screen." }
            setMeasuredDimension(0, 0)
            return
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    // Hide keyboard when clicking outside of the EditText
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        val v = context.scanForActivity()?.currentFocus

        if (v != null &&
            (ev?.action == MotionEvent.ACTION_UP || ev?.action == MotionEvent.ACTION_MOVE) &&
            (v is EditText || v is ChatView) &&
            !v.javaClass.name.startsWith("android.webkit.")
        ) {
            val scrcoords = IntArray(2)
            v.getLocationOnScreen(scrcoords)
            val x = ev.rawX + v.left - scrcoords[0]
            val y = ev.rawY + v.top - scrcoords[1]
            val outsideStickerKeyboardBound =
                (v.bottom - sticker_keyboard.height - button_chat_send.height)
            // Added check for image_height greater than 0 so bound position for touch should be above the send icon
            if (!edittext_chat_message.isTouching) {
                if (y < v.top || y > v.bottom || (y < outsideStickerKeyboardBound)) {
                    hideStickerKeyboard(KeyboardHideReason.TAP_OUTSIDE)
                    hideKeyboard(KeyboardHideReason.TAP_OUTSIDE)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private var isLastItemVisible = false
    private var autoScroll = false
    /**
     *  Sets the data source for this view.
     *  @param chatAdapter ChatAdapter used for creating this view.
     */
    private fun setDataSource(chatAdapter: ChatRecyclerAdapter) {
        chatdisplay.let { rv ->
            rv.adapter = chatAdapter
            val lm = rv.layoutManager as LinearLayoutManager
            lm.recycleChildrenOnDetach = true
            rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(
                    rv: RecyclerView,
                    dx: Int,
                    dy: Int
                ) {
                    val totalItemCount = lm.itemCount
                    val lastVisible = lm.findLastVisibleItemPosition()

                    val endHasBeenReached = lastVisible + 5 >= totalItemCount
                    if (!autoScroll)
                        isLastItemVisible = if (totalItemCount > 0 && endHasBeenReached) {
                            hideSnapToLive()
                            true
                        } else {
                            showSnapToLive()
                            false
                        }
                    if (endHasBeenReached) {
                        autoScroll = false
                    }
                }
            })
        }

        snap_live.setOnClickListener {
            autoScroll = true
            snapToLive()
        }

        button_chat_send.let { buttonChat ->
            buttonChat.setOnClickListener { sendMessageNow() }

            if (edittext_chat_message.text.isNullOrEmpty()) {
                buttonChat.visibility = View.GONE
                buttonChat.isEnabled = false
            } else {
                buttonChat.isEnabled = true
                buttonChat.visibility = View.VISIBLE
            }

            edittext_chat_message.apply {
                addTextChangedListener(object : TextWatcher {
                    var previousText: CharSequence = ""
                    override fun beforeTextChanged(
                        s: CharSequence,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {
                        previousText = s
                    }

                    override fun onTextChanged(
                        s: CharSequence,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {
                    }

                    override fun afterTextChanged(s: Editable) {
                        if (s.isNotEmpty()) {
                            buttonChat.isEnabled = true
                            buttonChat.visibility = View.VISIBLE
                        } else {
                            buttonChat.isEnabled = false
                            buttonChat.visibility = View.GONE
                        }
                    }
                })

                setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {
                        session?.analyticService?.trackKeyboardOpen(KeyboardType.STANDARD)
                        hideStickerKeyboard(KeyboardHideReason.CHANGING_KEYBOARD_TYPE)
                    }
                }
            }
        }
    }

    private fun hideStickerKeyboard(reason: KeyboardHideReason) {
        findViewById<StickerKeyboardView>(R.id.sticker_keyboard)?.apply {
//            if (visibility == View.VISIBLE) {
//                session?.analyticService?.trackKeyboardClose(KeyboardType.STICKER, reason)
//            }
            visibility = View.GONE
        }
    }

    private fun showStickerKeyboard() {
        uiScope.launch {
            hideKeyboard(KeyboardHideReason.MESSAGE_SENT)
            session?.analyticService?.trackKeyboardOpen(KeyboardType.STICKER)
            delay(200) // delay to make sure the keyboard is hidden
            findViewById<StickerKeyboardView>(R.id.sticker_keyboard)?.visibility = View.VISIBLE
        }
    }

    private fun showLoadingSpinner() {
        loadingSpinner.visibility = View.VISIBLE
        chatInput.visibility = View.GONE
        chatdisplay.visibility = View.GONE
        snap_live.visibility = View.GONE
    }

    private fun hideLoadingSpinner() {
        loadingSpinner.visibility = View.GONE
        chatInput.visibility = View.VISIBLE
        chatdisplay.visibility = View.VISIBLE
        wouldUpdateChatInputAccessibiltyFocus()
    }

    private fun wouldUpdateChatInputAccessibiltyFocus(time: Long = 500) {
        chatInput.postDelayed({
            edittext_chat_message.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
        }, time)
    }

    private fun hideKeyboard(reason: KeyboardHideReason) {
        val inputManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(
            edittext_chat_message.windowToken,
            0
        )

//        session?.analyticService?.trackKeyboardClose(KeyboardType.STANDARD, reason)
        setBackButtonInterceptor(this)
    }

    /**
     * use this to listen messages sent from this view
     **/
    var sentMessageListener: ((message: LiveLikeChatMessage) -> Unit)? = null

    /**
     * Use this function to hide any soft and sticker keyboards over the view.
     **/
    fun dismissKeyboard() {
        hideKeyboard(KeyboardHideReason.EXPLICIT_CALL)
        hideStickerKeyboard(KeyboardHideReason.EXPLICIT_CALL)
    }

    private fun sendMessageNow() {
        if (edittext_chat_message.text.isNullOrBlank()) {
            // Do nothing if the message is blank or empty
            return
        }
        val timeData = session?.getPlayheadTime() ?: EpochTime(0)

        // TODO all this can be moved to view model easily
        ChatMessage(
            PubnubChatEventType.MESSAGE_CREATED,
            viewModel?.currentChatRoom?.channels?.chat?.get(CHAT_PROVIDER) ?: "",
            edittext_chat_message.text.toString().trim(),
            currentUser?.id ?: "empty-id",
            currentUser?.nickname ?: "John Doe",
            currentUser?.userPic,
            isFromMe = true,
            image_width = 100,
            image_height = 100
        ).let {
            sentMessageListener?.invoke(it.toLiveLikeChatMessage())
            viewModel?.apply {
                displayChatMessage(it)
                val hasExternalImage = it.message.findImages().countMatches() > 0
                if (hasExternalImage) {
                    uploadAndPostImage(context, it, timeData)
                } else {
                    chatListener?.onChatMessageSend(it, timeData)
                }
                edittext_chat_message.setText("")
                snapToLive()
                analyticsService.trackMessageSent(
                    it.id,
                    it.message,
                    hasExternalImage
                )
            }
        }
    }

    private fun hideSnapToLive() {
        logDebug { "Chat hide Snap to Live: $showingSnapToLive" }
        if (!showingSnapToLive)
            return
        showingSnapToLive = false
        snap_live.visibility = View.GONE
        animateSnapToLiveButton()
    }

    private fun showSnapToLive() {
        logDebug { "Chat show Snap to Live: $showingSnapToLive" }
        if (showingSnapToLive)
            return
        showingSnapToLive = true
        snap_live.visibility = View.VISIBLE
        animateSnapToLiveButton()
    }

    private fun animateSnapToLiveButton() {
        snapToLiveAnimation?.cancel()

        val translateAnimation = ObjectAnimator.ofFloat(
            snap_live,
            "translationY",
            if (showingSnapToLive) 0f else dpToPx(if (displayUserProfile) SNAP_TO_LIVE_ANIMATION_DESTINATION else 10).toFloat()
        )
        translateAnimation?.duration = SNAP_TO_LIVE_ANIMATION_DURATION.toLong()
        val alphaAnimation =
            ObjectAnimator.ofFloat(snap_live, "alpha", if (showingSnapToLive) 1f else 0f)
        alphaAnimation.duration = (SNAP_TO_LIVE_ALPHA_ANIMATION_DURATION).toLong()
        alphaAnimation.addListener(object : Animator.AnimatorListener {
            override fun onAnimationEnd(animation: Animator) {
                snap_live.visibility = if (showingSnapToLive) View.VISIBLE else View.GONE
            }

            override fun onAnimationStart(animation: Animator) {
                snap_live.visibility = if (showingSnapToLive) View.GONE else View.VISIBLE
            }

            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        })

        snapToLiveAnimation = AnimatorSet()
        snapToLiveAnimation?.play(translateAnimation)?.with(alphaAnimation)
        snapToLiveAnimation?.start()
    }

    private fun snapToLive() {
        chatdisplay?.let { rv ->
            hideSnapToLive()
            viewModel?.messageList?.size?.let {
                val lm = rv.layoutManager as LinearLayoutManager
                val lastVisiblePosition = lm.itemCount - lm.findLastVisibleItemPosition()
                if (lastVisiblePosition < SMOOTH_SCROLL_MESSAGE_COUNT_LIMIT) {
                    rv.smoothScrollToPosition(it)
                } else {
                    chatdisplay.postDelayed({
                        rv.scrollToPosition(it - 1)
                    }, 200)
                }
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        wouldUpdateChatInputAccessibiltyFocus()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        chatdisplay.adapter = null
    }
}
