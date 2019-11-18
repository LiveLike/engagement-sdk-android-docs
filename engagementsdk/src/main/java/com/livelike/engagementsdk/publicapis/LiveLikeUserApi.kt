package com.livelike.engagementsdk.publicapis

/**
 * User pojo to be exposed, should be minimal in terms of fields
 */
data class LiveLikeUserApi(
    var nickname: String,
    val accessToken: String
)

data class LiveLikeChatMessage(val nickname: String,val userPic:String?, val message: String, val timestamp: String, val id : Long)