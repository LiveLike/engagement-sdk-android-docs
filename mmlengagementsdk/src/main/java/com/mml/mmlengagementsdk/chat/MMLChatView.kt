package com.mml.mmlengagementsdk.chat

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import com.example.mmlengagementsdk.R
import com.livelike.engagementsdk.chat.LiveLikeChatSession
import kotlinx.android.synthetic.main.mml_chat_view.view.chat_view

class MMLChatView(context: Context) : ConstraintLayout(context) {

    var chatSession: LiveLikeChatSession? = null

    init {
        val contextThemeWrapper: Context =
            ContextThemeWrapper(context, R.style.MMLChatTheme)
        inflate(contextThemeWrapper, R.layout.mml_chat_view, this)

    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        chatSession?.let {
            chat_view.setSession(it)
            chat_view.isChatInputVisible = false
            val emptyView =
                LayoutInflater.from(context).inflate(R.layout.mml_empty_chat_data_view, null)
            chat_view.emptyChatBackgroundView = emptyView
            chat_view.allowMediaFromKeyboard = false
        }
    }

//    override fun onDetachedFromWindow() {
//        super.onDetachedFromWindow()
//        chatSession?.close()
//    }
}