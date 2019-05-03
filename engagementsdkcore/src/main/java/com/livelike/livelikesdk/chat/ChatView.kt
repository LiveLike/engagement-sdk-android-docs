package com.livelike.livelikesdk.chat

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.res.ColorStateList
import android.os.Handler
import android.os.Looper
import android.support.constraint.ConstraintLayout
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AbsListView
import android.widget.BaseAdapter
import android.widget.EditText
import com.livelike.engagementsdkapi.ChatEventListener
import com.livelike.engagementsdkapi.ChatMessage
import com.livelike.engagementsdkapi.ChatRenderer
import com.livelike.engagementsdkapi.LiveLikeContentSession
import com.livelike.livelikesdk.analytics.analyticService
import com.livelike.livelikesdk.animation.easing.AnimationEaseAdapter
import com.livelike.livelikesdk.animation.easing.AnimationEaseInterpolator
import com.livelike.livelikesdk.util.AndroidResource.Companion.dpToPx
import com.livelike.livelikesdk.util.AndroidResource.Companion.pxToDp
import com.livelike.livelikesdk.util.logDebug
import com.livelike.livelikesdk.util.logError
import kotlinx.android.synthetic.main.chat_input.view.button_chat_send
import kotlinx.android.synthetic.main.chat_input.view.edittext_chat_message
import kotlinx.android.synthetic.main.chat_view.view.chatdisplay
import kotlinx.android.synthetic.main.chat_view.view.loadingSpinner
import kotlinx.android.synthetic.main.chat_view.view.snap_live
import kotlinx.android.synthetic.main.default_chat_cell.view.chatMessage
import kotlinx.android.synthetic.main.default_chat_cell.view.chat_nickname
import java.util.Date
import java.util.UUID

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
 *  Once the view has been created, provide data source for this view
 *  using [setDataSource]. See [ChatAdapter] class on how to create a data source.
 */

class ChatView(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs), ChatRenderer {
    companion object {
        const val SNAP_TO_LIVE_ANIMATION_DURATION = 400F
        const val SNAP_TO_LIVE_ALPHA_ANIMATION_DURATION = 320F
        const val SNAP_TO_LIVE_ANIMATION_DESTINATION = 50
        private const val CHAT_MINIMUM_SIZE_DP = 292
    }

    override var chatListener: ChatEventListener? = null
    override val chatContext: Context = context

    private val attrs: AttributeSet = attrs!!
    private lateinit var session: LiveLikeContentSession
    private lateinit var chatAdapter: ChatAdapter
    private var snapToLiveAnimation: AnimatorSet? = null
    private var showingSnapToLive: Boolean = false
    private val animationEaseAdapter = AnimationEaseAdapter()
    private var viewRoot: View = LayoutInflater.from(context)
        .inflate(com.livelike.livelikesdk.R.layout.chat_view, this, true)

    private var model: ChatViewModel

    init {
        (context as Activity).window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
                    or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        ) // INFO: Adjustresize doesn't work with Fullscreen app.. See issue https://stackoverflow.com/questions/7417123/android-how-to-adjust-layout-in-full-screen-mode-when-softkeyboard-is-visible
        model = ViewModelProviders.of(context as AppCompatActivity).get(ChatViewModel::class.java)
    }

    private fun verifyViewMinWidth(view: View) {
//        visibility = View.VISIBLE
        view.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
        val width = pxToDp(view.width)
        if (width < CHAT_MINIMUM_SIZE_DP) {
//            visibility = View.GONE
            logError { "The Chat zone is too small to be displayed. Minimum size is $CHAT_MINIMUM_SIZE_DP dp. Measured size here is: $width dp" }
        }
    }

    fun setSession(session: LiveLikeContentSession) {
        this.session = session
        session.chatRenderer = this
        if (model.chatAdapter == null) {
            model.chatAdapter = ChatAdapter(session, ChatTheme(), DefaultChatCellFactory(context, null))
            showLoadingSpinner()
        }
        setDataSource(model.chatAdapter!!)
    }

    override fun displayChatMessage(message: ChatMessage) {
        hideLoadingSpinner()
        Handler(Looper.getMainLooper()).post {
            this@ChatView.chatAdapter.addMessage(message)
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
                hideKeyboard()
        }
        return super.dispatchTouchEvent(ev)
    }

    /**
     *  Sets the data source for this view.
     *  @param chatAdapter ChatAdapter used for creating this view.
     */
    private fun setDataSource(chatAdapter: ChatAdapter) {
        this.chatAdapter = chatAdapter
        chatdisplay.adapter = this.chatAdapter
        chatdisplay.setOnScrollListener(object : AbsListView.OnScrollListener {
            override fun onScroll(
                view: AbsListView?,
                firstVisibleItem: Int,
                visibleItemCount: Int,
                totalItemCount: Int
            ) {
                val lastpos = view?.lastVisiblePosition ?: 0
                if (lastpos >= totalItemCount - 3)
                    hideSnapToLive()
                else
                    showSnapToLive()
            }

            override fun onScrollStateChanged(view: AbsListView?, scrollState: Int) {}
        })

        snap_live.setOnClickListener {
            snapToLive()
        }

        context.theme.obtainStyledAttributes(
            attrs,
            com.livelike.livelikesdk.R.styleable.ChatView,
            0, 0
        ).apply {

            try {
                val inputTextColor = getColor(
                    com.livelike.livelikesdk.R.styleable.ChatView_inputTextColor,
                    resources.getColor(com.livelike.livelikesdk.R.color.colorInputText)
                )
                ColorStateList.valueOf(inputTextColor).toString()
                val defaultText = getString(com.livelike.livelikesdk.R.styleable.ChatView_inputTextDefault)

                edittext_chat_message.setTextColor(inputTextColor)
                edittext_chat_message.setText(defaultText.orEmpty())

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

                // Send message on tap Enter
                edittext_chat_message.setOnEditorActionListener { v, actionId, event ->
                    if (event != null && (event.keyCode == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_ACTION_DONE)) {
                        hideKeyboard()
                        sendMessageNow()
                    }
                    false
                }

                button_chat_send.isEnabled = false
                button_chat_send.setOnClickListener { v ->
                    sendMessageNow()
                }
            } finally {
                recycle()
            }
        }
    }

    private fun showLoadingSpinner() {
        loadingSpinner.visibility = View.VISIBLE
    }

    private fun hideLoadingSpinner() {
        loadingSpinner.visibility = View.GONE
    }

    private fun hideKeyboard() {
        val inputManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(
            edittext_chat_message.windowToken,
            0
        )
    }

    private fun sendMessageNow() {
        val timeData = session.getPlayheadTime()
        val newMessage = ChatMessage(
            edittext_chat_message.text.toString(),
            session.currentUser?.sessionId ?: "empty-id",
            session.currentUser?.userName ?: "John Doe",
            UUID.randomUUID().toString(),
            Date(timeData.timeSinceEpochInMs).toString()
        )

        chatListener?.onChatMessageSend(newMessage, timeData)
        hideLoadingSpinner()
        this@ChatView.chatAdapter.addMessage(newMessage)

        snapToLive()
        edittext_chat_message.setText("")
        analyticService.trackMessageSent(false)
    }

    private fun hideSnapToLive() {
        if (!showingSnapToLive)
            return
        showingSnapToLive = false
        animateSnapToLiveButton()
    }

    private fun showSnapToLive() {
        if (showingSnapToLive)
            return
        showingSnapToLive = true
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
        val finalTranslationAnimator = animationEaseAdapter.createAnimationEffectWith(
            AnimationEaseInterpolator.Ease.EaseOutCubic,
            SNAP_TO_LIVE_ANIMATION_DURATION,
            translateAnimation
        )

        val alphaAnimation = ObjectAnimator.ofFloat(snap_live, "alpha", if (showingSnapToLive) 1f else 0f)
        alphaAnimation.duration = (SNAP_TO_LIVE_ALPHA_ANIMATION_DURATION).toLong()
        val finalAlphaAnimator = animationEaseAdapter.createAnimationEffectWith(
            AnimationEaseInterpolator.Ease.EaseOutCubic,
            SNAP_TO_LIVE_ALPHA_ANIMATION_DURATION,
            alphaAnimation
        )
        finalAlphaAnimator.addListener(object : Animator.AnimatorListener {
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
        snapToLiveAnimation?.play(finalTranslationAnimator)?.with(finalAlphaAnimator)
        snapToLiveAnimation?.start()
    }

    private fun snapToLive() {
        chatdisplay.smoothScrollToPositionFromTop(chatAdapter.count - 1, 0, 500)
    }
}

internal interface ChatCell {
    fun setMessage(
        message: ChatMessage?,
        isMe: Boolean?
    )

    fun getView(): View
}

/**
 *
 */
internal interface ChatCellFactory {
    fun getCell(): ChatCell
}

internal class DefaultChatCellFactory(val context: Context, cellattrs: AttributeSet?) :
    ChatCellFactory {
    private val attrs = cellattrs

    override fun getCell(): ChatCell {
        return DefaultChatCell(context, attrs)
    }
}

internal class DefaultChatCell(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs), ChatCell {
    init {
        LayoutInflater.from(context)
            .inflate(com.livelike.livelikesdk.R.layout.default_chat_cell, this, true)
    }

    override fun setMessage(
        message: ChatMessage?,
        isMe: Boolean?
    ) {
        message?.apply {
            if (isMe == true) {
                chat_nickname.setTextColor(
                    ContextCompat.getColor(
                        context,
                        com.livelike.livelikesdk.R.color.openChatNicknameMe
                    )
                )
                chat_nickname.text = "(Me) ${message.senderDisplayName}"
            } else {
                chat_nickname.setTextColor(
                    ContextCompat.getColor(
                        context,
                        com.livelike.livelikesdk.R.color.openChatNicknameOther
                    )
                )
                chat_nickname.text = message.senderDisplayName
            }
            chatMessage.text = message.message
        }
    }

    override fun getView(): View {
        return this
    }
}

/**
 * Chat adapter is the data set used for [ChatView]. Chat adapter is the binding layer between [LiveLikeContentSession]
 * Use this constructor to bind [LiveLikeContentSession] with the [ChatAdapter]. SDK would provide default [ChatTheme]
 * and [ChatCellFactory] in this case. See the overloaded constructor for providing custom [ChatTheme] and [ChatCellFactory]
 * @param session The [LiveLikeContentSession] which needs to be bounded with the Chat.
 */
internal class ChatAdapter() : BaseAdapter() {
    private lateinit var session: LiveLikeContentSession
    private lateinit var theme: ChatTheme
    private lateinit var cellFactory: ChatCellFactory

    /**
     *  Use this constructor to bind [LiveLikeContentSession] with the [ChatAdapter] and provide custom [ChatTheme].
     *  @param chatTheme The theme which would be applied to the Chat session.
     */
    constructor(chatTheme: ChatTheme) : this() {
        this.theme = chatTheme
    }

    /**
     *  Use this constructor to bind [LiveLikeContentSession] with the [ChatAdapter] and provide custom [ChatTheme] and their
     *  own [ChatCellFactory].
     *  @param session The [LiveLikeContentSession] which needs to be bounded with the Chat.
     *  @param theme The theme which would be applied to the Chat session.
     *  @param cellFactory The [ChatCell] which needs to be inflated when the chat session is created.
     */
    constructor(session: LiveLikeContentSession, theme: ChatTheme, cellFactory: ChatCellFactory) : this(theme) {
        this.session = session
        this.theme = theme
        this.cellFactory = cellFactory
    }

    private val chatMessages = mutableListOf<ChatCell>()

    fun addMessage(chat: ChatMessage) {
        logDebug { "NOW - Show Message on screen: $chat" }
        val cell = cellFactory.getCell()
        cell.setMessage(chat, session.currentUser?.sessionId == chat.senderId)
        chatMessages.add(cell)
        notifyDataSetChanged()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return chatMessages[position].getView()
    }

    override fun getItem(index: Int): Any {
        return chatMessages[index]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return chatMessages.size
    }
}