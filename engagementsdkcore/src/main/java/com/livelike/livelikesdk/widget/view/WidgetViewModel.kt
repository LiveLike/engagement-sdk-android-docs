package com.livelike.livelikesdk.widget.view

import android.arch.lifecycle.ViewModel
import android.view.View

class WidgetViewModel() : ViewModel() {
    var currentWidget: View? = null // this could be a map of Session and WidgetView..
}
