package com.livelike.engagementsdk.chat

import android.app.Activity
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.utils.AndroidResource

class ChatViewThemeAttributes {
    fun initAttributes(context: Activity, typedArray: TypedArray?) {
        typedArray?.apply {
            showChatAvatarLogo = getBoolean(R.styleable.LiveLike_ChatView_showChatAvatarLogo, false)
            chatAvatarCircle = getBoolean(R.styleable.LiveLike_ChatView_chatAvatarCircle, false)
            showStickerSend = getBoolean(R.styleable.LiveLike_ChatView_showStickerSend, true)
            closeKeyboardOnSend = getBoolean(R.styleable.LiveLike_ChatView_closeKeyboardOnSend, true)
            chatNickNameColor = getColor(
                R.styleable.LiveLike_ChatView_usernameColor,
                ContextCompat.getColor(context, R.color.livelike_openChatNicknameMe)
            )
            chatOtherNickNameColor = getColor(
                R.styleable.LiveLike_ChatView_otherUsernameColor,
                ContextCompat.getColor(context, R.color.livelike_openChatNicknameOther)
            )
            chatMessageColor = getColor(
                R.styleable.LiveLike_ChatView_messageColor,
                ContextCompat.getColor(
                    context,
                    R.color.livelike_default_chat_cell_message_color
                )
            )
            rankValueTextColor = getColor(
                R.styleable.LiveLike_ChatView_rankValueTextColor,
                Color.WHITE
            )

            sendImageTintColor = getColor(
                R.styleable.LiveLike_ChatView_sendIconTintColor,
                ContextCompat.getColor(context, android.R.color.white)
            )
            sendStickerTintColor = getColor(
                R.styleable.LiveLike_ChatView_stickerIconTintColor,
                ContextCompat.getColor(context, android.R.color.white)
            )

            chatAvatarGravity =
                getInt(R.styleable.LiveLike_ChatView_chatAvatarGravity, Gravity.NO_GRAVITY)

            val emptyBackValue=TypedValue()
            getValue(R.styleable.LiveLike_ChatView_emptyChatBackgroundImage,emptyBackValue)

            chatEmptyBackgroundImage = when(emptyBackValue.type){
                TypedValue.TYPE_REFERENCE, TypedValue.TYPE_STRING -> ContextCompat.getDrawable(
                    context,
                    getResourceId(
                        R.styleable.LiveLike_ChatView_emptyChatBackgroundImage,
                        R.drawable.placeholder_chat
                    )
                )
                else -> ContextCompat.getDrawable(
                    context,
                    R.drawable.placeholder_chat
                )
            }

            chatEmptyBackgroundText =
                getString(R.styleable.LiveLike_ChatView_emptyChatBackgroundText)

            chatEmptyBackgroundTextColor = getColor(
                R.styleable.LiveLike_ChatView_emptyChatBackgroundTextColor,
                Color.WHITE
            )

            chatEmptyBackgroundTextSize = getDimension(
                R.styleable.LiveLike_ChatView_emptyChatBackgroundTextSize,
                17f
            )

            val colorBubbleValue = TypedValue()
            getValue(R.styleable.LiveLike_ChatView_chatBubbleBackground, colorBubbleValue)

            chatBubbleBackgroundRes = when  {
                colorBubbleValue.type==TypedValue.TYPE_REFERENCE -> getResourceId(
                    R.styleable.LiveLike_ChatView_chatBubbleBackground,
                    R.drawable.ic_chat_message_bubble_rounded_rectangle
                )
                colorBubbleValue.type==TypedValue.TYPE_NULL -> R.drawable.ic_chat_message_bubble_rounded_rectangle
                colorBubbleValue.type >= TypedValue.TYPE_FIRST_COLOR_INT && colorBubbleValue.type <= TypedValue.TYPE_LAST_COLOR_INT ->colorBubbleValue.data
                else -> R.drawable.ic_chat_message_bubble_rounded_rectangle
            }

            val colorBackValue = TypedValue()
            getValue(R.styleable.LiveLike_ChatView_chatBackground, colorBackValue)

            chatBackgroundRes = when (colorBackValue.type) {
                TypedValue.TYPE_REFERENCE, TypedValue.TYPE_STRING -> ContextCompat.getDrawable(
                    context,
                    getResourceId(
                        R.styleable.LiveLike_ChatView_chatBackground,
                        android.R.color.transparent
                    )
                )
                TypedValue.TYPE_NULL -> ContextCompat.getDrawable(
                    context,
                    android.R.color.transparent
                )
                else -> ColorDrawable(colorBackValue.data)
            }

            val sendDrawable = TypedValue()
            getValue(R.styleable.LiveLike_ChatView_chatSendDrawable, sendDrawable)

            chatSendDrawable = when (sendDrawable.type) {
                TypedValue.TYPE_REFERENCE, TypedValue.TYPE_STRING -> ContextCompat.getDrawable(
                    context,
                    getResourceId(
                        R.styleable.LiveLike_ChatView_chatSendDrawable,
                        R.drawable.ic_chat_send
                    )
                )
                else -> ContextCompat.getDrawable(
                    context,
                    R.drawable.ic_chat_send
                )
            }

            val sendStickerDrawable = TypedValue()
            getValue(R.styleable.LiveLike_ChatView_chatStickerSendDrawable, sendDrawable)

            chatStickerSendDrawable = when (sendStickerDrawable.type) {
                TypedValue.TYPE_REFERENCE, TypedValue.TYPE_STRING -> ContextCompat.getDrawable(
                    context,
                    getResourceId(
                        R.styleable.LiveLike_ChatView_chatStickerSendDrawable,
                        R.drawable.ic_chat_emoji_ios_category_smileysandpeople
                    )
                )
                else -> ContextCompat.getDrawable(
                    context,
                    R.drawable.ic_chat_emoji_ios_category_smileysandpeople
                )
            }

            val userPicDrawable = TypedValue()
            getValue(R.styleable.LiveLike_ChatView_userPicDrawable, sendDrawable)

            chatUserPicDrawable = when (userPicDrawable.type) {
                TypedValue.TYPE_REFERENCE, TypedValue.TYPE_STRING -> ContextCompat.getDrawable(
                    context,
                    getResourceId(
                        R.styleable.LiveLike_ChatView_userPicDrawable,
                        R.drawable.ic_user_pic
                    )
                )
                else -> ContextCompat.getDrawable(
                    context,
                    R.drawable.ic_user_pic
                )
            }

            val chatSendBackValue = TypedValue()
            getValue(R.styleable.LiveLike_ChatView_chatSendBackground, chatSendBackValue)

            chatSendBackgroundDrawable = when (chatSendBackValue.type) {
                TypedValue.TYPE_REFERENCE, TypedValue.TYPE_STRING -> ContextCompat.getDrawable(
                    context,
                    getResourceId(
                        R.styleable.LiveLike_ChatView_chatSendBackground,
                        android.R.color.transparent
                    )
                )
                TypedValue.TYPE_NULL -> ContextCompat.getDrawable(
                    context,
                    android.R.color.transparent
                )
                else -> ColorDrawable(chatSendBackValue.data)
            }

            val colorReactionValue = TypedValue()
            getValue(R.styleable.LiveLike_ChatView_chatReactionBackground, colorReactionValue)

            chatReactionBackgroundRes = when (colorReactionValue.type) {
                TypedValue.TYPE_REFERENCE, TypedValue.TYPE_STRING -> ContextCompat.getDrawable(
                    context,
                    getResourceId(
                        R.styleable.LiveLike_ChatView_chatReactionBackground,
                        android.R.color.transparent
                    )
                )
                TypedValue.TYPE_NULL -> ContextCompat.getDrawable(
                    context,
                    android.R.color.transparent
                )
                else -> ColorDrawable(colorReactionValue.data)
            }

            val colorViewValue = TypedValue()
            getValue(R.styleable.LiveLike_ChatView_chatViewBackground, colorViewValue)

            chatViewBackgroundRes = when (colorViewValue.type) {
                TypedValue.TYPE_REFERENCE -> ContextCompat.getDrawable(
                    context,
                    getResourceId(
                        R.styleable.LiveLike_ChatView_chatViewBackground,
                        android.R.color.transparent
                    )
                )
                TypedValue.TYPE_NULL -> ColorDrawable(Color.TRANSPARENT)
                else -> ColorDrawable(colorViewValue.data)
            }

            val colorChatDisplayValue = TypedValue()
            getValue(R.styleable.LiveLike_ChatView_chatDisplayBackground, colorChatDisplayValue)

            chatDisplayBackgroundRes = when (colorChatDisplayValue.type) {
                TypedValue.TYPE_REFERENCE, TypedValue.TYPE_STRING -> ContextCompat.getDrawable(
                    context,
                    getResourceId(
                        R.styleable.LiveLike_ChatView_chatDisplayBackground,
                        android.R.color.transparent
                    )
                )
                TypedValue.TYPE_NULL -> ColorDrawable(Color.TRANSPARENT)
                else -> ColorDrawable(colorChatDisplayValue.data)
            }

            val colorInputBackgroundValue = TypedValue()
            getValue(R.styleable.LiveLike_ChatView_chatInputBackground, colorInputBackgroundValue)

            chatInputBackgroundRes = when (colorInputBackgroundValue.type) {
                TypedValue.TYPE_REFERENCE, TypedValue.TYPE_STRING -> ContextCompat.getDrawable(
                    context,
                    getResourceId(
                        R.styleable.LiveLike_ChatView_chatInputBackground,
                        R.drawable.ic_chat_input
                    )
                )
                TypedValue.TYPE_NULL -> ContextCompat.getDrawable(
                    context,
                    R.drawable.ic_chat_input
                )
                else -> ColorDrawable(colorInputBackgroundValue.data)
            }

            val colorInputViewBackgroundValue = TypedValue()
            getValue(
                R.styleable.LiveLike_ChatView_chatInputViewBackground,
                colorInputViewBackgroundValue
            )

            chatInputViewBackgroundRes = when (colorInputViewBackgroundValue.type) {
                TypedValue.TYPE_REFERENCE -> ContextCompat.getDrawable(
                    context,
                    getResourceId(
                        R.styleable.LiveLike_ChatView_chatInputViewBackground,
                        android.R.color.transparent
                    )
                )
                TypedValue.TYPE_NULL -> ColorDrawable(
                    ContextCompat.getColor(context, android.R.color.transparent)
                )
                else -> ColorDrawable(colorInputViewBackgroundValue.data)
            }

            chatInputTextColor = getColor(
                R.styleable.LiveLike_ChatView_chatInputTextColor,
                ContextCompat.getColor(context, R.color.livelike_chat_input_text_color)
            )
            chatInputHintTextColor = getColor(
                R.styleable.LiveLike_ChatView_chatInputTextHintColor,
                ContextCompat.getColor(context, R.color.livelike_chat_input_text_color)
            )

            chatWidth = getLayoutDimension(
                R.styleable.LiveLike_ChatView_chatWidth,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )

            sendIconHeight = getLayoutDimension(
                R.styleable.LiveLike_ChatView_sendButtonHeight,
                AndroidResource.dpToPx(40)
            )
            sendIconWidth = getLayoutDimension(
                R.styleable.LiveLike_ChatView_sendButtonWidth,
                AndroidResource.dpToPx(56)
            )

            chatInputTextSize = getDimensionPixelSize(
                R.styleable.LiveLike_ChatView_chatInputTextSize,
                resources.getDimensionPixelSize(R.dimen.livelike_default_chat_input_text_size)
            )
            chatReactionX =
                getDimensionPixelSize(
                    R.styleable.LiveLike_ChatView_chatReactionXPosition,
                    AndroidResource.dpToPx(8)
                )
            chatReactionY = getDimensionPixelSize(
                R.styleable.LiveLike_ChatView_chatReactionYPosition,
                AndroidResource.dpToPx(40)
            )
            chatReactionElevation = getDimensionPixelSize(
                R.styleable.LiveLike_ChatView_chatReactionElevation,
                AndroidResource.dpToPx(0)
            ).toFloat()
            chatReactionRadius = getDimensionPixelSize(
                R.styleable.LiveLike_ChatView_chatReactionRadius,
                AndroidResource.dpToPx(0)
            ).toFloat()
            chatReactionPadding =
                getDimensionPixelSize(
                    R.styleable.LiveLike_ChatView_chatReactionPadding,
                    AndroidResource.dpToPx(6)
                )
            chatAvatarHeight =
                getDimensionPixelSize(
                    R.styleable.LiveLike_ChatView_chatAvatarHeight,
                    AndroidResource.dpToPx(32)
                )
            chatAvatarWidth =
                getDimensionPixelSize(
                    R.styleable.LiveLike_ChatView_chatAvatarWidth,
                    AndroidResource.dpToPx(32)
                )
            chatAvatarRadius =
                getDimensionPixelSize(
                    R.styleable.LiveLike_ChatView_chatAvatarRadius,
                    AndroidResource.dpToPx(0)
                )
            chatAvatarMarginLeft =
                getDimensionPixelSize(
                    R.styleable.LiveLike_ChatView_chatAvatarMarginLeft,
                    AndroidResource.dpToPx(5)
                )
            chatAvatarMarginRight =
                getDimensionPixelSize(
                    R.styleable.LiveLike_ChatView_chatAvatarMarginRight,
                    AndroidResource.dpToPx(3)
                )
            chatAvatarMarginBottom = getDimensionPixelSize(
                R.styleable.LiveLike_ChatView_chatAvatarMarginBottom,
                AndroidResource.dpToPx(5)
            )
            chatAvatarMarginTop =
                getDimensionPixelSize(
                    R.styleable.LiveLike_ChatView_chatAvatarMarginTop,
                    AndroidResource.dpToPx(0)
                )

            chatReactionBackgroundColor = getColor(
                R.styleable.LiveLike_ChatView_chatReactionBackgroundColor,
                ContextCompat.getColor(context, android.R.color.transparent)
            )

            chatBubblePaddingLeft = getDimensionPixelOffset(
                R.styleable.LiveLike_ChatView_chatBubblePaddingLeft,
                resources.getDimension(R.dimen.livelike_default_chat_cell_padding_left).toInt()
            )
            chatBubblePaddingRight = getDimensionPixelOffset(
                R.styleable.LiveLike_ChatView_chatBubblePaddingRight,
                resources.getDimension(R.dimen.livelike_default_chat_cell_padding_right).toInt()
            )
            chatBubblePaddingTop = getDimensionPixelOffset(
                R.styleable.LiveLike_ChatView_chatBubblePaddingTop,
                resources.getDimension(R.dimen.livelike_default_chat_cell_padding_top).toInt()
            )
            chatBubblePaddingBottom = getDimensionPixelOffset(
                R.styleable.LiveLike_ChatView_chatBubblePaddingBottom,
                resources.getDimension(R.dimen.livelike_default_chat_cell_padding_bottom).toInt()
            )

            chatBubbleMarginLeft = getDimensionPixelOffset(
                R.styleable.LiveLike_ChatView_chatBubbleMarginLeft, 0
            )
            chatBubbleMarginRight = getDimensionPixelOffset(
                R.styleable.LiveLike_ChatView_chatBubbleMarginRight, 0
            )
            chatBubbleMarginTop = getDimensionPixelOffset(
                R.styleable.LiveLike_ChatView_chatBubbleMarginTop, 0
            )
            chatBubbleMarginBottom = getDimensionPixelOffset(
                R.styleable.LiveLike_ChatView_chatBubbleMarginBottom, 0
            )

            chatSendPaddingLeft = getDimensionPixelOffset(
                R.styleable.LiveLike_ChatView_chatSendButtonPaddingLeft,
                AndroidResource.dpToPx(13)
            )
            chatSendPaddingRight = getDimensionPixelOffset(
                R.styleable.LiveLike_ChatView_chatSendButtonPaddingRight,
                AndroidResource.dpToPx(13)
            )
            chatSendPaddingTop = getDimensionPixelOffset(
                R.styleable.LiveLike_ChatView_chatSendButtonPaddingTop,
                AndroidResource.dpToPx(0)
            )
            chatSendPaddingBottom = getDimensionPixelOffset(
                R.styleable.LiveLike_ChatView_chatSendButtonPaddingBottom,
                AndroidResource.dpToPx(0)
            )

            chatMarginLeft = getDimensionPixelOffset(
                R.styleable.LiveLike_ChatView_chatMarginLeft,
                AndroidResource.dpToPx(8)
            )
            chatMarginRight = getDimensionPixelOffset(
                R.styleable.LiveLike_ChatView_chatMarginRight,
                AndroidResource.dpToPx(8)
            )
            chatMarginTop =
                getDimensionPixelOffset(
                    R.styleable.LiveLike_ChatView_chatMarginTop,
                    AndroidResource.dpToPx(4)
                )
            chatMarginBottom = getDimensionPixelOffset(
                R.styleable.LiveLike_ChatView_chatMarginBottom,
                AndroidResource.dpToPx(4)
            )
            chatMarginBottom = getDimensionPixelOffset(
                R.styleable.LiveLike_ChatView_chatMarginBottom,
                AndroidResource.dpToPx(4)
            )
        }
    }

    var chatBubblePaddingLeft: Int = 0
    var chatBubblePaddingRight: Int = 0
    var chatBubblePaddingTop: Int = 0
    var chatBubblePaddingBottom: Int = 0
    var chatSendPaddingLeft: Int = 0
    var chatSendPaddingRight: Int = 0
    var chatSendPaddingTop: Int = 0
    var chatSendPaddingBottom: Int = 0
    var chatMarginLeft: Int = 0
    var chatMarginRight: Int = 0
    var chatMarginTop: Int = 0
    var chatMarginBottom: Int = 0
    var chatBubbleMarginLeft: Int = 0
    var chatBubbleMarginRight: Int = 0
    var chatBubbleMarginTop: Int = 0
    var chatBubbleMarginBottom: Int = 0
    var chatWidth: Int = 0
    var sendIconWidth: Int = 0
    var sendIconHeight: Int = 0
    var chatInputTextSize: Int = 0
    var chatBubbleBackgroundRes: Int? = null
    var chatBackgroundRes: Drawable? = null
    var chatViewBackgroundRes: Drawable? = null
    var chatReactionBackgroundRes: Drawable? = null
    var chatInputBackgroundRes: Drawable? = null
    var chatInputViewBackgroundRes: Drawable? = null
    var chatDisplayBackgroundRes: Drawable? = null
    var chatSendDrawable: Drawable? = null
    var chatStickerSendDrawable: Drawable? = null
    var chatUserPicDrawable: Drawable? = null
    var chatSendBackgroundDrawable: Drawable? = null
    var chatMessageColor: Int = Color.TRANSPARENT
    var sendImageTintColor: Int = Color.WHITE
    var sendStickerTintColor: Int = Color.WHITE
    var rankValueTextColor: Int = Color.WHITE
    var chatReactionBackgroundColor: Int = Color.TRANSPARENT
    var chatInputTextColor: Int = Color.TRANSPARENT
    var chatInputHintTextColor: Int = Color.TRANSPARENT
    var chatOtherNickNameColor: Int = Color.TRANSPARENT
    var chatNickNameColor: Int = Color.TRANSPARENT
    var chatReactionX: Int = 0
    var chatReactionY: Int = 0
    var chatReactionElevation: Float = 0f
    var chatReactionRadius: Float = 0f
    var chatReactionPadding: Int = 0
    var showChatAvatarLogo: Boolean = false
    var chatAvatarMarginRight: Int = AndroidResource.dpToPx(3)
    var chatAvatarMarginBottom: Int = AndroidResource.dpToPx(5)
    var chatAvatarMarginLeft: Int = AndroidResource.dpToPx(5)
    var chatAvatarMarginTop: Int = AndroidResource.dpToPx(0)
    var chatAvatarRadius: Int = AndroidResource.dpToPx(20)
    var chatAvatarCircle: Boolean = false
    var showStickerSend: Boolean = true
    var chatAvatarWidth: Int = AndroidResource.dpToPx(32)
    var chatAvatarHeight: Int = AndroidResource.dpToPx(32)
    var chatAvatarGravity: Int = Gravity.NO_GRAVITY
    var chatEmptyBackgroundImage:Drawable?=null
    var chatEmptyBackgroundText:String?=null
    var chatEmptyBackgroundTextColor:Int=Color.WHITE
    var chatEmptyBackgroundTextSize:Float = 17f
    var closeKeyboardOnSend: Boolean = true
}
