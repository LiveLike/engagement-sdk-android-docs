package com.livelike.engagementsdk.sponsorship

import com.livelike.engagementsdk.chat.data.remote.LiveLikePagination
import com.livelike.engagementsdk.publicapis.LiveLikeCallback

interface ISponsor {

    /**
     * Fetch sponsor associated to the specified program Id
     **/
    fun fetchByProgramId(
        programId: String,
        pagination: LiveLikePagination,
        callback: LiveLikeCallback<List<SponsorModel>>
    )

    /**
     * Fetch sponsor associated to the specified application
     **/
    fun fetchForApplication(
        pagination: LiveLikePagination,
        callback: LiveLikeCallback<List<SponsorModel>>
    )

    /**
     * Fetch sponsor associated to the specified chatRoom Id
     **/
    fun fetchByChatRoomId(
        chatRoomId: String,
        pagination: LiveLikePagination,
        callback: LiveLikeCallback<List<SponsorModel>>
    )
}
