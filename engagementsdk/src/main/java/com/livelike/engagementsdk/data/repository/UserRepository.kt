package com.livelike.engagementsdk.data.repository

import com.google.gson.JsonObject
import com.livelike.engagementsdk.LiveLikeUser
import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.services.network.EngagementDataClientImpl
import com.livelike.engagementsdk.utils.SubscriptionManager
import com.livelike.engagementsdk.utils.liveLikeSharedPrefs.getNickename
import com.livelike.engagementsdk.utils.liveLikeSharedPrefs.setNickname
import com.livelike.engagementsdk.utils.logError
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Repository that handles user data. It knows what data sources need to be
 * triggered to get user and where to store the data.
 * In Typical frontend application we have local and remote data source, we will move towards that gradually[TODO].
 */
internal class UserRepository(private val clientId: String) {
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
    fun initUser(userAccessToken: String?) {
        if (userAccessToken == null) {
            dataClient.createUserData(clientId) {
                publishUser(it)
            }
        } else {
            dataClient.getUserData(clientId, accessToken = userAccessToken) {
                // TODO add Result class wrapper for network result instead of treating null as a case of invalid access token
                if (it == null) {
                    logError { "Network error or invalid access token" }
                    initUser(null)
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
            GlobalScope.launch {
                patchNickNameOnRemote(it)
            }
        }
        currentUserStream.onNext(it)
    }

    suspend fun updateChatNickname(nickname: String) {
        setNickname(nickname)
        currentUserStream.latest()?.apply {
            this.nickname = nickname
            currentUserStream.onNext(this)
            patchNickNameOnRemote(this)
        }
    }

    private suspend fun patchNickNameOnRemote(liveLikeUser: LiveLikeUser) {
        val jsonObject = JsonObject()
        jsonObject.addProperty("id", liveLikeUser.id)
        jsonObject.addProperty("nickname", liveLikeUser.nickname)
        dataClient.patchUser(clientId, jsonObject, userAccessToken)
    }
}
