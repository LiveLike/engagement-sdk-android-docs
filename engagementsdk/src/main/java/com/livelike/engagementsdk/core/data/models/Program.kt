package com.livelike.engagementsdk.core.data.models

import com.google.gson.annotations.SerializedName
import com.livelike.engagementsdk.chat.data.remote.ChatRoom

internal data class Program(
    val programUrl: String,
    val timelineUrl: String,
    val rankUrl: String,
    val id: String,
    val title: String,
    val widgetsEnabled: Boolean,
    val chatEnabled: Boolean,
    val subscribeChannel: String,
    val chatChannel: String,
    val analyticsProps: Map<String, String>,
    val rewardsType: String,
    val leaderboardUrl: String,
    val stickerPacksUrl: String,
    val reactionPacksUrl: String,
    val chatRooms: List<ChatRoom>?,
    val defaultChatRoom: ChatRoom?,
    val reportUrl: String?
)

internal data class ProgramModel(
    @SerializedName("url")
    val programUrl: String?,
    @SerializedName("timeline_url")
    val timelineUrl: String?,
    @SerializedName("rank_url")
    val rankUrl: String?,
    @SerializedName("id")
    val id: String?,
    @SerializedName("title")
    val title: String?,
    @SerializedName("widgets_enabled")
    val widgetsEnabled: Boolean?,
    @SerializedName("chat_enabled")
    val chatEnabled: Boolean?,
    @SerializedName("subscribe_channel")
    val subscribeChannel: String?,
    @SerializedName("sendbird_channel")
    val chatChannel: String?,
    @SerializedName("analytics_properties")
    val analyticsProps: Map<String, String>?,
    @SerializedName("rewards_type")
    val rewardsType: String?, // none, points, bagdes
    val leaderboard_url: String?,
    val sticker_packs_url: String?,
    val reaction_packs_url: String?,
    @SerializedName("report_url")
    val reportUrl: String?,
    @SerializedName("chat_rooms")
    val chatRooms: List<ChatRoom>?,
    @SerializedName("default_chat_room")
    val defaultChatRoom: ChatRoom?
)

internal fun ProgramModel.toProgram(): Program {
    return Program(
        programUrl ?: "",
        timelineUrl ?: "",
        rankUrl ?: "",
        id ?: "",
        title ?: "",
        widgetsEnabled ?: true,
        chatEnabled ?: true,
        subscribeChannel ?: "",
        chatChannel ?: "",
        analyticsProps ?: mapOf(),
        rewardsType ?: "",
        leaderboard_url ?: "",
        sticker_packs_url ?: "",
        reaction_packs_url ?: "",
        chatRooms,
        defaultChatRoom,
        reportUrl
    )
}

enum class RewardsType(val key: String) {
    NONE("none"),
    POINTS("points"),
    BADGES("badges");
}
