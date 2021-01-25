package com.livelike.livelikedemo.mml

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import com.livelike.livelikedemo.mml.fragments.ChatFragment
import com.livelike.livelikedemo.mml.fragments.WidgetsFragment


class SectionsPagerAdapter(
    fm: FragmentManager
) :
    FragmentPagerAdapter(fm) {
    private val fragments =
        arrayListOf(ChatFragment(), WidgetsFragment())

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