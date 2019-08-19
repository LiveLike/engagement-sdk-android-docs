package com.livelike.livelikesdk.chat

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
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
import com.livelike.engagementsdkapi.EpochTime
import com.livelike.engagementsdkapi.KeyboardHideReason
import com.livelike.engagementsdkapi.KeyboardType
import com.livelike.livelikesdk.LiveLikeContentSession
import com.livelike.livelikesdk.utils.AndroidResource
import com.livelike.livelikesdk.utils.AndroidResource.Companion.dpToPx
import com.livelike.livelikesdk.utils.logError
import java.util.Date
import java.util.UUID
import kotlinx.android.synthetic.main.chat_input.view.button_chat_send
import kotlinx.android.synthetic.main.chat_input.view.edittext_chat_message
import kotlinx.android.synthetic.main.chat_view.view.chatdisplay
import kotlinx.android.synthetic.main.chat_view.view.loadingSpinner
import kotlinx.android.synthetic.main.chat_view.view.snap_live
import kotlinx.coroutines.GlobalScope
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

class ChatViewModel() {
    var chatListener: ChatEventListener? = null
    var chatAdapter: ChatRecyclerAdapter? = null
}

class ChatView(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs),
    ChatRenderer {
    companion object {
        const val SNAP_TO_LIVE_ANIMATION_DURATION = 400F
        const val SNAP_TO_LIVE_ALPHA_ANIMATION_DURATION = 320F
        const val SNAP_TO_LIVE_ANIMATION_DESTINATION = 50
        private const val CHAT_MINIMUM_SIZE_DP = 292
    }

    override val chatContext: Context = context

    private var session: LiveLikeContentSession? = null
    private var snapToLiveAnimation: AnimatorSet? = null
    private var showingSnapToLive: Boolean = false

    private val viewModel: ChatViewModel?
        get() = session?.chatViewModel

    init {
        (context as Activity).window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
                    or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        ) // INFO: Adjustresize doesn't work with Fullscreen app.. See issue https://stackoverflow.com/questions/7417123/android-how-to-adjust-layout-in-full-screen-mode-when-softkeyboard-is-visible

        LayoutInflater.from(context).inflate(com.livelike.livelikesdk.R.layout.chat_view, this, true)
    }

    fun setSession(session: LiveLikeContentSession) {
        this.session = session
        session.chatRenderer = this

        if (viewModel?.chatAdapter == null) {
            showLoadingSpinner()
            viewModel?.chatAdapter = ChatRecyclerAdapter()
        }
        viewModel?.chatAdapter?.let {
            setDataSource(it)
        }
        session.analyticService.trackOrientationChange(resources.configuration.orientation == 1)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthDp = AndroidResource.pxToDp(width)
        if (widthDp < 292 && widthDp != 0) {
            logError { "[CONFIG ERROR] Current ChatView Width is $widthDp, it must be more than 292dp or won't display on the screen." }
            setMeasuredDimension(0, 0)
            return
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun displayChatMessage(message: ChatMessage) {
        hideLoadingSpinner()
        Handler(Looper.getMainLooper()).post {
            messageList.add(message.apply { isFromMe = session?.currentUser?.sessionId == senderId })
            viewModel?.chatAdapter?.submitList(messageList)
        }
    }

    private val messageList = mutableListOf<ChatMessage>()

    override fun loadComplete() {
        hideLoadingSpinner()
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

    /**
     *  Sets the data source for this view.
     *  @param chatAdapter ChatAdapter used for creating this view.
     */
    private fun setDataSource(chatAdapter: ChatRecyclerAdapter) {
        chatdisplay.adapter = chatAdapter

        chatdisplay.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(
                rv: RecyclerView,
                dx: Int,
                dy: Int
            ) {
                val l = (rv.layoutManager as LinearLayoutManager)
                if (l.findLastVisibleItemPosition() < l.itemCount - 3)
                    showSnapToLive()
                else
                    hideSnapToLive()
            }
        })

        snap_live.setOnClickListener {
            snapToLive()
        }

        button_chat_send.visibility = View.GONE

        edittext_chat_message.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable) {
                if (s.isNotEmpty()) {
                    button_chat_send.isEnabled = true
                    button_chat_send.visibility = View.VISIBLE
                } else {
                    button_chat_send.isEnabled = false
                    button_chat_send.visibility = View.GONE
                }
            }
        })

        edittext_chat_message.setOnFocusChangeListener { _, hasFocus ->
            run {
                if (hasFocus) {
                    session?.analyticService?.trackKeyboardOpen(KeyboardType.STANDARD)
                }
            }
        }

        // Send message on tap Enter
        edittext_chat_message.setOnEditorActionListener { _, actionId, event ->
            if ((event != null && event.keyCode == KeyEvent.KEYCODE_ENTER) ||
                (actionId == EditorInfo.IME_ACTION_SEND)
            ) {
                sendMessageNow()
            }
            false
        }

        button_chat_send.isEnabled = false
        button_chat_send.setOnClickListener {
            sendMessageNow()
        }
    }

    private fun showLoadingSpinner() {
        loadingSpinner.visibility = View.VISIBLE
    }

    private fun hideLoadingSpinner() {
        loadingSpinner.visibility = View.GONE
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
        // Do nothing if the message is blank or empty
        if (edittext_chat_message.text.isBlank())
            return
        val hideMethod = KeyboardHideReason.MESSAGE_SENT
        hideKeyboard(hideMethod)
        val timeData = session?.getPlayheadTime() ?: EpochTime(0)
        val newMessage = ChatMessage(
            edittext_chat_message.text.toString(),
            session?.currentUser?.sessionId ?: "empty-id", // TODO: User the user profile ID here instead
            session?.currentUser?.userName ?: "John Doe",
            UUID.randomUUID().toString(),
            Date(timeData.timeSinceEpochInMs).toString(),
            true
        )
        messageList.add(newMessage)
        viewModel?.chatAdapter?.submitList(messageList)

        hideLoadingSpinner()
        edittext_chat_message.setText("")
        viewModel?.chatListener?.onChatMessageSend(newMessage, timeData)
        GlobalScope.launch {
            delay(200)
            snapToLive()
        }
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
                snap_live.visibility = View.VISIBLE
            }

            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        })

        snapToLiveAnimation = AnimatorSet()
        snapToLiveAnimation?.play(translateAnimation)?.with(alphaAnimation)
        snapToLiveAnimation?.start()
    }

    private fun snapToLive() {
        chatdisplay.layoutManager?.itemCount?.let { chatdisplay.smoothScrollToPosition(it) }
    }
}
