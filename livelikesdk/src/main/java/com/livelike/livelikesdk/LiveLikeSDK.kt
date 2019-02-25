package com.livelike.livelikesdk

import com.livelike.livelikesdk.messaging.EpochTime

/**
 * Use this class to initialize the LiveLike SDK. This is the entry point for SDK usage. This creates a singleton instance of LiveLike SDK.
 * The SDK is expected to be initialized only once. Once the SDK has been initialized, user can create multiple sessions
 * using [createContentSession]
 *
 * @param appId Application's id
 */

class LiveLikeSDK(appId: String) {
    var appId : String = appId

    init {
        // Initialize
    }

    /**
     *  Creates a content session.
     *  @param contentId
     *  @param currentPlayheadTime
     */
    fun createContentSession(contentId: String,
                             currentPlayheadTime: (() -> EpochTime)
    ): LiveLikeContentSessionImpl {
        return LiveLikeContentSessionImpl(contentId, currentPlayheadTime)
    }
}
