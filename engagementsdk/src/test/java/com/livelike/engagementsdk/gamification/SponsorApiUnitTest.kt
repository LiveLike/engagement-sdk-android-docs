package com.livelike.engagementsdk.gamification

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.livelike.engagementsdk.*
import com.livelike.engagementsdk.chat.data.remote.LiveLikePagination
import com.livelike.engagementsdk.publicapis.IEngagement
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.sponsorship.SponsorModel
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
import org.robolectric.Shadows.shadowOf
import readAll
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class SponsorApiUnitTest {

    val context = ApplicationProvider.getApplicationContext<Context>()

    private var mockWebServer = MockWebServer()


    private lateinit var sdk: IEngagement

    init {
        mockWebServer = MockWebServer()
    }

    @Before
    fun setup() {
        mockWebServer.takeRequest(1, TimeUnit.SECONDS)
        mockWebServer.start(8080)
        mockingAndroidServicesUsedByMixpanel()
        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                println("SponsorApiUnitTest.dispatch>${request.path}")
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
                    "/programs/498591f4-9d6b-4943-9671-f44d3afbb6a4/program.json" -> MockResponse().apply {
                        setResponseCode(200)
                        setBody(
                            javaClass.classLoader.getResourceAsStream(PROGRAM_RESOURCE)
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
                    "/api/v1/sponsors/?program_id=498591f4-9d6b-4943-9671-f44d3afbb6a4",
                    "/sponsors/e8f3b5d2-3353-4c8e-b54e-32ecca6b7482/sponsors.json",
                    "/api/v1/sponsors/?client_id=GaEBcpVrCxiJOSNu4bvX6krEaguxHR9Hlp63tK6L"
                    -> MockResponse().apply {
                        setResponseCode(200)
                        setBody(
                            javaClass.classLoader.getResourceAsStream("sponsor_list.json")
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
    fun sponsorApi_program_success_with_data() {
        val callback =
            Mockito.mock(LiveLikeCallback::class.java) as LiveLikeCallback<List<SponsorModel>>
        Thread.sleep(5000)
        shadowOf(Looper.getMainLooper()).idle()
        sdk.sponsor().fetchByProgramId(
            "498591f4-9d6b-4943-9671-f44d3afbb6a4",
            LiveLikePagination.FIRST,
            callback
        )
        Thread.sleep(2000)
        shadowOf(Looper.getMainLooper()).idle()
        val resultCaptor =
            argumentCaptor<List<SponsorModel>>()
        val errorCaptor =
            argumentCaptor<String>()
        shadowOf(Looper.getMainLooper()).idle()
        Thread.sleep(2000)
        shadowOf(Looper.getMainLooper()).idle()
        verify(callback, timeout(5000)).onResponse(resultCaptor.capture(), errorCaptor.capture())
        assert(resultCaptor.firstValue.isNotEmpty())
        assert(resultCaptor.firstValue[0].name == "sponsor1")
    }

    @Test
    fun sponsorApi_chatRoom_success_with_data() {
        val callback =
            Mockito.mock(LiveLikeCallback::class.java) as LiveLikeCallback<List<SponsorModel>>
        Thread.sleep(5000)
        shadowOf(Looper.getMainLooper()).idle()
        sdk.sponsor().fetchByChatRoomId(
            "e8f3b5d2-3353-4c8e-b54e-32ecca6b7482",
            LiveLikePagination.FIRST,
            callback
        )
        Thread.sleep(2000)
        shadowOf(Looper.getMainLooper()).idle()
        val resultCaptor =
            argumentCaptor<List<SponsorModel>>()
        val errorCaptor =
            argumentCaptor<String>()
        shadowOf(Looper.getMainLooper()).idle()
        Thread.sleep(2000)
        shadowOf(Looper.getMainLooper()).idle()
        verify(callback, timeout(5000)).onResponse(resultCaptor.capture(), errorCaptor.capture())
        assert(resultCaptor.firstValue.isNotEmpty())
        assert(resultCaptor.firstValue[0].name == "sponsor1")
    }

    @Test
    fun sponsorApi_application_success_with_data() {
        val callback =
            Mockito.mock(LiveLikeCallback::class.java) as LiveLikeCallback<List<SponsorModel>>
        Thread.sleep(5000)
        shadowOf(Looper.getMainLooper()).idle()
        sdk.sponsor().fetchForApplication(
            LiveLikePagination.FIRST,
            callback
        )
        Thread.sleep(2000)
        shadowOf(Looper.getMainLooper()).idle()
        val resultCaptor =
            argumentCaptor<List<SponsorModel>>()
        val errorCaptor =
            argumentCaptor<String>()
        shadowOf(Looper.getMainLooper()).idle()
        Thread.sleep(2000)
        shadowOf(Looper.getMainLooper()).idle()
        verify(callback, timeout(5000)).onResponse(resultCaptor.capture(), errorCaptor.capture())
        assert(resultCaptor.firstValue.isNotEmpty())
        assert(resultCaptor.firstValue[0].name == "sponsor1")
    }


}