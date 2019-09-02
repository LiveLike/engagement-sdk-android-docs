package com.livelike.engagementsdk.widget.view

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
