package com.example.mmlengagementsdk.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.mmlengagementsdk.R
import com.example.mmlengagementsdk.timeline.WidgetsTimeLineView
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.LiveLikeContentSession
import kotlinx.android.synthetic.main.fragment_widgets.root_view

class WidgetsFragment : Fragment() {
    lateinit var sdk: EngagementSDK
    lateinit var session: LiveLikeContentSession
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_widgets, container, false)
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        context?.let {
            root_view.addView(WidgetsTimeLineView(it, session, sdk))
        }
    }


}