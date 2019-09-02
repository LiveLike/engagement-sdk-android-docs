package com.livelike.engagementsdk.chat

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.support.constraint.ConstraintLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import com.livelike.engagementsdk.ContentSession
import com.livelike.engagementsdk.EpochTime
import com.livelike.engagementsdk.KeyboardHideReason
import com.livelike.engagementsdk.KeyboardType
import com.livelike.engagementsdk.LiveLikeContentSession
import com.livelike.engagementsdk.LiveLikeUser
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.utils.AndroidResource
import com.livelike.engagementsdk.utils.AndroidResource.Companion.dpToPx
import com.livelike.engagementsdk.utils.logError
import java.util.Date
import kotlinx.android.synthetic.main.chat_input.view.button_chat_send
import kotlinx.android.synthetic.main.chat_input.view.edittext_chat_message
import kotlinx.android.synthetic.main.chat_input.view.user_profile_display_LL
import kotlinx.android.synthetic.main.chat_user_profile_bar.view.pointView
import kotlinx.android.synthetic.main.chat_user_profile_bar.view.user_profile_tv
import kotlinx.android.synthetic.main.chat_view.view.chatInput
import kotlinx.android.synthetic.main.chat_view.view.chatdisplay
import kotlinx.android.synthetic.main.chat_view.view.loadingSpinner
import kotlinx.android.synthetic.main.chat_view.view.snap_live
import kotlinx.android.synthetic.main.chat_view.view.topBarGradient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
        get() = (session as ContentSession)?.chatViewModel

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
            programRepository.programRankStream.subscribe(javaClass.simpleName) {
                it?.let { programRank ->
                        pointView.startAnimation(programRank.points)
                }
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
        topBarGradient.visibility = View.GONE
    }

    private fun hideLoadingSpinner() {
        loadingSpinner.visibility = View.GONE
        chatInput.visibility = View.VISIBLE
        chatdisplay.visibility = View.VISIBLE
        topBarGradient.visibility = View.VISIBLE
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
