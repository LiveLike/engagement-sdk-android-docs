package com.livelike.livelikedemo.customchat

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.livelike.engagementsdk.chat.data.remote.PinMessageInfo
import com.google.gson.JsonParser
import com.livelike.engagementsdk.ChatRoomListener
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.MessageListener
import com.livelike.engagementsdk.chat.ChatRoomInfo
import com.livelike.engagementsdk.chat.MessageSwipeController
import com.livelike.engagementsdk.chat.SwipeControllerActions
import com.livelike.engagementsdk.chat.data.remote.LiveLikeOrdering
import com.livelike.engagementsdk.chat.data.remote.LiveLikePagination
import com.livelike.engagementsdk.chat.stickerKeyboard.findImages
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.publicapis.LiveLikeChatMessage
import com.livelike.engagementsdk.publicapis.LiveLikeEmptyResponse
import com.livelike.livelikedemo.CustomChatActivity
import com.livelike.livelikedemo.LiveLikeApplication
import com.livelike.livelikedemo.PREFERENCES_APP_ID
import com.livelike.livelikedemo.R
import kotlinx.android.synthetic.main.custom_chat_item.view.*
import kotlinx.android.synthetic.main.fragment_chat.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import java.text.SimpleDateFormat
import java.util.*


class ChatFragment : Fragment() {

    private var currentQuoteMessage: LiveLikeChatMessage? = null
        set(value) {
            field = value
            if (value != null) {
                lay_quote.visibility = View.VISIBLE
                txt_quote_msg.text = when (value.imageUrl != null) {
                    true -> "Image"
                    else -> value.message
                }
            } else {
                lay_quote.visibility = View.INVISIBLE
                txt_quote_msg.text = ""
            }
        }
    val images = arrayListOf<String>(
        "https://i.picsum.photos/id/574/200/300.jpg?hmac=8A2sOGZU1xgRXI46snJ80xNY3Yx-KcLVsBG-wRchwFg",
        "https://i.picsum.photos/id/958/200/300.jpg?hmac=oCwv3AFzS5VqZv3nvDJ3H5RzcDH2OiL2g-GGwWL5fsI",
        "https://i.picsum.photos/id/658/200/300.jpg?hmac=K1TI0jSVU6uQZCZkkCMzBiau45UABMHNIqoaB9icB_0",
        "https://i.picsum.photos/id/237/200/300.jpg?hmac=TmmQSbShHz9CdQm0NkEjx1Dyh_Y984R9LpNrpvH2D_U",
        "https://i.picsum.photos/id/185/200/300.jpg?hmac=77sYncM4jSlhNlIKtqotElWQuIV3br7wNsq18rlbKnA"
    )
    val gifs = arrayListOf<String>(
        "https://media.giphy.com/media/SggILpMXO7Xt6/giphy.gif",
        "https://media.giphy.com/media/xULW8PLGQwyZNaw68U/giphy.gif",
        "https://media.giphy.com/media/wqb5K5564JSlW/giphy.gif",
        "https://media.giphy.com/media/401riYDwVhHluIByQH/giphy.gif",
        "https://media.giphy.com/media/4rW9yu8QaZJFC/giphy.gif"
    )

    private val authorization = "Authorization"
    private var accessToken: String =
        "Bearer dyE3arFtBKdtN4b_PhlJXXgivpfLIvbFkQon96Dk4PN2_5mTwkLJZw"
    private val client = OkHttpClient().newBuilder()
        .build()

    private val contentType = "Content-Type"
    private val applicationJSON: String = "application/json"
    private val mediaType: MediaType? = applicationJSON.toMediaTypeOrNull()
    val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val adapter = CustomChatAdapter()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        rcyl_chat.adapter = adapter
        context?.let {
            val messageSwipeController =
                MessageSwipeController(it, object : SwipeControllerActions {
                    override fun showReplyUI(position: Int) {
                        currentQuoteMessage = adapter.getChatMessage(position)
                        Toast.makeText(context, "Send Reply", Toast.LENGTH_SHORT).show()
                    }
                })
            val itemTouchHelper = ItemTouchHelper(messageSwipeController)
            itemTouchHelper.attachToRecyclerView(rcyl_chat)
        }

        // presently program id and chat room has been harcoded for just
        // testing purpose wheather we the data is received and rendered correctly
        // will remove this latter

        /* (activity as CustomChatActivity).selectedHomeChat?.let { homeChat ->
             var programId = homeChat.channel.llProgram

             if(programId.equals("5b4b2538-02c3-4ad2-820a-2c5e6da66314",ignoreCase = true)){
                 custom.visibility = View.VISIBLE
                 (activity as CustomChatActivity).selectedHomeChat?.session?.chatSession?.
                 connectToChatRoom("4121f7af-9f18-43e5-a658-0ac364e2f176", object : LiveLikeCallback<Unit>() {
                     override fun onResponse(result: Unit?, error: String?) {
                         println("ChatOnlyActivity.onResponse -> $result -> $error")
                     }
                 })
             }else{
                 custom.visibility = View.GONE
             }
         }*/
        adapter.sdk = (activity as CustomChatActivity).sdk

        ed_msg.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                val matcher = s.toString().findImages()
                if (matcher.matches()) {
                    sendMessage(null, s.toString().substring(1, s.toString().length - 1))
                    ed_msg.text?.clear()
                }
            }
        })

        (activity as CustomChatActivity).selectedHomeChat?.let { homeChat ->
            adapter.chatRoomId = homeChat.session.chatSession.getCurrentChatRoom()

            img_quote_remove.setOnClickListener {
                currentQuoteMessage = null
            }

            adapter.loadPinnedMessage(context)
            adapter.chatList.clear()
            adapter.chatList.addAll(homeChat.session.chatSession.getLoadedMessages())
            homeChat.session.chatSession.setChatRoomListener(object : ChatRoomListener {
                override fun onChatRoomUpdate(chatRoom: ChatRoomInfo) {
                    activity?.runOnUiThread {
//                        switch_quote_message.isChecked = chatRoom.enableMessageReply
                    }
                }
            })
//            switch_reply_message.setOnCheckedChangeListener { buttonView, isChecked ->
//                (activity as CustomChatActivity).sdk?.chat()
//                    ?.updateChatRoom(homeChat.session.chatSession.getCurrentChatRoom(),
//                        enableMessageReply = isChecked,
//                        liveLikeCallback = object : LiveLikeCallback<ChatRoomInfo>() {
//                            override fun onResponse(result: ChatRoomInfo?, error: String?) {
//                                switch_reply_message.isChecked = result?.enableMessageReply ?: false
//                                error?.let {
//                                    Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
//                                }
//                            }
//                        })
//            }
            (activity as CustomChatActivity).sdk?.chat()
                ?.getChatRoom(homeChat.session.chatSession.getCurrentChatRoom(),
                    object : LiveLikeCallback<ChatRoomInfo>() {
                        override fun onResponse(result: ChatRoomInfo?, error: String?) {
                            error?.let {
                                Toast.makeText(context, "FetchError: $it", Toast.LENGTH_SHORT)
                                    .show()
                            }
//                            switch_quote_message.isChecked = result?.enableMessageReply ?: false

                        }
                    })

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

                override fun onPinMessage(message: PinMessageInfo) {
                    println("ChatFragment.onPinMessage")
                    activity?.runOnUiThread {
                        Toast.makeText(
                            context,
                            "Pinned: ${message.messageId}\n${message.pinnedById}\n${message.messagePayload?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        adapter.pinnedList.add(message)
                        adapter.notifyDataSetChanged()
                    }
                }

                override fun onUnPinMessage(pinMessageId: String) {
                    activity?.runOnUiThread {
                        Toast.makeText(context, "UnPinned: $pinMessageId", Toast.LENGTH_SHORT)
                            .show()
                        val index = adapter.pinnedList.indexOfFirst { it.id == pinMessageId }
                        if (index != -1) {
                            adapter.pinnedList.removeAt(index)
                            adapter.notifyDataSetChanged()
                        }
                    }
                }

                override fun onErrorMessage(error: String) {
                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
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
                    currentQuoteMessage = null
                }
            }
            btn_img_send.setOnClickListener {
                sendMessage(null, images.random())
            }
            btn_gif_send.setOnClickListener {
                sendMessage(null, gifs.random())
            }

            // send custom message
            com.livelike.engagementsdk.BuildConfig.CONFIG_URL
            custom.setOnClickListener {
                scope.launch(Dispatchers.IO) {
                    sendCustomMessage(
                        "{\n" +
                                "  \"check1\": \"heyaa, this is for testing\"\n" +
                                "}"
                    )
                }
            }
        }
    }

    private fun sendMessage(message: String?, imageUrl: String?) {
        (activity as CustomChatActivity).selectedHomeChat?.let { homeChat ->
            val callback = object : LiveLikeCallback<LiveLikeChatMessage>() {
                override fun onResponse(result: LiveLikeChatMessage?, error: String?) {
                    ed_msg.text?.clear()
                    result?.let { message ->
                        val index = adapter.chatList.indexOfFirst { message.id == it.id }
                        if (index == -1) {
                            adapter.chatList.add(message)
                            adapter.notifyItemInserted(adapter.chatList.size - 1)
                            rcyl_chat.scrollToPosition(adapter.itemCount - 1)
                        }
                    }
                }
            }
            if (currentQuoteMessage != null) {
                homeChat.session.chatSession.quoteMessage(
                    message,
                    imageUrl,
                    imageWidth = 150,
                    imageHeight = 150,
                    liveLikeCallback = callback,
                    quoteMessage = currentQuoteMessage!!,
                    quoteMessageId = currentQuoteMessage!!.id!!
                )
            } else {
                homeChat.session.chatSession.sendMessage(
                    message, imageUrl = imageUrl, imageWidth = 150, imageHeight = 150,
                    liveLikeCallback = callback
                )
            }
        }
    }

    // custom message api call
    private fun sendCustomMessage(post: String? = null) {
        (activity as CustomChatActivity).selectedHomeChat?.let { homeChat ->
            post?.let {
                homeChat.session.chatSession.sendCustomChatMessage(
                    post,
                    object : LiveLikeCallback<LiveLikeChatMessage>() {
                        override fun onResponse(result: LiveLikeChatMessage?, error: String?) {
                            activity?.runOnUiThread {
                                result?.let {
                                    Log.d("responseCode", result.id!!)
                                }
                                error?.let {
                                    println("ChatFragment.onResponse>> $error")
                                    Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    })
            }

//            var chatRoomId = homeChat.session.chatSession.getCurrentChatRoom()
//            Log.d("custom-message1", chatRoomId)
//            val urls =
//                "${com.livelike.engagementsdk.BuildConfig.CONFIG_URL}chat-rooms/$chatRoomId/custom-messages/"
//
//            Log.d("custom-message", urls)
//
//            val body = post?.toRequestBody()
//            val request: Request = Request.Builder()
//                .url(urls)
//                .method("POST", body)
//                .addHeader(
//                    authorization,
//                    accessToken
//                )
//                .addHeader(contentType, applicationJSON)
//                .build()
//            val response: Response = client.newCall(request).execute()
//            val code = response.code
//            Log.d("responseCode", code.toString())
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
    var chatRoomId: String? = null
    val chatList = arrayListOf<LiveLikeChatMessage>()
    var sdk: EngagementSDK? = null
    var pinnedList = arrayListOf<PinMessageInfo>()

    fun loadPinnedMessage(context: Context?) {
        sdk?.chat()?.getPinMessageInfoList(
            chatRoomId!!,
            LiveLikeOrdering.ASC,
            LiveLikePagination.FIRST,
            object : LiveLikeCallback<List<PinMessageInfo>>() {
                override fun onResponse(result: List<PinMessageInfo>?, error: String?) {
                    result?.let {
                        pinnedList.addAll(it.toSet())
                        notifyDataSetChanged()
                    }
                    error?.let {
                        Toast.makeText(
                            context,
                            it,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            })
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomChatViewHolder {
        return CustomChatViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.custom_chat_item, parent, false)
        )
    }

    override fun getItemViewType(position: Int): Int {
        val chat = chatList[position]
        if (chat.quoteMessage != null) {
            return 2
        }
        return 1
    }

    override fun onBindViewHolder(holder: CustomChatViewHolder, position: Int) {
        val chatMessage = chatList[position]

        if (chatMessage.quoteMessage != null) {
            holder.itemView.lay_quote_chat_msg.visibility = View.VISIBLE
            holder.itemView.txt_name_quote.text = chatMessage.quoteMessage?.nickname
            holder.itemView.txt_message_quote.text = chatMessage.quoteMessage?.message
            if (chatMessage.quoteMessage?.imageUrl != null) {
                holder.itemView.img_message_quote.visibility = View.VISIBLE
                Glide.with(holder.itemView.img_message_quote.context)
                    .load(chatMessage.quoteMessage!!.imageUrl!!)
                    .apply(
                        RequestOptions().override(
                            chatMessage.quoteMessage!!.image_width!!,
                            chatMessage.quoteMessage!!.image_height!!
                        )
                    )
                    .into(holder.itemView.img_message_quote)

            } else {
                holder.itemView.img_message_quote.visibility = View.GONE
            }
        } else {
            holder.itemView.lay_quote_chat_msg.visibility = View.GONE
        }

        holder.itemView.normal_message.visibility = View.VISIBLE
        holder.itemView.custom_messages.visibility = View.GONE
        holder.itemView.custom_tv.visibility = View.GONE

        holder.itemView.txt_name.text = chatMessage.nickname
        val dateTime = Date()
        chatMessage.timestamp?.let {
            dateTime.time = it.toLong()
        }
        holder.itemView.img_pin.visibility = when {
            pinnedList.any { it.messageId == chatMessage.id } -> {
                View.VISIBLE
            }
            else -> {
                View.INVISIBLE
            }
        }
        if (chatMessage.imageUrl != null && chatMessage.image_width != null && chatMessage.image_height != null
        ) {
            holder.itemView.img_message.visibility = View.VISIBLE
            chatMessage.imageUrl?.let {
                Glide.with(holder.itemView.img_message.context)
                    .load(it)
                    .apply(
                        RequestOptions().override(
                            chatMessage.image_width!!,
                            chatMessage.image_height!!
                        )
                    )
                    .into(holder.itemView.img_message)
            }
            holder.itemView.txt_message.text = ""
        } else {
            holder.itemView.img_message.visibility = View.GONE

            if (!chatMessage.message.isNullOrEmpty()) {
                holder.itemView.txt_message.text = chatMessage.message
            } else if (!chatMessage.custom_data.isNullOrEmpty()) {
                var customData = chatMessage.custom_data

                holder.itemView.normal_message.visibility = View.GONE
                holder.itemView.custom_messages.visibility = View.VISIBLE
                try {
                    val jsonObject =
                        JsonParser.parseString(customData.toString()).asJsonObject

                    if (jsonObject.has("kind")) {
                        holder.itemView.widget_view.visibility = View.VISIBLE
                        holder.itemView.widget_view.displayWidget(
                            (holder.itemView.context.applicationContext as LiveLikeApplication).sdk,
                            jsonObject
                        )
                    } else {
                        holder.itemView.custom_tv.visibility = View.VISIBLE
                        holder.itemView.widget_view.visibility = View.GONE
                        holder.itemView.custom_tv.text = jsonObject.toString()
                    }
                } catch (ex: Exception) {
                    holder.itemView.custom_tv.text = customData
                    holder.itemView.custom_tv.visibility = View.VISIBLE
                    holder.itemView.widget_view.visibility = View.GONE
                }
            }
        }

        holder.itemView.setOnClickListener {
            val index = pinnedList.indexOfFirst { it.messageId == chatMessage.id }
            if (index != -1) {
                sdk?.chat()?.unPinMessage(
                    pinnedList[index].id!!,
                    object : LiveLikeCallback<LiveLikeEmptyResponse>() {
                        override fun onResponse(
                            result: LiveLikeEmptyResponse?,
                            error: String?
                        ) {
                            error?.let {
                                Toast.makeText(
                                    holder.itemView.context,
                                    it,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            result?.let {
                                Toast.makeText(
                                    holder.itemView.context,
                                    "Message Unpinned",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    })
            } else {
                sdk?.chat()?.pinMessage(
                    chatMessage.id!!,
                    chatRoomId!!,
                    chatMessage,
                    object : LiveLikeCallback<PinMessageInfo>() {
                        override fun onResponse(result: PinMessageInfo?, error: String?) {
                            error?.let {
                                Toast.makeText(holder.itemView.context, it, Toast.LENGTH_SHORT)
                                    .show()
                            }
                            result?.let { messageInfo ->
                                Toast.makeText(
                                    holder.itemView.context,
                                    "Pin Message ${messageInfo.id}",
                                    Toast.LENGTH_SHORT
                                )
                                    .show()

                            }
                        }
                    })
            }
        }

        holder.itemView.txt_msg_time.text = SimpleDateFormat(
            "MMM d, h:mm a",
            Locale.getDefault()
        ).format(dateTime)

    }

    override fun getItemCount(): Int = chatList.size
    fun getChatMessage(position: Int): LiveLikeChatMessage {
        val msg = chatList[position]
        return LiveLikeChatMessage(msg.message).apply {
            isDeleted = msg.isDeleted
            channel = msg.channel
            custom_data = msg.custom_data
            id = msg.id
            imageUrl = msg.imageUrl
            image_height = msg.image_height
            image_width = msg.image_width
            nickname = msg.nickname
            quoteMessage = msg.quoteMessage
            senderId = msg.senderId
            timestamp = msg.timestamp
            userPic = msg.userPic
            type = msg.type
        }
    }

}

class CustomChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
