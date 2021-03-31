package com.livelike.livelikedemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.livelike.engagementsdk.MessageListener
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.publicapis.LiveLikeChatMessage
import kotlinx.android.synthetic.main.activity_custom_chat.btn_send
import kotlinx.android.synthetic.main.activity_custom_chat.ed_msg
import kotlinx.android.synthetic.main.activity_custom_chat.lay_swipe
import kotlinx.android.synthetic.main.activity_custom_chat.rcyl_chat
import kotlinx.android.synthetic.main.custom_chat_item.view.txt_message
import kotlinx.android.synthetic.main.custom_chat_item.view.txt_msg_time
import kotlinx.android.synthetic.main.custom_chat_item.view.txt_name
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CustomChatActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_chat)
        val adapter = CustomChatAdapter()
        rcyl_chat.adapter = adapter
        (application as LiveLikeApplication).channelManager.let { channel ->
            val session =
                (application as LiveLikeApplication).createPublicSession(channel.selectedChannel.llProgram.toString())
            adapter.chatList.addAll(session.chatSession.getLoadedMessages())
            session.chatSession.setMessageListener(object : MessageListener {
                override fun onNewMessage(chatRoom: String, message: LiveLikeChatMessage) {
                    val index = adapter.chatList.indexOfFirst { it.id == message.id }
                    if (index > -1) {
                        adapter.chatList[index] = message
                    } else {
                        adapter.chatList.add(message)
                    }
                    runOnUiThread {
                        if (index > -1) {
                            adapter.notifyItemChanged(index)
                            if (index == adapter.itemCount - 1) {
                                rcyl_chat.scrollToPosition(adapter.itemCount - 1)
                            }
                        } else {
                            adapter.notifyItemInserted(adapter.chatList.size - 1)
                            rcyl_chat.scrollToPosition(adapter.itemCount - 1)
                        }
                    }
                }

                override fun onHistoryMessage(
                    chatRoom: String,
                    messages: List<LiveLikeChatMessage>
                ) {
                    messages.toMutableList()
                        .removeAll { session.chatSession.getDeletedMessages().contains(it.id) }
                    runOnUiThread {
                        adapter.chatList.addAll(0, messages)
                        adapter.notifyDataSetChanged()
                        lay_swipe.isRefreshing = false
                    }
                }


                override fun onDeleteMessage(messageId: String) {
                    val index = adapter.chatList.indexOfFirst { it.id == messageId }
                    if (index > -1) {
                        adapter.chatList.removeAt(index)
                        runOnUiThread {
                            adapter.notifyItemRemoved(index)
                        }
                    }
                }
            })

            lay_swipe.isRefreshing = true
            session.chatSession.loadNextHistory()
            lay_swipe.setOnRefreshListener {
                session.chatSession.loadNextHistory()
            }
            btn_send.setOnClickListener {
                val msg = ed_msg.text.toString()
                session.chatSession.sendChatMessage(
                    msg,
                    liveLikeCallback = object : LiveLikeCallback<LiveLikeChatMessage>() {
                        override fun onResponse(result: LiveLikeChatMessage?, error: String?) {
                            if (error != null) {
                                Toast.makeText(this@CustomChatActivity, error, Toast.LENGTH_SHORT)
                                    .show()
                            } else {
                                ed_msg.text.clear()
                                result?.let { message ->
                                    val index =
                                        adapter.chatList.indexOfFirst { message.id == it.id }
                                    if (index == -1) {
                                        adapter.chatList.add(message)
                                        adapter.notifyItemInserted(adapter.chatList.size - 1)
                                        rcyl_chat.scrollToPosition(adapter.itemCount - 1)
                                    }
                                }
                            }
                        }
                    })
            }

        }
    }
}

class CustomChatAdapter : RecyclerView.Adapter<CustomChatViewHolder>() {
    val chatList = arrayListOf<LiveLikeChatMessage>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomChatViewHolder {
        return CustomChatViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.custom_chat_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: CustomChatViewHolder, position: Int) {
        val chatMessage = chatList[position]
        holder.itemView.txt_message.text = chatMessage.message
        holder.itemView.txt_name.text = chatMessage.nickname
        val dateTime = Date()
        chatMessage.timestamp?.let {
            dateTime.time = it.toLong()
        }
        holder.itemView.txt_msg_time.text = SimpleDateFormat(
            "MMM d, h:mm a",
            Locale.getDefault()
        ).format(dateTime)
    }

    override fun getItemCount(): Int = chatList.size
}

class CustomChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)