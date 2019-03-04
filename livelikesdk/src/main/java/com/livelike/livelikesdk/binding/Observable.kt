package com.livelike.livelikesdk.binding

interface Observable {
    fun registerObserver(widgetObserver: WidgetObserver)
    fun unRegisterObserver(widgetObserver: WidgetObserver)
}