package com.livelike.livelikedemo.mml

import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import com.livelike.livelikedemo.R

class MMLActivity : AppCompatActivity() {
    lateinit var liveLikeSDKHelper: com.example.mmlengagementsdk.LiveLikeSDKHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_m_m_l)

        liveLikeSDKHelper = com.example.mmlengagementsdk.LiveLikeSDKHelper(
            applicationContext,
            "3WtkbrjmyPFUHTSckcVVUlikAAdHEy1P0zqqczF0",
            "000301a4-34ca-4e8c-9e4d-da05499c0bf2"
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