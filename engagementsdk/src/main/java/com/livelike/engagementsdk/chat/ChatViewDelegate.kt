package com.livelike.engagementsdk.chat

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.livelike.engagementsdk.publicapis.ChatMessageType
import com.livelike.engagementsdk.publicapis.LiveLikeChatMessage

interface ChatViewDelegate {
    /**
     * Allow integrator to provide viewHolder class based on viewType, currently it supports only #ChatMessageType.CUSTOM_MESSAGE_CREATED
     */
    fun onCreateViewHolder(parent: ViewGroup, viewType: ChatMessageType): RecyclerView.ViewHolder

    /**
     * Allow integrator to bind data to the view elements inside the holder instance, right supports for only #ChatMessageType.CUSTOM_MESSAGE_CREATED
     */
    fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        liveLikeChatMessage: LiveLikeChatMessage,
        chatViewThemeAttributes: ChatViewThemeAttributes,
        showChatAvatarLogo: Boolean
    )
}