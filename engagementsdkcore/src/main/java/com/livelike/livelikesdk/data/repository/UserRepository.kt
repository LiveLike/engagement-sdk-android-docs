package com.livelike.livelikesdk.data.repository

import com.livelike.engagementsdkapi.LiveLikeUser
import com.livelike.livelikesdk.Stream
import com.livelike.livelikesdk.services.network.EngagementDataClientImpl
import com.livelike.livelikesdk.utils.SubscriptionManager
import com.livelike.livelikesdk.utils.liveLikeSharedPrefs.getNickename
import com.livelike.livelikesdk.utils.logError

/**
 * Repository that handles user data. It knows what data sources need to be
 * triggered to get user and where to store the data.
 * In Typical frontend application we have local and remote data source, we will move towards that gradually[TODO].
 */
internal object UserRepository {

    private val dataClient = EngagementDataClientImpl()

    /**
     *  User returned will be new if no access-token passed.
     *  Null latest value in stream means sdk initialization process not completed yet.
     */
    val currentUserStream: Stream<LiveLikeUser> = SubscriptionManager()

    val userAccessToken: String?
        get() = currentUserStream.latest()?.accessToken

    /**
     * Create or init user according to passed access token.
     * If no access token new user profile will be created.
     * If invalid token passed then also new user created with error.
     */
    fun initUser(clientId: String, userAccessToken: String?) {

        if (userAccessToken == null) {
            dataClient.createUserData(clientId) {
                publishUser(it)
            }
        } else {
            dataClient.getUserData(clientId, accessToken = userAccessToken) {
                // TODO add Result class wrapper for network result instead of treating null as a case of invalid access token
                if (it == null) {
                    logError { "Network error or invalid access token" }
                    initUser(clientId, null)
                } else {
                    publishUser(it)
                }
            }
        }
    }

    private fun publishUser(it: LiveLikeUser) {
        val nickname =
            getNickename() // Checking again the saved nickname as it could have changed during the web request.
        if (nickname.isNotEmpty()) {
            it.nickname = nickname
        }
        currentUserStream.onNext(it)
    }
}
