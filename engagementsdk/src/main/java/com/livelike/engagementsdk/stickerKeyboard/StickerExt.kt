package com.livelike.engagementsdk.stickerKeyboard

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import android.widget.EditText
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.livelike.engagementsdk.utils.AndroidResource
import pl.droidsonroids.gif.GifDrawable
import pl.droidsonroids.gif.MultiCallback
import java.io.IOException
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.math.roundToInt

fun String.findStickers(): Matcher {
    val regex = ":[^ :\\s]*:"
    val pattern = Pattern.compile(regex)
    return pattern.matcher(this)
}

fun String.findImages(): Matcher {
    val regex = ":content://[^ :\\s]*:|:https://[^ :\\s]*:"
    val pattern = Pattern.compile(regex)
    return pattern.matcher(this)
}

fun String.findIsOnlyStickers(): Matcher {
    val regex = "(:[^ :\\s]*:)+"
    val pattern = Pattern.compile(regex)
    return pattern.matcher(this)
}

fun Matcher.countMatches(): Int {
    var counter = 0
    while (this.find())
        counter++
    return counter
}

const val stickerSize=100

fun replaceWithStickers(s: Spannable?, context: Context, stickerPackRepository: StickerPackRepository, edittext_chat_message: EditText?,callback: MultiCallback?, size: Int = 50, onMatch: (() -> Unit)? = null) {
    val existingSpans = s?.getSpans(0, s.length, ImageSpan::class.java)
    val existingSpanPositions = ArrayList<Int>(existingSpans?.size ?: 0)
    existingSpans?.forEach { imageSpan ->
        existingSpanPositions.add(s.getSpanStart(imageSpan))
    }
    var matcher = s.toString().findStickers()

    while (matcher.find()) {

        val url = stickerPackRepository.getSticker(matcher.group().replace(":", ""))?.file

        val startIndex = matcher.start()
        val end = matcher.end()

        if (url.isNullOrEmpty() || // No url for this shortcode
            existingSpanPositions.contains(startIndex) // The shortcode has already been replaced by an image
        ) {
            onMatch?.invoke()
            continue
        }

        if (url.contains(".gif")) {
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
                            drawable.reset()
                            drawable.start()
                            drawable.callback = callback
                            val span = ImageSpan(drawable, url, DynamicDrawableSpan.ALIGN_BASELINE)
                            s?.setSpan(span, startIndex, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                            onMatch?.invoke()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                })
        } else {
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
                            onMatch?.invoke()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                })
        }
    }
}

var targetDrawables = mutableMapOf<String, CustomTarget<Drawable>?>()
var targetByteArrays = mutableMapOf<String, CustomTarget<ByteArray>?>()

fun clearTarget(id: String, context: Context){
    Glide.with(context).clear(targetByteArrays[id])
    Glide.with(context).clear(targetDrawables[id])
}

fun replaceWithImages(
    s: Spannable?,
    context: Context,
    callback: MultiCallback?,
    id: String = "",
    onMatch: (() -> Unit)? = null
) {
    val existingSpans = s?.getSpans(0, s.length, ImageSpan::class.java)
    val existingSpanPositions = ArrayList<Int>(existingSpans?.size ?: 0)
    existingSpans?.forEach { imageSpan ->
        existingSpanPositions.add(s.getSpanStart(imageSpan))
    }

    val matcher = s.toString().findImages()

    clearTarget(id, context)

    while (matcher.find()) {

        val fullUrl = matcher.group()
        val url = matcher.group().substring(1, fullUrl.length -1)

        val startIndex = matcher.start()
        val end = matcher.end()

        if (url.contains(".gif")) {
            targetByteArrays[id] = Glide.with(context)
                .`as`(ByteArray::class.java)
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(object : CustomTarget<ByteArray>(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL) {
                    override fun onLoadCleared(placeholder: Drawable?) {
                    }

                    override fun onResourceReady(
                        resource: ByteArray,
                        transition: Transition<in ByteArray>?
                    ) {
                        try {
                            val drawable = GifDrawable(resource)
                            setupBounds(drawable)
                            drawable.reset()
                            drawable.start()
                            drawable.callback = callback
                            val span = ImageSpan(drawable, url, DynamicDrawableSpan.ALIGN_BASELINE)
                            s?.setSpan(span, startIndex, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                            onMatch?.invoke()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                })
        } else {
            targetDrawables[id] = Glide.with(context)
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(object : CustomTarget<Drawable>(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL) {
                    override fun onLoadCleared(placeholder: Drawable?) {
                    }

                    override fun onResourceReady(
                        drawable: Drawable,
                        transition: Transition<in Drawable>?
                    ) {
                        try {
                            setupBounds(drawable)
                            val span = ImageSpan(drawable, url, DynamicDrawableSpan.ALIGN_BASELINE)
                            s?.setSpan(span, startIndex, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                            onMatch?.invoke()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                })
        }
    }
}

internal fun setupBounds(
    drawable: Drawable,
    edittext_chat_message: EditText?,
    overrideSize: Int
) {
    val padding = AndroidResource.dpToPx(8)
    var ratioWidth = drawable.intrinsicWidth.toFloat()/overrideSize.toFloat()
    var ratioHeight = drawable.intrinsicHeight.toFloat()/overrideSize.toFloat()

    if (overrideSize == AndroidResource.dpToPx(stickerSize) && drawable.intrinsicWidth <= AndroidResource.dpToPx(
            stickerSize)) {
        ratioWidth = 1f
        ratioHeight = 1f
    }

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

// This method is following iOS guidelines. Make sure to discuss with the iOS team before modifying it
internal fun setupBounds(
    drawable: Drawable
) {
    val padding = AndroidResource.dpToPx(8)

    val overrideSize = AndroidResource.dpToPx(stickerSize)

    val height = overrideSize
    val width = (overrideSize.toFloat()*drawable.intrinsicWidth.toFloat()/drawable.intrinsicHeight.toFloat()).roundToInt()

    drawable.setBounds(
        0,
        padding,
        width,
        height+padding
    )
}