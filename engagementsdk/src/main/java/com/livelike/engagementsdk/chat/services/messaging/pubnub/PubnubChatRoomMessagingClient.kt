package com.livelike.engagementsdk.chat.services.messaging.pubnub

import com.google.gson.reflect.TypeToken
import com.livelike.engagementsdk.chat.data.remote.PubnubChatEvent
import com.livelike.engagementsdk.chat.data.remote.PubnubChatEventType
import com.livelike.engagementsdk.chat.data.remote.toPubnubChatEventType
import com.livelike.engagementsdk.core.utils.extractStringOrEmpty
import com.livelike.engagementsdk.core.utils.gson
import com.livelike.engagementsdk.core.utils.logError
import com.livelike.engagementsdk.core.utils.logVerbose
import com.livelike.engagementsdk.publicapis.ChatRoomAdd
import com.livelike.engagementsdk.publicapis.ChatRoomDelegate
import com.livelike.engagementsdk.publicapis.ChatRoomInvitation
import com.livelike.engagementsdk.widget.services.messaging.pubnub.PubnubMessagingClient
import com.livelike.engagementsdk.widget.services.messaging.pubnub.PubnubSubscribeCallbackAdapter
import com.pubnub.api.PubNub
import com.pubnub.api.callbacks.PNCallback
import com.pubnub.api.models.consumer.PNStatus
import com.pubnub.api.models.consumer.pubsub.PNMessageResult
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult

internal open class PubnubChatRoomMessagingClient(
    subscriberKey: String,
    pubnubHeartbeatInterval: Int,
    uuid: String,
    pubnubPresenceTimeout: Int
) : PubnubMessagingClient(subscriberKey, pubnubHeartbeatInterval, uuid, pubnubPresenceTimeout) {

    var chatRoomDelegate: ChatRoomDelegate? = null

    override fun addPubnubListener() {
        pubnub.addListener(object : PubnubSubscribeCallbackAdapter() {
            override fun status(pubnub: PubNub, pnStatus: PNStatus) {

            }

            override fun message(pubnub: PubNub, pnMessageResult: PNMessageResult) {
                logMessage(pnMessageResult)
                val event = pnMessageResult.message.asJsonObject.extractStringOrEmpty("event")
                    .toPubnubChatEventType()
                when (event) {
                    PubnubChatEventType.CHATROOM_ADDED -> {
                        val pubnubChatRoomEvent: PubnubChatEvent<ChatRoomAdd> = gson.fromJson(
                            pnMessageResult.message.asJsonObject,
                            object : TypeToken<PubnubChatEvent<ChatRoomAdd>>() {}.type
                        )
                        chatRoomDelegate?.onNewChatRoomAdded(pubnubChatRoomEvent.payload)
                    }
                    PubnubChatEventType.CHATROOM_INVITE -> {
                        val pubnubChatRoomEvent: PubnubChatEvent<ChatRoomInvitation> = gson.fromJson(
                            pnMessageResult.message.asJsonObject,
                            object : TypeToken<PubnubChatEvent<ChatRoomInvitation>>() {}.type
                        )
                        chatRoomDelegate?.onReceiveInvitation(pubnubChatRoomEvent.payload)
                    }
                    else -> {
                        logError { "Event: $event not supported"  }
                    }
                }
            }

            override fun presence(pubnub: PubNub, pnPresenceEventResult: PNPresenceEventResult) {}

            fun logMessage(message: PNMessageResult) {
                logVerbose { "Message publisher: " + message.publisher }
                logVerbose { "Message Payload: " + message.message }
                logVerbose { "Message Subscription: " + message.subscription }
                logVerbose { "Message Channel: " + message.channel }
                logVerbose { "Message timetoken: " + message.timetoken!! }
            }
        })
    }
}