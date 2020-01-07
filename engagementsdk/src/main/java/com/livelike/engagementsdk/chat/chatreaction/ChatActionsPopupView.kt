package com.livelike.engagementsdk.chat.chatreaction

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.chat.ChatViewThemeAttributes
import com.livelike.engagementsdk.utils.AndroidResource
import com.livelike.engagementsdk.widget.view.loadImage
import kotlinx.android.synthetic.main.popup_chat_reaction.view.chat_reaction_background_card
import kotlinx.android.synthetic.main.popup_chat_reaction.view.moderation_flag
import kotlinx.android.synthetic.main.popup_chat_reaction.view.moderation_flag_lay
import java.util.Random
import kotlin.math.ln
import kotlin.math.pow


/**
 * Chat reactions and Chat moderation actions view that will popup when use long press chat
 */
internal class ChatActionsPopupView(
    val context: Context,
    private val chatReactionRepository: ChatReactionRepository,
    flagClick: View.OnClickListener,
    hideFloatingUi: () -> Unit,
    isOwnMessage: Boolean,
    val userReaction: Reaction? = null,
    private val chatViewThemeAttributes: ChatViewThemeAttributes,
    val selectReactionListener: SelectReactionListener? = null
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

            if (!isOwnMessage) {
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
        }
        initReactions()
    }

    private fun formattedReactionCount(count: Int): String {
        if (count < 1000) return "" + count
        val exp = (ln(count.toDouble()) / ln(1000.0)).toInt()
        return String.format(
            "%.1f%c",
            count / 1000.0.pow(exp.toDouble()),
            "kMGTPE"[exp - 1]
        )
    }

    private fun initReactions() {
        val reactionsBox =
            contentView.findViewById<ImageView>(R.id.reaction_panel_interaction_box) as ViewGroup
        reactionsBox.removeAllViews()
        val eightDp = AndroidResource.dpToPx(8)
        chatReactionRepository.reactionList?.forEach { reaction ->
            val frameLayout = LinearLayout(context)
            val countView = TextView(context)
            val imageView = ImageView(context)
            frameLayout.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            }
            frameLayout.orientation = LinearLayout.VERTICAL
            imageView.loadImage(reaction.file, context.resources.getDimensionPixelSize(R.dimen.livelike_chat_reaction_size))

            userReaction?.let {
                if(it.name == reaction.name)
                    frameLayout.setBackgroundResource(R.drawable.chat_reaction_tap_background)
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
                                if (userReaction.name == reaction.name) {
                                    it.onSelectReaction(null) //No selection
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

            val cnt = Random().nextInt(10000)
            countView.apply {
                text = formattedReactionCount(cnt)
                setTextColor(chatViewThemeAttributes.chatReactionPanelCountColor)
                setTypeface(null, Typeface.BOLD)
                textSize = context.resources.getDimension(R.dimen.livelike_chat_reaction_popup_text_size)
                visibility = when (cnt) {
                    0 -> View.GONE
                    else -> View.VISIBLE
                }
            }
            frameLayout.addView(countView,LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.RIGHT
            })
            frameLayout.addView(imageView,LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.LEFT
                setMargins(eightDp,0,eightDp,0)
            })
            reactionsBox.addView(frameLayout)
        }
        contentView.chat_reaction_background_card.visibility =
            if ((chatReactionRepository.reactionList?.size ?: 0) > 0) {
                View.VISIBLE
            } else {
                View.INVISIBLE
            }
    }
}
internal interface SelectReactionListener{
    fun onSelectReaction(reaction: Reaction?)
}