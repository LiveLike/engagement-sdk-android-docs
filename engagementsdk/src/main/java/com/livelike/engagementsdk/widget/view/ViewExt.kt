package com.livelike.engagementsdk.widget.view

import android.content.Context
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.FitCenter
import com.bumptech.glide.request.RequestOptions

fun View.clipParents(isClip: Boolean) {
    var view = this
    if (view is ViewGroup) {
        view.clipChildren = isClip
        view.clipToPadding = isClip
    }
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

/**
 * Returns a color associated with a particular resource ID
 *
 *
 * Starting in [android.os.Build.VERSION_CODES.M], the returned
 * color will be styled for the specified Context's theme.
 *
 * @param id The desired resource identifier, as generated by the aapt
 * tool. This integer encodes the package, type, and resource
 * entry. The value 0 is an invalid identifier.
 * @return A single color value in the form 0xAARRGGBB.
 * @throws android.content.res.Resources.NotFoundException if the given ID
 * does not exist.
 */
@Suppress("DEPRECATION")
internal fun Context.getColorCompat(id: Int): Int {
    return if (Build.VERSION.SDK_INT >= 23) {
        this.getColor(id)
    } else {
        this.resources.getColor(id)
    }
}
