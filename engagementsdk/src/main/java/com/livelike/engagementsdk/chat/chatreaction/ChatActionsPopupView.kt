package com.livelike.engagementsdk.chat.chatreaction

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.RelativeLayout
import android.widget.TextView
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.chat.ChatMessageReaction
import com.livelike.engagementsdk.chat.ChatViewThemeAttributes
import com.livelike.engagementsdk.utils.AndroidResource
import com.livelike.engagementsdk.widget.view.loadImage
import kotlinx.android.synthetic.main.popup_chat_reaction.view.chat_reaction_background_card
import kotlinx.android.synthetic.main.popup_chat_reaction.view.moderation_flag
import kotlinx.android.synthetic.main.popup_chat_reaction.view.moderation_flag_lay

/**
 * Chat reactions and Chat moderation actions view that will popup when use long press chat
 */
internal class ChatActionsPopupView(
    val context: Context,
    private val chatReactionRepository: ChatReactionRepository,
    flagClick: View.OnClickListener,
    hideFloatingUi: () -> Unit,
    isOwnMessage: Boolean,
    val userReaction: ChatMessageReaction? = null,
    val emojiCountMap: MutableMap<String, Int>? = null,
    private val chatViewThemeAttributes: ChatViewThemeAttributes,
    val selectReactionListener: SelectReactionListener? = null,
    val isPublichat: Boolean
) : PopupWindow(context) {

    init {
        chatViewThemeAttributes.apply {
            contentView = LayoutInflater.from(context).inflate(R.layout.popup_chat_reaction, null)
            contentView.chat_reaction_background_card.apply {
                setCardBackgroundColor(chatReactionPanelColor)
                cardElevation = chatReactionElevation
                radius = chatReactionRadius
                setContentPadding(
                    chatReactionPadding,
                    chatReactionPadding,
                    chatReactionPadding,
                    chatReactionPadding
                )
            }

            if (!isOwnMessage && isPublichat) {
                contentView.moderation_flag_lay.apply {
                    visibility = View.VISIBLE
                    setOnClickListener {
                        dismiss()
                        flagClick.onClick(it)
                    }
                    radius = chatReactionRadius
                    setCardBackgroundColor(chatReactionPanelColor)
                }
                contentView.moderation_flag.setColorFilter(
                    chatReactionFlagTintColor
                )
            }
            setOnDismissListener(hideFloatingUi)
            isOutsideTouchable = true
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            contentView.chat_reaction_background_card.postDelayed({
                contentView.chat_reaction_background_card.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
            },500)
        }
        initReactions()
    }

    private fun formattedReactionCount(count: Int): String {
        return if (count < 99)
            "$count"
        else
            "99+"
    }

    private fun initReactions() {
        val reactionsBox =
            contentView.findViewById<LinearLayout>(R.id.reaction_panel_interaction_box)
        reactionsBox.removeAllViews()
        chatReactionRepository.reactionList?.forEach { reaction ->
            val relativeLayout = RelativeLayout(context)
            val countView = TextView(context)
            val imageView = ImageView(context)
            imageView.id = View.generateViewId()
            imageView.contentDescription = reaction.name
            imageView.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            imageView.isFocusable = true
            imageView.loadImage(reaction.file, context.resources.getDimensionPixelSize(R.dimen.livelike_chat_reaction_size))

            userReaction?.let {
                if (it.emojiId == reaction.id)
                    relativeLayout.setBackgroundResource(R.drawable.chat_reaction_tap_background)
            }
            imageView.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        v.animate().scaleX(1.2f).scaleY(1.2f).setDuration(50).start()
                        return@setOnTouchListener true
                    }
                    MotionEvent.ACTION_UP -> {
                        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(50).start()
                        selectReactionListener?.let {
                            if (userReaction != null) {
                                if (userReaction.emojiId == reaction.id) {
                                    it.onSelectReaction(null) // No selection
                                } else
                                    it.onSelectReaction(reaction)
                            } else
                                it.onSelectReaction(reaction)
                            dismiss()
                        }
                        return@setOnTouchListener true
                    }
                }
                return@setOnTouchListener false
            }

            imageView.scaleType = ImageView.ScaleType.CENTER

            val count = emojiCountMap?.get(reaction.id) ?: 0
            countView.apply {
                gravity = Gravity.RIGHT
                text = formattedReactionCount(count)
                setTextColor(chatViewThemeAttributes.chatReactionPanelCountColor)
                setTypeface(null, Typeface.BOLD)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, context.resources.getDimension(R.dimen.livelike_chat_reaction_popup_text_size))
            }
            relativeLayout.addView(imageView, RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.MATCH_PARENT).apply {
                addRule(RelativeLayout.CENTER_IN_PARENT)
            })
            relativeLayout.addView(countView, RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
                addRule(RelativeLayout.ALIGN_TOP, imageView.id)
                addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
            })
            reactionsBox.addView(relativeLayout, LinearLayout.LayoutParams(AndroidResource.dpToPx(35), AndroidResource.dpToPx(35)))
        }
        contentView.chat_reaction_background_card.visibility =
            if ((chatReactionRepository.reactionList?.size ?: 0) > 0) {
                View.VISIBLE
            } else {
                View.INVISIBLE
            }
    }
}
internal interface SelectReactionListener {
    fun onSelectReaction(reaction: Reaction?)
}
