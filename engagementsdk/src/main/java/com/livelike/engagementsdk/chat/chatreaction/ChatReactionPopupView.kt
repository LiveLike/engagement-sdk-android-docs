package com.livelike.engagementsdk.chat.chatreaction

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupWindow
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.utils.AndroidResource
import com.livelike.engagementsdk.widget.view.loadImage

/**
 * Chat reactions view that will popup when use long press chat
 * Add future chat reactions support here
 */
internal class ChatReactionPopupView(
    val context: Context,
    val chatReactionRepository: ChatReactionRepository,
    flagClick: View.OnClickListener,
    hideFloatinUi: () -> Unit
) : PopupWindow(context) {

    init {
        contentView = LayoutInflater.from(context).inflate(R.layout.popup_chat_reaction, null)
        contentView.findViewById<ImageView>(R.id.moderation_flag).setOnClickListener {
            dismiss()
            flagClick.onClick(it)
        }
        setOnDismissListener(hideFloatinUi)
        isOutsideTouchable = true
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        initReactions()
    }

    private fun initReactions() {
        val reactionsBox = contentView.findViewById<ImageView>(R.id.reaction_panel_interaction_box) as ViewGroup
        reactionsBox.removeAllViews()
        chatReactionRepository.reactionList?.forEach { reaction ->
            val imageView = ImageView(context)
            imageView.loadImage(reaction.file, AndroidResource.dpToPx(24))
            reactionsBox.addView(imageView)
        }
    }
}
