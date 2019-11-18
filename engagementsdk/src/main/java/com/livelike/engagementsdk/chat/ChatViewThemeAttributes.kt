package com.livelike.engagementsdk.chat

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.Gravity
import com.livelike.engagementsdk.utils.AndroidResource

class ChatViewThemeAttributes {
     var chatPaddingLeft: Int=0
     var chatPaddingRight: Int=0
     var chatPaddingTop: Int=0
     var chatPaddingBottom: Int=0
     var chatSendPaddingLeft: Int=0
     var chatSendPaddingRight: Int=0
     var chatSendPaddingTop: Int=0
     var chatSendPaddingBottom: Int=0
     var chatMarginLeft: Int=0
     var chatMarginRight: Int=0
     var chatMarginTop: Int=0
     var chatMarginBottom: Int=0
     var chatWidth: Int=0
     var sendIconWidth: Int=0
     var sendIconHeight: Int=0
     var chatInputTextSize: Int=0
     var chatBubbleBackgroundRes: Drawable?=null
     var chatBackgroundRes: Drawable?=null
     var chatViewBackgroundRes: Drawable?=null
     var chatReactionBackgroundRes: Drawable?=null
     var chatInputBackgroundRes: Drawable?=null
     var chatInputViewBackgroundRes: Drawable?=null
     var chatDisplayBackgroundRes: Drawable?=null
     var chatSendDrawable: Drawable?=null
     var chatStickerSendDrawable: Drawable?=null
     var chatUserPicDrawable: Drawable?=null
     var chatSendBackgroundDrawable: Drawable?=null
     var chatMessageColor: Int= Color.TRANSPARENT
     var sendImageTintColor: Int= Color.WHITE
     var sendStickerTintColor: Int= Color.WHITE
     var chatReactionBackgroundColor: Int=Color.TRANSPARENT
     var chatInputTextColor: Int=Color.TRANSPARENT
     var chatInputHintTextColor: Int=Color.TRANSPARENT
     var chatOtherNickNameColor: Int=Color.TRANSPARENT
     var chatNickNameColor: Int=Color.TRANSPARENT
     var chatReactionX:Int=0
     var chatReactionY:Int=0
     var chatReactionElevation:Float=0f
     var chatReactionRadius:Float=0f
     var chatReactionPadding:Int=0
     var showChatAvatarLogo:Boolean=false
     var chatAvatarMarginRight:Int= AndroidResource.dpToPx(3)
     var chatAvatarMarginBottom:Int= AndroidResource.dpToPx(5)
     var chatAvatarMarginLeft:Int= AndroidResource.dpToPx(5)
     var chatAvatarMarginTop:Int= AndroidResource.dpToPx(0)
     var chatAvatarRadius:Int= AndroidResource.dpToPx(20)
     var chatAvatarCircle:Boolean=false
     var showStickerSend:Boolean=true
     var chatAvatarWidth:Int= AndroidResource.dpToPx(32)
     var chatAvatarHeight:Int= AndroidResource.dpToPx(32)
     var chatAvatarGravity:Int= Gravity.NO_GRAVITY

}