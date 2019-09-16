package com.livelike.engagementsdk.widget.view

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.FitCenter
import com.bumptech.glide.request.RequestOptions

fun View.clipParents(isClip: Boolean) {
    var view = this
    while (view.parent != null && view.parent is ViewGroup) {
        val viewGroup = view.parent as ViewGroup
        viewGroup.clipChildren = isClip
        viewGroup.clipToPadding = isClip
        view = viewGroup
    }
}

fun ImageView.loadImage(url: String, size: Int) {
    Glide.with(context)
        .load(url)
        .apply(
            RequestOptions().override(size, size)
                .transform(FitCenter())
        )
        .into(this)
}
