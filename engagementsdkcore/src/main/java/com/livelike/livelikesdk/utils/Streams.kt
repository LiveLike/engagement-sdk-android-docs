package com.livelike.livelikesdk.utils

import android.os.Handler
import android.os.Looper
import com.livelike.engagementsdkapi.Stream
import java.util.concurrent.ConcurrentHashMap

internal interface Provider<T> {
    fun subscribe(ready: (T) -> Unit)
}

internal class SubscriptionManager<T> : Stream<T> {
    private val observerMap = ConcurrentHashMap<Any, (T?) -> Unit>()
    var currentData: T? = null
        private set

    override fun onNext(data1: T?) {
        observerMap.forEach {
            it.value.invoke(data1)
        }
        currentData = data1
    }

    override fun subscribe(key: Any, observer: (T?) -> Unit) {
        observerMap[key] = observer
        observer.invoke(currentData)
    }

    override fun unsubscribe(key: Any) {
        observerMap.remove(key)
    }

    override fun clear() {
        currentData = null
        onNext(null)
        observerMap.clear()
    }
}

internal fun <T> SubscriptionManager<T>.debounce(duration: Long = 2000L) : SubscriptionManager<T> = SubscriptionManager<T>().let { mgr ->
    val source = this
    val handler = Handler(Looper.getMainLooper())
    var running = false

    fun runnable() : Runnable {
        return Runnable {
            running = false
            mgr.onNext(source.currentData)
        }
    }

    source.subscribe(source) {
        if(!running){
            running = true
            handler.removeCallbacks(runnable())
            handler.postDelayed(runnable(), duration)
        }
    }

    return mgr
}