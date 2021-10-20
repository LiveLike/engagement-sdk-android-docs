package com.livelike.engagementsdk.gamification

import android.content.Context
import android.os.Looper
import android.view.Display
import android.view.WindowManager
import androidx.test.core.app.ApplicationProvider
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.publicapis.IEngagement
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.sponsorship.SponsorModel
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
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowApplication
import readAll

@RunWith(RobolectricTestRunner::class)
class SponsorApiUnitTest {

    val context = ApplicationProvider.getApplicationContext<Context>()

    private var mockWebServer = MockWebServer()


    private lateinit var sdk: IEngagement

    init {
        mockWebServer = MockWebServer()
    }

    private fun mockingAndroidServicesUsedByMixpanel() {
        val mock = Mockito.mock(WindowManager::class.java)
        // try shadowOf(context as Application)
        ShadowApplication.getInstance().setSystemService(
            Context.WINDOW_SERVICE,
            mock
        )
        whenever(mock.defaultDisplay).thenReturn(Mockito.mock(Display::class.java))
    }

    @Before
    fun setup() {
        mockWebServer.start(8080)
        mockWebServer.enqueue(MockResponse().apply {
            setResponseCode(200)
            setBody(
                javaClass.classLoader.getResourceAsStream("application_resource.json").readAll()
            )
        })
        mockWebServer.enqueue(MockResponse().apply {
            setResponseCode(200)
            setBody(
                javaClass.classLoader.getResourceAsStream("user_profile_resource.json").readAll()
            )
        })
        mockWebServer.enqueue(MockResponse().apply {
            setResponseCode(200)
            setBody(javaClass.classLoader.getResourceAsStream("program_resource.json").readAll())
        })
        mockingAndroidServicesUsedByMixpanel()
    }

    @After
    fun shutDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun sponsorApi_success_with_data() {
        sdk = EngagementSDK(
            "GaEBcpVrCxiJOSNu4bvX6krEaguxHR9Hlp63tK6L",
            context,
            null,
            "http://localhost:8080/",
            null
        )
        val callback =
            Mockito.mock(LiveLikeCallback::class.java) as LiveLikeCallback<List<SponsorModel>>
        Thread.sleep(1000)
        sdk.sponsor().fetchByProgramId("498591f4-9d6b-4943-9671-f44d3afbb6a4", callback)
        val resultCaptor =
            argumentCaptor<List<SponsorModel>>()
        val errorCaptor =
            argumentCaptor<String>()
        shadowOf(Looper.getMainLooper()).idle()
        verify(callback, timeout(5000)).onResponse(resultCaptor.capture(), errorCaptor.capture())
        assert(resultCaptor.firstValue.isNotEmpty())
        assert(resultCaptor.firstValue[0].name == "Ringside Coffee Company")
        mockWebServer.shutdown()
    }


}