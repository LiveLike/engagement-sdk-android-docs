package com.livelike.livelikesdk.binding

interface Observable {
    fun registerObserver(observer: Observer)
    fun unRegisterObserver(observer: Observer)
}