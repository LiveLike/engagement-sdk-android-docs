package com.livelike.livelikesdk.messaging

import com.google.gson.JsonObject


/**
 *  Represents a messaging client which LiveLike uses to communicate with a widget or chat backend source
 */
interface MessagingClient {
    fun subscribe(channels: List<String>)
    fun unsubscribe(channels: List<String>)
    fun unsubscribeAll()
    fun addMessagingEventListener(listener: MessagingEventListener)
}

/**
 *  Represents a messaging client triggerListener which will receive MessagingClient messages
 */
interface MessagingEventListener {
    fun onClientMessageEvent(client: MessagingClient, event: ClientMessage)
    fun onClientMessageError(client: MessagingClient, error: Error)
    fun onClientMessageStatus(client: MessagingClient, status: ConnectionStatus)
}


/**
 * Represents a client message that can be sent from a MessagingClient
 */
data class ClientMessage(
    val message: JsonObject,
    val channel: String,
    val timeStamp: EpochTime,
    val timeoutMs: Long = 4000
)

/**
 * Represents a MessagingClient error that can be sent from a MessagingClient
 */
data class Error(val type: String, val message: String)

/**
 * Represents the ConnectionStatus of a MessagingClient
 */
enum class ConnectionStatus {
    CONNECTED,
    DISCONNECTED
}


class EpochTime(val timeSinceEpochInMs: Long) : Comparable<EpochTime> {
    override fun compareTo(other: EpochTime): Int {
        return timeSinceEpochInMs.compareTo(other.timeSinceEpochInMs)
    }

    operator fun minus(et: EpochTime) = EpochTime(timeSinceEpochInMs - et.timeSinceEpochInMs)
    operator fun minus(timeStamp: Long) = EpochTime(timeSinceEpochInMs - timeStamp)
}
