package com.livelike.engagementsdk.widget.view

import android.content.res.Resources
import android.view.View
import android.view.ViewGroup

fun View.clipParents(isClip: Boolean) {
    var view = this
    while (view.parent != null && view.parent is ViewGroup) {
        val viewGroup = view.parent as ViewGroup
        viewGroup.clipChildren = isClip
        viewGroup.clipToPadding = isClip
        view = viewGroup
    }
}

fun dpToPx(dp: Float): Int {
    val scale = Resources.getSystem().displayMetrics.density
    return (dp * scale + 0.5f).toInt()
}
