package com.livelike.livelikedemo

import android.content.Context
import android.support.test.InstrumentationRegistry
import com.livelike.engagementsdk.EngagementSDK
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class SDKInitializationTest(
    private val clientId: String,
    private val programId: String
) {

    private lateinit var instrumentationContext: Context

    @Before
    fun setup() {
        instrumentationContext = InstrumentationRegistry.getInstrumentation().context
    }

    @Test
    fun init_sdk_no_crash() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        val sdk = EngagementSDK(clientId, context)
        sdk.createContentSession(programId)
        sdk.updateChatNickname("hello")
        Thread.sleep(5000)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun clientId() = listOf(
            arrayOf("", ""),
            arrayOf("wrong client id", ""),
            arrayOf("WV6W1rkAJAAXAS9l0LpqHzjDyEcPbuGJjX7Kc2hk", "wrong program id"), // Prod
            arrayOf(
                "WV6W1rkAJAAXAS9l0LpqHzjDyEcPbuGJjX7Kc2hk",
                "43a0b614-e248-421d-8d6f-3ecb3977a5d8"
            ), // Prod
            arrayOf("l3euw77STbgCVQn59AQZudZ163qO2EhzWVOKBfpu", "wrong program id"), // Staging
            arrayOf(
                "l3euw77STbgCVQn59AQZudZ163qO2EhzWVOKBfpu",
                "13f0387b-5ea8-439f-84ea-f97e6c03177b"
            ) // Staging
        )
    }
}
