package com.livelike.engagementsdk.chat.chatreaction

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.utils.AndroidResource
import com.livelike.engagementsdk.widget.view.loadImage
import kotlinx.android.synthetic.main.popup_chat_reaction.view.chat_reaction_background_card
import kotlinx.android.synthetic.main.popup_chat_reaction.view.reaction_panel_interaction_box

/**
 * Chat reactions and Chat moderation actions view that will popup when use long press chat
 */
internal class ChatActionsPopupView(
    val context: Context,
    val chatReactionRepository: ChatReactionRepository,
    flagClick: View.OnClickListener,
    hideFloatinUi: () -> Unit,
    isOwnMessage: Boolean,
    chatReactionBackground: Drawable? = ColorDrawable(Color.TRANSPARENT),
    chatReactionElevation: Float = 0f,
    chatReactionRadius: Float = 0f,
    chatReactionBackgroundColor: Int = Color.TRANSPARENT,
    chatReactionPadding: Int = AndroidResource.dpToPx(6)
) : PopupWindow(context) {

    init {
        contentView = LayoutInflater.from(context).inflate(R.layout.popup_chat_reaction, null)
        contentView.chat_reaction_background_card.apply {
            setCardBackgroundColor(chatReactionBackgroundColor)
            cardElevation = chatReactionElevation
            radius = chatReactionRadius
        }
        contentView.reaction_panel_interaction_box.setPadding(chatReactionPadding, chatReactionPadding, chatReactionPadding, chatReactionPadding)
        if (!isOwnMessage) {
        val moderationFlagView = contentView.findViewById<ImageView>(R.id.moderation_flag)
            moderationFlagView.visibility = View.VISIBLE
            moderationFlagView.setOnClickListener {
                dismiss()
                flagClick.onClick(it)
            }
        }
        setOnDismissListener(hideFloatinUi)
        isOutsideTouchable = true
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        initReactions()
    }

    private fun initReactions() {
        val reactionsBox = contentView.findViewById<ImageView>(R.id.reaction_panel_interaction_box) as ViewGroup
        reactionsBox.removeAllViews()
        val threeDp = AndroidResource.dpToPx(3)
        chatReactionRepository.reactionList?.forEach { reaction ->
            val frameLayout = FrameLayout(context)
            val countView = TextView(context)
            val imageView = ImageView(context)
            frameLayout.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                setMargins(threeDp, 0, threeDp, 0)
            }
            imageView.loadImage(reaction.file, AndroidResource.dpToPx(24))

            countView.apply {
                text = "${reaction.reactionsCount}"
                setTextColor(Color.BLACK)
                setTypeface(null,Typeface.BOLD)
                setTextSize(TypedValue.COMPLEX_UNIT_SP,13f)
                visibility = when(reaction.reactionsCount){
                    0 -> View.GONE
                    else -> View.VISIBLE
                }
            }
            frameLayout.addView(imageView,FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,FrameLayout.LayoutParams.WRAP_CONTENT,Gravity.CENTER).apply {
                setMargins(0, 5, 0, 0)
            })
            frameLayout.addView(countView,FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,FrameLayout.LayoutParams.WRAP_CONTENT,Gravity.TOP or Gravity.RIGHT))
            reactionsBox.addView(frameLayout)
        }
    }
}
