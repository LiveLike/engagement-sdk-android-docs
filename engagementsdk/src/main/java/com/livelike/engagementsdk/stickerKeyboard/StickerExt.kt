package com.livelike.engagementsdk.stickerKeyboard

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import android.util.Log
import android.widget.EditText
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.livelike.engagementsdk.utils.AndroidResource
import pl.droidsonroids.gif.GifDrawable
import java.io.IOException
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.math.roundToInt


fun String.findStickers() : Matcher
{
    val regex = ":[^ :\\s]*:"
    val pattern = Pattern.compile (regex)
    return pattern.matcher (this)
}

fun String.findIsOnlyStickers() : Matcher
{
    val regex = "(:[^ :\\s]*:)+"
    val pattern = Pattern.compile (regex)
    return pattern.matcher (this)
}

fun Matcher.countMatches(): Int {
    var counter = 0
    while (this.find())
        counter++
    return counter
}

fun replaceWithStickers(s: Spannable?, context : Context, stickerPackRepository: StickerPackRepository, edittext_chat_message: EditText?, size : Int = 50, onComplete: (()->Unit)? = null) {
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
            onComplete?.invoke()
            continue
        }

        if(url.contains(".gif")){
            Glide.with(context)
                .`as`(ByteArray::class.java)
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(object : CustomTarget<ByteArray>(size, size) {
                    override fun onLoadCleared(placeholder: Drawable?) {
                    }

                    override fun onResourceReady(
                        resource: ByteArray,
                        transition: Transition<in ByteArray>?
                    ) {
                        try {
                            val drawable = GifDrawable(resource)
                            setupBounds(drawable, edittext_chat_message, size)
                            drawable.start()
                            val span = ImageSpan(drawable, url, DynamicDrawableSpan.ALIGN_BASELINE)
                            s?.setSpan(span, startIndex, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                            onComplete?.invoke()

                        } catch (e : IOException) {
                            e.printStackTrace()
                        }


                    }
                })
        }else{
            Glide.with(context)
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(object : CustomTarget<Drawable>(size, size) {
                    override fun onLoadCleared(placeholder: Drawable?) {
                    }

                    override fun onResourceReady(
                        drawable: Drawable,
                        transition: Transition<in Drawable>?
                    ) {
                        try {
                            setupBounds(drawable, edittext_chat_message, size)
                            val span = ImageSpan(drawable, url, DynamicDrawableSpan.ALIGN_BASELINE)
                            s?.setSpan(span, startIndex, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                            onComplete?.invoke()
                        } catch (e : IOException) {
                            e.printStackTrace()
                        }


                    }
                })
        }


    }


}

private fun setupBounds(
    drawable: Drawable,
    edittext_chat_message: EditText?,
    overrideSize : Int
) {
    val padding = AndroidResource.dpToPx(8)
    val ratioWidth = drawable.intrinsicWidth.toFloat()/overrideSize.toFloat()
    val ratioHeight = drawable.intrinsicHeight.toFloat()/overrideSize.toFloat()
    if (edittext_chat_message != null && overrideSize > edittext_chat_message.width) {
        drawable.setBounds(
            0,
            padding,
            (edittext_chat_message.width*ratioWidth).roundToInt(),
            edittext_chat_message.width+padding
        )
    } else {
        drawable.setBounds(
            0,
            padding,
            (overrideSize*ratioWidth).roundToInt(),
            (overrideSize*ratioHeight).roundToInt()+padding
        )
    }
}