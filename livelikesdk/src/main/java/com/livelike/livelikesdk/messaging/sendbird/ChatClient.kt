package com.livelike.livelikesdk.messaging.sendbird

import com.livelike.livelikesdk.messaging.ClientMessage

interface ChatClient{
    fun sendMessage(message: ClientMessage)
    fun updateMessage(message: ClientMessage)
    fun deleteMessage(message: ClientMessage)
}