package com.livelike.engagementsdk.stickerKeyboard

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import android.widget.EditText
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import java.util.regex.Matcher
import java.util.regex.Pattern

fun String.findStickers() : Matcher
{
    val regex = ":[^:\\s]*:"
    val pattern = Pattern.compile (regex)
    return pattern.matcher (this)
}

fun replaceWithStickers(s: Spannable?, context : Context, stickerPackRepository: StickerPackRepository, edittext_chat_message: EditText?, onComplete: (()->Unit)? = null) {
    val existingSpans = s?.getSpans(0, s.length, ImageSpan::class.java)
    val existingSpanPositions = ArrayList<Int>(existingSpans?.size ?: 0)
    existingSpans?.forEach { imageSpan ->
        existingSpanPositions.add(s.getSpanStart(imageSpan))
    }
    val matcher = s.toString().findStickers()

    while (matcher.find()) {

        val url = stickerPackRepository.getSticker(matcher.group().replace(":", ""))?.file

        val startIndex = matcher.start()
        val end = matcher.end()

        if (url.isNullOrEmpty() // No url for this shortcode
            || existingSpanPositions.contains(startIndex) // The shortcode has already been replaced by an image
        ) {
            continue
        }

        Glide.with(context)
            .load(url)
            .into(object : CustomTarget<Drawable>(50, 50) {
                override fun onLoadCleared(placeholder: Drawable?) {
                }

                override fun onResourceReady(
                    resource: Drawable,
                    transition: Transition<in Drawable>?
                ) {
                    if (edittext_chat_message!= null && resource.intrinsicWidth > edittext_chat_message.width ) {
                        val aspectRatio =
                            resource.intrinsicHeight.toFloat() / resource.intrinsicWidth.toFloat()
                        resource.setBounds(
                            0,
                            0,
                            edittext_chat_message.width,
                            (aspectRatio * edittext_chat_message.width).toInt()
                        )
                    } else {
                        resource.setBounds(
                            0,
                            0,
                            resource.intrinsicWidth,
                            resource.intrinsicHeight
                        )
                    }
                    val span = ImageSpan(resource, url, DynamicDrawableSpan.ALIGN_BASELINE)
                    s?.setSpan(span, startIndex, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    onComplete?.invoke()
                }
            })
    }
}