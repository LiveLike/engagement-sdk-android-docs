package com.livelike.engagementsdk.services.messaging.pubnub

import com.pubnub.api.PubNub
import com.pubnub.api.callbacks.SubscribeCallback
import com.pubnub.api.models.consumer.PNStatus
import com.pubnub.api.models.consumer.pubsub.PNMessageResult
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult
import com.pubnub.api.models.consumer.pubsub.PNSignalResult
import com.pubnub.api.models.consumer.pubsub.message_actions.PNMessageActionResult
import com.pubnub.api.models.consumer.pubsub.objects.PNMembershipResult
import com.pubnub.api.models.consumer.pubsub.objects.PNSpaceResult
import com.pubnub.api.models.consumer.pubsub.objects.PNUserResult

/**
* This adapter class provides empty implementations of the methods from pubnub subscribe callbacks.
* Any custom listener that cares only about a subset of the methods of this listener can simply
* subclass this adapter class instead of implementing the class directly.
**/
internal abstract class PubnubSubscribeCallbackAdapter : SubscribeCallback() {

    override fun signal(pubnub: PubNub, pnSignalResult: PNSignalResult) {
    }

    override fun status(pubnub: PubNub, pnStatus: PNStatus) {
    }

    override fun user(pubnub: PubNub, pnUserResult: PNUserResult) {
    }

    override fun messageAction(pubnub: PubNub, pnMessageActionResult: PNMessageActionResult) {
    }

    override fun presence(pubnub: PubNub, pnPresenceEventResult: PNPresenceEventResult) {
    }

    override fun membership(pubnub: PubNub, pnMembershipResult: PNMembershipResult) {
    }

    override fun message(pubnub: PubNub, pnMessageResult: PNMessageResult) {
    }

    override fun space(pubnub: PubNub, pnSpaceResult: PNSpaceResult) {
    }
}
