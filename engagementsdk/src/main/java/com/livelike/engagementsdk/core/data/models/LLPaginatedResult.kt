package com.livelike.engagementsdk.core.data.models

class LLPaginatedResult<out T> {

    internal val previous: String? = null
    internal val next: String? = null
    val count: Int = 0
    val results: List<T>? = null

    fun hasNext(): Boolean {
        return next != null
    }

    fun hasPrev(): Boolean {
        return previous != null
    }
}
