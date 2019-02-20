package com.livelike.livelikesdk.binding

import com.livelike.livelikesdk.widget.Observer

interface Observable {
    fun registerObserver(observer: Observer)
    fun unRegisterObserver(observer: Observer)
}