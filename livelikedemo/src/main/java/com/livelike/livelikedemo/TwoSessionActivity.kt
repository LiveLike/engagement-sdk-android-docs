package com.livelike.livelikedemo

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import kotlinx.android.synthetic.main.two_sessions_activity.widget_session_1
import kotlinx.android.synthetic.main.two_sessions_activity.widget_session_2

class TwoSessionActivity : Activity(){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.two_sessions_activity)

        (application as LiveLikeApplication).sdk?.let {
            widget_session_1.setSession(it.createContentSession("3d639cb0-b2c4-4f70-8875-e39c74197580"))
            widget_session_2.setSession(it.createContentSession("50feace1-37d0-4bbb-afbb-3c3799188520"))
        }
    }
}