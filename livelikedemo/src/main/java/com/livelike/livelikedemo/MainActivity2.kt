package com.livelike.livelikedemo

import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import com.livelike.livelikedemo.channel.ChannelManager
import com.livelike.livelikedemo.ui.main.SectionsPagerAdapter

class MainActivity2 : AppCompatActivity() {

    private lateinit var channelManager: ChannelManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        channelManager = (application as LiveLikeApplication).channelManager
        val channel = channelManager.selectedChannel
        if (channel != ChannelManager.NONE_CHANNEL) {
            val session =
                (application as LiveLikeApplication).createPublicSession(channel.llProgram.toString())
            val sectionsPagerAdapter = SectionsPagerAdapter(this, session)
            val viewPager: ViewPager = findViewById(R.id.view_pager)
            viewPager.offscreenPageLimit = 1
            viewPager.adapter = sectionsPagerAdapter
            val tabs: TabLayout = findViewById(R.id.tabs)
            tabs.setupWithViewPager(viewPager)
        }
    }
}