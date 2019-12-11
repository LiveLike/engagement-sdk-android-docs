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
import java.util.Random
import kotlin.math.abs


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
    chatReactionRadius: Float = 4f,
    chatReactionBackgroundColor: Int = Color.WHITE,
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

    private fun formattedReactionCount(count: Int): String {
        return when {
            abs(count / 1000000) > 1 -> {
                (count / 1000000).toString() + "m"
            }
            abs(count / 1000) > 1 -> {
                (count / 1000).toString() + "k"
            }
            else -> {
                count.toString()
            }
        }
    }

    private fun initReactions() {
        val reactionsBox = contentView.findViewById<ImageView>(R.id.reaction_panel_interaction_box) as ViewGroup
        reactionsBox.removeAllViews()
        val threeDp = AndroidResource.dpToPx(3)
        val fiveDp = AndroidResource.dpToPx(5)
        chatReactionRepository.reactionList?.forEach { reaction ->
            val frameLayout = FrameLayout(context)
            val countView = TextView(context)
            val imageView = ImageView(context)
            frameLayout.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                setMargins(2,2,2,2)
            }
            frameLayout.setPadding(fiveDp, threeDp, fiveDp, threeDp)
            frameLayout.setBackgroundResource(R.drawable.chat_reaction_tap_background_selector)
            frameLayout.isClickable = true
            frameLayout.setOnClickListener {  }
            imageView.loadImage(reaction.file, AndroidResource.dpToPx(28))

            val cnt=Random().nextInt(100000)
            countView.apply {
                text = formattedReactionCount(cnt)
                setTextColor(Color.BLACK)
                setTypeface(null,Typeface.BOLD)
                setTextSize(TypedValue.COMPLEX_UNIT_SP,10f)
                visibility = when(cnt){
                    0 -> View.GONE
                    else -> View.VISIBLE
                }
            }
            frameLayout.addView(imageView,FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,FrameLayout.LayoutParams.WRAP_CONTENT,Gravity.CENTER).apply {
                setMargins(0, 7, 3, 0)
            })
            frameLayout.addView(countView,FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,FrameLayout.LayoutParams.WRAP_CONTENT,Gravity.TOP or Gravity.RIGHT))
            reactionsBox.addView(frameLayout)
        }
    }
}
