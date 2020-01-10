package com.livelike.engagementsdk.chat

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.support.v13.view.inputmethod.EditorInfoCompat
import android.support.v13.view.inputmethod.InputConnectionCompat
import android.support.v7.widget.AppCompatEditText
import android.text.Spannable
import android.text.SpannableString
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.livelike.engagementsdk.core.exceptionhelpers.BugsnagClient.client
import com.livelike.engagementsdk.stickerKeyboard.setupBounds
import com.livelike.engagementsdk.utils.addAuthorizationBearer
import com.livelike.engagementsdk.utils.addUserAgent
import com.livelike.engagementsdk.utils.logError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import okio.ByteString
import pl.droidsonroids.gif.GifDrawable
import pl.droidsonroids.gif.MultiCallback
import java.io.File
import java.io.IOException
import java.net.URI
import java.net.URL
import kotlin.coroutines.resume


class RichContentEditText : AppCompatEditText{
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    override fun onCreateInputConnection(editorInfo: EditorInfo): InputConnection {
        val ic: InputConnection = super.onCreateInputConnection(editorInfo)
        EditorInfoCompat.setContentMimeTypes(editorInfo, arrayOf("image/png", "image/gif"))

        val callback =
            InputConnectionCompat.OnCommitContentListener { inputContentInfo, flags, opts ->
                val lacksPermission = (flags and
                        InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION) != 0
                // read and display inputContentInfo asynchronously
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 && lacksPermission) {
                    try {
                        inputContentInfo.requestPermission()
                    } catch (e: Exception) {
                        return@OnCommitContentListener false
                    }
                }

                setText(":${inputContentInfo.contentUri}:")
//
//                GlobalScope.launch (Dispatchers.IO) {
//                    val logging = HttpLoggingInterceptor()
//                    logging.level = (HttpLoggingInterceptor.Level.BODY)
//                    val client = OkHttpClient.Builder()
//                    client.addInterceptor(logging)
//                    val fileBytes = context.contentResolver.openInputStream(inputContentInfo.contentUri).readBytes()
//
//                   val requestBody =
//                       MultipartBody
//                           .Builder()
//                          .setType(MultipartBody.FORM)
//                          .addPart(
//                              Headers.of("Content-Disposition", "form-data; name=\"title\""),
//                              RequestBody.create(null, "Square Logo"))
//                          .addPart(
//                              Headers.of("Content-Disposition", "form-data; name=\"image\""),
//                              RequestBody.create(MediaType.parse("image/*"), fileBytes))
//                          .build()
//
//                    val request = Request.Builder()
//                        .url("https://api.imgur.com/3/image")
//                        .addHeader("Authorization", "Bearer adddc49cbc0d86a46b7ce36454e99e17d5248472")
//                        .method("POST", requestBody)
//                        .build()
//                    val call = client.build().newCall(request)
//                    call.enqueue(object : Callback {
//                        override fun onResponse(call: Call?, response: Response) {
//                            logError { "DONE ${response.body()}" } // uploaded url under data.link
//                        }
//                        override fun onFailure(call: Call?, e: IOException?) {
//                            logError { e }
//                        }
//                    })
//                }

                true
            }
        return InputConnectionCompat.createWrapper(ic, editorInfo, callback)
    }
}
