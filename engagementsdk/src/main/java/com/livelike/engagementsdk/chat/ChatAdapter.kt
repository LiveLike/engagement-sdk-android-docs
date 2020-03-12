package com.livelike.engagementsdk.chat

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.ACCESSIBILITY_SERVICE
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.support.constraint.ConstraintLayout
import android.support.v7.app.AlertDialog
import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.text.SpannableString
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.chat.chatreaction.ChatActionsPopupView
import com.livelike.engagementsdk.chat.chatreaction.ChatReactionRepository
import com.livelike.engagementsdk.chat.chatreaction.Reaction
import com.livelike.engagementsdk.chat.chatreaction.SelectReactionListener
import com.livelike.engagementsdk.stickerKeyboard.StickerPackRepository
import com.livelike.engagementsdk.stickerKeyboard.clearTarget
import com.livelike.engagementsdk.stickerKeyboard.countMatches
import com.livelike.engagementsdk.stickerKeyboard.findImages
import com.livelike.engagementsdk.stickerKeyboard.findIsOnlyStickers
import com.livelike.engagementsdk.stickerKeyboard.findStickers
import com.livelike.engagementsdk.stickerKeyboard.replaceWithImages
import com.livelike.engagementsdk.stickerKeyboard.replaceWithStickers
import com.livelike.engagementsdk.utils.AndroidResource
import com.livelike.engagementsdk.utils.liveLikeSharedPrefs.blockUser
import com.livelike.engagementsdk.widget.view.getLocationOnScreen
import com.livelike.engagementsdk.widget.view.loadImage
import kotlinx.android.synthetic.main.default_chat_cell.view.border_bottom
import kotlinx.android.synthetic.main.default_chat_cell.view.border_top
import kotlinx.android.synthetic.main.default_chat_cell.view.chatBackground
import kotlinx.android.synthetic.main.default_chat_cell.view.chatBubbleBackground
import kotlinx.android.synthetic.main.default_chat_cell.view.chatMessage
import kotlinx.android.synthetic.main.default_chat_cell.view.chat_nickname
import kotlinx.android.synthetic.main.default_chat_cell.view.img_chat_avatar
import kotlinx.android.synthetic.main.default_chat_cell.view.message_date_time
import kotlinx.android.synthetic.main.default_chat_cell.view.rel_reactions_lay
import kotlinx.android.synthetic.main.default_chat_cell.view.txt_chat_reactions_count
import pl.droidsonroids.gif.MultiCallback
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.regex.Matcher
import java.util.regex.Pattern

private val diffChatMessage: DiffUtil.ItemCallback<ChatMessage> =
    object : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(p0: ChatMessage, p1: ChatMessage): Boolean {
            return p0.id == p1.id
        }

        override fun areContentsTheSame(p0: ChatMessage, p1: ChatMessage): Boolean {
            return p0.message == p1.message && p0.senderDisplayName == p1.senderDisplayName
        }
    }

internal class ChatRecyclerAdapter(
    private val analyticsService: AnalyticsService,
    private val reporter: (ChatMessage) -> Unit
) : ListAdapter<ChatMessage, ChatRecyclerAdapter.ViewHolder>(diffChatMessage) {

    private var lastFloatingUiAnchorView: View? = null
    var chatRepository: ChatRepository? = null
    lateinit var stickerPackRepository: StickerPackRepository
    lateinit var chatReactionRepository: ChatReactionRepository

    lateinit var checkListIsAtTop: (position: Int) -> Boolean
    lateinit var chatViewThemeAttribute: ChatViewThemeAttributes

    internal var isPublicChat: Boolean = true

    internal var messageTimeFormatter: ((time: Long?) -> String)? = null
    private var currentChatReactionPopUpViewPos: Int = -1
    private var chatPopUpView: ChatActionsPopupView? = null

    override fun onCreateViewHolder(root: ViewGroup, position: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(root.context).inflate(
                R.layout.default_chat_cell,
                root,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindTo(getItem(position))
        holder.itemView.requestLayout()
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        holder.hideFloatingUI()
        super.onViewDetachedFromWindow(holder)
    }

    private fun isAccessibilityEnabled(context: Context): Boolean {
        val am = context.getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        return am.isEnabled && am.isTouchExplorationEnabled
    }

    inner class ViewHolder(val v: View) : RecyclerView.ViewHolder(v), View.OnLongClickListener,
        View.OnClickListener {
        private var message: ChatMessage? = null
        private val bounceAnimation: Animation =
            AnimationUtils.loadAnimation(v.context, R.anim.bounce_animation)
        private val dialogOptions = listOf(
            v.context.getString(R.string.flag_ui_blocking_title) to { msg: ChatMessage ->
                AlertDialog.Builder(v.context).apply {
                    setMessage(
                        context.getString(
                            R.string.flag_ui_blocking_message,
                            msg.senderDisplayName
                        )
                    )
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

        override fun onLongClick(view: View?): Boolean {
            return true
        }

        private fun wouldShowFloatingUi(view: View?) {
            if (lastFloatingUiAnchorView == view) {
                lastFloatingUiAnchorView = null
                return
            }
            lastFloatingUiAnchorView = view
            val isOwnMessage = (view?.tag as ChatMessage?)?.isFromMe ?: false
            val reactionsAvailable = (chatReactionRepository.reactionList?.size ?: 0) > 0
            if (reactionsAvailable || !isOwnMessage) {
                showFloatingUI(
                    isOwnMessage,
                    message?.myChatMessageReaction,
                    checkListIsAtTop(adapterPosition) && itemCount > 1
                )
            }
        }

        override fun onClick(view: View?) {
            if (chatPopUpView?.isShowing == true) {
                hideFloatingUI()
            } else {
                wouldShowFloatingUi(view)
            }
        }

        init {
            chatViewThemeAttribute.chatBubbleBackgroundRes?.let { res ->
                if (res < 0) {
                    v.chatBubbleBackground.setBackgroundColor(res)
                } else {
                    val value = TypedValue()
                    v.context.resources.getValue(res, value, true)
                    when {
                        value.type == TypedValue.TYPE_REFERENCE -> v.chatBubbleBackground.setBackgroundResource(
                            res
                        )
                        value.type == TypedValue.TYPE_NULL -> v.chatBubbleBackground.setBackgroundResource(
                            R.drawable.ic_chat_message_bubble_rounded_rectangle
                        )
                        value.type >= TypedValue.TYPE_FIRST_COLOR_INT && value.type <= TypedValue.TYPE_LAST_COLOR_INT -> ColorDrawable(
                            value.data
                        )
                        else -> v.chatBubbleBackground.setBackgroundResource(R.drawable.ic_chat_message_bubble_rounded_rectangle)
                    }
                }
            }
            v.setOnLongClickListener(this)
            v.setOnClickListener(this)
        }

        val callback = MultiCallback(true)

        fun bindTo(item: ChatMessage?) {
            v.tag = item
            setMessage(item)
            updateBackground()
            if (item?.timetoken != 0L) {
                v.postDelayed({
                    v.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
                }, 100)
            }
            if (currentChatReactionPopUpViewPos > -1 && currentChatReactionPopUpViewPos == adapterPosition) {
                if (chatPopUpView?.isShowing == true) {
                    chatPopUpView?.updatePopView(
                        message?.emojiCountMap,
                        message?.myChatMessageReaction
                    )
                }
            }
        }

        private fun showFloatingUI(
            isOwnMessage: Boolean,
            reaction: ChatMessageReaction? = null,
            checkItemIsAtTop: Boolean
        ) {
            currentChatReactionPopUpViewPos = adapterPosition
            updateBackground()
            if (chatPopUpView?.isShowing == true)
                chatPopUpView?.dismiss()

            val locationOnScreen = v.getLocationOnScreen()
            var y = locationOnScreen.y - chatViewThemeAttribute.chatReactionY
            if (checkItemIsAtTop) {
                y = locationOnScreen.y + v.height + 30
            }
            chatPopUpView = ChatActionsPopupView(
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
                userReaction = reaction,
                emojiCountMap = message?.emojiCountMap,
                chatViewThemeAttributes = chatViewThemeAttribute,
                selectReactionListener = object : SelectReactionListener {
                    override fun onSelectReaction(reaction: Reaction?) {
                        if (currentChatReactionPopUpViewPos > -1 && currentChatReactionPopUpViewPos < itemCount) {
                            getItem(currentChatReactionPopUpViewPos)?.apply {
                                val reactionId: String?
                                val reactionAction: String
                                if (reaction == null) {
                                    reactionId = myChatMessageReaction?.emojiId
                                    myChatMessageReaction?.let { myChatMessageReaction ->
                                        emojiCountMap[myChatMessageReaction.emojiId] =
                                            (emojiCountMap[myChatMessageReaction.emojiId] ?: 0) - 1
                                        myChatMessageReaction.pubnubActionToken?.let { pubnubActionToken ->
                                            timetoken?.let {
                                                chatRepository?.removeMessageReaction(
                                                    channel,
                                                    it,
                                                    pubnubActionToken
                                                )
                                            }
                                        }
                                    }
                                    myChatMessageReaction = null
                                    reactionAction = "Removed"
                                } else {
                                    myChatMessageReaction?.let {
                                        emojiCountMap[it.emojiId] =
                                            (emojiCountMap[it.emojiId] ?: 0) - 1
                                        it.pubnubActionToken?.let { pubnubActionToken ->
                                            timetoken?.let {
                                                chatRepository?.removeMessageReaction(
                                                    channel,
                                                    it,
                                                    pubnubActionToken
                                                )
                                            }
                                        }
                                    }
                                    reactionId = reaction.id
                                    myChatMessageReaction = ChatMessageReaction(reaction.id)
                                    emojiCountMap[reaction.id] =
                                        (emojiCountMap[reaction.id] ?: 0) + 1
                                    timetoken?.let { pubnubMessageToken ->
                                        chatRepository?.addMessageReaction(
                                            channel,
                                            pubnubMessageToken,
                                            reaction.id
                                        )
                                    }
                                    reactionAction = "Added"
                                }
                                reactionId?.let {
                                    analyticsService.trackChatReactionSelected(
                                        id,
                                        it,
                                        reactionAction
                                    )
                                }
                                notifyItemChanged(currentChatReactionPopUpViewPos)
                            }
                        }
                    }
                },
                isPublichat = isPublicChat
            ).apply {
                animationStyle = when {
                    checkItemIsAtTop -> R.style.ChatReactionAnimationReverse
                    else -> R.style.ChatReactionAnimation
                }
                showAtLocation(
                    v,
                    Gravity.NO_GRAVITY,
                    locationOnScreen.x + chatViewThemeAttribute.chatReactionX,
                    y
                )
                message?.id?.let {
                    analyticsService.trackChatReactionPanelOpen(it)
                }
            }
        }

        private fun updateBackground() {
            // TODO: Need to check before functionality make it in more proper way
            v.apply {
                if (currentChatReactionPopUpViewPos > -1 && adapterPosition > -1 && currentChatReactionPopUpViewPos == adapterPosition) {
                    chatViewThemeAttribute.chatReactionMessageBubbleHighlightedBackground?.let { res ->
                        updateUI(v.chatBubbleBackground, res)
                    }
                    chatViewThemeAttribute.chatReactionMessageBackHighlightedBackground?.let { res ->
                        updateUI(v.chatBackground, res)
                    }
                } else {
                    chatViewThemeAttribute.chatBubbleBackgroundRes?.let { res ->
                        updateUI(v.chatBubbleBackground, res)
                    }
                    chatViewThemeAttribute.chatBackgroundRes?.let { res ->
                        updateUI(v.chatBackground, res)
                    }
                }
            }
        }

        private fun updateUI(view: View, res: Int) {
            if (res < 0) {
                view.setBackgroundColor(res)
            } else {
                val value = TypedValue()
                try {
                    v.context.resources.getValue(res, value, true)
                    when (value.type) {
                        TypedValue.TYPE_REFERENCE, TypedValue.TYPE_STRING -> view.setBackgroundResource(
                            res
                        )
                        TypedValue.TYPE_NULL -> view.setBackgroundColor(
                            Color.TRANSPARENT
                        )
                        else -> view.setBackgroundColor(Color.TRANSPARENT)
                    }
                } catch (e: Resources.NotFoundException) {
                    view.setBackgroundColor(res)
                }
            }
        }

        fun hideFloatingUI() {
            if (chatPopUpView?.isShowing == true)
                chatPopUpView?.dismiss()
            chatPopUpView = null
            if (currentChatReactionPopUpViewPos > -1) {
                notifyItemChanged(currentChatReactionPopUpViewPos)
            }
            currentChatReactionPopUpViewPos = -1
            updateBackground()
        }

        // HH:MM:SS eg 02:45:12
        private fun Long.toTimeString(): String =
            SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date().apply {
                time = this@toTimeString
            })

        @SuppressLint("SetTextI18n")
        private fun setMessage(
            message: ChatMessage?
        ) {
            v.apply {
                this@ViewHolder.message = message
                message?.apply {

                    // Cleanup the recycled item
                    chatMessage.text = ""
                    Glide.with(context).clear(chatMessage)

                    chatViewThemeAttribute.apply {
                        v.chatMessage.setTextColor(chatMessageColor)

                        if (message.isFromMe) {
                            chat_nickname.setTextColor(chatNickNameColor)
                            chat_nickname.text =
                                message.senderDisplayName
                        } else {
                            chat_nickname.setTextColor(chatOtherNickNameColor)
                            chat_nickname.text = message.senderDisplayName
                        }
                        if (chatViewThemeAttribute.showMessageDateTime) {
                            v.message_date_time.visibility = View.VISIBLE
                            if (EngagementSDK.enableDebug) {
                                val pdt = message.timeStamp?.toLong() ?: 0
                                val createdAt = message.getUnixTimeStamp()?.toTimeString()?:""
                                val syncedTime = pdt.toTimeString()

                                v.message_date_time.text =
                                    "Created :  $createdAt | Synced : $syncedTime "
                            } else {
                                v.message_date_time.text = messageTimeFormatter?.invoke(
                                    message.getUnixTimeStamp()
                                )
                            }
                        } else {
                            v.message_date_time.visibility = View.GONE
                        }
                
                        val topBorderLP = v.border_top.layoutParams
                        topBorderLP.height = chatMessageTopBorderHeight
                        v.border_top.layoutParams = topBorderLP

                        val bottomBorderLP = v.border_bottom.layoutParams
                        bottomBorderLP.height = chatMessageBottomBorderHeight
                        v.border_bottom.layoutParams = bottomBorderLP

                        v.border_top.setBackgroundColor(chatMessageTopBorderColor)
                        v.border_bottom.setBackgroundColor(chatMessageBottomBorderColor)

                        val layoutParam =
                            v.chatBackground.layoutParams as ConstraintLayout.LayoutParams
                        layoutParam.setMargins(
                            chatMarginLeft,
                            chatMarginTop + AndroidResource.dpToPx(6),
                            chatMarginRight,
                            chatMarginBottom
                        )
                        layoutParam.width = chatBackgroundWidth
                        v.chatBackground.layoutParams = layoutParam
                        v.chatBubbleBackground.setPadding(
                            chatBubblePaddingLeft,
                            chatBubblePaddingTop,
                            chatBubblePaddingRight,
                            chatBubblePaddingBottom
                        )
                        val layoutParam1: LinearLayout.LayoutParams =
                            v.chatBubbleBackground.layoutParams as LinearLayout.LayoutParams
                        layoutParam1.setMargins(
                            chatBubbleMarginLeft,
                            chatBubbleMarginTop,
                            chatBubbleMarginRight,
                            chatBubbleMarginBottom
                        )
                        layoutParam1.width = chatBubbleWidth
                        v.chatBubbleBackground.layoutParams = layoutParam1

                        v.img_chat_avatar.visibility =
                            when (showChatAvatarLogo) {
                                true -> View.VISIBLE
                                else -> View.GONE
                            }
                        val layoutParamAvatar = LinearLayout.LayoutParams(
                            chatAvatarWidth,
                            chatAvatarHeight
                        )
                        layoutParamAvatar.setMargins(
                            chatAvatarMarginLeft,
                            chatAvatarMarginTop,
                            chatAvatarMarginRight,
                            chatAvatarMarginBottom
                        )
                        layoutParamAvatar.gravity = chatAvatarGravity
                        v.img_chat_avatar.layoutParams = layoutParamAvatar

                        val options = RequestOptions()
                        if (chatAvatarCircle) {
                            options.optionalCircleCrop()
                        }
                        if (chatAvatarRadius > 0) {
                            options.transform(
                                CenterCrop(),
                                RoundedCorners(chatAvatarRadius)
                            )
                        }
                        message.senderDisplayPic.let {
                            if (!it.isNullOrEmpty())
                                Glide.with(context).load(it)
                                    .apply(options)
                                    .placeholder(chatUserPicDrawable)
                                    .into(img_chat_avatar)
                            else
                                img_chat_avatar.setImageDrawable(chatUserPicDrawable)
                        }

                        val spaceRemover = Pattern.compile("[\\s]")
                        val inputNoString = spaceRemover.matcher(message.message)
                            .replaceAll(Matcher.quoteReplacement(""))
                        val isOnlyStickers =
                            inputNoString.findIsOnlyStickers().matches() || message.message.findImages().matches()
                        val atLeastOneSticker =
                            inputNoString.findStickers().find() || message.message.findImages().matches()
                        val numberOfStickers = message.message.findStickers().countMatches()
                        val isExternalImage = message.message.findImages().matches()

                        chatMessage.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                        callback.addView(chatMessage)
                        chatMessage.contentDescription = if (isExternalImage) {
                            context.getString(R.string.image)
                        } else {
                            message.message
                        }
                        when {
                            isExternalImage -> {
                                val s = SpannableString(message.message)
                                replaceWithImages(s, context, callback, false, message.id, message.image_width ?: largerStickerSize, message.image_height ?: largerStickerSize) {
                                    // TODO this might write to the wrong messageView on slow connection.
                                    chatMessage.text = s
                                }
                            }
                            (isOnlyStickers && numberOfStickers < 2) -> {
                                val s = SpannableString(message.message)
                                replaceWithStickers(s, context, stickerPackRepository, null, callback, largerStickerSize) {
                                    // TODO this might write to the wrong messageView on slow connection.
                                    chatMessage.text = s
                                }
                            }
                            atLeastOneSticker -> {
                                val s = SpannableString(message.message)
                                replaceWithStickers(s, context, stickerPackRepository, null, callback, mediumStickerSize) {
                                    // TODO this might write to the wrong messageView on slow connection.
                                    chatMessage.text = s
                                }
                            }
                            else -> {
                                clearTarget(message.id, context)
                                chatMessage.text = message.message
                            }
                        }

                        var imageView: ImageView
                        val size =
                            context.resources.getDimensionPixelSize(R.dimen.livelike_chat_reaction_display_size)
                        rel_reactions_lay.removeAllViews()

                        // TODO need to check for updating list and work on remove the reaction with animation
                        emojiCountMap.keys.filter { return@filter (emojiCountMap[it] ?: 0) > 0 }
                            .forEachIndexed { index, reactionId ->
                                if ((emojiCountMap[reactionId] ?: 0) > 0) {
                                    imageView = ImageView(context)
                                    val reaction = chatReactionRepository.getReaction(reactionId)
                                    reaction?.let { reaction ->
                                        imageView.contentDescription = reaction.name
                                        imageView.loadImage(reaction.file, size)
                                        val paramsImage: FrameLayout.LayoutParams =
                                            FrameLayout.LayoutParams(size, size)
                                        paramsImage.gravity = Gravity.LEFT
                                        val left = ((size / 1.2) * (index)).toInt()
                                        paramsImage.setMargins(left, 0, 0, 0)
                                        rel_reactions_lay.addView(imageView, paramsImage)
                                        imageView.bringToFront()
                                        imageView.invalidate()

                                        myChatMessageReaction?.let {
                                            if (it.emojiId == reaction.id) {
                                                imageView.startAnimation(bounceAnimation)
                                            }
                                        }
                                    }
                                }
                            }
                        txt_chat_reactions_count.setTextColor(chatReactionDisplayCountColor)
                        val sumCount = emojiCountMap.values.sum()
                        val isReactionsAvaiable =
                            (chatReactionRepository.reactionList?.size ?: 0) > 0

                        if (emojiCountMap.isNotEmpty() && sumCount > 0 && isReactionsAvaiable) {
                            txt_chat_reactions_count.visibility = View.VISIBLE
                            txt_chat_reactions_count.text = "$sumCount"
                        } else if (isReactionsAvaiable) {
                            txt_chat_reactions_count.visibility = View.INVISIBLE
                            txt_chat_reactions_count.text = "  "
                            if (chatViewThemeAttribute.chatReactionHintEnable) {
                                val imageView = ImageView(context)
                                imageView.contentDescription =
                                    context.getString(R.string.you_can_add_reaction_hint)
                                imageView.setImageResource(chatViewThemeAttribute.chatReactionHintIcon)
                                val params: FrameLayout.LayoutParams =
                                    FrameLayout.LayoutParams(size, size)
                                rel_reactions_lay.addView(imageView, params)
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val largerStickerSize = 100
private const val mediumStickerSize = 50
