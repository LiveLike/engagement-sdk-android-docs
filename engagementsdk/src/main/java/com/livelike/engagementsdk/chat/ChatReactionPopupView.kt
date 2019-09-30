package com.livelike.engagementsdk.chat

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.PopupWindow
import com.livelike.engagementsdk.R

/**
 * Chat reactions view that will popup when use long press chat
 * Add future chat reactions support here
 */
class ChatReactionPopupView(
    context: Context,
    flagClick: View.OnClickListener,
    hideFloatinUi: () -> Unit
) : PopupWindow(context) {

    init {
        contentView = LayoutInflater.from(context).inflate(R.layout.popup_chat_reaction, null)
        contentView.findViewById<ImageView>(R.id.floatingUi).setOnClickListener {
            dismiss()
            flagClick.onClick(it)
        }
        setOnDismissListener(hideFloatinUi)
        isOutsideTouchable = true
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }
}
