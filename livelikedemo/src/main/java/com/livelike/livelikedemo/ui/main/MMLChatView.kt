package com.livelike.livelikedemo.ui.main

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import com.livelike.engagementsdk.LiveLikeContentSession
import com.livelike.engagementsdk.chat.LiveLikeChatSession
import com.livelike.livelikedemo.R
import kotlinx.android.synthetic.main.mml_chat_view.view.*

class MMLChatView(
    context: Context,
    var chatSession: LiveLikeChatSession,
    var widgetSession: LiveLikeContentSession
) : ConstraintLayout(context) {

    init {
//        val contextThemeWrapper: Context =
//            ContextThemeWrapper(context, R.style.MMLChatTheme)
        val view = inflate(context, R.layout.mml_chat_view, this)
        view.findViewById<ViewGroup>(R.id.chat_view).layoutTransition.setAnimateParentHierarchy(
            false
        )
        chatSession.let {
            custom_chat_view.setSession(it)
            custom_chat_view.isChatInputVisible = false
            val emptyView =
                LayoutInflater.from(context).inflate(R.layout.empty_chat_data_view, null)
            custom_chat_view.emptyChatBackgroundView = emptyView
            custom_chat_view.allowMediaFromKeyboard = false
        }
    }

    override fun onStartTemporaryDetach() {
        super.onStartTemporaryDetach()
        custom_chat_view.hidePopUpReactionPanel()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        custom_chat_view.hidePopUpReactionPanel()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        widgetSession.let {
            widget_view.setSession(it)
        }
    }

    fun dismissReactionPanel() {
        custom_chat_view.hidePopUpReactionPanel()
    }

//    override fun onDetachedFromWindow() {
//        super.onDetachedFromWindow()
//        chatSession?.close()
//    }
}
