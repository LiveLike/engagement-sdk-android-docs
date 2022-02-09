package com.livelike.engagementsdk.chat

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.text.*
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Patterns
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
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
import com.livelike.engagementsdk.chat.data.remote.PubnubChatEventType
import com.livelike.engagementsdk.chat.data.repository.ChatRepository
import com.livelike.engagementsdk.chat.stickerKeyboard.*
import com.livelike.engagementsdk.core.utils.AndroidResource
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.core.utils.logError
import com.livelike.engagementsdk.publicapis.ChatMessageType
import com.livelike.engagementsdk.publicapis.toChatMessageType
import com.livelike.engagementsdk.publicapis.toLiveLikeChatMessage
import com.livelike.engagementsdk.widget.view.loadImage
import kotlinx.android.synthetic.main.default_chat_cell.view.*
import pl.droidsonroids.gif.MultiCallback
import java.text.SimpleDateFormat
import java.util.*
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
    internal var analyticsService: AnalyticsService,
    private val reporter: (ChatMessage) -> Unit,
    private val blockProfile: (String) -> Unit
) : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(diffChatMessage) {
    var isKeyboardOpen: Boolean = false
    internal var chatRoomId: String? = null
    internal var chatRoomName: String? = null
    private var lastFloatingUiAnchorView: View? = null
    var chatRepository: ChatRepository? = null
    lateinit var stickerPackRepository: StickerPackRepository
    var chatReactionRepository: ChatReactionRepository? = null
    internal var showLinks = false
    internal var linksRegex = Patterns.WEB_URL.toRegex()
    var checkListIsAtTop: (((position: Int) -> Boolean)?) = null

    lateinit var chatViewThemeAttribute: ChatViewThemeAttributes

    internal var isPublicChat: Boolean = true
    internal var mRecyclerView: RecyclerView? = null
    internal var messageTimeFormatter: ((time: Long?) -> String)? = null
    var currentChatReactionPopUpViewPos: Int = -1
    internal var chatPopUpView: ChatActionsPopupView? = null
    var showChatAvatarLogo = true
    var chatViewDelegate: ChatViewDelegate? = null
    var enableMessageReply: Boolean = false

    override fun onCreateViewHolder(root: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (chatViewDelegate != null) {
            val messageType = ChatMessageType.values().find { it.ordinal == viewType }
            if (messageType == ChatMessageType.CUSTOM_MESSAGE_CREATED) {
                return chatViewDelegate!!.onCreateViewHolder(root, messageType)
            }
        }
        return ViewHolder(
            LayoutInflater.from(root.context).inflate(
                R.layout.default_chat_cell,
                root,
                false
            )
        )
    }

    override fun getItemViewType(position: Int): Int {
        return if (chatViewDelegate != null)
            getItem(position).messageEvent.toChatMessageType()?.ordinal ?: -1
        else
            super.getItemViewType(position)
    }

    fun isReactionPopUpShowing(): Boolean {
        return chatPopUpView?.isShowing ?: false
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val chatMessage = getItem(position)
        if (chatViewDelegate != null && chatMessage.messageEvent == PubnubChatEventType.CUSTOM_MESSAGE_CREATED) {
            chatViewDelegate!!.onBindViewHolder(
                holder,
                chatMessage.toLiveLikeChatMessage(),
                chatViewThemeAttribute,
                showChatAvatarLogo
            )
        } else if (holder is ViewHolder) {
            holder.bindTo(chatMessage)
        }
        holder.itemView.requestLayout()
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        mRecyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        mRecyclerView = null
    }

    fun getChatMessage(position: Int): ChatMessage = getItem(position)

    /** Commenting this code for now so QA finalize whether old issues are coming or not
    Flowing code helps in accessbility related issues
     **/
//    override fun onViewDetachedFromWindow(holder: ViewHolder) {
//        holder.hideFloatingUI()
//        super.onViewDetachedFromWindow(holder)
//    }

    inner class ViewHolder(val v: View) :
        RecyclerView.ViewHolder(v),
        View.OnLongClickListener,
        View.OnClickListener {
        private var message: ChatMessage? = null
        private val bounceAnimation: Animation =
            AnimationUtils.loadAnimation(v.context, R.anim.bounce_animation)

        @SuppressLint("StringFormatInvalid")
        private val dialogOptions = listOf(
            v.context.getString(R.string.flag_ui_blocking_title) to { msg: ChatMessage ->
                AlertDialog.Builder(v.context).apply {
                    setMessage(
                        context.getString(
                            R.string.flag_ui_blocking_message,
                            msg.senderDisplayName
                        )
                    )
                    setPositiveButton(context.getString(R.string.livelike_chat_alert_blocked_confirm)) { _, _ ->
                        analyticsService.trackBlockingUser()
                        blockProfile(msg.senderId)
                        reporter(msg)
                    }
                    create()
                }.show()
            },
            v.context.getString(R.string.flag_ui_reporting_title) to { msg: ChatMessage ->
                AlertDialog.Builder(v.context).apply {
                    setMessage(context.getString(R.string.flag_ui_reporting_message))
                    setPositiveButton(context.getString(R.string.livelike_chat_report_message_confirm)) { _, _ ->
                        analyticsService.trackReportingMessage()
                        reporter(msg)
                    }
                    create()
                }.show()
            }
        )

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
            val isDeletedMessage = (view?.tag as ChatMessage?)?.isDeleted ?: false
            val reactionsAvailable = (chatReactionRepository?.reactionList?.size ?: 0) > 0
            if ((reactionsAvailable || !isOwnMessage) && !isDeletedMessage) {
                showFloatingUI(
                    isOwnMessage,
                    message?.myChatMessageReaction,
                    (checkListIsAtTop?.invoke(adapterPosition) ?: false) && itemCount > 1
                )
            }
        }

        override fun onClick(view: View?) {
            if (chatPopUpView?.isShowing == true) {
                hideFloatingUI()
            } else {
                if (!isKeyboardOpen)
                    wouldShowFloatingUi(view)
                else
                    isKeyboardOpen = false
            }
        }

        fun bindTo(item: ChatMessage?) {
            v.tag = item

            setMessage(item)
            updateBackground()
            if (item?.timetoken != 0L) {
                v.postDelayed(
                    {
                        v.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
                    },
                    100
                )
            }
            if (currentChatReactionPopUpViewPos > -1 && currentChatReactionPopUpViewPos == adapterPosition) {
                if (chatPopUpView?.isShowing == true) {
                    chatPopUpView?.updatePopView(
                        message?.emojiCountMap,
                        message?.myChatMessageReaction
                    )
                }
            }
            item?.let {
                analyticsService.trackMessageDisplayed(item.id, item.message)
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

            var y = chatViewThemeAttribute.chatReactionY
            if (chatViewThemeAttribute.chatReactionPanelGravity == Gravity.TOP ||
                chatViewThemeAttribute.chatReactionPanelGravity == Gravity.TOP or Gravity.RIGHT ||
                chatViewThemeAttribute.chatReactionPanelGravity == Gravity.TOP or Gravity.LEFT ||
                chatViewThemeAttribute.chatReactionPanelGravity == Gravity.TOP or Gravity.CENTER ||
                chatViewThemeAttribute.chatReactionPanelGravity == Gravity.TOP or Gravity.START ||
                chatViewThemeAttribute.chatReactionPanelGravity == Gravity.TOP or Gravity.END
            ) {
                y -= v.height
                if (checkItemIsAtTop) {
                    y += v.height
                }
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
                                val isRemoved: Boolean
                                if (reaction == null) {
                                    reactionId = myChatMessageReaction?.emojiId
                                    myChatMessageReaction?.let { myChatMessageReaction ->
                                        emojiCountMap[myChatMessageReaction.emojiId] =
                                            (emojiCountMap[myChatMessageReaction.emojiId] ?: 0) - 1
                                        myChatMessageReaction.pubnubActionToken?.let { pubnubActionToken ->
                                            timetoken.let {
                                                chatRepository?.removeMessageReaction(
                                                    channel,
                                                    it,
                                                    pubnubActionToken
                                                )
                                            }
                                        }
                                    }
                                    myChatMessageReaction = null
                                    isRemoved = true
                                } else {
                                    myChatMessageReaction?.let {
                                        emojiCountMap[it.emojiId] =
                                            (emojiCountMap[it.emojiId] ?: 0) - 1
                                        it.pubnubActionToken?.let { pubnubActionToken ->
                                            timetoken.let { it1 ->
                                                chatRepository?.removeMessageReaction(
                                                    channel,
                                                    it1,
                                                    pubnubActionToken
                                                )
                                            }
                                        }
                                    }
                                    reactionId = reaction.id
                                    myChatMessageReaction = ChatMessageReaction(reaction.id)
                                    emojiCountMap[reaction.id] =
                                        (emojiCountMap[reaction.id] ?: 0) + 1
                                    timetoken.let { pubnubMessageToken ->
                                        chatRepository?.addMessageReaction(
                                            channel,
                                            pubnubMessageToken,
                                            reaction.id
                                        )
                                    }
                                    isRemoved = false
                                }
                                reactionId?.let {
                                    chatRoomId?.let {
                                        analyticsService.trackChatReactionSelected(
                                            it,
                                            id,
                                            reactionId,
                                            isRemoved
                                        )
                                    }
                                }
                                // removing unecessary call
                                // notifyItemChanged(currentChatReactionPopUpViewPos)
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
                // I had to specify the width and height in order to be shown on that version (which is still compatible with the rest of the versions as well).
                width = ViewGroup.LayoutParams.WRAP_CONTENT
                height = ViewGroup.LayoutParams.WRAP_CONTENT

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    showAsDropDown(
                        v,
                        chatViewThemeAttribute.chatReactionX,
                        y,
                        chatViewThemeAttribute.chatReactionPanelGravity
                    )
                } else {
                    showAsDropDown(v, chatViewThemeAttribute.chatReactionX, y)
                }
                message?.id?.let {
                    analyticsService.trackChatReactionPanelOpen(it)
                }
            }
        }

        private fun updateBackground() {
            // TODO: Need to check before functionality make it in more proper way
            v.apply {
                if (currentChatReactionPopUpViewPos > -1 && adapterPosition > -1 && currentChatReactionPopUpViewPos == adapterPosition) {
                    chatViewThemeAttribute.apply {
                        updateUI(v.chatBubbleBackground,
                            chatReactionMessageBubbleHighlightedBackground
                        )
                        chatReactionMessageBackHighlightedBackground?.let { res ->
                            updateUI(v.chatBackground, res)
                        }
                        updateUI(v.lay_parent_message,
                            parentChatReactionMessageBackHighlightedBackground
                        )
                    }
                } else {
                    chatViewThemeAttribute.apply {
                        updateUI(v.chatBubbleBackground, chatBubbleBackgroundRes)
                        chatBackgroundRes?.let { res ->
                            updateUI(v.chatBackground, res)
                        }
                        updateUI(v.lay_parent_message, parentChatBackgroundRes)
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

        internal fun hideFloatingUI() {
            if (chatPopUpView?.isShowing == true)
                chatPopUpView?.dismiss()
            chatPopUpView = null
            lastFloatingUiAnchorView = null
            logDebug { "Computing:${mRecyclerView?.isComputingLayout} ,ScrollState: ${mRecyclerView?.scrollState} ,pos:$currentChatReactionPopUpViewPos ,adapt Pos:$adapterPosition" }
            if (mRecyclerView?.isComputingLayout == false) {
                // Add check for checking computing and check with current adapter position
                if (currentChatReactionPopUpViewPos > -1 && currentChatReactionPopUpViewPos == adapterPosition) {
                    try {
                        notifyItemChanged(currentChatReactionPopUpViewPos)
                    } catch (e: IllegalStateException) {
                        e.printStackTrace()
                        logError { e.message }
                    }
                }
                currentChatReactionPopUpViewPos = -1
                updateBackground()
            }
        }

        // HH:MM:SS eg 02:45:12
        private fun Long.toTimeString(): String =
            when (this) {
                0L -> ""
                else -> SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(
                    Date().apply {
                        time = this@toTimeString
                    }
                )
            }

        private fun setCustomFontWithTextStyle(
            textView: TextView,
            fontPath: String?,
            textStyle: Int
        ) {
            if (fontPath != null) {
                try {
                    val typeFace =
                        Typeface.createFromAsset(
                            textView.context.assets,
                            fontPath
                        )
                    textView.setTypeface(typeFace, textStyle)
                } catch (e: Exception) {
                    e.printStackTrace()
                    textView.setTypeface(null, textStyle)
                }
            } else {
                textView.setTypeface(null, textStyle)
            }
        }

        private fun setLetterSpacingForTextView(textView: TextView, letterSpacing: Float) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                textView.letterSpacing = letterSpacing
            }
        }

        private fun getTextWithCustomLinks(spannableString: SpannableString): SpannableString {
            val result = linksRegex.toPattern().matcher(spannableString)
            while (result.find()) {
                val start = result.start()
                val end = result.end()
                spannableString.setSpan(
                    InternalURLSpan(
                        spannableString.subSequence(start, end).toString(),
                        message?.id,
                        chatRoomId,
                        chatRoomName,
                        analyticsService
                    ),
                    start,
                    end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            return spannableString
        }

        /**
         * Creating this function to get line count of string assuming the width as some value
         * it is estimated not exact value
         */
        private fun String.getLinesCount(textSize: Float): Int {
            val density = v.context.resources.displayMetrics.density
            val paint = TextPaint()
            paint.textSize = textSize * density
            // Using static width for now ,can be replace with dynamic for later
            val width = (AndroidResource.dpToPx(300) * density).toInt()
            val alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL
            val layout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                StaticLayout.Builder.obtain(this, 0, this.length, paint, width)
                    .setAlignment(alignment)
                    .setLineSpacing(0F, 1F)
                    .setIncludePad(false)
                    .build()
            } else {
                @Suppress("DEPRECATION") //suppressed as needed to support pre M
                StaticLayout(this, paint, width, alignment, 1F, 0F, false)
            }
            return layout.lineCount
        }

        private fun String.withoutStickers(): String {
            var result = this
            this.findStickerCodes().allMatches().forEach {
                result = result.replace(it, "")
            }
            return result
        }

        @SuppressLint("SetTextI18n")
        private fun setMessage(
            message: ChatMessage?
        ) {
            v.apply {
                this@ViewHolder.message = message
                message?.apply {

                    // Cleanup the recycled item
                    chatMessage.text = ""
                    Glide.with(context.applicationContext).clear(chatMessage)

                    chatViewThemeAttribute.apply {
                        v.chatMessage.setTextColor(chatMessageColor)
                        v.parent_chatMessage.setTextColor(parentChatMessageColor)
                        if (message.isFromMe) {
                            chat_nickname.setTextColor(chatNickNameColor)
                            chat_nickname.text =
                                message.senderDisplayName
                        } else {
                            chat_nickname.setTextColor(chatOtherNickNameColor)
                            chat_nickname.text = message.senderDisplayName
                        }
                        chat_parent_nickname.setTextColor(parentChatNickNameColor)
                        chat_nickname.setTextSize(TypedValue.COMPLEX_UNIT_PX, chatUserNameTextSize)
                        chat_parent_nickname.setTextSize(
                            TypedValue.COMPLEX_UNIT_PX,
                            parentChatUserNameTextSize
                        )
                        chat_nickname.isAllCaps = chatUserNameTextAllCaps
                        setCustomFontWithTextStyle(
                            chat_nickname,
                            chatUserNameCustomFontPath,
                            chatUserNameTextStyle
                        )
                        setCustomFontWithTextStyle(
                            chat_parent_nickname,
                            parentChatUserNameCustomFontPath,
                            parentChatUserNameTextStyle
                        )
                        setLetterSpacingForTextView(chat_nickname, chatUserNameTextLetterSpacing)
                        setLetterSpacingForTextView(
                            chat_parent_nickname,
                            parentChatUserNameTextLetterSpacing
                        )
                        chatMessage.setTextSize(TypedValue.COMPLEX_UNIT_PX, chatMessageTextSize)
                        parent_chatMessage.setTextSize(
                            TypedValue.COMPLEX_UNIT_PX,
                            parentChatMessageTextSize
                        )
                        if (showLinks) {
                            chatMessage.apply {
                                linksClickable = showLinks
                                setLinkTextColor(chatMessageLinkTextColor)
                                movementMethod = LinkMovementMethod.getInstance()
                            }
                            parent_chatMessage.apply {
                                linksClickable = showLinks
                                setLinkTextColor(parentChatMessageLinkTextColor)
                                movementMethod = LinkMovementMethod.getInstance()
                            }
                        }
                        setCustomFontWithTextStyle(
                            chatMessage,
                            chatMessageCustomFontPath,
                            when (isDeleted) {
                                true -> Typeface.ITALIC
                                else -> chatMessageTextStyle
                            }
                        )
                        setCustomFontWithTextStyle(
                            parent_chatMessage,
                            parentChatMessageCustomFontPath,
                            when (isDeleted) {
                                true -> Typeface.ITALIC
                                else -> parentChatMessageTextStyle
                            }
                        )
                        setLetterSpacingForTextView(chatMessage, chatMessageTextLetterSpacing)
                        setLetterSpacingForTextView(
                            parent_chatMessage,
                            parentChatMessageTextLetterSpacing
                        )
                        if (chatViewThemeAttribute.showMessageDateTime) {
                            v.message_date_time.visibility = View.VISIBLE
                            if (EngagementSDK.enableDebug) {
                                val pdt = message.timeStamp?.toLong() ?: 0
                                val createdAt = message.getUnixTimeStamp()?.toTimeString() ?: ""
                                val syncedTime = pdt.toTimeString()

                                v.message_date_time.text =
                                    "Created :  $createdAt | Synced : $syncedTime "
                            } else {
                                v.message_date_time.text = messageTimeFormatter?.invoke(
                                    message.getUnixTimeStamp()
                                )
                            }
                            v.message_date_time.setTextSize(
                                TypedValue.COMPLEX_UNIT_PX,
                                chatMessageTimeTextSize
                            )
                            setCustomFontWithTextStyle(
                                v.message_date_time,
                                chatMessageTimeCustomFontPath,
                                chatMessageTimeTextStyle
                            )
                            v.message_date_time.isAllCaps = chatMessageTimeTextAllCaps
                            v.message_date_time.setTextColor(chatMessageTimeTextColor)
                            setLetterSpacingForTextView(
                                v.message_date_time,
                                chatMessageTimeTextLetterSpacing
                            )
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
                        v.lay_parent_message.setPadding(
                            parentChatBubblePaddingLeft,
                            parentChatBubblePaddingTop,
                            parentChatBubblePaddingRight,
                            parentChatBubblePaddingBottom
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
                            when (this@ChatRecyclerAdapter.showChatAvatarLogo) {
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

                        var options = RequestOptions()
                        if (chatAvatarCircle) {
                            options = options.optionalCircleCrop()
                        }
                        if (chatAvatarRadius > 0) {
                            options = options.transform(
                                CenterCrop(),
                                RoundedCorners(chatAvatarRadius)
                            )
                        }

                        // load local image with glide, so that (chatAvatarCircle and chatAvatarRadius) properties can be applied.
                        // more details on https://livelike.atlassian.net/browse/ES-1790
                        // replace context with applicationContext related to ES-2185
                        if (message.senderDisplayPic.isNullOrEmpty()) {
                            // load local image
                            Glide.with(context.applicationContext)
                                .load(R.drawable.default_avatar)
                                .apply(options)
                                .placeholder(chatUserPicDrawable)
                                .into(img_chat_avatar)
                        } else {
                            Glide.with(context.applicationContext).load(message.senderDisplayPic)
                                .apply(options)
                                .placeholder(chatUserPicDrawable)
                                .error(chatUserPicDrawable)
                                .into(img_chat_avatar)
                        }

                        setTextOrImageToView(
                            message,
                            chatMessage,
                            img_chat_message,
                            false,
                            chatMessageTextSize
                        )

                        var imageView: ImageView
                        rel_reactions_lay.removeAllViews()

                        val chatReactionParam =
                            rel_reactions_lay.layoutParams as ConstraintLayout.LayoutParams
                        chatReactionParam.leftMargin = chatReactionIconsMarginLeft
                        chatReactionParam.rightMargin = chatReactionIconsMarginRight
                        chatReactionParam.topMargin = chatReactionIconsMarginTop
                        chatReactionParam.bottomMargin = chatReactionIconsMarginBottom
                        chatReactionParam.height = chatReactionDisplaySize
                        if (chatReactionIconsPositionAtBottom) {
                            chatReactionParam.topToTop = ConstraintLayout.LayoutParams.UNSET
                            chatReactionParam.bottomToBottom = chat_constraint_box.id
                        }
                        rel_reactions_lay.layoutParams = chatReactionParam

                        val chatReactionCountParam =
                            txt_chat_reactions_count.layoutParams as ConstraintLayout.LayoutParams
                        chatReactionCountParam.leftMargin = chatReactionCountMarginLeft
                        chatReactionCountParam.rightMargin = chatReactionCountMarginRight
                        chatReactionCountParam.topMargin = chatReactionCountMarginTop
                        chatReactionCountParam.bottomMargin = chatReactionCountMarginBottom
                        if (chatReactionCountPositionAtBottom) {
                            chatReactionCountParam.topToTop = ConstraintLayout.LayoutParams.UNSET
                            chatReactionCountParam.bottomToBottom = chat_constraint_box.id
                        }
                        txt_chat_reactions_count.layoutParams = chatReactionCountParam

                        setCustomFontWithTextStyle(
                            txt_chat_reactions_count,
                            chatReactionDisplayCountCustomFontPath,
                            chatReactionDisplayCountTextStyle
                        )
                        txt_chat_reactions_count.setTextSize(
                            TypedValue.COMPLEX_UNIT_PX,
                            chatReactionDisplayCountTextSize
                        )
                        // TODO need to check for updating list and work on remove the reaction with animation
                        emojiCountMap.keys.filter { return@filter (emojiCountMap[it] ?: 0) > 0 }
                            .forEachIndexed { index, reactionId ->
                                if ((emojiCountMap[reactionId] ?: 0) > 0) {
                                    imageView = ImageView(context)
                                    val reaction = chatReactionRepository?.getReaction(reactionId)
                                    reaction?.let {
                                        imageView.contentDescription = reaction.name
                                        imageView.loadImage(reaction.file, chatReactionDisplaySize)
                                        val paramsImage: FrameLayout.LayoutParams =
                                            FrameLayout.LayoutParams(
                                                chatReactionDisplaySize,
                                                chatReactionDisplaySize
                                            )
                                        paramsImage.gravity = Gravity.LEFT
                                        val left =
                                            ((chatReactionDisplaySize / chatReactionIconsFactor) * (index)).toInt()
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
                            (chatReactionRepository?.reactionList?.size ?: 0) > 0

                        if (chatViewThemeAttribute.chatReactionHintEnable && sumCount == 0) {
                            val innerImageView = ImageView(context)
                            innerImageView.contentDescription =
                                context.getString(R.string.you_can_add_reaction_hint)
                            innerImageView.setImageResource(chatViewThemeAttribute.chatReactionHintIcon)
                            val params: FrameLayout.LayoutParams =
                                FrameLayout.LayoutParams(
                                    chatReactionDisplaySize,
                                    chatReactionDisplaySize
                                )
                            rel_reactions_lay.addView(innerImageView, params)
                        }

                        if (emojiCountMap.isNotEmpty() && sumCount > 0 && isReactionsAvaiable) {
                            txt_chat_reactions_count.visibility = View.VISIBLE
                            txt_chat_reactions_count.text = "$sumCount"
                        } else {
                            txt_chat_reactions_count.visibility = View.INVISIBLE
                            txt_chat_reactions_count.text = "  "
                        }
                    }

                    lay_parent_message.visibility = when (parentMessage != null && enableMessageReply && !isDeleted) {
                        true -> View.VISIBLE
                        else -> View.GONE
                    }
                    parentMessage?.let {
                        setTextOrImageToView(
                            it,
                            parent_chatMessage,
                            img_parent_chat_message,
                            true,
                            chatViewThemeAttribute.parentChatMessageTextSize
                        )
                        chat_parent_nickname.text = it.senderDisplayName
                    }
                }
            }
        }

        private fun setTextOrImageToView(
            chatMessage: ChatMessage?,
            textView: TextView,
            imageView: ImageView,
            parent: Boolean = false,
            textSize: Float
        ) {
            val callback = MultiCallback(true)
            chatMessage?.apply {
                val tag = when (parent) {
                    true -> "parent_$id"
                    else -> id
                }
                textView.tag = tag
                val spaceRemover = Pattern.compile("[\\s]")
                val inputNoString = spaceRemover.matcher(message ?: "")
                    .replaceAll(Matcher.quoteReplacement(""))
                val isOnlyStickers =
                    inputNoString.findIsOnlyStickers()
                        .matches() || message?.findImages()?.matches() == true
                val atLeastOneSticker =
                    inputNoString.findStickers().find() || message?.findImages()
                        ?.matches() == true
                val numberOfStickers = message?.findStickers()?.countMatches() ?: 0
                val isExternalImage = message?.findImages()?.matches() ?: false

                textView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                callback.addView(textView)
                textView.contentDescription = if (isExternalImage) {
                    textView.context.getString(R.string.image)
                } else {
                    message
                }
                when {
                    !isDeleted && isExternalImage -> {
                        imageView.contentDescription = if (isExternalImage) {
                            textView.context.getString(R.string.image)
                        } else {
                            message
                        }
                        textView.minHeight = 0
                        textView.text = ""
                        textView.visibility = View.GONE
                        imageView.visibility = View.VISIBLE
                        imageView.minimumHeight =
                            AndroidResource.dpToPx(LARGER_STICKER_SIZE)
                        Glide.with(imageView.context)
                            .load(imageUrl)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .apply(
                                RequestOptions().override(
                                    image_width ?: LARGER_STICKER_SIZE,
                                    image_height ?: LARGER_STICKER_SIZE
                                )
                            )
                            .into(imageView)
                    }
                    !isDeleted && (isOnlyStickers && numberOfStickers < 2) -> {
                        textView.visibility = View.VISIBLE
                        imageView.visibility = View.GONE
                        textView.minHeight =
                            AndroidResource.dpToPx(MEDIUM_STICKER_SIZE)
                        val s = SpannableString(message)
                        replaceWithStickers(
                            s,
                            textView.context.applicationContext,
                            stickerPackRepository,
                            null,
                            callback,
                            MEDIUM_STICKER_SIZE
                        ) {
                            // TODO this might write to the wrong messageView on slow connection.
                            if (textView.tag == tag) {
                                textView.text = when (showLinks) {
                                    true -> getTextWithCustomLinks(s)
                                    else -> s
                                }
                            }
                        }
                    }
                    !isDeleted && atLeastOneSticker -> {
                        textView.visibility = View.VISIBLE
                        imageView.visibility = View.GONE
                        var columnCount = numberOfStickers / 8
                        val lines = message?.withoutStickers()?.getLinesCount(textSize) ?: 0
                        if (columnCount == 0) {
                            columnCount = 1
                        }
                        textView.minHeight =
                            (textSize.toInt() * columnCount) + when {
                                lines != columnCount -> (lines * textSize.toInt())
                                else -> 0
                            }
                        val s = SpannableString(message)
                        replaceWithStickers(
                            s,
                            textView.context.applicationContext,
                            stickerPackRepository,
                            null,
                            callback,
                            SMALL_STICKER_SIZE
                        ) {
                            // TODO this might write to the wrong messageView on slow connection.
                            if (textView.tag == tag) {
                                textView.text = when (showLinks) {
                                    true -> getTextWithCustomLinks(s)
                                    else -> s
                                }
                            }
                        }
                    }
                    else -> {
                        imageView.visibility = View.GONE
                        textView.visibility = View.VISIBLE
                        clearTarget(id, textView.context)
                        textView.minHeight = textSize.toInt()
                        textView.text = when (showLinks) {
                            true -> getTextWithCustomLinks(SpannableString(message))
                            else -> message
                        }
                    }
                }
            }
        }
    }
}

class InternalURLSpan(
    private var clickedSpan: String,
    private val messageId: String?,
    private val chatRoomId: String?,
    private val chatRoomName: String?,
    private val analyticsService: AnalyticsService
) : ClickableSpan() {
    override fun onClick(textView: View) {
        val i = Intent(Intent.ACTION_VIEW)
        if (!clickedSpan.startsWith("http://") && !clickedSpan.startsWith("https://"))
            clickedSpan = "http://$clickedSpan"
        i.data = Uri.parse(clickedSpan)
        try {
            textView.context.startActivity(i)
        } catch (e: ActivityNotFoundException) {
            logError { e.message }
        }
        chatRoomId?.let {
            analyticsService.trackMessageLinkClicked(
                chatRoomId,
                chatRoomName,
                messageId,
                clickedSpan
            )
        }
    }
}

// const val should be in uppercase always
private const val LARGER_STICKER_SIZE = 100
private const val MEDIUM_STICKER_SIZE = 50
private const val SMALL_STICKER_SIZE = 28
