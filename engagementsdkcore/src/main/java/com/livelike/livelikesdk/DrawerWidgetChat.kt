package com.livelike.livelikesdk

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import com.livelike.livelikesdk.chat.ChatView
import com.livelike.livelikesdk.slidinglayer.SlidingLayer.STICK_TO_BOTTOM
import com.livelike.livelikesdk.slidinglayer.SlidingLayer.STICK_TO_LEFT
import com.livelike.livelikesdk.slidinglayer.SlidingLayer.STICK_TO_RIGHT
import com.livelike.livelikesdk.slidinglayer.SlidingLayer.STICK_TO_TOP
import com.livelike.livelikesdk.widget.view.WidgetView
import kotlinx.android.synthetic.main.drawer_chat_widget.view.*

class DrawerWidgetChat(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {
    var chat: ChatView
    var widgets: WidgetView

    init {
        val viewRoot: View = LayoutInflater.from(context).inflate(R.layout.drawer_chat_widget, this, true)
        view.isSlidingEnabled = true

        chat = viewRoot.chatView
        widgets = viewRoot.widgetView

        val ta = context.obtainStyledAttributes(attrs, R.styleable.SlidingLayer)
        view.setStickTo(ta.getInt(R.styleable.SlidingLayer_stickTo, STICK_TO_RIGHT))
    }

    fun setDrawerOrientation(orientation: DrawerOrientation) {
        when (orientation) {
            DrawerOrientation.STICK_TO_BOTTOM -> view.setStickTo(STICK_TO_BOTTOM)
            DrawerOrientation.STICK_TO_TOP -> view.setStickTo(STICK_TO_TOP)
            DrawerOrientation.STICK_TO_RIGHT -> view.setStickTo(STICK_TO_RIGHT)
            DrawerOrientation.STICK_TO_LEFT -> view.setStickTo(STICK_TO_LEFT)
        }
    }

    enum class DrawerOrientation {
        STICK_TO_RIGHT,
        STICK_TO_LEFT,
        STICK_TO_TOP,
        STICK_TO_BOTTOM
    }
}