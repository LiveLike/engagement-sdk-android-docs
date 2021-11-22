package com.livelike.engagementsdk.gamification

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.livelike.engagementsdk.APPLICATION_RESOURCE
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.LEADERBOARD_DETAILS_RESOURCE
import com.livelike.engagementsdk.PROGRAM_RESOURCE
import com.livelike.engagementsdk.USER_PROFILE_RESOURCE
import com.livelike.engagementsdk.core.data.models.LeaderBoard
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
class LeaderBoardClientUnitTest {

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
                    "/api/v1/leaderboards/424d25bd-7404-417c-bb7c-9558a064cce4/" -> MockResponse().apply {
                        setResponseCode(200)
                        setBody(
                            javaClass.classLoader.getResourceAsStream(LEADERBOARD_DETAILS_RESOURCE)
                                .readAll()
                        )
                    }

                    "/api/programs/498591f4-9d6b-4943-9671-f44d3afbb6a4/program.json" -> MockResponse().apply {
                        setResponseCode(200)
                        setBody(
                            javaClass.classLoader.getResourceAsStream(PROGRAM_RESOURCE)
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
    fun getLeaderBoardDetails_success_with_data() {
        val callback =
            Mockito.mock(LiveLikeCallback::class.java) as LiveLikeCallback<LeaderBoard>
        Thread.sleep(5000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        sdk.leaderboard().getLeaderBoardDetails("424d25bd-7404-417c-bb7c-9558a064cce4", callback)
        val resultCaptor =
            argumentCaptor<LeaderBoard>()
        val errorCaptor =
            argumentCaptor<String>()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        Thread.sleep(2000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        verify(callback, timeout(5000)).onResponse(resultCaptor.capture(), errorCaptor.capture())
        assert(resultCaptor.firstValue.name.isNotEmpty())
        assert(resultCaptor.firstValue.name == "TestfreeLeaderboard")
        mockWebServer.shutdown()
    }


    @Test
    fun getLeaderBoardForPrograms_success_with_data() {
        val callback =
            Mockito.mock(LiveLikeCallback::class.java) as LiveLikeCallback<List<LeaderBoard>>
        Thread.sleep(5000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        sdk.leaderboard().getLeaderBoardsForProgram("498591f4-9d6b-4943-9671-f44d3afbb6a4", callback)
        val resultCaptor =
            argumentCaptor<List<LeaderBoard>>()
        val errorCaptor =
            argumentCaptor<String>()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        Thread.sleep(2000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        verify(callback, timeout(2000)).onResponse(resultCaptor.capture(), errorCaptor.capture())
        assert(resultCaptor.firstValue.isNotEmpty())
        assert(resultCaptor.firstValue[0].id == "424d25bd-7404-417c-bb7c-9558a064cce4")
        assert(resultCaptor.firstValue[0].name == "TestfreeLeaderboard")
        mockWebServer.shutdown()
    }
}