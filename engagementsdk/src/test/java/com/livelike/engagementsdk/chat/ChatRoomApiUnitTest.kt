package com.livelike.engagementsdk.chat

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.livelike.engagementsdk.*
import com.livelike.engagementsdk.publicapis.IEngagement
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import mockingAndroidServicesUsedByMixpanel
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import readAll

@RunWith(RobolectricTestRunner::class)
class ChatRoomApiUnitTest {

    val context = ApplicationProvider.getApplicationContext<Context>()
    private var mockWebServer = MockWebServer()

    private lateinit var sdk: IEngagement

    init {
        mockWebServer = MockWebServer()
    }

    @Before
    fun setup() {
        mockWebServer.start(8080)
        mockWebServer.enqueue(MockResponse().apply {
            setResponseCode(200)
            setBody(
                javaClass.classLoader.getResourceAsStream(APPLICATION_RESOURCE).readAll()
            )
        })
        mockWebServer.enqueue(MockResponse().apply {
            setResponseCode(200)
            setBody(
                javaClass.classLoader.getResourceAsStream(USER_PROFILE_RESOURCE).readAll()
            )
        })

        mockingAndroidServicesUsedByMixpanel()

        sdk = EngagementSDK(
            "GaEBcpVrCxiJOSNu4bvX6krEaguxHR9Hlp63tK6L",
            context,
            null,
            "http://localhost:8080/",
            null
        )
    }

    @After
    fun shutDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun test_chat_room_details_success() {
        mockWebServer.enqueue(MockResponse().apply {
            setResponseCode(200)
            setBody(
                javaClass.classLoader.getResourceAsStream(SINGLE_CHATROOM_DETAIL_SUCCESS).readAll()
            )
        })
        val callback = Mockito.mock(LiveLikeCallback::class.java) as LiveLikeCallback<ChatRoomInfo>
        Thread.sleep(5000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        println("ChatRoomApiUnitTest.test_chat_room_details_success111")
        sdk.chat().getChatRoom("e8f3b5d2-3353-4c8e-b54e-32ecca6b7482", callback)
        val resultCaptor =
            argumentCaptor<ChatRoomInfo>()
        val errorCaptor =
            argumentCaptor<String>()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        Thread.sleep(5000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        verify(callback, timeout(5000)).onResponse(resultCaptor.capture(), errorCaptor.capture())
        assert(resultCaptor.firstValue.id == "e8f3b5d2-3353-4c8e-b54e-32ecca6b7482")
    }

    @Test
    fun test_chat_room_details_error() {
        mockWebServer.enqueue(MockResponse().apply {
            setResponseCode(404)
            setBody(
                javaClass.classLoader.getResourceAsStream(SINGLE_CHATROOM_DETAIL_ERROR).readAll()
            )
        })
        val callback = Mockito.mock(LiveLikeCallback::class.java) as LiveLikeCallback<ChatRoomInfo>
        Thread.sleep(5000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        sdk.chat().getChatRoom("e8f3b5d2-3353-4c8e-b54e-32ecca6b7411", callback)
        val resultCaptor =
            argumentCaptor<ChatRoomInfo>()
        val errorCaptor =
            argumentCaptor<String>()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        Thread.sleep(5000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        verify(callback, timeout(5000)).onResponse(resultCaptor.capture(), errorCaptor.capture())
        println("eRror>${errorCaptor.firstValue}")
        assert(errorCaptor.firstValue == "No chat room found.")
    }
}