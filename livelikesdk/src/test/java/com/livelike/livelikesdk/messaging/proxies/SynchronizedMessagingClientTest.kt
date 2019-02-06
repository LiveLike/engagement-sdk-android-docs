package com.livelike.livelikesdk.messaging.proxies

import com.google.gson.JsonObject
import com.livelike.livelikesdk.messaging.ClientMessage
import com.livelike.livelikesdk.messaging.MessagingClient
import com.livelike.livelikesdk.messaging.MessagingEventListener
import com.livelike.livelikesdk.messaging.EpochTime
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

class SynchronizedMessagingClientTest {

    @Mock lateinit var messaingClient: MessagingClient

    private var timeSource: EpochTime = EpochTime(100L)
    private lateinit var  subject : SynchronizedMessagingClient
    private lateinit var listener : MessagingEventListener

    @Before
    fun setup(){
        MockitoAnnotations.initMocks(this)
        listener = mock(MessagingEventListener::class.java)
        subject = SynchronizedMessagingClient(messaingClient, timeSource)
    }

    @Test
    fun `should publish event if timestamp zero` (){
        val clientMessage = ClientMessage( JsonObject(),"", EpochTime(0) )
        subject.onClientMessageEvent(messaingClient, clientMessage)
        subject.listener = listener
        subject.processQueueForScheduledEvent()
        verify(listener).onClientMessageEvent(subject, clientMessage)
    }

    @Test
    fun `should publish event if timestamp gt time` (){
        val clientMessage = ClientMessage( JsonObject(),"",   EpochTime(timeSource.timeSinceEpochInMs + 50 ))
        subject.onClientMessageEvent(messaingClient, clientMessage)
        subject.listener = listener
        subject.processQueueForScheduledEvent()
        verify(listener).onClientMessageEvent(subject, clientMessage)
    }

    @Test
    fun `should not publish event if timestamp lt time` (){
        val clientMessage = ClientMessage( JsonObject(),"",   EpochTime(timeSource.timeSinceEpochInMs - 50 ))
        subject.onClientMessageEvent(messaingClient, clientMessage)
        subject.listener = listener
        subject.processQueueForScheduledEvent()
        verify(listener, never()).onClientMessageEvent(subject, clientMessage)
    }
}
