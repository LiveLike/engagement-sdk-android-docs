package com.livelike.engagementsdk.gamification

import android.content.Context
import android.os.Looper
import android.view.Display
import android.view.WindowManager
import androidx.test.core.app.ApplicationProvider
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.chat.data.remote.LiveLikePagination
import com.livelike.engagementsdk.core.data.models.LLPaginatedResult
import com.livelike.engagementsdk.gamification.models.Badge
import com.livelike.engagementsdk.gamification.models.BadgeProgress
import com.livelike.engagementsdk.gamification.models.ProfileBadge
import com.livelike.engagementsdk.publicapis.IEngagement
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.timeout
import org.mockito.Mockito.verify
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowApplication
import readAll

@RunWith(RobolectricTestRunner::class)
class BadgesClientUnitTest {


    val context = ApplicationProvider.getApplicationContext<Context>()

    private var mockWebServer = MockWebServer()


    private lateinit var sdk: IEngagement

    init {
        mockWebServer = MockWebServer()
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
        mockingAndroidServicesUsedByMixpanel()
        sdk = EngagementSDK(
            "GaEBcpVrCxiJOSNu4bvX6krEaguxHR9Hlp63tK6L",
            context,
            null,
            "http://localhost:8080/",
            null
        )
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

    @Test
    fun applicationBadges_success_with_data() {
        val callback =
            Mockito.mock(LiveLikeCallback::class.java) as LiveLikeCallback<LLPaginatedResult<Badge>>
        Thread.sleep(1000)
        mockWebServer.enqueue(MockResponse().apply {
            setResponseCode(200)
            setBody(
                javaClass.classLoader.getResourceAsStream("all_badges_resource.json").readAll()
            )
        })
        sdk.badges().getApplicationBadges(LiveLikePagination.FIRST, callback)
        val resultCaptor =
            argumentCaptor<LLPaginatedResult<Badge>>()
        val errorCaptor =
            argumentCaptor<String>()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        verify(callback, timeout(5000)).onResponse(resultCaptor.capture(), errorCaptor.capture())
        assert(resultCaptor.firstValue.results?.isNotEmpty()?:false)
        assert(resultCaptor.firstValue.results?.get(0)?.name == "TestFree")
        mockWebServer.shutdown()
    }

    @Test
    fun badge_progression_success_with_data() {
        val callback =
            Mockito.mock(LiveLikeCallback::class.java) as LiveLikeCallback<List<BadgeProgress>>
        Thread.sleep(1000)
        mockWebServer.enqueue(MockResponse().apply {
            setResponseCode(200)
            setBody(
                javaClass.classLoader.getResourceAsStream("user_profile_resource.json").readAll()
            )
        })
        mockWebServer.enqueue(MockResponse().apply {
            setResponseCode(200)
            setBody(
                javaClass.classLoader.getResourceAsStream("badge_progression.json").readAll()
            )
        })
        sdk.badges().getProfileBadgeProgress("e58b8f5c-01b9-4423-bc2d-80a62ce28891", mutableListOf<String>().apply{
            add("7c2a61af-2e88-40de-8df9-027127a6a1e2")
        }, callback)
        val resultCaptor =
            argumentCaptor<List<BadgeProgress>>()
        val errorCaptor =
            argumentCaptor<String>()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        verify(callback, timeout(5000)).onResponse(resultCaptor.capture(), errorCaptor.capture())
        assert(resultCaptor.firstValue.isNotEmpty()?:false)
        assert(resultCaptor.firstValue[0]?.badge.name == "TestFree")
        mockWebServer.shutdown()
    }

    @Test
    fun profile_badges_success_with_data() {
        val callback =
            Mockito.mock(LiveLikeCallback::class.java) as LiveLikeCallback<LLPaginatedResult<ProfileBadge>>
        Thread.sleep(1000)
        mockWebServer.enqueue(MockResponse().apply {
            setResponseCode(200)
            setBody(
                javaClass.classLoader.getResourceAsStream("user_profile_resource.json").readAll()
            )
        })
        mockWebServer.enqueue(MockResponse().apply {
            setResponseCode(200)
            setBody(
                javaClass.classLoader.getResourceAsStream("profile_badges.json").readAll()
            )
        })
        sdk.badges().getProfileBadges("e58b8f5c-01b9-4423-bc2d-80a62ce28891", LiveLikePagination.FIRST, callback)
        val resultCaptor =
            argumentCaptor<LLPaginatedResult<ProfileBadge>>()
        val errorCaptor =
            argumentCaptor<String>()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        verify(callback, timeout(5000)).onResponse(resultCaptor.capture(), errorCaptor.capture())
        assert(resultCaptor.firstValue.results?.isNotEmpty()?:false)
        assert(resultCaptor.firstValue.results?.get(0)?.badge?.name.equals("My Badge"))
        mockWebServer.shutdown()
    }



}