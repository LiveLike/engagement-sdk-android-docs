package com.livelike.engagementsdk.chat

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.example.example.PinMessageInfo
import com.livelike.engagementsdk.*
import com.livelike.engagementsdk.chat.data.remote.LiveLikePagination
import com.livelike.engagementsdk.chat.data.remote.PinMessageOrder
import com.livelike.engagementsdk.core.utils.gson
import com.livelike.engagementsdk.publicapis.IEngagement
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.publicapis.LiveLikeChatMessage
import com.livelike.engagementsdk.publicapis.LiveLikeEmptyResponse
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
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class ChatRoomApiUnitTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private var mockWebServer = MockWebServer()
    private lateinit var sdk: IEngagement

    @Before
    fun setup() {
        mockWebServer.takeRequest(1, TimeUnit.SECONDS)
        mockWebServer.start(8080)
        mockingAndroidServicesUsedByMixpanel()
        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                println("ChatRoomApiUnitTest.dispatch>${request.path}>>${request.method}")
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
                    "/api/v1/chat-rooms/" -> MockResponse().apply {
                        setResponseCode(200)
                        setBody(
                            javaClass.classLoader.getResourceAsStream("create_chatroom_resource.json")
                                .readAll()
                        )
                    }
                    "/api/v1/profiles/e58b8f5c-01b9-4423-bc2d-80a62ce28891/chat-room-memberships/" -> MockResponse().apply {
                        setResponseCode(200)
                        setBody(
                            javaClass.classLoader.getResourceAsStream("chat_room_list.json")
                                .readAll()
                        )
                    }
                    "/api/v1/chat-rooms/e8f3b5d2-3353-4c8e-b54e-32ecca6b7482/memberships/" -> when (request.method) {
                        "DELETE" -> MockResponse().apply {
                            setResponseCode(200)
                        }
                        else -> MockResponse().apply {
                            setResponseCode(200)
                            setBody(
                                javaClass.classLoader.getResourceAsStream("chat_room_members_list.json")
                                    .readAll()
                            )
                        }
                    }
                    "/api/v1/pin-message/" -> when (request.method) {
                        "POST" -> MockResponse().apply {
                            setResponseCode(200)
                            setBody(
                                javaClass.classLoader.getResourceAsStream("pin_message_success.json")
                                    .readAll()
                            )
                        }
                        else -> MockResponse().apply {
                            setResponseCode(500)
                        }
                    }
                    "/api/v1/pin-message/777e8267-2992-4e76-9001-faec2f552b57" -> when (request.method) {
                        "DELETE" -> MockResponse().apply {
                            setResponseCode(200)
                        }
                        else -> MockResponse().apply {
                            setResponseCode(500)
                        }
                    }
                    "/api/v1/pin-message/?chat_room_id=fc5feb36-6272-4e4b-8daa-5fe987bec9fc&ordering=pinned_at" -> MockResponse().apply {
                        setResponseCode(200)
                        setBody(
                            javaClass.classLoader.getResourceAsStream("pin_message_list.json")
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
        Thread.sleep(2000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
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
        val callback = Mockito.mock(LiveLikeCallback::class.java) as LiveLikeCallback<ChatRoomInfo>
        Thread.sleep(5000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        sdk.chat().getChatRoom("e8f3b5d2-3353-4c8e-b54e-32ecca6b7411", callback)
        Thread.sleep(2000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
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

    @Test
    fun test_create_chat_room_success() {
        val callback = Mockito.mock(LiveLikeCallback::class.java) as LiveLikeCallback<ChatRoomInfo>
        Thread.sleep(5000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        sdk.chat().createChatRoom("abc", Visibility.everyone, callback)
        Thread.sleep(2000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val resultCaptor =
            argumentCaptor<ChatRoomInfo>()
        val errorCaptor =
            argumentCaptor<String>()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        Thread.sleep(2000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        verify(callback, timeout(2000)).onResponse(resultCaptor.capture(), errorCaptor.capture())
        assert(resultCaptor.firstValue.title == "abc")
    }

    @Test
    fun test_user_chat_room_list() {
        val callback =
            Mockito.mock(LiveLikeCallback::class.java) as LiveLikeCallback<List<ChatRoomInfo>>
        Thread.sleep(5000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        sdk.chat().getCurrentUserChatRoomList(LiveLikePagination.FIRST, callback)
        Thread.sleep(2000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val resultCaptor =
            argumentCaptor<List<ChatRoomInfo>>()
        val errorCaptor =
            argumentCaptor<String>()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        Thread.sleep(2000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        verify(callback, timeout(2000)).onResponse(resultCaptor.capture(), errorCaptor.capture())
        assert(resultCaptor.firstValue.isNotEmpty() && resultCaptor.firstValue.size == 5 && resultCaptor.firstValue.first().title == "ChatRoom1623335984")
    }

    @Test
    fun test_chat_room_member_list() {
        val callback =
            Mockito.mock(LiveLikeCallback::class.java) as LiveLikeCallback<List<LiveLikeUser>>
        Thread.sleep(5000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        sdk.chat().getMembersOfChatRoom(
            "e8f3b5d2-3353-4c8e-b54e-32ecca6b7482",
            LiveLikePagination.FIRST,
            callback
        )
        Thread.sleep(2000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val resultCaptor =
            argumentCaptor<List<LiveLikeUser>>()
        val errorCaptor =
            argumentCaptor<String>()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        Thread.sleep(2000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        Thread.sleep(2000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        verify(callback, timeout(2000)).onResponse(resultCaptor.capture(), errorCaptor.capture())
        assert(resultCaptor.firstValue.isNotEmpty() && resultCaptor.firstValue.size == 1 && resultCaptor.firstValue.first().id == "7f70af22-3f32-4525-bb3b-6277b7775536")
    }

    @Test
    fun test_delete_chat_room_membership() {
        val callback =
            Mockito.mock(LiveLikeCallback::class.java) as LiveLikeCallback<LiveLikeEmptyResponse>
        Thread.sleep(5000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        sdk.chat().deleteCurrentUserFromChatRoom(
            "e8f3b5d2-3353-4c8e-b54e-32ecca6b7482",
            callback
        )
        Thread.sleep(2000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val resultCaptor =
            argumentCaptor<LiveLikeEmptyResponse>()
        val errorCaptor =
            argumentCaptor<String>()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        Thread.sleep(2000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        Thread.sleep(2000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        verify(callback, timeout(2000)).onResponse(resultCaptor.capture(), errorCaptor.capture())
        assert(resultCaptor.firstValue != null && resultCaptor.firstValue::class.java == LiveLikeEmptyResponse::class.java)
    }

    @Test
    fun pin_message_in_chat_room() {
        val callback =
            Mockito.mock(LiveLikeCallback::class.java) as LiveLikeCallback<PinMessageInfo>
        Thread.sleep(5000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        sdk.chat().pinMessage(
            "777e8267-2992-4e76-9001-faec2f552b57",
            "fc5feb36-6272-4e4b-8daa-5fe987bec9fc",
            gson.fromJson(
                "{\"custom_data\":\"\",\"created_at\":\"2021-12-01T07:10:33+0000\",\"filtered_sender_nickname\":null,\"image_height\":150,\"image_width\":150,\"content_filter\":[],\"sender_id\":\"1c285e1a-aa93-4135-8361-ecd27317c66a\",\"sender_profile_url\":\"https://cf-blast-dig.livelikecdn.com/api/v1/profiles/1c285e1a-aa93-4135-8361-ecd27317c66a/\",\"sender_image_url\":null,\"badge_image_url\":null,\"program_date_time\":null,\"filtered_message\":null,\"id\":\"777e8267-2992-4e76-9001-faec2f552b57\",\"sender_nickname\":\"Virtual Champion\",\"message\":\"hfhckxul\"}",
                LiveLikeChatMessage::class.java
            ),
            callback
        )
        Thread.sleep(3000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val resultCaptor =
            argumentCaptor<PinMessageInfo>()
        val errorCaptor =
            argumentCaptor<String>()
        Thread.sleep(2000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        Thread.sleep(2000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        verify(callback, timeout(2000)).onResponse(resultCaptor.capture(), errorCaptor.capture())
        assert(resultCaptor.firstValue.messageId == "777e8267-2992-4e76-9001-faec2f552b57" && resultCaptor.firstValue.chatRoomId == "fc5feb36-6272-4e4b-8daa-5fe987bec9fc")
    }

    @Test
    fun unpin_message_in_chat_room() {
        val callback =
            Mockito.mock(LiveLikeCallback::class.java) as LiveLikeCallback<LiveLikeEmptyResponse>
        Thread.sleep(5000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        sdk.chat().unPinMessage(
            "777e8267-2992-4e76-9001-faec2f552b57",
            callback
        )
        Thread.sleep(3000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val resultCaptor =
            argumentCaptor<LiveLikeEmptyResponse>()
        val errorCaptor =
            argumentCaptor<String>()
        Thread.sleep(2000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        Thread.sleep(2000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        verify(callback, timeout(2000)).onResponse(resultCaptor.capture(), errorCaptor.capture())
        assert(resultCaptor.firstValue != null && resultCaptor.firstValue::class.java == LiveLikeEmptyResponse::class.java)
    }

    @Test
    fun get_list_of_pin_message_in_chat_room() {
        val callback =
            Mockito.mock(LiveLikeCallback::class.java) as LiveLikeCallback<List<PinMessageInfo>>
        Thread.sleep(5000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        sdk.chat().getPinMessageInfoList(
            "fc5feb36-6272-4e4b-8daa-5fe987bec9fc",
            PinMessageOrder.ASC,
            LiveLikePagination.FIRST,
            callback
        )
        Thread.sleep(3000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val resultCaptor =
            argumentCaptor<List<PinMessageInfo>>()
        val errorCaptor =
            argumentCaptor<String>()
        Thread.sleep(2000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        Thread.sleep(2000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        verify(callback, timeout(2000)).onResponse(resultCaptor.capture(), errorCaptor.capture())
        assert(resultCaptor.firstValue != null && resultCaptor.firstValue.isNotEmpty() && resultCaptor.firstValue.first().id == "37e1720a-fc7b-4962-b216-6be9ed69dc96")
    }


}