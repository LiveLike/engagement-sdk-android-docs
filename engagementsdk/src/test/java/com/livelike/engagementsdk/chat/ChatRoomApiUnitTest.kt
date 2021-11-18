package com.livelike.engagementsdk.chat

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.livelike.engagementsdk.*
import com.livelike.engagementsdk.publicapis.IEngagement
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import mockingAndroidServicesUsedByMixpanel
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
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
        mockingAndroidServicesUsedByMixpanel()
        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when (request.path) {
                    "/api/v1/applications/GaEBcpVrCxiJOSNu4bvX6krEaguxHR9Hlp63tK6L" -> MockResponse().apply {
                        setResponseCode(200)
                        setBody(
                            javaClass.classLoader.getResourceAsStream(APPLICATION_RESOURCE)
                                .readAll()
                        )
                    }
                    "/api/v1/applications/GaEBcpVrCxiJOSNu4bvX6krEaguxHR9Hlp63tK6L/profile/" -> MockResponse().apply {
                        setResponseCode(200)
                        setBody(
                            javaClass.classLoader.getResourceAsStream(USER_PROFILE_RESOURCE)
                                .readAll()
                        )
                    }
                    "/api/v1/chat-rooms/e8f3b5d2-3353-4c8e-b54e-32ecca6b7482/" -> MockResponse().apply {
                        setResponseCode(200)
                        setBody(
                            javaClass.classLoader.getResourceAsStream(SINGLE_CHATROOM_DETAIL_SUCCESS)
                                .readAll()
                        )
                    }
                    "/api/v1/chat-rooms/e8f3b5d2-3353-4c8e-b54e-32ecca6b7411/" -> MockResponse().apply {
                        setResponseCode(404)
                        setBody(
                            javaClass.classLoader.getResourceAsStream(SINGLE_CHATROOM_DETAIL_ERROR)
                                .readAll()
                        )
                    }
                    else -> MockResponse().apply {
                        setResponseCode(500)
                    }
                }
            }
        }
        sdk = EngagementSDK(
            "GaEBcpVrCxiJOSNu4bvX6krEaguxHR9Hlp63tK6L",
            context,
            null,
            "http://localhost:8080",
            null
        )
    }

    @After
    fun shutDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun test_chat_room_details_success() {
        val callback = Mockito.mock(LiveLikeCallback::class.java) as LiveLikeCallback<ChatRoomInfo>
        Thread.sleep(5000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        sdk.chat().getChatRoom("e8f3b5d2-3353-4c8e-b54e-32ecca6b7482", callback)
        val resultCaptor =
            argumentCaptor<ChatRoomInfo>()
        val errorCaptor =
            argumentCaptor<String>()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        Thread.sleep(2000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        verify(callback, timeout(2000)).onResponse(resultCaptor.capture(), errorCaptor.capture())
        println("ChatRoomApiUnitTest.test_chat_room_details_success111>${resultCaptor.firstValue.id}")
        assert(resultCaptor.firstValue.id == "e8f3b5d2-3353-4c8e-b54e-32ecca6b7482")
    }

    @Test
    fun test_chat_room_details_error() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        Thread.sleep(2000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val callback = Mockito.mock(LiveLikeCallback::class.java) as LiveLikeCallback<ChatRoomInfo>
        Thread.sleep(2000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        Thread.sleep(2000)
        sdk.chat().getChatRoom("e8f3b5d2-3353-4c8e-b54e-32ecca6b7411", callback)
        val resultCaptor =
            argumentCaptor<ChatRoomInfo>()
        val errorCaptor =
            argumentCaptor<String>()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        Thread.sleep(2000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        verify(callback, timeout(2000)).onResponse(resultCaptor.capture(), errorCaptor.capture())
        println("eRror>${errorCaptor.firstValue}")
        assert(errorCaptor.firstValue == "response code : 404 - Client Error")
    }
}