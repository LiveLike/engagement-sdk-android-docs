package com.livelike.engagementsdk.publicapis

import com.livelike.engagementsdk.services.network.Result

abstract class LiveLikeCallback<T : Any> {

    abstract fun onResponse(result: T?, error: String?)

    internal fun processResult(result: Result<T>) {
        if (result is Result.Success) {
            onResponse(result.data, null)
        } else if (result is Result.Error) {
            onResponse(null, result.exception.message ?: "Error in fetching data")
        }
    }
}
