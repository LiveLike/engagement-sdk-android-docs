package com.livelike.livelikesdk.chat

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.livelike.livelikesdk.R
import kotlinx.android.synthetic.main.chat_view.view.*
import kotlinx.android.synthetic.main.default_chat_cell.view.*

class ChatView (context: Context, attrs: AttributeSet?): ConstraintLayout(context, attrs)  {
    var chatAdapter = ChatAdapter(DefaultChatCellFactory(context, null))
    init {
        LayoutInflater.from(context)
            .inflate(R.layout.chat_view, this, true)

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
                            chatAdapter.addMessage(
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

            chatdisplay.adapter = chatAdapter
        }
    }
}

interface ChatCell {
    fun setMessage(chat: ChatMessage)
    fun getView() : View
}

data class ChatMessage(val message: String, val senderId: Int, val senderDisplayName: String )

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

open class ChatAdapter(val  cellFactory: ChatCellFactory) : BaseAdapter() {

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