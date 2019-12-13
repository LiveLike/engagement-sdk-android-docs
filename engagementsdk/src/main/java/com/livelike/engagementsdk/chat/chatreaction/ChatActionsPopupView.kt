package com.livelike.engagementsdk.chat.chatreaction

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
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
import kotlinx.android.synthetic.main.popup_chat_reaction.view.moderation_flag
import kotlinx.android.synthetic.main.popup_chat_reaction.view.moderation_flag_lay
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
    val userReaction: Reaction?=null,
    chatReactionBackground: Drawable? = ColorDrawable(Color.TRANSPARENT),
    chatReactionElevation: Float = 0f,
    chatReactionRadius: Float = 4f,
    chatReactionPanelColor: Int = Color.WHITE,
    var chatReactionPanelCountColor: Int = Color.BLACK,
    var chatReactionFlagTintColor:Int=Color.BLACK,
    chatReactionPadding: Int = AndroidResource.dpToPx(6),
    val selectReactionListener: SelectReactionListener?=null
) : PopupWindow(context) {

    init {
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
                chatReactionFlagTintColor,
                PorterDuff.Mode.MULTIPLY
            )
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
        val reactionsBox =
            contentView.findViewById<ImageView>(R.id.reaction_panel_interaction_box) as ViewGroup
        reactionsBox.removeAllViews()
        val threeDp = AndroidResource.dpToPx(2)
        val fiveDp = AndroidResource.dpToPx(5)
        chatReactionRepository.reactionList?.forEach { reaction ->
            val frameLayout = FrameLayout(context)
            val countView = TextView(context)
            val imageView = ImageView(context)
            frameLayout.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(2, 2, 2, 2)
            }
            frameLayout.setPadding(1, 0, 1, 0)

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

            imageView.loadImage(reaction.file, AndroidResource.dpToPx(24))
            imageView.scaleType = ImageView.ScaleType.CENTER

            val cnt = Random().nextInt(10000)
            countView.apply {
                text = formattedReactionCount(cnt)
                setTextColor(chatReactionPanelCountColor)
                setTypeface(null, Typeface.BOLD)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                visibility = when (cnt) {
                    0 -> View.GONE
                    else -> View.VISIBLE
                }
            }
            frameLayout.addView(
                imageView,
                FrameLayout.LayoutParams(
                    AndroidResource.dpToPx(29),
                    AndroidResource.dpToPx(29),
                    Gravity.CENTER
                ).apply {
                    setMargins(threeDp, 7, threeDp, 3)
                })
            frameLayout.addView(
                countView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.TOP or Gravity.RIGHT
                )
            )
            reactionsBox.addView(frameLayout)
        }
    }
}
interface SelectReactionListener{
    fun onSelectReaction(reaction: Reaction?)
}