package com.livelike.livelikesdk.binding

internal interface Observable {
    fun registerObserver(widgetObserver: WidgetObserver)
    fun unRegisterObserver(widgetObserver: WidgetObserver)
}