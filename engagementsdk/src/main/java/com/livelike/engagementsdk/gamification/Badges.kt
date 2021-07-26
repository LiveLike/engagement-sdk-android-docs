package com.livelike.engagementsdk.gamification

import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.LiveLikeUser
import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.TEMPLATE_PROFILE_ID
import com.livelike.engagementsdk.chat.data.remote.LiveLikePagination
import com.livelike.engagementsdk.chat.services.network.ChatDataClient
import com.livelike.engagementsdk.core.data.models.LLPaginatedResult
import com.livelike.engagementsdk.core.data.models.LeaderBoardEntryResult
import com.livelike.engagementsdk.core.data.respository.BaseRepository
import com.livelike.engagementsdk.core.data.respository.UserRepository
import com.livelike.engagementsdk.core.exceptionhelpers.safeRemoteApiCall
import com.livelike.engagementsdk.core.services.network.EngagementDataClientImpl
import com.livelike.engagementsdk.core.services.network.EngagementSdkDataClient
import com.livelike.engagementsdk.core.services.network.RequestType
import com.livelike.engagementsdk.core.services.network.Result
import com.livelike.engagementsdk.core.utils.toFlow
import com.livelike.engagementsdk.core.utils.validateUuid
import com.livelike.engagementsdk.gamification.models.ProfileBadge
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import okhttp3.internal.wait

class Badges internal constructor(
    private val applicationResourceStream: Stream<EngagementSDK.SdkConfiguration>,
    private val dataClient: EngagementDataClientImpl,
    private val sdkScope: CoroutineScope
) {


    private var profileBadgesResultMap = mutableMapOf<String, LLPaginatedResult<ProfileBadge>>()


    /**
     * fetch all the badges associated to provided profile id in pages
     * to fetch next page function need to be called again with LiveLikePagination.NEXT and for first call as LiveLikePagination.FIRST
     **/
    fun getProfileBadges(
        profileId: String,
        liveLikePagination: LiveLikePagination,
        liveLikeCallback: LiveLikeCallback<LLPaginatedResult<ProfileBadge>>
    ) {

        if (!validateUuid(profileId)) {
            liveLikeCallback.onResponse(null, "Invalid Profile ID")
            return
        }

        val result = profileBadgesResultMap[profileId]

        var fetchUrl: String? = null

        sdkScope.launch {
            if (result == null || liveLikePagination == LiveLikePagination.FIRST) {
                applicationResourceStream.toFlow().collect { applicatoionResource ->
                    applicatoionResource?.let {
                        dataClient.remoteCall<LiveLikeUser>(
                            it.profileDetailUrlTemplate.replace(
                                TEMPLATE_PROFILE_ID, profileId
                            ), RequestType.GET, null, null
                        ).run {
                            if (this is Result.Success) {
                                fetchUrl = this.data.badgesUrl
                            }
                        }
                    }
                }
            } else {
                fetchUrl = when (liveLikePagination) {
                    LiveLikePagination.NEXT -> result.next
                    LiveLikePagination.PREVIOUS -> result.previous
                    else -> null
                }
            }

            if (fetchUrl == null) {
                liveLikeCallback.onResponse(null, "No more data")
            } else {
                dataClient.remoteCall<LLPaginatedResult<ProfileBadge>>(
                    fetchUrl ?: "",
                    RequestType.GET,
                    null,
                    null
                ).run {
                    if (this is Result.Success) {
                        profileBadgesResultMap[profileId] = this.data
                    }
                    liveLikeCallback.processResult(this)
                }
            }

        }
    }


}