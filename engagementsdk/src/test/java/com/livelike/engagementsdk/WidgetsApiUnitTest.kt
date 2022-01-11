package com.livelike.engagementsdk

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.livelike.engagementsdk.chat.data.remote.LiveLikePagination
import com.livelike.engagementsdk.publicapis.IEngagement
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.widget.data.models.WidgetUserInteractionBase
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
class WidgetsApiUnitTest {

    val context = ApplicationProvider.getApplicationContext<Context>()
    private var mockWebServer = MockWebServer()
    private lateinit var sdk: IEngagement
    private lateinit var contentSession:LiveLikeContentSession

    init {
        mockWebServer = MockWebServer()
    }

    @Before
    fun setup() {
        mockWebServer.start(8080)
        mockingAndroidServicesUsedByMixpanel()
        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                println(request.path)
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

                    "/api/v1/programs/498591f4-9d6b-4943-9671-f44d3afbb6a4/program.json" -> MockResponse().apply {
                        setResponseCode(200)
                        setBody(
                            javaClass.classLoader.getResourceAsStream(PROGRAM_RESOURCE).readAll()

                        )
                    }

                    "/api/v1/chat-rooms/e8f3b5d2-3353-4c8e-b54e-32ecca6b7482/" -> MockResponse().apply {
                        setResponseCode(200)
                        setBody(
                            javaClass.classLoader.getResourceAsStream(SINGLE_CHATROOM_DETAIL_SUCCESS)
                                .readAll()
                        )
                    }

                    "/api/v1/programs/498591f4-9d6b-4943-9671-f44d3afbb6a4/widgets/" -> MockResponse().apply {
                        setResponseCode(200)
                        setBody(
                            javaClass.classLoader.getResourceAsStream(WIDGET_RESOURCE).readAll()

                        )
                    }

                    "/api/v1/programs/498591f4-9d6b-4943-9671-f44d3afbb6a4/widgets/?ordering=recent&status=published" -> MockResponse().apply {
                        setResponseCode(200)
                        setBody(
                            javaClass.classLoader.getResourceAsStream(WIDGET_TIMELINE_JSON).readAll()

                        )
                    }

                    else -> MockResponse().apply {
                        setResponseCode(500)
                    }
                }
            }
        }
        context.applicationInfo.nonLocalizedLabel = "livelike"
        sdk = EngagementSDK(
            "GaEBcpVrCxiJOSNu4bvX6krEaguxHR9Hlp63tK6L",
            context,
            null,
            "http://localhost:8080",
            null
        )

        contentSession = (sdk as EngagementSDK).createContentSession("498591f4-9d6b-4943-9671-f44d3afbb6a4", null)
    }



    @After
    fun shutDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun getWidgetApi_success_with_data(){
        val callback =
            Mockito.mock(LiveLikeCallback::class.java) as LiveLikeCallback<List<LiveLikeWidget>>
        Thread.sleep(5000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        contentSession.getWidgets(LiveLikePagination.FIRST, null ,callback)
        Thread.sleep(3000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        Thread.sleep(3000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val resultCaptor = argumentCaptor<List<LiveLikeWidget>>()
        val errorCaptor = argumentCaptor<String>()
        Thread.sleep(2000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        Thread.sleep(2000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        verify(callback, timeout(3000)).onResponse(resultCaptor.capture(), errorCaptor.capture())
        assert(resultCaptor.firstValue.isNotEmpty())
        assert(resultCaptor.firstValue[0].id == "766e0cfe-6a0f-41c8-8b97-ca1734fb1a03")
        assert(resultCaptor.firstValue[0].kind == "text-quiz")
        mockWebServer.shutdown()

    }

    @Test
    fun getWidgetTimelineApi_success_with_data(){
        val callback =
            Mockito.mock(LiveLikeCallback::class.java) as LiveLikeCallback<List<LiveLikeWidget>>
        Thread.sleep(5000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        contentSession.getPublishedWidgets(LiveLikePagination.FIRST,callback)
        Thread.sleep(3000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        Thread.sleep(3000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val resultCaptor = argumentCaptor<List<LiveLikeWidget>>()
        val errorCaptor = argumentCaptor<String>()
        Thread.sleep(2000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        Thread.sleep(2000)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        verify(callback, timeout(3000)).onResponse(resultCaptor.capture(), errorCaptor.capture())
        assert(resultCaptor.firstValue.isNotEmpty())
        assert(resultCaptor.firstValue[0].kind == "text-prediction")

    }
}