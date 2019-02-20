package com.livelike.livelikesdk.messaging.sendbird

import android.content.Context
import com.livelike.livelikesdk.LiveLikeContentSession
import com.livelike.livelikesdk.messaging.ClientMessage

interface ChatClient{
    fun sendMessage(message: ClientMessage)
    fun updateMessage(message: ClientMessage)
    fun deleteMessage(message: ClientMessage)
    fun setSession(session: LiveLikeContentSession, context: Context)
}