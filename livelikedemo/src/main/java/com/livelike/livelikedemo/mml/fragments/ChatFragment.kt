package com.livelike.livelikedemo.mml.fragments

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.livelike.engagementsdk.chat.LiveLikeChatSession
import com.livelike.livelikedemo.R
import kotlinx.android.synthetic.main.fragment_chat.chat_view


class ChatFragment : Fragment() {
    private var session: LiveLikeChatSession? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val contextThemeWrapper: Context =
            ContextThemeWrapper(activity, R.style.TurnerChatTheme)
        // clone the inflater using the ContextThemeWrapper
        val localInflater = inflater.cloneInContext(contextThemeWrapper)
        return localInflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        chat_view.post {
            session?.let {
                chat_view.setSession(it)
                chat_view.isChatInputVisible = false
                val emptyView =
                    LayoutInflater.from(context).inflate(R.layout.empty_chat_data_view, null)
                chat_view.emptyChatBackgroundView = emptyView
                chat_view.allowMediaFromKeyboard = false
            }
        }
    }

    fun setSession(session: LiveLikeChatSession) {
        this.session = session
    }

}