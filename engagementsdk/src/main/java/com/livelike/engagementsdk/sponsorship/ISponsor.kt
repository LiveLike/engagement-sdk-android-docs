package com.livelike.engagementsdk.sponsorship

import com.livelike.engagementsdk.publicapis.LiveLikeCallback

interface ISponsor {

    /**
     * Fetch sponsor associated to the specified program Id
     **/
    fun fetchByProgramId(programId: String, callback: LiveLikeCallback<List<SponsorModel>>)
    /**
     * Fetch sponsor associated to the specified application
     **/
    fun fetchForApplication(callback: LiveLikeCallback<List<SponsorModel>>)
    /**
     * Fetch sponsor associated to the specified chatRoom Id
     **/
    fun fetchByChatRoomId(chatRoomId: String, callback: LiveLikeCallback<List<SponsorModel>>)
}
