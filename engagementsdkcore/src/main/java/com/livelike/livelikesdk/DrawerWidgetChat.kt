package com.livelike.livelikesdk

import android.content.Context
import android.graphics.Color
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.RelativeLayout
import com.livelike.livelikesdk.chat.ChatView
import com.livelike.livelikesdk.slidinglayer.SlidingLayer.STICK_TO_BOTTOM
import com.livelike.livelikesdk.slidinglayer.SlidingLayer.STICK_TO_LEFT
import com.livelike.livelikesdk.slidinglayer.SlidingLayer.STICK_TO_RIGHT
import com.livelike.livelikesdk.slidinglayer.SlidingLayer.STICK_TO_TOP
import com.livelike.livelikesdk.util.AndroidResource.Companion.dpToPx
import com.livelike.livelikesdk.widget.view.WidgetView
import kotlinx.android.synthetic.main.drawer_chat_widget.view.*

class DrawerWidgetChat(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {
    var chat: ChatView
    var widgets: WidgetView
    var previewSize: Int

    init {
        val viewRoot: View = LayoutInflater.from(context).inflate(R.layout.drawer_chat_widget, this, true)
        view.isSlidingEnabled = true

        chat = viewRoot.chatView
        widgets = viewRoot.widgetView

        val ta = context.obtainStyledAttributes(attrs, R.styleable.DrawerWidgetChat)

        previewSize = ta.getInt(R.styleable.DrawerWidgetChat_preview_size, 30)
        view.setPreviewOffsetDistance(dpToPx(previewSize))

        setDrawerOrientation(ta.getInt(R.styleable.DrawerWidgetChat_stick_to, STICK_TO_RIGHT))

        view.setChangeStateOnTap(true)

        view.setBackgroundColor(ta.getColor(R.styleable.DrawerWidgetChat_chat_background_color, Color.TRANSPARENT))

        view.setChangeStateOnTap(ta.getBoolean(R.styleable.DrawerWidgetChat_change_state_on_tap, true))
        view.openLayer(false) // open drawer immediately
    }

    private fun setDrawerOrientation(orientation: Int) {
        view.setStickTo(orientation)

        (chatView.layoutParams as RelativeLayout.LayoutParams).removeRule(RelativeLayout.BELOW)
        (chatView.layoutParams as RelativeLayout.LayoutParams).removeRule(RelativeLayout.ABOVE)
        (chatView.layoutParams as RelativeLayout.LayoutParams).removeRule(RelativeLayout.RIGHT_OF)
        (chatView.layoutParams as RelativeLayout.LayoutParams).removeRule(RelativeLayout.ABOVE)

        when (orientation) {
            STICK_TO_BOTTOM -> {
                chatView.apply {
                    val params = layoutParams as RelativeLayout.LayoutParams
                    params.addRule(RelativeLayout.BELOW, R.id.handle)

                }
                handle.layoutParams.height = dpToPx(previewSize)
                handle.layoutParams.width = LayoutParams.MATCH_PARENT
            }
            STICK_TO_TOP -> {
                chatView.apply {
                    val params = layoutParams as RelativeLayout.LayoutParams
                    params.addRule(RelativeLayout.ABOVE, R.id.handle)

                }
                handle.layoutParams.height = dpToPx(previewSize)
                handle.layoutParams.width = LayoutParams.MATCH_PARENT
            }
            STICK_TO_RIGHT -> {
                chatView.apply {
                    val params = layoutParams as RelativeLayout.LayoutParams
                    params.addRule(RelativeLayout.RIGHT_OF, R.id.handle)
                }
                handle.layoutParams.width = dpToPx(previewSize)
                handle.layoutParams.height = LayoutParams.MATCH_PARENT
            }
            STICK_TO_LEFT -> {
                chatView.apply {
                    val params = layoutParams as RelativeLayout.LayoutParams
                    params.addRule(RelativeLayout.LEFT_OF, R.id.handle)

                }
                handle.layoutParams.width = dpToPx(previewSize)
                handle.layoutParams.height = LayoutParams.MATCH_PARENT
            }
        }
    }

}