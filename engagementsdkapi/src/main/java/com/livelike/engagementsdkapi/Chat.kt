package com.livelike.engagementsdkapi

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter

interface ChatEventListener {
    fun onChatMessageSend(message: ChatMessage, timeData: EpochTime)
}

interface ChatRenderer {
    var chatListener: ChatEventListener?
    val chatContext: Context
    fun displayChatMessage(message: ChatMessage)
    fun loadComplete()
}

/**
 *  Represents a chat message.
 *  @param message The message user wants to send.
 *  @param senderId This is unique user id.
 *  @param senderDisplayName This is display name user is associated with.
 *  @param id A unique ID to identify the message.
 *  @param timeStamp Message timeStamp.
 */
data class ChatMessage(
    val message: String,
    val senderId: String,
    val senderDisplayName: String,
    val id: String,
    val timeStamp: String = ""
)

interface ChatCell {
    fun setMessage(
        message: ChatMessage?,
        isMe: Boolean?
    )

    fun getView(): View
}

interface ChatCellFactory {
    fun getCell(): ChatCell
}

/**
 * Chat adapter is the data set used for [ChatView]. Chat adapter is the binding layer between [LiveLikeContentSession]
 * Use this constructor to bind [LiveLikeContentSession] with the [ChatAdapter]. SDK would provide default [ChatTheme]
 * and [ChatCellFactory] in this case. See the overloaded constructor for providing custom [ChatTheme] and [ChatCellFactory]
 * @param session The [LiveLikeContentSession] which needs to be bounded with the Chat.
 */
class ChatAdapter() : BaseAdapter() {
    private lateinit var session: LiveLikeContentSession
    private lateinit var cellFactory: ChatCellFactory

    /**
     *  Use this constructor to bind [LiveLikeContentSession] with the [ChatAdapter] and their
     *  own [ChatCellFactory].
     *  @param session The [LiveLikeContentSession] which needs to be bounded with the Chat.
     *  @param cellFactory The [ChatCell] which needs to be inflated when the chat session is created.
     */
    constructor(session: LiveLikeContentSession, cellFactory: ChatCellFactory) : this() {
        this.session = session
        this.cellFactory = cellFactory
    }

    private val chatMessages = mutableListOf<ChatCell>()

    fun addMessage(chat: ChatMessage) {
        val cell = cellFactory.getCell().also {
            it.setMessage(chat, session.currentUser?.sessionId == chat.senderId)
        }
        if (!chatMessages.contains(cell)) {
            // avoid duplicated messages
            chatMessages.add(cell)
            notifyDataSetChanged()
        }
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

class ChatViewModel(val analyticsService: AnalyticsService) {
    fun clear() {
        chatAdapter = null
        chatLastPos = null
    }

    var chatAdapter: ChatAdapter? = null
    var chatLastPos: Int? = null
}
