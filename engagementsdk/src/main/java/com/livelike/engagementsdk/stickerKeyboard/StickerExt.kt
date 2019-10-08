package com.livelike.engagementsdk.stickerKeyboard

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import android.util.Log
import android.widget.EditText
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.load.Options
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import java.util.regex.Matcher
import java.util.regex.Pattern
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder
import com.bumptech.glide.load.resource.bytes.BytesResource
import com.bumptech.glide.load.ResourceDecoder
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.ByteBufferBitmapDecoder
import com.bumptech.glide.load.resource.bitmap.DefaultImageHeaderParser
import com.bumptech.glide.load.resource.bitmap.Downsampler
import com.bumptech.glide.load.resource.drawable.DrawableResource
import com.bumptech.glide.load.resource.transcode.BitmapBytesTranscoder
import com.bumptech.glide.load.resource.transcode.DrawableBytesTranscoder
import com.bumptech.glide.request.RequestOptions
import pl.droidsonroids.gif.GifDrawable
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream


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

class StreamByteArrayResourceDecoder : ResourceDecoder<InputStream, ByteArray> {
    override fun decode(
        source: InputStream,
        width: Int,
        height: Int,
        options: Options
    ): Resource<ByteArray>? {
        val bytes = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        var count = 0
        while (count != -1) {
            count = source.read(buffer)
            bytes.write(buffer, 0, count)
        }
        return BytesResource(bytes.toByteArray())
    }

    override fun handles(source: InputStream, options: Options): Boolean {
        return true
    }

    val id: String
        get() = javaClass.name
}

class GifDrawableByteTranscoder : ResourceTranscoder<ByteArray, GifDrawable> {
    override fun transcode(
        toTranscode: Resource<ByteArray>,
        options: Options
    ): Resource<GifDrawable>? {
        return try {
            MyDrawableResource(GifDrawable(toTranscode.get()))
        } catch (ex: IOException) {
            Log.e("GifDrawable", "Cannot decode bytes", ex)
            null
        }
    }

    val id: String
        get() = javaClass.name
}

internal class MyDrawableResource(drawable: pl.droidsonroids.gif.GifDrawable) :
    DrawableResource<GifDrawable>(drawable) {
    override fun getResourceClass(): Class<GifDrawable> {
        return GifDrawable::class.java
    }

    override fun getSize(): Int {
        return drawable.inputSourceByteCount.toInt()
    }

    override fun recycle() {
        drawable.stop()
        drawable.recycle()
    }
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
            continue
        }

        if(url.contains(".gif")){
            Glide.with(context)
                .`as`(ByteArray::class.java)
                .load(url)
                .into(object : CustomTarget<ByteArray>(size, size) {
                    override fun onLoadCleared(placeholder: Drawable?) {
                    }

                    override fun onResourceReady(
                        resource: ByteArray,
                        transition: Transition<in ByteArray>?
                    ) {
                        try {
                            val drawable = GifDrawable(resource)
                            if (edittext_chat_message!= null && drawable.intrinsicWidth > edittext_chat_message.width ) {
                                val aspectRatio =
                                    drawable.intrinsicHeight.toFloat() / drawable.intrinsicWidth.toFloat()
                                drawable.setBounds(
                                    0,
                                    0,
                                    edittext_chat_message.width,
                                    (aspectRatio * edittext_chat_message.width).toInt()
                                )
                            } else {
                                drawable.setBounds(
                                    0,
                                    0,
                                    drawable.intrinsicWidth,
                                    drawable.intrinsicHeight
                                )
                            }
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
                .into(object : CustomTarget<Drawable>(size, size) {
                    override fun onLoadCleared(placeholder: Drawable?) {
                    }

                    override fun onResourceReady(
                        drawable: Drawable,
                        transition: Transition<in Drawable>?
                    ) {
                        try {
                            if (edittext_chat_message!= null && drawable.intrinsicWidth > edittext_chat_message.width ) {
                                val aspectRatio =
                                    drawable.intrinsicHeight.toFloat() / drawable.intrinsicWidth.toFloat()
                                drawable.setBounds(
                                    0,
                                    0,
                                    edittext_chat_message.width,
                                    (aspectRatio * edittext_chat_message.width).toInt()
                                )
                            } else {
                                drawable.setBounds(
                                    0,
                                    0,
                                    drawable.intrinsicWidth,
                                    drawable.intrinsicHeight
                                )
                            }
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