package com.livelike.livelikesdk.chat

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.support.constraint.ConstraintLayout
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.BaseAdapter
import com.livelike.livelikesdk.LiveLikeContentSession
import com.livelike.livelikesdk.R
import kotlinx.android.synthetic.main.chat_input.view.*
import kotlinx.android.synthetic.main.chat_view.view.*
import kotlinx.android.synthetic.main.default_chat_cell.view.*

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

class ChatView (context: Context, attrs: AttributeSet?): ConstraintLayout(context, attrs), ChatRenderer  {
    override var chatListener: ChatEventListener? = null
    override val chatContext: Context = context

    private val attrs: AttributeSet = attrs!!
    private lateinit var session: LiveLikeContentSession
    private lateinit var chatAdapter: ChatAdapter

    init {
        LayoutInflater.from(context)
                .inflate(R.layout.chat_view, this, true)

        (context as Activity).window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        context.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    fun setSession(session: LiveLikeContentSession) {
        this.session = session
        session.chatRenderer = this
    }

    override fun displayChatMessage(message: ChatMessage) {
        //Might not need the looper here?
        Handler(Looper.getMainLooper()).post {
            this@ChatView.chatAdapter.addMessage(message)
        }
    }

    /**
     *  Sets the data source for this view.
     *  @param chatAdapter ChatAdapter used for creating this view.
     */
    fun setDataSource(chatAdapter: ChatAdapter) {
        this.chatAdapter = chatAdapter
        chatdisplay.adapter = this.chatAdapter

        context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.ChatView,
                0, 0).apply {

            try {
                val inputTextColor = getInteger(R.styleable.ChatView_inputTextColor, R.color.colorInputText)
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
                        val inputManager =
                            context!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

                        // Hide keyboard
                        inputManager.hideSoftInputFromWindow(
                            v.windowToken,
                            InputMethodManager.HIDE_NOT_ALWAYS
                        )
                        sendMessageNow()
                    }
                    false
                }

                button_chat_send.isEnabled = false
                button_chat_send.setOnClickListener { v ->
                    chatdisplay.post {
                        chatdisplay.smoothScrollToPosition(0)
                    }
                    sendMessageNow()
                }

            } finally {
                recycle()
            }
        }
    }

    fun sendMessageNow() {
        val newMessage = ChatMessage(
            edittext_chat_message.text.toString(),
            "user-id",
            "User123",
            "message_id"
        )

        val timeData = session.getPlayheadTime()
        chatListener?.onChatMessageSend(newMessage, timeData)
        this@ChatView.chatAdapter.addMessage(newMessage)
        edittext_chat_message.setText("")
    }


}

interface ChatCell {
    fun setMessage(message: ChatMessage)
    fun getView() : View
}

/**
 *  Represents a chat message.
 *  @param message The message user wants to send.
 *  @param senderId This is unique user id.
 *  @param senderDisplayName This is display name user is associated with.
 *  @param id A unique ID to identify the message.
 */
data class ChatMessage(val message: String, val senderId: String, val senderDisplayName: String, val id: String)

/**
 *
 */
interface ChatCellFactory {
    fun getCell() : ChatCell
}

class DefaultChatCellFactory (val context: Context, cellattrs: AttributeSet?):
        ChatCellFactory {
    val attrs = cellattrs

    override fun getCell(): ChatCell {
        return DefaultChatCell(context, attrs)
    }
}

class DefaultChatCell(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs), ChatCell {
    init {
        LayoutInflater.from(context)
                .inflate(R.layout.default_chat_cell, this, true)
    }

    override fun setMessage(message: ChatMessage) {
        chat_nickname.text = message.senderDisplayName
        chatMessage.text = message.message
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
open class ChatAdapter(session: LiveLikeContentSession) : BaseAdapter() {
    private lateinit var session : LiveLikeContentSession
    private lateinit var theme: ChatTheme
    private lateinit var cellFactory: ChatCellFactory

    /**
     *  Use this constructor to bind [LiveLikeContentSession] with the [ChatAdapter] and provide custom [ChatTheme].
     *  @param session The [LiveLikeContentSession] which needs to be bounded with the Chat.
     *  @param theme The theme which would be applied to the Chat session.
     */
    constructor(session: LiveLikeContentSession, chatTheme: ChatTheme) : this(session) {
        this.theme = chatTheme
    }

    /**
     *  Use this constructor to bind [LiveLikeContentSession] with the [ChatAdapter] and provide custom [ChatTheme] and their
     *  own [ChatCellFactory].
     *  @param session The [LiveLikeContentSession] which needs to be bounded with the Chat.
     *  @param theme The theme which would be applied to the Chat session.
     *  @param cellFactory The [ChatCell] which needs to be inflated when the chat session is created.
     */
    constructor(session: LiveLikeContentSession, theme: ChatTheme, cellFactory: ChatCellFactory) : this(session, theme) {
        this.session = session
        this.theme = theme
        this.cellFactory = cellFactory
    }

    val chatMessages = mutableListOf<ChatCell>()

    fun addMessage(chat : ChatMessage) {
        val cell = cellFactory.getCell()
        cell.setMessage(chat)
        chatMessages.add(cell)
        notifyDataSetChanged()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return chatMessages.get(position).getView()
    }

    override fun getItem(index: Int): Any {
        return chatMessages.get(index)
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return chatMessages.size
    }
}