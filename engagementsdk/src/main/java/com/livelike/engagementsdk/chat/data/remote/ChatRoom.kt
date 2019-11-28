package com.livelike.engagementsdk.chat.data.remote

import com.google.gson.annotations.SerializedName

data class ChatRoom(
    @SerializedName("channels")
    val channels: Channels,
    @SerializedName("client_id")
    val clientId: String,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("id")
    val id: String,
    @SerializedName("url")
    val url: String
)

data class Channels(
    @SerializedName("chat")
    val chat: Map<String, String>,
    @SerializedName("reactions")
    val reactions: Map<String, String>
)
