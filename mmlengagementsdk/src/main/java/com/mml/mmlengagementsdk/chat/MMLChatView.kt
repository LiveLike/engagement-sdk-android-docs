package com.mml.mmlengagementsdk.chat

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import com.example.mmlengagementsdk.R
import com.livelike.engagementsdk.chat.LiveLikeChatSession
import kotlinx.android.synthetic.main.mml_chat_view.view.chat_view

class MMLChatView : ConstraintLayout {

    var chatSession: LiveLikeChatSession? = null

    constructor(context: Context) : super(context) {
        init(null, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(attrs, defStyle)
    }

    private fun init(attrs: AttributeSet?, defStyle: Int) {
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