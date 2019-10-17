package com.livelike.engagementsdk.chat

import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.text.SpannableString
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.stickerKeyboard.StickerPackRepository
import com.livelike.engagementsdk.stickerKeyboard.countMatches
import com.livelike.engagementsdk.stickerKeyboard.findIsOnlyStickers
import com.livelike.engagementsdk.stickerKeyboard.findStickers
import com.livelike.engagementsdk.stickerKeyboard.replaceWithStickers
import com.livelike.engagementsdk.utils.AndroidResource
import com.livelike.engagementsdk.utils.liveLikeSharedPrefs.blockUser
import kotlinx.android.synthetic.main.default_chat_cell.view.chatBackground
import kotlinx.android.synthetic.main.default_chat_cell.view.chatMessage
import kotlinx.android.synthetic.main.default_chat_cell.view.chat_nickname
import java.util.regex.Matcher
import java.util.regex.Pattern

private val diffChatMessage: DiffUtil.ItemCallback<ChatMessage> = object : DiffUtil.ItemCallback<ChatMessage>() {
    override fun areItemsTheSame(p0: ChatMessage, p1: ChatMessage): Boolean {
        return p0.id == p1.id
    }

    override fun areContentsTheSame(p0: ChatMessage, p1: ChatMessage): Boolean {
        return p0.message == p1.message && p0.senderDisplayName == p1.senderDisplayName
    }
}

class ChatRecyclerAdapter(private val analyticsService: AnalyticsService, private val reporter: (ChatMessage) -> Unit, private val stickerPackRepository: StickerPackRepository) : ListAdapter<ChatMessage, ChatRecyclerAdapter.ViewHolder>(diffChatMessage) {
    override fun onCreateViewHolder(root: ViewGroup, position: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(root.context).inflate(R.layout.default_chat_cell, root, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindTo(getItem(position))
    }

    inner class ViewHolder(val v: View) : RecyclerView.ViewHolder(v), View.OnLongClickListener, View.OnClickListener {
        private var message: ChatMessage? = null
        private val dialogOptions = listOf(
            v.context.getString(R.string.flag_ui_blocking_title) to { msg: ChatMessage ->
                AlertDialog.Builder(v.context).apply {
                    setMessage(context.getString(R.string.flag_ui_blocking_message, msg.senderDisplayName))
                    setPositiveButton("OK") { _, _ ->
                        analyticsService.trackBlockingUser()
                        blockUser(msg.senderId)
                    }
                    create()
                }.show()
            },
            v.context.getString(R.string.flag_ui_reporting_title) to { msg: ChatMessage ->
                AlertDialog.Builder(v.context).apply {
                    setMessage(context.getString(R.string.flag_ui_reporting_message))
                    setPositiveButton("OK") { _, _ ->
                        analyticsService.trackReportingMessage()
                        reporter(msg)
                    }
                    create()
                }.show()
            })

        override fun onLongClick(p0: View?): Boolean {
            if (p0?.chat_nickname?.text?.contains("(Me)") == false) {
                showFloatingUI()
            }
            return true
        }
        override fun onClick(p0: View?) {
            hideFloatingUI()
        }

        init {
            v.setOnLongClickListener(this)
            v.setOnClickListener(this)
        }

        fun bindTo(item: ChatMessage?) {
            setMessage(item)
            hideFloatingUI()
        }

        private fun showFloatingUI() {
            v.chatBackground.alpha = 0.5f
            ChatReactionPopupView(v.context, View.OnClickListener { view ->
                analyticsService.trackFlagButtonPressed()
                hideFloatingUI()
                v.context?.let { ctx ->
                    AlertDialog.Builder(ctx).apply {
                        setTitle(context.getString(R.string.flag_ui_title))
                        setItems(dialogOptions.map { it.first }.toTypedArray()) { dialog, which ->
                            message?.let {
                                dialogOptions[which].second.invoke(it)
                            }
                        }
                        setOnCancelListener { analyticsService.trackCancelFlagUi() }
                        create()
                    }.show()
                } }, ::hideFloatingUI).showAsDropDown(v, 0, -AndroidResource.dpToPx(8))
        }

        private fun hideFloatingUI() {
            v.apply {
                chatBackground.alpha = 1f
            }
    }

        private fun setMessage(
            message: ChatMessage?
        ) {
            v.apply {
                this@ViewHolder.message = message
                message?.apply {
                    if (message.isFromMe) {
                        chat_nickname.setTextColor(
                            ContextCompat.getColor(
                                context,
                                R.color.livelike_openChatNicknameMe
                            )
                        )
                        chat_nickname.text = context.getString(R.string.chat_pre_nickname_me, message.senderDisplayName)
                    } else {
                        chat_nickname.setTextColor(
                            ContextCompat.getColor(
                                context,
                                R.color.livelike_openChatNicknameOther
                            )
                        )
                        chat_nickname.text = message.senderDisplayName
                    }
                    val spaceRemover = Pattern.compile("[\\s]")
                    val inputNoString = spaceRemover.matcher(message.message).replaceAll(Matcher.quoteReplacement(""))
                    val isOnlyStickers = inputNoString.findIsOnlyStickers().matches()
                    val atLeastOneSticker = inputNoString.findStickers().find()
                    val numberOfStickers = message.message.findStickers().countMatches()

                    when {
                        (isOnlyStickers && numberOfStickers == 1) -> {
                            val s = SpannableString(message.message)
                            replaceWithStickers(s, context, stickerPackRepository, null, 200){
                                    chatMessage.text = s
                            }
                        }
                        atLeastOneSticker -> {
                            val s = SpannableString(message.message)
                            replaceWithStickers(s, context, stickerPackRepository, null){
                                    chatMessage.text = s
                            }
                        }
                        else -> chatMessage.text = message.message
                    }
                }
            }
        }
    }
}
