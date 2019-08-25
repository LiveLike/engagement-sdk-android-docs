package com.livelike.livelikesdk.publicapis

import com.livelike.livelikesdk.Stream

interface IEngagement {

//    TODO add remaining public SDK functions in this interface

    /**
     *  Returns access token associated with user to be used for future sdk initialization.
     *  This access token acts as a unique identifier for a user profile in LiveLike system.
     *  Null value means sdk initialization process not completed.
     */
    val userAccessToken: String?

    /**
     * Returns public user stream.
     */
    val userStream: Stream<LiveLikeUserApi>

    /** Override the default auto-generated chat nickname **/
    fun updateChatNickname(nickname: String)
}
