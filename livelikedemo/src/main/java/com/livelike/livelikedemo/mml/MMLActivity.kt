package com.livelike.livelikedemo.mml

import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import com.livelike.livelikedemo.R
import com.mml.mmlengagementsdk.LiveLikeSDKIntegrationManager

class MMLActivity : AppCompatActivity() {
    lateinit var liveLikeSDKIntegrationManager: LiveLikeSDKIntegrationManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_m_m_l)

        liveLikeSDKIntegrationManager = LiveLikeSDKIntegrationManager(
            applicationContext,
            "3WtkbrjmyPFUHTSckcVVUlikAAdHEy1P0zqqczF0",
            "000301a4-34ca-4e8c-9e4d-da05499c0bf2"
        )
        val mmlPagerAdapter = MMLPagerAdapter(this, liveLikeSDKIntegrationManager)
        val viewPager: ViewPager = findViewById(R.id.view_pager)
        viewPager.adapter = mmlPagerAdapter
        val tabs: TabLayout = findViewById(R.id.tabs)
        tabs.setupWithViewPager(viewPager)
    }
}