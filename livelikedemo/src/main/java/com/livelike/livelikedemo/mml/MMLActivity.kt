package com.livelike.livelikedemo.mml

import android.content.Context
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.LiveLikeContentSession
import com.livelike.engagementsdk.core.AccessTokenDelegate
import com.livelike.livelikedemo.R

class MMLActivity : AppCompatActivity() {
    lateinit var engagementSDK: EngagementSDK
    lateinit var session: LiveLikeContentSession
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_m_m_l)
        val sharedPreference = getSharedPreferences("MMLSharedPrefs", Context.MODE_PRIVATE)
        engagementSDK = EngagementSDK("3WtkbrjmyPFUHTSckcVVUlikAAdHEy1P0zqqczF0", applicationContext, accessTokenDelegate = object :
            AccessTokenDelegate {
            override fun getAccessToken(): String? {
                return sharedPreference.getString("accessToken", null)
            }

            override fun storeAccessToken(accessToken: String?) {
                sharedPreference.edit().putString("accessToken", accessToken).apply()
            }
        })

        session = engagementSDK.createContentSession("000301a4-34ca-4e8c-9e4d-da05499c0bf2")
        val sectionsPagerAdapter =
            SectionsPagerAdapter(
                supportFragmentManager
            )
        val viewPager: ViewPager = findViewById(R.id.view_pager)
        viewPager.adapter = sectionsPagerAdapter
        val tabs: TabLayout = findViewById(R.id.tabs)
        tabs.setupWithViewPager(viewPager)
    }
}