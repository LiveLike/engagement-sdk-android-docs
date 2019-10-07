package com.livelike.engagementsdk.chat

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.support.constraint.ConstraintLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.Spannable
import android.text.TextWatcher
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
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
import com.livelike.engagementsdk.utils.AndroidResource
import com.livelike.engagementsdk.utils.AndroidResource.Companion.dpToPx
import com.livelike.engagementsdk.utils.animators.buildScaleAnimator
import com.livelike.engagementsdk.utils.logError
import com.livelike.engagementsdk.widget.view.loadImage
import java.util.Date
import kotlinx.android.synthetic.main.chat_input.view.button_chat_send
import kotlinx.android.synthetic.main.chat_input.view.button_emoji
import kotlinx.android.synthetic.main.chat_input.view.edittext_chat_message
import kotlinx.android.synthetic.main.chat_input.view.user_profile_display_LL
import kotlinx.android.synthetic.main.chat_user_profile_bar.view.gamification_badge_iv
import kotlinx.android.synthetic.main.chat_user_profile_bar.view.pointView
import kotlinx.android.synthetic.main.chat_user_profile_bar.view.rank_label
import kotlinx.android.synthetic.main.chat_user_profile_bar.view.rank_value
import kotlinx.android.synthetic.main.chat_user_profile_bar.view.user_profile_tv
import kotlinx.android.synthetic.main.chat_view.view.chatInput
import kotlinx.android.synthetic.main.chat_view.view.chatdisplay
import kotlinx.android.synthetic.main.chat_view.view.loadingSpinner
import kotlinx.android.synthetic.main.chat_view.view.snap_live
import kotlinx.android.synthetic.main.chat_view.view.sticker_keyboard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.regex.Matcher
import java.util.regex.Pattern
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
class ChatView(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {
    companion object {
        const val SNAP_TO_LIVE_ANIMATION_DURATION = 400F
        const val SNAP_TO_LIVE_ALPHA_ANIMATION_DURATION = 320F
        const val SNAP_TO_LIVE_ANIMATION_DESTINATION = 50
        private const val CHAT_MINIMUM_SIZE_DP = 292
    }

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

        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.ChatView,
            0, 0).apply {
            try {
                displayUserProfile = getBoolean(R.styleable.ChatView_displayUserProfile, false)
            } finally {
                recycle()
            }
        }

        initView(context)
    }

    private fun initView(context: Context) {
        LayoutInflater.from(context).inflate(R.layout.chat_view, this, true)
        user_profile_display_LL.visibility = if (displayUserProfile) View.VISIBLE else View.GONE
    }

    fun setSession(session: LiveLikeContentSession) {
        hideGamification()
        this.session = session.apply {
            analyticService.trackOrientationChange(resources.configuration.orientation == 1)
        }

        viewModel?.apply {
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
                                    6300)
                            }
                        } else {
                            pointView.apply {
                                postDelayed(
                                    {
                                        startAnimationFromTop(programRank.points)
                                        showUserRank(programRank)
                                    },
                                    1000)
                            }
                        }
                }
            }
            animationEventsStream.subscribe(javaClass.simpleName) {
                if (it == ViewAnimationEvents.BADGE_COLLECTED) {
                    programRepository.programGamificationProfileStream.latest()?.let { programGamificationProfile ->
                        wouldShowBadge(programGamificationProfile, true) }
                }
            }
        }

        sticker_keyboard.setProgram(session.contentSessionId()){
            if(it.isNullOrEmpty()){
                button_emoji?.visibility = View.GONE
                sticker_keyboard?.visibility = View.GONE
            }else {
                button_emoji?.visibility = View.VISIBLE
            }
        }
        // used to pass the shortcode to the keyboard
        sticker_keyboard.setOnClickListener(object : FragmentClickListener {
            override fun onClick(sticker: Sticker) {
                val textToInsert = ":${sticker.shortcode}:"
                val start = max(edittext_chat_message.selectionStart, 0)
                val end = max(edittext_chat_message.selectionEnd, 0)
                edittext_chat_message.text.replace( // replace selected text or start where the cursor is
                    min(start, end), max(start, end),
                    textToInsert, 0, textToInsert.length)
            }
        })

        edittext_chat_message.addTextChangedListener(object : TextWatcher{
            override fun afterTextChanged(s: Editable?) {
                val existingSpans = s?.getSpans(0, s.length, ImageSpan::class.java)
                val existingSpanPositions = ArrayList<Int>(existingSpans?.size ?: 0)
                existingSpans?.forEach { imageSpan ->
                    existingSpanPositions.add(s.getSpanStart(imageSpan))
                }
                val matcher = getMatcher(s.toString())

                while (matcher.find()){

                    val url = sticker_keyboard.getUrlFromShortcode(matcher.group().replace(":",""))

                    val startIndex = matcher.start()
                    val end = matcher.end()

                    if(url.isEmpty() // No url for this shortcode
                        || existingSpanPositions.contains(startIndex) // The shortcode has already been replaced by an image
                    ){
                        continue
                    }

                    Glide.with(this@ChatView)
                        .load(url)
                        .into( object: CustomTarget<Drawable>(50,50){
                            override fun onLoadCleared(placeholder: Drawable?) {
                            }

                            override fun onResourceReady(
                                resource: Drawable,
                                transition: Transition<in Drawable>?
                            ) {
                                if (resource.intrinsicWidth > edittext_chat_message.width) {
                                    val aspectRatio = resource.intrinsicHeight.toFloat() / resource.intrinsicWidth.toFloat()
                                    resource.setBounds(0, 0, edittext_chat_message.width, (aspectRatio * edittext_chat_message.width).toInt())
                                } else {
                                    resource.setBounds(0, 0, resource.intrinsicWidth, resource.intrinsicHeight)
                                }
                                val span = ImageSpan(resource, url, DynamicDrawableSpan.ALIGN_BASELINE)
                                s?.setSpan(span, startIndex, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                            }
                        } )
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

        })

        button_emoji.setOnClickListener {
            sticker_keyboard.visibility = if(sticker_keyboard.visibility==View.GONE) View.VISIBLE else View.GONE
        }
    }

    private fun getMatcher(text: String) : Matcher
    {
        val regex = ":[^:\\s]*:"
        val pattern = Pattern.compile (regex)
        return pattern.matcher (text)
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
                gamification_badge_iv.buildScaleAnimator(0f, 1f, 500).start()
            }
        }
    }

    private fun hideGamification() {
        pointView.visibility = View.GONE
        rank_label.visibility = View.GONE
        rank_value.visibility = View.GONE
        gamification_badge_iv.visibility = View.GONE
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
            v is EditText &&
            !v.javaClass.name.startsWith("android.webkit.")
        ) {
            val scrcoords = IntArray(2)
            v.getLocationOnScreen(scrcoords)
            val x = ev.rawX + v.left - scrcoords[0]
            val y = ev.rawY + v.top - scrcoords[1]

            if (x < v.left || x > v.right || y < v.top || y > v.bottom)
                hideKeyboard(KeyboardHideReason.TAP_OUTSIDE)
        }
        return super.dispatchTouchEvent(ev)
    }

    private var isLastItemVisible = false
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
                    isLastItemVisible = if (totalItemCount > 0 && endHasBeenReached) {
                        hideSnapToLive()
                        true
                    } else {
                        showSnapToLive()
                        false
                    }
                }
            })
        }

        snap_live.setOnClickListener {
            snapToLive()
        }

        button_chat_send.let { buttonChat ->
            buttonChat.setOnClickListener { sendMessageNow() }
            buttonChat.visibility = View.GONE
            buttonChat.isEnabled = false

            edittext_chat_message.apply {
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

                    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

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
    }

    private fun sendMessageNow() {
        if (edittext_chat_message.text.isBlank()) {
            // Do nothing if the message is blank or empty
            return
        }

        hideKeyboard(KeyboardHideReason.MESSAGE_SENT)
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
            if (showingSnapToLive) 0f else dpToPx(SNAP_TO_LIVE_ANIMATION_DESTINATION).toFloat()
        )
        translateAnimation?.duration = SNAP_TO_LIVE_ANIMATION_DURATION.toLong()
        val alphaAnimation = ObjectAnimator.ofFloat(snap_live, "alpha", if (showingSnapToLive) 1f else 0f)
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
            viewModel?.chatAdapter?.itemCount?.let {
                rv.smoothScrollToPosition(it)
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        chatdisplay.adapter = null
    }
}
