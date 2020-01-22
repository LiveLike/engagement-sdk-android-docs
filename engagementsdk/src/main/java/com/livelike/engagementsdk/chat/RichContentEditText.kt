package com.livelike.engagementsdk.chat

import android.content.Context
import android.os.Build
import android.support.v13.view.inputmethod.EditorInfoCompat
import android.support.v13.view.inputmethod.InputConnectionCompat
import android.support.v7.widget.AppCompatEditText
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection


class RichContentEditText : AppCompatEditText {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    override fun onCreateInputConnection(editorInfo: EditorInfo): InputConnection {
        val ic: InputConnection = super.onCreateInputConnection(editorInfo)
        if (allowMediaFromKeyboard) {
            EditorInfoCompat.setContentMimeTypes(editorInfo, arrayOf("image/*"))

            val callback =
                InputConnectionCompat.OnCommitContentListener { inputContentInfo, flags, opts ->
                    val lacksPermission = (flags and
                            InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION) != 0
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 && lacksPermission) {
                        try {
                            inputContentInfo.requestPermission()
                        } catch (e: Exception) {
                            return@OnCommitContentListener false
                        }
                    }
                    context.contentResolver.openAssetFileDescriptor(
                        inputContentInfo.contentUri,
                        "r"
                    )
                        ?.use {
                            if (it.length > 3_000_000) {
                                return@OnCommitContentListener false
                            }
                        }

                    setText(":${inputContentInfo.contentUri}:")
                    true
                }
            return InputConnectionCompat.createWrapper(ic, editorInfo, callback)
        }
        return ic
    }

    var allowMediaFromKeyboard: Boolean=true
    var isTouching = false

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        isTouching =
            event?.action == MotionEvent.ACTION_DOWN || event?.action == MotionEvent.ACTION_MOVE
        return super.onTouchEvent(event)
    }
}
