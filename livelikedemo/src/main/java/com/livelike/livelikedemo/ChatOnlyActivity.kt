package com.livelike.livelikedemo

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.livelike.engagementsdkapi.ChatMessage
import com.livelike.engagementsdkapi.ChatRenderer
import com.livelike.engagementsdkapi.EpochTime
import com.livelike.engagementsdkapi.LiveLikeContentSession
import com.livelike.engagementsdkapi.LiveLikeUser
import com.livelike.engagementsdkapi.WidgetRenderer
import com.livelike.livelikesdk.LiveLikeSDK
import kotlinx.android.synthetic.main.activity_chat_only.*
import java.util.*

class ChatOnlyActivity : AppCompatActivity() {
    private val chatMessageList = mutableListOf<ChatMessage>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_only)
        LiveLikeSDK("1234", baseContext)
        updateToolbar()
        val session = TestSession()
        chat_view.setSession(session)

        prePopulateChatWithMessages()
        pushMessageIntoChatViewAtFixedRate()
    }

    private fun updateToolbar() {
        chat_toolbar.apply {
            title = "Chat Only"
            setNavigationIcon(R.drawable.ic_arrow_back_white_24dp)
            setBackgroundColor(Color.parseColor("#00ae8b"))
            setNavigationOnClickListener {
                startActivity(Intent(context, MainActivity::class.java))
            }
        }
    }

    private fun pushMessageIntoChatViewAtFixedRate() {
        var senderId = 0
        val messageUpdateRate: Long = 5000
        Timer().scheduleAtFixedRate(
            object : TimerTask() {
                override fun run() {
                    senderId++
                    chat_view.displayChatMessage(
                        ChatMessage(
                            "Pushed message $senderId",
                            senderId.toString(),
                            "Admin $senderId",
                            senderId.toString(),
                            "$senderId:00:00"
                        )
                    )
                }
            }, 0, messageUpdateRate
        )
    }

    private fun prePopulateChatWithMessages() {
        updateChatMessageList()
        pushMessageOnView()
    }

    private fun pushMessageOnView() {
        for (i in 1..3)
            chatMessageList.forEach { message -> chat_view.displayChatMessage(message) }
    }

    private fun updateChatMessageList() {
        val emojiMessage =
            "emojiMessage ${getEmojiByUnicode(0x1F60A)} ${getEmojiByUnicode(0x1F604)} ${getEmojiByUnicode(0x1F64C)}"
        chatMessageList.add(ChatMessage("We will rock you!", "1", "Queen", "1", "12:00:00"))
        chatMessageList.add(ChatMessage("Stairway to heaven", "2", "Led Zeppelin", "2", "11:00:00"))
        chatMessageList.add(ChatMessage("Pour some sugar on me", "3", "Def Leppard", "3", "10:00:00"))
        chatMessageList.add(ChatMessage("Fear of the dark", "4", "Iron Maiden", "5", "7:00:00"))
        chatMessageList.add(ChatMessage("Another brick in the wall", "5", "Pink Floyd", "5", "6:00:00"))
        chatMessageList.add(ChatMessage("Turbo lover", "6", "Judas Priest", "6", "5:00:00"))
        chatMessageList.add(ChatMessage(getString(R.string.longChatMessage), "7", "Random", "7", "4:00:00"))
        chatMessageList.add(ChatMessage(emojiMessage, "8", "Emojicon", "8", "3:00:00"))
    }

    inner class TestSession : LiveLikeContentSession {
        override val programUrl: String get() = ""
        override var widgetRenderer: WidgetRenderer?
            get() = null
            set(value) {}
        override var chatRenderer: ChatRenderer?
            get() = null
            set(value) {}
        override val currentUser: LiveLikeUser?
            get() = LiveLikeUser("1234", "TestUser")

        override fun pause() {}
        override fun resume() {}
        override fun clearChatHistory() {}
        override fun clearFeedbackQueue() {}
        override fun close() {}
        override fun getPlayheadTime(): EpochTime { return EpochTime(1000L) }
        override fun contentSessionId(): String { return "TestSession" }
    }

    fun getEmojiByUnicode(unicode: Int) : String {
        return String(Character.toChars(unicode))
    }
}
