package com.livelike.engagementsdk

/**
 * A LiveLikeUser is the representation of a user in the library.
 *
 * @property id The id of the user. This value is defined by LiveLike CMS.
 * @property nickname The username of the user. The default value is generated by LiveLike CMS, then this value can be changed if needed/requested. TODO: Need to add a proper setter for the nickname
 * @property accessToken Access token of the user.
 * @property isWidgetEnabled widget feature config for user.
 * @property isChatEnabled chat feature config for user.
 * */
data class LiveLikeUser(
    val id: String,
    var nickname: String,
    val accessToken: String,
    var isWidgetEnabled: Boolean,
    var isChatEnabled: Boolean
)
