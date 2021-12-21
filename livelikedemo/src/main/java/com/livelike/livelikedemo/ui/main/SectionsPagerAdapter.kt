package com.livelike.livelikedemo.ui.main

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.PagerAdapter
import com.livelike.engagementsdk.LiveLikeContentSession
import com.livelike.livelikedemo.R

/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
class SectionsPagerAdapter(
    private val context: Context,
    session: LiveLikeContentSession
) :
    PagerAdapter() {
    val views =
        arrayListOf<View>(
            LayoutInflater.from(context).inflate(R.layout.empty_chat_data_view, null),
            LayoutInflater.from(context).inflate(R.layout.empty_chat_data_view, null),
            MMLChatView(context, session.chatSession, session),
            LayoutInflater.from(context).inflate(R.layout.empty_chat_data_view, null)
        )

    override fun getPageTitle(position: Int): CharSequence? {
        return "Tab$position"
    }

    override fun isViewFromObject(p0: View, p1: Any): Boolean {
        return p0 == p1
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val view = views[position]
        container.addView(view)
        return view
    }

    override fun getCount(): Int {
        return 4
    }
}
