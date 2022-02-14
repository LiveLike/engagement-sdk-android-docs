package com.livelike.engagementsdk.chat.utils

import android.os.Build
import android.text.*
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.livelike.engagementsdk.AnalyticsService
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.chat.ChatMessage
import com.livelike.engagementsdk.chat.InternalURLSpan
import com.livelike.engagementsdk.chat.stickerKeyboard.*
import com.livelike.engagementsdk.core.utils.AndroidResource
import com.livelike.engagementsdk.core.utils.logDebug
import pl.droidsonroids.gif.MultiCallback
import java.util.regex.Matcher
import java.util.regex.Pattern


// const val should be in uppercase always
private const val LARGER_STICKER_SIZE = 100
private const val MEDIUM_STICKER_SIZE = 50
private const val SMALL_STICKER_SIZE = 28

internal fun setTextOrImageToView(
    chatMessage: ChatMessage?,
    textView: TextView,
    imageView: ImageView,
    parent: Boolean = false,
    textSize: Float,
    stickerPackRepository: StickerPackRepository,
    showLinks: Boolean,
    density: Float,
    linksRegex: Regex,
    chatRoomId: String?,
    chatRoomName: String?,
    analyticsService: AnalyticsService,
    input: Boolean
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
        val linkText = getTextWithCustomLinks(
            linksRegex,
            SpannableString(message),
            chatMessage.id,
            chatRoomId,
            chatRoomName,
            analyticsService
        )
        logDebug { "Stickers: $numberOfStickers,isExternalImage:$isExternalImage,atLeastOneSticker:$atLeastOneSticker,isOnlySticker:$isOnlyStickers,linkText:$linkText" }

        textView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        callback.addView(textView)
        textView.contentDescription = if (isExternalImage) {
            textView.context.getString(R.string.image)
        } else {
            message
        }
        when {
            !isDeleted && isExternalImage && !isBlocked -> {
                imageView.contentDescription = if (isExternalImage) {
                    textView.context.getString(R.string.image)
                } else {
                    message
                }
                textView.minHeight = 0
                textView.text = ""
                textView.visibility = View.GONE
                imageView.visibility = View.VISIBLE
                val size = when (parent) {
                    true -> MEDIUM_STICKER_SIZE
                    else -> LARGER_STICKER_SIZE
                }
                imageView.minimumHeight = AndroidResource.dpToPx(size)
                val factor: Double = when (parent) {
                    true -> when (input) {
                        true -> 2.2
                        else -> 1.75
                    }
                    else -> 1.0
                }
                Glide.with(imageView.context)
                    .load(imageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .apply(
                        RequestOptions().override(
                            ((image_width ?: size) / factor).toInt(),
                            ((image_height ?: size) / factor).toInt()
                        )
                    )
                    .into(imageView)
            }
            !isDeleted && (isOnlyStickers && numberOfStickers < 2) && !isBlocked -> {
                textView.visibility = View.VISIBLE
                imageView.visibility = View.GONE
                textView.minHeight =
                    AndroidResource.dpToPx(MEDIUM_STICKER_SIZE)
                val s = when (showLinks) {
                    true -> linkText
                    else -> SpannableString(message)
                }
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
                        textView.text = s
                    }
                }
            }
            !isDeleted && atLeastOneSticker && !isBlocked -> {
                textView.visibility = View.VISIBLE
                imageView.visibility = View.GONE
                var columnCount = numberOfStickers / 8
                val lines = message?.withoutStickers()?.getLinesCount(density, textSize) ?: 0
                if (columnCount == 0) {
                    columnCount = 1
                }
                textView.minHeight =
                    (textSize.toInt() * columnCount) + when {
                        lines != columnCount -> (lines * textSize.toInt())
                        else -> 0
                    }
                val s = when (showLinks) {
                    true -> linkText
                    else -> SpannableString(message)
                }
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
                        textView.text = s
                    }
                }
            }
            else -> {
                imageView.visibility = View.GONE
                textView.visibility = View.VISIBLE
                clearTarget(id, textView.context)
                textView.minHeight = textSize.toInt()
                textView.text = when (parent && isBlocked) {
                    true -> textView.context.getString(R.string.parent_blocked_message)
                    else -> when (showLinks) {
                        true -> linkText
                        else -> message
                    }
                }
            }
        }
    }
}

private fun getTextWithCustomLinks(
    linksRegex: Regex,
    spannableString: SpannableString,
    messageId: String,
    chatRoomId: String?,
    chatRoomName: String?,
    analyticsService: AnalyticsService
): SpannableString {
    val result = linksRegex.toPattern().matcher(spannableString)
    while (result.find()) {
        val start = result.start()
        val end = result.end()
        spannableString.setSpan(
            InternalURLSpan(
                spannableString.subSequence(start, end).toString(),
                messageId,
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
private fun String.getLinesCount(density: Float, textSize: Float): Int {
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
        (StaticLayout(this, paint, width, alignment, 1F, 0F, false))
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