package com.livelike.livelikedemo.customchat

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.livelike.engagementsdk.MessageListener
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.publicapis.LiveLikeChatMessage
import com.livelike.livelikedemo.CustomChatActivity
import com.livelike.livelikedemo.PREFERENCES_APP_ID
import com.livelike.livelikedemo.R
import kotlinx.android.synthetic.main.custom_chat_item.view.txt_message
import kotlinx.android.synthetic.main.custom_chat_item.view.txt_msg_time
import kotlinx.android.synthetic.main.custom_chat_item.view.txt_name
import kotlinx.android.synthetic.main.fragment_chat.btn_send
import kotlinx.android.synthetic.main.fragment_chat.ed_msg
import kotlinx.android.synthetic.main.fragment_chat.lay_swipe
import kotlinx.android.synthetic.main.fragment_chat.rcyl_chat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ChatFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val adapter = CustomChatAdapter()
        rcyl_chat.adapter = adapter
        (activity as CustomChatActivity).selectedHomeChat?.let { homeChat ->
            adapter.chatList.addAll(homeChat.session.chatSession.getLoadedMessages())
            homeChat.session.chatSession.setMessageListener(object : MessageListener {

                override fun onNewMessage(message: LiveLikeChatMessage) {
                    val index = adapter.chatList.indexOfFirst { it.id == message.id }
                    if (index > -1) {
                        adapter.chatList[index] = message
                    } else {
                        adapter.chatList.add(message)
                    }
                    activity?.runOnUiThread {
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

                override fun onHistoryMessage(messages: List<LiveLikeChatMessage>) {
                    messages.toMutableList()
                        .removeAll {
                            homeChat.session.chatSession.getDeletedMessages().contains(it.id)
                        }
                    activity?.runOnUiThread {
                        val empty = adapter.chatList.isEmpty()
                        adapter.chatList.addAll(0, messages)
                        adapter.notifyDataSetChanged()
                        lay_swipe.isRefreshing = false
                        if (empty) {
                            rcyl_chat.scrollToPosition(adapter.itemCount - 1)
                        }
                    }
                }


                override fun onDeleteMessage(messageId: String) {
                    val index = adapter.chatList.indexOfFirst { it.id == messageId }
                    if (index > -1) {
                        adapter.chatList.removeAt(index)
                        activity?.runOnUiThread {
                            adapter.notifyItemRemoved(index)
                        }
                    }
                }
            })

            lay_swipe.isRefreshing = true
            homeChat.session.chatSession.loadNextHistory()
            lay_swipe.setOnRefreshListener {
                homeChat.session.chatSession.loadNextHistory()
            }
            btn_send.setOnClickListener {
                val msg = ed_msg.text.toString()
                homeChat.session.chatSession.sendChatMessage(
                    msg,
                    liveLikeCallback = object : LiveLikeCallback<LiveLikeChatMessage>() {
                        override fun onResponse(result: LiveLikeChatMessage?, error: String?) {
                            ed_msg.text.clear()
                            result?.let { message ->
                                val index = adapter.chatList.indexOfFirst { message.id == it.id }
                                if (index == -1) {
                                    adapter.chatList.add(message)
                                    adapter.notifyItemInserted(adapter.chatList.size - 1)
                                    rcyl_chat.scrollToPosition(adapter.itemCount - 1)
                                }
                            }
                        }
                    })
            }
        }
    }

    override fun onPause() {
        super.onPause()
        (activity as? CustomChatActivity)?.selectedHomeChat?.let {
            val sharedPref =
                (activity as? CustomChatActivity)?.getSharedPreferences(
                    PREFERENCES_APP_ID,
                    Context.MODE_PRIVATE
                )
            sharedPref?.edit()?.putLong(
                "msg_time_${it.channel.llProgram}",
                Calendar.getInstance().timeInMillis
            )?.apply()
        }
    }


    companion object {
        @JvmStatic
        fun newInstance() = ChatFragment()
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
