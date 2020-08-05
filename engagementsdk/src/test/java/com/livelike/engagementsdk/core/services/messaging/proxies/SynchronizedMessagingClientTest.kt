package com.livelike.engagementsdk.core.services.messaging.proxies

import com.google.gson.JsonObject
import com.livelike.engagementsdk.EpochTime
import com.livelike.engagementsdk.core.services.messaging.ClientMessage
import com.livelike.engagementsdk.core.services.messaging.MessagingClient
import com.livelike.engagementsdk.core.services.messaging.MessagingEventListener
import com.livelike.engagementsdk.core.utils.LogLevel
import com.livelike.engagementsdk.core.utils.minimumLogLevel
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

class SynchronizedMessagingClientTest {

    @Mock
    private lateinit var messaingClient: MessagingClient

    private var timeSource: () -> EpochTime = {
        EpochTime(
            100L
        )
    }
    private lateinit var subject: SynchronizedMessagingClient
    private lateinit var listener: MessagingEventListener

    companion object {
        @JvmStatic
        @BeforeClass
        fun setUpClass() {
            minimumLogLevel = LogLevel.None
        }
    }

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        listener = mock(MessagingEventListener::class.java)
        subject = SynchronizedMessagingClient(messaingClient, timeSource, 86000L)
    }

    @Test
    fun `should publish event if timestamp zero`() {
        val clientMessage = ClientMessage(JsonObject(), "", EpochTime(0))
        subject.listener = listener
        subject.onClientMessageEvent(messaingClient, clientMessage)
        subject.processQueueForScheduledEvent()
        verify(listener).onClientMessageEvent(subject, clientMessage)
    }

    @Test
    fun `should not publish event if timestamp gt time`() {
        val clientMessage = ClientMessage(
            JsonObject(), "",
            EpochTime(timeSource.invoke().timeSinceEpochInMs + 50)
        )
        subject.listener = listener
        subject.onClientMessageEvent(messaingClient, clientMessage)
        subject.processQueueForScheduledEvent()
        verify(listener, never()).onClientMessageEvent(subject, clientMessage)
    }

    @Test
    fun `should publish event if timestamp lt time`() {
        val clientMessage = ClientMessage(
            JsonObject(), "",
            EpochTime(timeSource.invoke().timeSinceEpochInMs - 50)
        )
        subject.listener = listener
        subject.onClientMessageEvent(messaingClient, clientMessage)
        subject.processQueueForScheduledEvent()
        verify(listener).onClientMessageEvent(subject, clientMessage)
    }
}
