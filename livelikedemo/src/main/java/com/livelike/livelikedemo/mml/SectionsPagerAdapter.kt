package com.livelike.livelikedemo.mml

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter


class SectionsPagerAdapter(
    fm: FragmentManager,
    liveLikeSDKHelper: com.example.mmlengagementsdk.LiveLikeSDKIntegrationManager
) :
    FragmentPagerAdapter(fm) {
    private val fragments =
        arrayListOf(liveLikeSDKHelper.getChatFragment(), liveLikeSDKHelper.getWidgetsFragment())

    override fun getItem(position: Int): Fragment {
        return fragments[position]
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return when (position) {
            0 -> "Chat"
            1 -> "Widget"
            else -> ""
        }
    }

    override fun getCount(): Int {
        return fragments.size
    }
}