package com.livelike.engagementsdk.chat

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.support.constraint.ConstraintLayout
import android.support.v4.content.ContextCompat
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.Spannable
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import com.livelike.engagementsdk.ContentSession
import com.livelike.engagementsdk.EpochTime
import com.livelike.engagementsdk.KeyboardHideReason
import com.livelike.engagementsdk.KeyboardType
import com.livelike.engagementsdk.LiveLikeContentSession
import com.livelike.engagementsdk.LiveLikeUser
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.ViewAnimationEvents
import com.livelike.engagementsdk.core.exceptionhelpers.getTargetObject
import com.livelike.engagementsdk.data.models.ProgramGamificationProfile
import com.livelike.engagementsdk.stickerKeyboard.FragmentClickListener
import com.livelike.engagementsdk.stickerKeyboard.Sticker
import com.livelike.engagementsdk.stickerKeyboard.StickerKeyboardView
import com.livelike.engagementsdk.stickerKeyboard.replaceWithStickers
import com.livelike.engagementsdk.utils.AndroidResource
import com.livelike.engagementsdk.utils.AndroidResource.Companion.dpToPx
import com.livelike.engagementsdk.utils.animators.buildScaleAnimator
import com.livelike.engagementsdk.utils.logError
import com.livelike.engagementsdk.widget.view.loadImage
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
import kotlinx.android.synthetic.main.chat_view.view.loadingSpinner
import kotlinx.android.synthetic.main.chat_view.view.snap_live
import kotlinx.android.synthetic.main.chat_view.view.sticker_keyboard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date
import kotlin.math.max
import kotlin.math.min

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
class ChatView(context: Context, private val attrs: AttributeSet?) :
    ConstraintLayout(context, attrs) {
    companion object {
        const val SNAP_TO_LIVE_ANIMATION_DURATION = 400F
        const val SNAP_TO_LIVE_ALPHA_ANIMATION_DURATION = 320F
        const val SNAP_TO_LIVE_ANIMATION_DESTINATION = 50
        private const val CHAT_MINIMUM_SIZE_DP = 292
        private const val SMOOTH_SCROLL_MESSAGE_COUNT_LIMIT = 100
    }

    private var chatPaddingLeft: Int
    private var chatPaddingRight: Int
    private var chatPaddingTop: Int
    private var chatPaddingBottom: Int
    private var chatMarginLeft: Int
    private var chatMarginRight: Int
    private var chatMarginTop: Int
    private var chatMarginBottom: Int
    private var chatWidth: Int
    private var chatInputTextSize: Int
    private var chatBubbleBackgroundRes: Drawable?
    private var chatViewBackgroundRes: Drawable?
    private var chatReactionBackgroundRes: Drawable?
    private var chatInputBackgroundRes: Drawable?
    private var chatInputViewBackgroundRes: Drawable?
    private var chatDisplayBackgroundRes: Drawable?
    private var chatInputDrawableRight: Drawable?
    private var chatMessageColor: Int
    private var chatReactionBackgroundColor: Int
    private var chatInputTextColor: Int
    private var chatInputHintTextColor: Int
    private var chatOtherNickNameColor: Int
    private var chatNickNameColor: Int
    private var chatReactionX:Int=0
    private var chatReactionY:Int=0
    private var chatReactionElevation:Float=0f
    private var chatReactionRadius:Float=0f
    private var chatReactionPadding:Int=0
    private val uiScope = CoroutineScope(Dispatchers.Main)

    private var session: LiveLikeContentSession? = null
    private var snapToLiveAnimation: AnimatorSet? = null
    private var showingSnapToLive: Boolean = false

    private var currentUser: LiveLikeUser? = null

    /** Boolean option to enable / disable the profile display inside chat view */
    var displayUserProfile: Boolean = false
        set(value) {
            field = value
            user_profile_display_LL?.apply {
                visibility = if (value) View.VISIBLE else View.GONE
            }
        }

    private val viewModel: ChatViewModel?
        get() = (session.getTargetObject() as ContentSession?)?.chatViewModel

    init {
        (context as Activity).window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
                    or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        ) // INFO: Adjustresize doesn't work with Fullscreen app.. See issue https://stackoverflow.com/questions/7417123/android-how-to-adjust-layout-in-full-screen-mode-when-softkeyboard-is-visible

        context.obtainStyledAttributes(
            attrs,
            R.styleable.ChatView,
            0, 0
        ).apply {
            try {
                displayUserProfile = getBoolean(R.styleable.ChatView_displayUserProfile, false)
                chatNickNameColor = getColor(
                    R.styleable.ChatView_usernameColor,
                    ContextCompat.getColor(context, R.color.livelike_openChatNicknameMe)
                )
                chatOtherNickNameColor = getColor(
                    R.styleable.ChatView_otherUsernameColor,
                    ContextCompat.getColor(context, R.color.livelike_openChatNicknameOther)
                )
                chatMessageColor = getColor(
                    R.styleable.ChatView_messageColor,
                    ContextCompat.getColor(
                        context,
                        R.color.livelike_default_chat_cell_message_color
                    )
                )

                val colorBubbleValue = TypedValue()
                getValue(R.styleable.ChatView_chatBubbleBackground, colorBubbleValue)

                chatBubbleBackgroundRes = when {
                    colorBubbleValue.type == TypedValue.TYPE_REFERENCE -> ContextCompat.getDrawable(
                        context,
                        getResourceId(
                            R.styleable.ChatView_chatBubbleBackground,
                            R.drawable.ic_chat_message_bubble_rounded_rectangle
                        )
                    )
                    colorBubbleValue.type == TypedValue.TYPE_NULL -> ContextCompat.getDrawable(
                        context,
                        R.drawable.ic_chat_message_bubble_rounded_rectangle
                    )
                    else -> ColorDrawable(colorBubbleValue.data)
                }


                val inputDrawableRight = TypedValue()
                getValue(R.styleable.ChatView_chatInputDrawableRight, inputDrawableRight)

                chatInputDrawableRight = when {
                    inputDrawableRight.type == TypedValue.TYPE_REFERENCE || inputDrawableRight.type == TypedValue.TYPE_STRING -> ContextCompat.getDrawable(
                        context,
                        getResourceId(
                            R.styleable.ChatView_chatInputDrawableRight,
                            R.drawable.ic_chat_emoji_ios_category_smileysandpeople
                        )
                    )
                    else -> ContextCompat.getDrawable(
                        context,
                        R.drawable.ic_chat_emoji_ios_category_smileysandpeople
                    )
                }


                val colorReactionValue = TypedValue()
                getValue(R.styleable.ChatView_chatReactionBackground, colorReactionValue)

                chatReactionBackgroundRes = when {
                    colorReactionValue.type == TypedValue.TYPE_REFERENCE || colorReactionValue.type == TypedValue.TYPE_STRING -> ContextCompat.getDrawable(
                        context,
                        getResourceId(
                            R.styleable.ChatView_chatReactionBackground,
                            android.R.color.transparent
                        )
                    )
                    colorReactionValue.type == TypedValue.TYPE_NULL -> ContextCompat.getDrawable(
                        context,
                        android.R.color.transparent
                    )
                    else -> ColorDrawable(colorReactionValue.data)
                }


                val colorViewValue = TypedValue()
                getValue(R.styleable.ChatView_chatViewBackground, colorViewValue)

                chatViewBackgroundRes = when {
                    colorViewValue.type == TypedValue.TYPE_REFERENCE -> ContextCompat.getDrawable(
                        context,
                        getResourceId(
                            R.styleable.ChatView_chatViewBackground,
                            android.R.color.transparent
                        )
                    )
                    colorViewValue.type == TypedValue.TYPE_NULL -> ColorDrawable(Color.TRANSPARENT)
                    else -> ColorDrawable(colorViewValue.data)
                }

                val colorChatDisplayValue = TypedValue()
                getValue(R.styleable.ChatView_chatDisplayBackground, colorChatDisplayValue)

                chatDisplayBackgroundRes = when {
                    colorChatDisplayValue.type == TypedValue.TYPE_REFERENCE || colorChatDisplayValue.type == TypedValue.TYPE_STRING -> ContextCompat.getDrawable(
                        context,
                        getResourceId(
                            R.styleable.ChatView_chatDisplayBackground,
                            android.R.color.transparent
                        )
                    )
                    colorChatDisplayValue.type == TypedValue.TYPE_NULL -> ColorDrawable(Color.TRANSPARENT)
                    else -> ColorDrawable(colorChatDisplayValue.data)
                }

                val colorInputBackgroundValue = TypedValue()
                getValue(R.styleable.ChatView_chatInputBackground, colorInputBackgroundValue)

                chatInputBackgroundRes = when {
                    colorInputBackgroundValue.type == TypedValue.TYPE_REFERENCE || colorInputBackgroundValue.type == TypedValue.TYPE_STRING -> ContextCompat.getDrawable(
                        context,
                        getResourceId(
                            R.styleable.ChatView_chatInputBackground,
                            R.drawable.ic_chat_input
                        )
                    )
                    colorInputBackgroundValue.type == TypedValue.TYPE_NULL -> ContextCompat.getDrawable(
                        context,
                        R.drawable.ic_chat_input
                    )
                    else -> ColorDrawable(colorInputBackgroundValue.data)
                }

                val colorInputViewBackgroundValue = TypedValue()
                getValue(
                    R.styleable.ChatView_chatInputViewBackground,
                    colorInputViewBackgroundValue
                )

                chatInputViewBackgroundRes = when {
                    colorInputViewBackgroundValue.type == TypedValue.TYPE_REFERENCE -> ContextCompat.getDrawable(
                        context,
                        getResourceId(
                            R.styleable.ChatView_chatInputViewBackground,
                            android.R.color.transparent
                        )
                    )
                    colorInputViewBackgroundValue.type == TypedValue.TYPE_NULL -> ColorDrawable(
                        ContextCompat.getColor(context, android.R.color.transparent)
                    )
                    else -> ColorDrawable(colorInputViewBackgroundValue.data)
                }

                chatInputTextColor = getColor(
                    R.styleable.ChatView_chatInputTextColor,
                    ContextCompat.getColor(context, R.color.livelike_chat_input_text_color)
                )
                chatInputHintTextColor = getColor(
                    R.styleable.ChatView_chatInputTextHintColor,
                    ContextCompat.getColor(context, R.color.livelike_chat_input_text_color)
                )

                chatWidth = getLayoutDimension(
                    R.styleable.ChatView_chatWidth,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )

                chatInputTextSize = getDimensionPixelSize(R.styleable.ChatView_chatInputTextSize,resources.getDimensionPixelSize(R.dimen.livelike_default_chat_input_text_size))
                chatReactionX = getDimensionPixelSize(R.styleable.ChatView_chatReactionXPosition,dpToPx(8))
                chatReactionY = getDimensionPixelSize(R.styleable.ChatView_chatReactionYPosition,dpToPx(40))
                chatReactionElevation = getDimensionPixelSize(R.styleable.ChatView_chatReactionElevation,dpToPx(0)).toFloat()
                chatReactionRadius = getDimensionPixelSize(R.styleable.ChatView_chatReactionRadius,dpToPx(0)).toFloat()
                chatReactionPadding = getDimensionPixelSize(R.styleable.ChatView_chatReactionPadding,dpToPx(6))

                chatReactionBackgroundColor = getColor(
                    R.styleable.ChatView_chatReactionBackgroundColor,
                    ContextCompat.getColor(context, android.R.color.transparent)
                )

                chatPaddingLeft = getDimensionPixelOffset(
                    R.styleable.ChatView_chatPaddingLeft,
                    resources.getDimension(R.dimen.livelike_default_chat_cell_padding_left).toInt()
                )
                chatPaddingRight = getDimensionPixelOffset(
                    R.styleable.ChatView_chatPaddingRight,
                    resources.getDimension(R.dimen.livelike_default_chat_cell_padding_right).toInt()
                )
                chatPaddingTop = getDimensionPixelOffset(
                    R.styleable.ChatView_chatPaddingTop,
                    resources.getDimension(R.dimen.livelike_default_chat_cell_padding_top).toInt()
                )
                chatPaddingBottom = getDimensionPixelOffset(
                    R.styleable.ChatView_chatPaddingBottom,
                    resources.getDimension(R.dimen.livelike_default_chat_cell_padding_bottom).toInt()
                )

                chatMarginLeft = getDimensionPixelOffset(
                    R.styleable.ChatView_chatMarginLeft,
                    convertDpToPixel(8)
                )
                chatMarginRight = getDimensionPixelOffset(
                    R.styleable.ChatView_chatMarginRight,
                    convertDpToPixel(8)
                )
                chatMarginTop =
                    getDimensionPixelOffset(R.styleable.ChatView_chatMarginTop, convertDpToPixel(4))
                chatMarginBottom = getDimensionPixelOffset(
                    R.styleable.ChatView_chatMarginBottom,
                    convertDpToPixel(4)
                )
                chatMarginBottom = getDimensionPixelOffset(
                    R.styleable.ChatView_chatMarginBottom,
                    convertDpToPixel(4)
                )

            } finally {
                recycle()
            }
        }

        initView(context)
    }

    private fun convertDpToPixel(dp: Int): Int {
        return (dp * (resources.displayMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT))
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
        chat_view.background = chatViewBackgroundRes
        chatDisplayBackgroundRes?.let {
            chatdisplay.background = it
        }
        chat_input_background.background = chatInputViewBackgroundRes
        chat_input_border.background = chatInputBackgroundRes
        edittext_chat_message.setTextColor(chatInputTextColor)
        edittext_chat_message.setHintTextColor(chatInputHintTextColor)
        edittext_chat_message.setTextSize(TypedValue.COMPLEX_UNIT_PX,chatInputTextSize.toFloat())
        button_emoji.setImageDrawable(chatInputDrawableRight)
    }

    fun setSession(session: LiveLikeContentSession) {
        hideGamification()
        this.session = session.apply {
            analyticService.trackOrientationChange(resources.configuration.orientation == 1)
        }

        viewModel?.apply {
            uiScope.launch { chatAdapter.chatReactionRepository.preloadImages(context) }
            chatAdapter.chatNickNameColor = chatNickNameColor
            chatAdapter.chatWidth = chatWidth
            chatAdapter.chatBubbleBackgroundRes = chatBubbleBackgroundRes
            chatAdapter.chatMarginBottom = chatMarginBottom
            chatAdapter.chatMarginLeft = chatMarginLeft
            chatAdapter.chatMarginRight = chatMarginRight
            chatAdapter.chatMarginTop = chatMarginTop
            chatAdapter.chatMessageColor = chatMessageColor
            chatAdapter.chatOtherNickNameColor = chatOtherNickNameColor
            chatAdapter.chatPaddingBottom = chatPaddingBottom
            chatAdapter.chatPaddingLeft = chatPaddingLeft
            chatAdapter.chatPaddingRight = chatPaddingRight
            chatAdapter.chatPaddingTop = chatPaddingTop
            chatAdapter.chatReactionBackgroundRes=chatReactionBackgroundRes
            chatAdapter.chatReactionX=chatReactionX
            chatAdapter.chatReactionY=chatReactionY
            chatAdapter.chatReactionRadius=chatReactionRadius
            chatAdapter.chatReactionElevation=chatReactionElevation
            chatAdapter.chatReactionBackgroundColor=chatReactionBackgroundColor
            chatAdapter.chatReactionPadding=chatReactionPadding

            setDataSource(chatAdapter)
            eventStream.subscribe(javaClass.simpleName) {
                when (it) {
                    ChatViewModel.EVENT_NEW_MESSAGE -> {
                        // Auto scroll if user is looking at the latest messages
                        if (isLastItemVisible) {
                            snapToLive()
                        }
                    }
                    ChatViewModel.EVENT_LOADING_COMPLETE -> {
                        uiScope.launch {
                            hideLoadingSpinner()
                            delay(100)
                            snapToLive()
                        }
                    }
                }
            }
            userStream.subscribe(javaClass.simpleName) {
                currentUser = it
                it?.let {
                    uiScope.launch {
                        user_profile_tv.text = it.nickname
                    }
                }
            }
            programRepository.programGamificationProfileStream.subscribe(javaClass.simpleName) {
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
            animationEventsStream.subscribe(javaClass.simpleName) {
                if (it == ViewAnimationEvents.BADGE_COLLECTED) {
                    programRepository.programGamificationProfileStream.latest()
                        ?.let { programGamificationProfile ->
                            wouldShowBadge(programGamificationProfile, true)
                        }
                }
            }

            initStickerKeyboard(sticker_keyboard, this)

            edittext_chat_message.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    replaceWithStickers(
                        s as Spannable,
                        this@ChatView.context,
                        stickerPackRepository,
                        edittext_chat_message
                    )
                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                }
            })

            button_emoji.setOnClickListener {
                if (sticker_keyboard.visibility == View.GONE) showStickerKeyboard() else hideStickerKeyboard(
                    KeyboardHideReason.CHANGING_KEYBOARD_TYPE
                )
            }
        }
    }

    private fun initStickerKeyboard(
        stickerKeyboardView: StickerKeyboardView,
        chatViewModel: ChatViewModel
    ) {
        stickerKeyboardView.setProgram(chatViewModel.stickerPackRepository) {
            if (it.isNullOrEmpty()) {
                button_emoji?.visibility = View.GONE
                sticker_keyboard?.visibility = View.GONE
            } else {
                button_emoji?.visibility = View.VISIBLE
            }
        }
        // used to pass the shortcode to the keyboard
        stickerKeyboardView.setOnClickListener(object : FragmentClickListener {
            override fun onClick(sticker: Sticker) {
                val textToInsert = ":${sticker.shortcode}:"
                val start = max(edittext_chat_message.selectionStart, 0)
                val end = max(edittext_chat_message.selectionEnd, 0)
                if (edittext_chat_message.text.length + textToInsert.length < 150) {
                    // replace selected text or start where the cursor is
                    edittext_chat_message.text.replace(
                        min(start, end), max(start, end),
                        textToInsert, 0, textToInsert.length
                    )
                }
            }
        })
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
        val v = (context as Activity).currentFocus

        if (v != null &&
            (ev?.action == MotionEvent.ACTION_UP || ev?.action == MotionEvent.ACTION_MOVE) &&
            (v is EditText || v is ChatView) &&
            !v.javaClass.name.startsWith("android.webkit.")
        ) {
            val scrcoords = IntArray(2)
            v.getLocationOnScreen(scrcoords)
            val x = ev.rawX + v.left - scrcoords[0]
            val y = ev.rawY + v.top - scrcoords[1]
            val outsideStickerKeyboardBound =  (v.bottom - sticker_keyboard.height)
            if (y < v.top || y > v.bottom || y < outsideStickerKeyboardBound) {
                hideStickerKeyboard(KeyboardHideReason.TAP_OUTSIDE)
                hideKeyboard(KeyboardHideReason.TAP_OUTSIDE)
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
        if (chatAdapter.itemCount < 1) {
            showLoadingSpinner()
        }
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
            buttonChat.visibility = View.GONE
            buttonChat.isEnabled = false

            edittext_chat_message.apply {
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {
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
                    if (!hasFocus) {
                        hideKeyboard(KeyboardHideReason.TAP_OUTSIDE)
                    }
                }

                // Send message on tap Enter
                setOnEditorActionListener { _, actionId, event ->
                    if ((event != null && event.keyCode == KeyEvent.KEYCODE_ENTER) ||
                        (actionId == EditorInfo.IME_ACTION_SEND)
                    ) {
                        sendMessageNow()
                    }
                    false
                }
            }
        }
    }

    private fun hideStickerKeyboard(reason: KeyboardHideReason) {
        findViewById<StickerKeyboardView>(R.id.sticker_keyboard)?.visibility = View.GONE
        session?.analyticService?.trackKeyboardClose(KeyboardType.STICKER, reason)
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
    }

    private fun hideKeyboard(reason: KeyboardHideReason) {
        val inputManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(
            edittext_chat_message.windowToken,
            0
        )

        if (reason != KeyboardHideReason.MESSAGE_SENT) {
            session?.analyticService?.trackKeyboardClose(KeyboardType.STANDARD, reason)
        }

        setBackButtonInterceptor(this)
    }

    private fun sendMessageNow() {
        if (edittext_chat_message.text.isBlank()) {
            // Do nothing if the message is blank or empty
            return
        }

        hideKeyboard(KeyboardHideReason.MESSAGE_SENT)
        hideStickerKeyboard(KeyboardHideReason.MESSAGE_SENT)
        val timeData = session?.getPlayheadTime() ?: EpochTime(0)

        ChatMessage(
            viewModel?.programRepository?.program?.chatChannel ?: "",
            edittext_chat_message.text.toString(),
            currentUser?.id ?: "empty-id",
            currentUser?.nickname ?: "John Doe",
            Date(timeData.timeSinceEpochInMs).toString(),
            isFromMe = true
        ).let {
            viewModel?.apply {
                displayChatMessage(it)
                chatListener?.onChatMessageSend(it, timeData)
            }
        }
        edittext_chat_message.setText("")
        snapToLive()
    }

    private fun hideSnapToLive() {
        if (!showingSnapToLive)
            return
        showingSnapToLive = false
        snap_live.visibility = View.GONE
        animateSnapToLiveButton()
    }

    private fun showSnapToLive() {
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
            viewModel?.chatAdapter?.itemCount?.let {
                val lm = rv.layoutManager as LinearLayoutManager
                val lastVisiblePosition = lm.itemCount - lm.findLastVisibleItemPosition()
                if (lastVisiblePosition < SMOOTH_SCROLL_MESSAGE_COUNT_LIMIT) {
                    rv.smoothScrollToPosition(it)
                } else {
                    rv.scrollToPosition(it - 1)
                }
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        chatdisplay.adapter = null
    }
}
