package com.livelike.engagementsdk.chat.data.remote

import com.google.gson.annotations.SerializedName

/**
 * Chat Rooms are abstraction over the chat providers in our infra
 **/
internal data class ChatRoom(
    @SerializedName("channels")
    val channels: Channels,
    @SerializedName("client_id")
    val clientId: String,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("id")
    val id: String,
    @SerializedName("url")
    val url: String,
    @SerializedName("upload_url")
    val uploadUrl: String
)

internal data class Channels(
    @SerializedName("chat")
    val chat: Map<String, String>,
    @SerializedName("reactions")
    val reactions: Map<String, String>
)
