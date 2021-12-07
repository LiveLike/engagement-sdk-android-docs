package com.livelike.engagementsdk.chat

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.livelike.engagementsdk.*
import com.livelike.engagementsdk.chat.data.remote.LiveLikePagination
import com.livelike.engagementsdk.publicapis.BlockedInfo
import com.livelike.engagementsdk.publicapis.IEngagement
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
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
                    "/api/v1/profile-blocks/" -> MockResponse().apply {
                        setResponseCode(200)
                        setBody(
                            javaClass.classLoader.getResourceAsStream("block_profile_response.json")
                                .readAll()
                        )
                    }
                    "/api/v1/profile-blocks/?blocked_profile_id=" -> MockResponse().apply {
                        setResponseCode(200)
                        setBody(
                            javaClass.classLoader.getResourceAsStream("block_profile_list_response.json")
                                .readAll()
                        )
                    }
                    "/api/v1/profile-blocks/5cb2295b-fde9-488a-8105-c2f5cbdd7801/" -> when (request.method) {
                        "DELETE" -> MockResponse().apply {
                            setResponseCode(200)
                        }
                        else -> MockResponse().apply {
                            setResponseCode(500)
                        }
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

    @Test
    fun test_create_chat_room_success() {
        val callback = Mockito.mock(LiveLikeCallback::class.java) as LiveLikeCallback<ChatRoomInfo>
        Thread.sleep(5000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        sdk.chat().createChatRoom("abc", Visibility.everyone, callback)
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
    fun test_block_user() {
        val callback =
            Mockito.mock(LiveLikeCallback::class.java) as LiveLikeCallback<BlockedInfo>
        Thread.sleep(5000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        sdk.chat().blockProfile(
            "5cb2295b-fde9-488a-8105-c2f5cbdd7801",
            callback
        )
        val resultCaptor =
            argumentCaptor<BlockedInfo>()
        val errorCaptor =
            argumentCaptor<String>()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        Thread.sleep(2000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        Thread.sleep(2000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        verify(callback, timeout(2000)).onResponse(resultCaptor.capture(), errorCaptor.capture())
        assert(resultCaptor.firstValue.blockedProfileID == "5cb2295b-fde9-488a-8105-c2f5cbdd7801")
    }

    @Test
    fun test_unblock_user() {
        val callback =
            Mockito.mock(LiveLikeCallback::class.java) as LiveLikeCallback<LiveLikeEmptyResponse>
        Thread.sleep(5000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        sdk.chat().unBlockProfile(
            "5cb2295b-fde9-488a-8105-c2f5cbdd7801",
            callback
        )
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
        assert(resultCaptor.firstValue::class.java == LiveLikeEmptyResponse::class.java)
    }

    @Test
    fun test_list_of_block_users() {
        val callback =
            Mockito.mock(LiveLikeCallback::class.java) as LiveLikeCallback<List<BlockedInfo>>
        Thread.sleep(5000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        sdk.chat().getBlockedProfileList(
            LiveLikePagination.FIRST,
            callback
        )
        val resultCaptor =
            argumentCaptor<List<BlockedInfo>>()
        val errorCaptor =
            argumentCaptor<String>()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        Thread.sleep(2000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        Thread.sleep(2000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        verify(callback, timeout(2000)).onResponse(resultCaptor.capture(), errorCaptor.capture())
        assert(resultCaptor.firstValue.isNotEmpty() && resultCaptor.firstValue.first().blockedProfileID == "5cb2295b-fde9-488a-8105-c2f5cbdd7801")
    }


}