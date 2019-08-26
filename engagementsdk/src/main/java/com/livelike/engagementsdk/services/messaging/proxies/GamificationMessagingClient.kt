package com.livelike.engagementsdk.services.messaging.proxies

import com.google.gson.JsonObject
import com.livelike.engagementsdk.EpochTime
import com.livelike.engagementsdk.services.messaging.ClientMessage
import com.livelike.engagementsdk.services.messaging.MessagingClient
import com.livelike.engagementsdk.utils.liveLikeSharedPrefs.getTotalPoints
import com.livelike.engagementsdk.utils.liveLikeSharedPrefs.pointTutorialSeen
import com.livelike.engagementsdk.utils.liveLikeSharedPrefs.shouldShowPointTutorial
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class GamificationMessagingClient(
    upstream: MessagingClient
) :
    MessagingClientProxy(upstream) {

    init {
        GlobalScope.launch {
            while (shouldShowPointTutorial()) {
                // Check if user scored points
                delay(5000)

                if (getTotalPoints() != 0 && getTotalPoints() < 20) {
                    // If first time scoring points, publish Points Tutorial
                    val message = ClientMessage(
                        JsonObject().apply {
                            addProperty("event", "points-tutorial")
                            add("payload", JsonObject().apply {
                                addProperty("id", "none")
                            })
                            addProperty("priority", 2)
                        },
                        "",
                        EpochTime(0),
                        5000
                    )
                    pointTutorialSeen()
                    listener?.onClientMessageEvent(this@GamificationMessagingClient, message)
                }
            }
        }
    }

    override fun publishMessage(message: String, channel: String, timeSinceEpoch: EpochTime) {
        upstream.publishMessage(message, channel, timeSinceEpoch)
    }

    override fun stop() {
        upstream.stop()
    }

    override fun resume() {
        upstream.resume()
    }

    override fun onClientMessageEvent(client: MessagingClient, event: ClientMessage) {
        listener?.onClientMessageEvent(client, event)
    }
}

internal fun MessagingClient.gamify(): GamificationMessagingClient {
    return GamificationMessagingClient(this)
}
