package com.livelike.livelikedemo.customchat

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.livelike.engagementsdk.MessageListener
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.publicapis.LiveLikeChatMessage
import com.livelike.livelikedemo.CustomChatActivity
import com.livelike.livelikedemo.PREFERENCES_APP_ID
import com.livelike.livelikedemo.R
import kotlinx.android.synthetic.main.custom_chat_item.view.img_message
import kotlinx.android.synthetic.main.custom_chat_item.view.txt_message
import kotlinx.android.synthetic.main.custom_chat_item.view.txt_msg_time
import kotlinx.android.synthetic.main.custom_chat_item.view.txt_name
import kotlinx.android.synthetic.main.fragment_chat.btn_gif_send
import kotlinx.android.synthetic.main.fragment_chat.btn_img_send
import kotlinx.android.synthetic.main.fragment_chat.btn_send
import kotlinx.android.synthetic.main.fragment_chat.ed_msg
import kotlinx.android.synthetic.main.fragment_chat.lay_swipe
import kotlinx.android.synthetic.main.fragment_chat.rcyl_chat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ChatFragment : Fragment() {

    val images = arrayListOf<String>(
        "https://homepages.cae.wisc.edu/~ece533/images/fruits.png",
        "https://homepages.cae.wisc.edu/~ece533/images/monarch.png",
        "https://homepages.cae.wisc.edu/~ece533/images/mountain.png",
        "https://homepages.cae.wisc.edu/~ece533/images/watch.png",
        "https://homepages.cae.wisc.edu/~ece533/images/serrano.png"
    )
    val gifs = arrayListOf<String>(
        "https://media.giphy.com/media/SggILpMXO7Xt6/giphy.gif",
        "https://media.giphy.com/media/xULW8PLGQwyZNaw68U/giphy.gif",
        "https://media.giphy.com/media/wqb5K5564JSlW/giphy.gif",
        "https://media.giphy.com/media/401riYDwVhHluIByQH/giphy.gif",
        "https://media.giphy.com/media/4rW9yu8QaZJFC/giphy.gif"
    )

    private val adapter = CustomChatAdapter()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        rcyl_chat.adapter = adapter
        (activity as CustomChatActivity).selectedHomeChat?.let { homeChat ->
            adapter.chatList.addAll(homeChat.session.chatSession.getLoadedMessages())
            homeChat.session.chatSession.setMessageListener(object : MessageListener {
                private val TAG = "LiveLike"
                override fun onNewMessage(message: LiveLikeChatMessage) {
                    Log.i(TAG, "onNewMessage: $message")
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
                    Log.d(TAG, "onHistoryMessage: ${messages.size}")
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
                    Log.d(TAG, "onDeleteMessage: $messageId")
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
                if (msg.trim().isNotEmpty()) {
                    sendMessage(msg, null)
                }
            }
            btn_img_send.setOnClickListener {
                sendMessage(null, images.random())
            }
            btn_gif_send.setOnClickListener {
                sendMessage(null, gifs.random())
            }
        }
    }

    private fun sendMessage(message: String?, imageUrl: String?) {
        (activity as CustomChatActivity).selectedHomeChat?.let { homeChat ->
            homeChat.session.chatSession.sendChatMessage(
                message, imageUrl = imageUrl,
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
        if (chatMessage.imageUrl != null) {
            holder.itemView.img_message.visibility = View.VISIBLE
            Glide.with(holder.itemView.img_message.context).load(chatMessage.imageUrl)
                .apply(
                    RequestOptions().override(
                        chatMessage.image_width!!,
                        chatMessage.image_height!!
                    )
                )
                .into(holder.itemView.img_message)
        } else {
            holder.itemView.img_message.visibility = View.GONE
        }
        holder.itemView.txt_msg_time.text = SimpleDateFormat(
            "MMM d, h:mm a",
            Locale.getDefault()
        ).format(dateTime)
    }

    override fun getItemCount(): Int = chatList.size
}

class CustomChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
