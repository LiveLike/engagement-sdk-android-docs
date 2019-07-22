package com.livelike.livelikesdk.services.messaging

import com.google.gson.JsonObject
import com.livelike.engagementsdkapi.EpochTime

/**
 *  Represents a messaging client which LiveLike uses to communicate with a widget or chat backend source
 */
internal interface MessagingClient {
    fun subscribe(channels: List<String>)
    fun unsubscribe(channels: List<String>)
    fun unsubscribeAll()
    fun publishMessage(message: String, channel: String, timeSinceEpoch: EpochTime)
    fun stop()
    fun resume()
    fun addMessagingEventListener(listener: MessagingEventListener)
}

/**
 *  Represents a messaging client triggerListener which will receive MessagingClient messages
 */
internal interface MessagingEventListener {
    fun onClientMessageEvent(client: MessagingClient, event: ClientMessage)
    fun onClientMessageError(client: MessagingClient, error: Error)
    fun onClientMessageStatus(client: MessagingClient, status: ConnectionStatus)
}

/**
 * Represents a client message that can be sent from a MessagingClient
 */
internal data class ClientMessage(
    val message: JsonObject,
    val channel: String,
    val timeStamp: EpochTime,
    val timeoutMs: Long = 4000
)

/**
 * Represents a MessagingClient error that can be sent from a MessagingClient
 */
internal data class Error(val type: String, val message: String)

/**
 * Represents the ConnectionStatus of a MessagingClient
 */
internal enum class ConnectionStatus {
    CONNECTED,
    DISCONNECTED
}
