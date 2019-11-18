package com.livelike.engagementsdk.chat

import android.support.v7.app.AlertDialog
import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.text.SpannableString
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.chat.chatreaction.ChatActionsPopupView
import com.livelike.engagementsdk.chat.chatreaction.ChatReactionRepository
import com.livelike.engagementsdk.stickerKeyboard.StickerPackRepository
import com.livelike.engagementsdk.stickerKeyboard.countMatches
import com.livelike.engagementsdk.stickerKeyboard.findIsOnlyStickers
import com.livelike.engagementsdk.stickerKeyboard.findStickers
import com.livelike.engagementsdk.stickerKeyboard.replaceWithStickers
import com.livelike.engagementsdk.utils.liveLikeSharedPrefs.blockUser
import com.livelike.engagementsdk.widget.view.getLocationOnScreen
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlinx.android.synthetic.main.default_chat_cell.view.chatBubbleBackground
import kotlinx.android.synthetic.main.default_chat_cell.view.chatMessage
import kotlinx.android.synthetic.main.default_chat_cell.view.chat_nickname
import kotlinx.android.synthetic.main.default_chat_cell.view.img_chat_avatar
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import kotlinx.android.synthetic.main.default_chat_cell.view.chatBackground


private val diffChatMessage: DiffUtil.ItemCallback<ChatMessage> = object : DiffUtil.ItemCallback<ChatMessage>() {
    override fun areItemsTheSame(p0: ChatMessage, p1: ChatMessage): Boolean {
        return p0.id == p1.id
    }

    override fun areContentsTheSame(p0: ChatMessage, p1: ChatMessage): Boolean {
        return p0.message == p1.message && p0.senderDisplayName == p1.senderDisplayName
    }
}

internal class ChatRecyclerAdapter(
    private val analyticsService: AnalyticsService,
    private val reporter: (ChatMessage) -> Unit,
    private val stickerPackRepository: StickerPackRepository,
    val chatReactionRepository: ChatReactionRepository

) : ListAdapter<ChatMessage, ChatRecyclerAdapter.ViewHolder>(diffChatMessage) {

    lateinit var chatViewThemeAttribute:ChatViewThemeAttributes

    internal var isPublicChat: Boolean = true

    override fun onCreateViewHolder(root: ViewGroup, position: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(root.context).inflate(R.layout.default_chat_cell, root, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindTo(getItem(position))
    }



    inner class ViewHolder(val v: View) : RecyclerView.ViewHolder(v), View.OnLongClickListener, View.OnClickListener {
        private var message: ChatMessage? = null
        private val dialogOptions = listOf(
            v.context.getString(R.string.flag_ui_blocking_title) to { msg: ChatMessage ->
                AlertDialog.Builder(v.context).apply {
                    setMessage(context.getString(R.string.flag_ui_blocking_message, msg.senderDisplayName))
                    setPositiveButton("OK") { _, _ ->
                        analyticsService.trackBlockingUser()
                        blockUser(msg.senderId)
                    }
                    create()
                }.show()
            },
            v.context.getString(R.string.flag_ui_reporting_title) to { msg: ChatMessage ->
                AlertDialog.Builder(v.context).apply {
                    setMessage(context.getString(R.string.flag_ui_reporting_message))
                    setPositiveButton("OK") { _, _ ->
                        analyticsService.trackReportingMessage()
                        reporter(msg)
                    }
                    create()
                }.show()
            })

        override fun onLongClick(p0: View?): Boolean {
            if (isPublicChat)
                showFloatingUI((p0?.tag as ChatMessage?)?.isFromMe ?: false)
            return true
        }

        override fun onClick(p0: View?) {
            hideFloatingUI()
        }

        init {
            v.chatMessage.setTextColor(chatViewThemeAttribute.chatMessageColor)


            v.setOnLongClickListener(this)
            v.setOnClickListener(this)
        }

        fun bindTo(item: ChatMessage?) {
            v.tag = item
            setMessage(item)
            hideFloatingUI()
        }

        private fun showFloatingUI(isOwnMessage: Boolean) {
            v.chatBackground.alpha = 0.5f
            val locationOnScreen = v.getLocationOnScreen()
            ChatActionsPopupView(
                v.context,
                chatReactionRepository,
                View.OnClickListener { _ ->
                    analyticsService.trackFlagButtonPressed()
                    hideFloatingUI()
                    v.context?.let { ctx ->
                        AlertDialog.Builder(ctx).apply {
                            setTitle(context.getString(R.string.flag_ui_title))
                            setItems(dialogOptions.map { it.first }.toTypedArray()) { _, which ->
                                message?.let {
                                    dialogOptions[which].second.invoke(it)
                                }
                            }
                            setOnCancelListener { analyticsService.trackCancelFlagUi() }
                            create()
                        }.show()
                    }
                },
                ::hideFloatingUI,
                isOwnMessage,
                chatReactionBackground = chatViewThemeAttribute.chatReactionBackgroundRes,
                chatReactionElevation = chatViewThemeAttribute.chatReactionElevation,
                chatReactionRadius = chatViewThemeAttribute.chatReactionRadius,
                chatReactionBackgroundColor=chatViewThemeAttribute.chatReactionBackgroundColor,
                        chatReactionPadding=chatViewThemeAttribute.chatReactionPadding
            ).showAtLocation(v, Gravity.NO_GRAVITY, locationOnScreen.x + chatViewThemeAttribute.chatReactionX, locationOnScreen.y - chatViewThemeAttribute.chatReactionY)
        }

        private fun hideFloatingUI() {
            v.apply {
                chatBackground.alpha = 1f
            }
        }

        private fun setMessage(
            message: ChatMessage?
        ) {
            v.apply {
                this@ViewHolder.message = message
                message?.apply {

                    if (message.isFromMe) {
                        chat_nickname.setTextColor(chatViewThemeAttribute.chatNickNameColor)
                        chat_nickname.text = context.getString(R.string.chat_pre_nickname_me, message.senderDisplayName)
                    } else {
                        chat_nickname.setTextColor(chatViewThemeAttribute.chatOtherNickNameColor)
                        chat_nickname.text = message.senderDisplayName
                    }

                    val layoutParam=FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,FrameLayout.LayoutParams.WRAP_CONTENT)
                    layoutParam.setMargins(chatViewThemeAttribute.chatMarginLeft,chatViewThemeAttribute.chatMarginTop,chatViewThemeAttribute.chatMarginRight,chatViewThemeAttribute.chatMarginBottom)
                    v.chatBackground.layoutParams = layoutParam

                    val layoutParam1=LinearLayout.LayoutParams(chatViewThemeAttribute.chatWidth,LinearLayout.LayoutParams.WRAP_CONTENT)
                    // layoutParam1.setMargins(chatMarginLeft,chatMarginTop,chatMarginRight,chatMarginBottom)
                    v.chatBubbleBackground.layoutParams = layoutParam1


                    v.img_chat_avatar.visibility=when(chatViewThemeAttribute.showChatAvatarLogo){
                        true -> View.VISIBLE
                        else -> View.GONE
                    }
                    val layoutParamAvatar=LinearLayout.LayoutParams(chatViewThemeAttribute.chatAvatarWidth,chatViewThemeAttribute.chatAvatarHeight)
                    layoutParamAvatar.setMargins(chatViewThemeAttribute.chatAvatarMarginLeft,chatViewThemeAttribute.chatAvatarMarginTop,chatViewThemeAttribute.chatAvatarMarginRight,chatViewThemeAttribute.chatAvatarMarginBottom)
                    layoutParamAvatar.gravity=chatViewThemeAttribute.chatAvatarGravity
                    v.img_chat_avatar.layoutParams=layoutParamAvatar

                    v.chatBackground.background=chatViewThemeAttribute.chatBackgroundRes

                    v.chatBubbleBackground.background=chatViewThemeAttribute.chatBubbleBackgroundRes
                    v.chatBubbleBackground.setPadding(chatViewThemeAttribute.chatPaddingLeft,chatViewThemeAttribute.chatPaddingTop,chatViewThemeAttribute.chatPaddingRight,chatViewThemeAttribute.chatPaddingBottom)


                    val options = RequestOptions()
                    if(chatViewThemeAttribute.chatAvatarCircle){
                     options.optionalCircleCrop()
                    }
                    if(chatViewThemeAttribute.chatAvatarRadius>0){
                        options.transform(CenterCrop(), RoundedCorners(chatViewThemeAttribute.chatAvatarRadius))
                    }
                    message.senderDisplayPic.let {
                        if(it.isNotEmpty())
                        Glide.with(context).load(it)
                            .apply(options)
                            .placeholder(chatViewThemeAttribute.chatUserPicDrawable)
                            .into(img_chat_avatar)
                        else
                            img_chat_avatar.setImageDrawable(chatViewThemeAttribute.chatUserPicDrawable)
                    }

                    val spaceRemover = Pattern.compile("[\\s]")
                    val inputNoString = spaceRemover.matcher(message.message).replaceAll(Matcher.quoteReplacement(""))
                    val isOnlyStickers = inputNoString.findIsOnlyStickers().matches()
                    val atLeastOneSticker = inputNoString.findStickers().find()
                    val numberOfStickers = message.message.findStickers().countMatches()

                    when {
                        (isOnlyStickers && numberOfStickers == 1) -> {
                            val s = SpannableString(message.message)
                            replaceWithStickers(s, context, stickerPackRepository, null, 200) {
                                    // TODO this might write to the wrong messageView on slow connection.
                                    chatMessage.text = s
                            }
                        }
                        atLeastOneSticker -> {
                            val s = SpannableString(message.message)
                            replaceWithStickers(s, context, stickerPackRepository, null) {
                                    // TODO this might write to the wrong messageView on slow connection.
                                    chatMessage.text = s
                            }
                        }
                        else -> chatMessage.text = message.message
                    }
                }
            }
        }
    }
}
