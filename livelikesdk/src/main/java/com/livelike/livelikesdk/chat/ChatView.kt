package com.livelike.livelikesdk.chat

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.livelike.livelikesdk.LiveLikeContentSession
import com.livelike.livelikesdk.R
import kotlinx.android.synthetic.main.chat_view.view.chatdisplay
import kotlinx.android.synthetic.main.chat_view.view.chatinput
import kotlinx.android.synthetic.main.default_chat_cell.view.user
import kotlinx.android.synthetic.main.default_chat_cell.view.message

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

class ChatView (context: Context, attrs: AttributeSet?): ConstraintLayout(context, attrs)  {
    val attrs: AttributeSet = attrs!!
    lateinit var chatAdapter: ChatAdapter

    init {
        LayoutInflater.from(context)
                .inflate(R.layout.chat_view, this, true)
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

                chatinput.setTextColor(inputTextColor)
                chatinput.setText(defaultText.orEmpty())
                chatinput.setOnKeyListener(object : View.OnKeyListener {
                    override fun onKey(v: View, keyCode: Int, event: KeyEvent): Boolean {
                        if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER && chatinput.text.isNotEmpty()) {
                            this@ChatView.chatAdapter.addMessage(
                                    ChatMessage(
                                            chatinput.text.toString(),
                                            0,
                                            "User123"
                                    )
                            )
                            chatinput.setText("")
                            return true
                        }
                        return false
                    }
                })
            } finally {
                recycle()
            }
        }
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
 */
data class ChatMessage(val message: String, val senderId: Int, val senderDisplayName: String )

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

    override fun setMessage(chat: ChatMessage) {
        user.text = chat.senderDisplayName
        message.text = chat.message
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