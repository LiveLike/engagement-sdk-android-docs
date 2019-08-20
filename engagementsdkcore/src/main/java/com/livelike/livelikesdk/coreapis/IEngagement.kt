package com.livelike.livelikesdk.coreapis

interface IEngagement {

//    TODO add remaining SDK functions in this interface

    /**
     * create or init user according to passed access token
     * if no access token new user profile will be created
     * if invalid token passed then also new user created with error. TODO: we need to review this behaviour
     */
    fun initUser(userAccessToken: String?)

    /**
     *  returns access token associated with user to be used for future sdk initialization
     *  this access token acts as a unique identifier for a user profile in LiveLike system
     *  null value means sdk initialization process not completed
     */
    fun getUserAccessToken(): String?
}
