package com.livelike.livelikedemo.mml

import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import com.livelike.livelikedemo.R

class MMLActivity : AppCompatActivity() {
    lateinit var liveLikeSDKHelper: LiveLikeSDKHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_m_m_l)

        liveLikeSDKHelper = LiveLikeSDKHelper(
            applicationContext,
            "pnODbVXg0UI80s0l2aH5Y7FOuGbftoAdSNqpdvo6",
            "e7df6164-bbc9-47d0-b7e8-ad4c86fa2e26"
        )
        val sectionsPagerAdapter =
            SectionsPagerAdapter(
                supportFragmentManager, liveLikeSDKHelper
            )
        val viewPager: ViewPager = findViewById(R.id.view_pager)
        viewPager.adapter = sectionsPagerAdapter
        val tabs: TabLayout = findViewById(R.id.tabs)
        tabs.setupWithViewPager(viewPager)
    }
}