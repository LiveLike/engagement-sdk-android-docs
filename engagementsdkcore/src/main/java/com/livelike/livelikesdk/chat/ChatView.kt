package com.livelike.livelikesdk.chat

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.support.constraint.ConstraintLayout
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
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
import android.widget.AbsListView
import android.widget.EditText
import com.livelike.engagementsdkapi.ChatAdapter
import com.livelike.engagementsdkapi.ChatCell
import com.livelike.engagementsdkapi.ChatCellFactory
import com.livelike.engagementsdkapi.ChatMessage
import com.livelike.engagementsdkapi.ChatRenderer
import com.livelike.engagementsdkapi.ChatViewModel
import com.livelike.engagementsdkapi.EpochTime
import com.livelike.engagementsdkapi.KeyboardHideReason
import com.livelike.engagementsdkapi.KeyboardType
import com.livelike.engagementsdkapi.LiveLikeContentSession
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
import kotlinx.android.synthetic.main.default_chat_cell.view.chatBackground
import kotlinx.android.synthetic.main.default_chat_cell.view.chatMessage
import kotlinx.android.synthetic.main.default_chat_cell.view.chat_nickname
import kotlinx.android.synthetic.main.default_chat_cell.view.floatingUi

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

class ChatView(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs), ChatRenderer {
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
            viewModel?.chatAdapter =
                ChatAdapter(
                session,
                    DefaultChatCellFactory(context, null)
                )
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
            viewModel?.chatAdapter?.addMessage(message)
        }
    }

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
    private fun setDataSource(chatAdapter: ChatAdapter) {
        chatdisplay.adapter = chatAdapter

        chatdisplay.setOnItemLongClickListener { adapterView, view, i, l ->
            // Hide all floating menus
            for (iterator in 0 until adapterView.count) {
                (adapterView.getItemAtPosition(iterator) as DefaultChatCell).hideFloatingUI()
            }
            // Show clicked item floating menu
            (adapterView.getItemAtPosition(i) as DefaultChatCell).showFloatingUI()
            true
        }

        chatdisplay.setOnItemClickListener { adapterView, view, i, l ->
            // Hide all floating menus
            for (iterator in 0 until adapterView.count) {
                (adapterView.getItemAtPosition(iterator) as DefaultChatCell).hideFloatingUI()
            }
        }

        chatdisplay.setOnScrollListener(object : AbsListView.OnScrollListener {
            override fun onScroll(
                view: AbsListView?,
                firstVisibleItem: Int,
                visibleItemCount: Int,
                totalItemCount: Int
            ) {
                val lastPos = view?.lastVisiblePosition ?: 0
                if (lastPos > 0) {
                    viewModel?.chatLastPos = view?.selectedItemPosition
                }
                if (lastPos >= totalItemCount - 3)
                    hideSnapToLive()
                else
                    showSnapToLive()
            }

            override fun onScrollStateChanged(view: AbsListView?, scrollState: Int) {}
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

        viewModel?.chatLastPos?.let {
            Handler(Looper.getMainLooper()).post {
                chatdisplay.setSelection(it)
            }
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
            session?.currentUser?.sessionId ?: "empty-id",
            session?.currentUser?.userName ?: "John Doe",
            UUID.randomUUID().toString(),
            Date(timeData.timeSinceEpochInMs).toString()
        )
        viewModel?.chatAdapter?.addMessage(newMessage)

        hideLoadingSpinner()
        snapToLive()
        edittext_chat_message.setText("")
        viewModel?.chatListener?.onChatMessageSend(newMessage, timeData)
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
        chatdisplay.smoothScrollToPositionFromTop(viewModel?.chatAdapter?.count?.minus(1) ?: 0, 0, 500)
    }
}

internal class DefaultChatCellFactory(val context: Context, cellattrs: AttributeSet?) :
    ChatCellFactory {
    private val attrs = cellattrs

    override fun getCell(): ChatCell {
        return DefaultChatCell(context, attrs)
    }
}

internal class DefaultChatCell(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs), ChatCell {
    private var message: ChatMessage? = null
    private val dialogOptions = listOf(
        "Block this user" to { msg: ChatMessage ->
            logError { "Blocking ${msg.message}" }
        },
        "Report message" to { msg: ChatMessage ->
            logError { "Reporting ${msg.message}" }
        })

    init {
        LayoutInflater.from(context)
            .inflate(com.livelike.livelikesdk.R.layout.default_chat_cell, this, true)
        floatingUi.visibility = View.GONE
    }

    fun showFloatingUI() {
        floatingUi.visibility = View.VISIBLE
        chatBackground.alpha = 0.5f
        context?.let { ctx ->
            AlertDialog.Builder(ctx).apply {
                setTitle("A problem?")
                setItems(dialogOptions.map { it.first }.toTypedArray()) { dialog, which ->
                    message?.let {
                        dialogOptions[which].second.invoke(it)
                    }
                }
                create()
            }.show()
        }
    }

    fun hideFloatingUI() {
        floatingUi.visibility = View.GONE
        chatBackground.alpha = 1f
    }

    override fun setMessage(
        message: ChatMessage?,
        isMe: Boolean?
    ) {
        this.message = message
        message?.apply {
            if (isMe == true) {
                chat_nickname.setTextColor(
                    ContextCompat.getColor(
                        context,
                        com.livelike.livelikesdk.R.color.livelike_openChatNicknameMe
                    )
                )
                chat_nickname.text = "(Me) ${message.senderDisplayName}"
            } else {
                chat_nickname.setTextColor(
                    ContextCompat.getColor(
                        context,
                        com.livelike.livelikesdk.R.color.livelike_openChatNicknameOther
                    )
                )
                chat_nickname.text = message.senderDisplayName
            }
            chatMessage.text = message.message
        }
    }

    override fun getView(): View {
        floatingUi?.setOnClickListener {
            floatingUi?.visibility = View.GONE
            chatBackground?.alpha = 1f
        }
        return this
    }
}
