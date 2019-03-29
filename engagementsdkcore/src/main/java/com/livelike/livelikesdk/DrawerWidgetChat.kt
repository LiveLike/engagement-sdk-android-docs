package com.livelike.livelikesdk

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import com.livelike.livelikesdk.chat.ChatView
import com.livelike.livelikesdk.util.logVerbose
import com.livelike.livelikesdk.widget.view.WidgetView
import kotlinx.android.synthetic.main.drawer_chat_widget.view.*

class DrawerWidgetChat(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {
    var chat: ChatView
    var widgets: WidgetView

    init {
        val viewRoot: View = LayoutInflater.from(context).inflate(R.layout.drawer_chat_widget, this, true)
        viewRoot.button.setOnClickListener {
            viewRoot.view.apply {
                visibility = if (visibility == View.VISIBLE) View.GONE else View.VISIBLE
                logVerbose { "Set visibility to $visibility" }
            }
        }
        chat = viewRoot.chatView
        widgets = viewRoot.widgetView
    }
}