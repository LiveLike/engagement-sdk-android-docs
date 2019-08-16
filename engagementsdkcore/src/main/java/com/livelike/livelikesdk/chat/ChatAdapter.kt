package com.livelike.livelikesdk.chat

import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.utils.logError
import kotlinx.android.synthetic.main.default_chat_cell.view.chatBackground
import kotlinx.android.synthetic.main.default_chat_cell.view.chatMessage
import kotlinx.android.synthetic.main.default_chat_cell.view.chat_nickname
import kotlinx.android.synthetic.main.default_chat_cell.view.floatingUi

private val diffChatMessage: DiffUtil.ItemCallback<ChatMessage> = object : DiffUtil.ItemCallback<ChatMessage>() {
    override fun areItemsTheSame(p0: ChatMessage, p1: ChatMessage): Boolean {
        return p0.id == p1.id
    }

    override fun areContentsTheSame(p0: ChatMessage, p1: ChatMessage): Boolean {
        return p0.message == p1.message && p0.senderDisplayName == p1.senderDisplayName
    }
}

class ChatRecyclerAdapter : ListAdapter<ChatMessage, ChatRecyclerAdapter.ViewHolder>(diffChatMessage) {
    override fun onCreateViewHolder(root: ViewGroup, position: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(root.context).inflate(R.layout.default_chat_cell, root, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindTo(getItem(position))
    }

    class ViewHolder(val v: View) : RecyclerView.ViewHolder(v), View.OnLongClickListener, View.OnClickListener {
        private var message: ChatMessage? = null
        private val dialogOptions = listOf(
            "Block this user" to { msg: ChatMessage ->
                logError { "Blocking ${msg.message}" }
            },
            "Report message" to { msg: ChatMessage ->
                logError { "Reporting ${msg.message}" }
            })

        override fun onLongClick(p0: View?): Boolean {
            showFloatingUI()
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
            v.apply {
                floatingUi.visibility = View.VISIBLE
                chatBackground.alpha = 0.5f
                floatingUi?.setOnClickListener {
                    hideFloatingUI()
                    context?.let { ctx ->
                        AlertDialog.Builder(ctx).apply {
                            setTitle("A problem?")
                            setItems(dialogOptions.map { it.first }.toTypedArray()) { dialog, which ->
                                message?.let {
                                    dialogOptions[which].second.invoke(it)
                                }
                            }
                            create()
                        }.show()
                    }
                }
            }
        }

        private fun hideFloatingUI() {
            v.apply {
                floatingUi.visibility = View.INVISIBLE
                chatBackground.alpha = 1f
            }
        }

        fun setMessage(
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
                        chat_nickname.text = "(Me) ${message.senderDisplayName}"
                    } else {
                        chat_nickname.setTextColor(
                            ContextCompat.getColor(
                                context,
                                R.color.livelike_openChatNicknameOther
                            )
                        )
                        chat_nickname.text = message.senderDisplayName
                    }
                    chatMessage.text = message.message
                }
            }
        }
    }
}
