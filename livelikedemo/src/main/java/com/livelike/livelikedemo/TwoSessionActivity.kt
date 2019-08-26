package com.livelike.livelikedemo

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import kotlinx.android.synthetic.main.two_sessions_activity.widget_session_1
import kotlinx.android.synthetic.main.two_sessions_activity.widget_session_2

class TwoSessionActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.two_sessions_activity)

        (application as LiveLikeApplication).sdk.let {
            widget_session_1.setSession(it.createContentSession("3d639cb0-b2c4-4f70-8875-e39c74197580"))
        }
        (application as LiveLikeApplication).sdk2?.let {
            widget_session_2.setSession(it.createContentSession("fef045d9-4ae4-4a87-8e47-7a5929c14cce"))
        }
    }
}
