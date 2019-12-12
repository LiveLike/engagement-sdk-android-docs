package com.livelike.engagementsdk.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

internal fun Context.scanForActivity(): Activity? {
    if (this is Activity)
        return this
    else if (this is ContextWrapper)
        return this.baseContext.scanForActivity()
    return null
}
