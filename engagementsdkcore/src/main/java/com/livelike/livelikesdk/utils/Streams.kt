package com.livelike.livelikesdk.utils

import android.os.Handler
import android.os.Looper
import com.livelike.livelikesdk.Stream
import java.util.concurrent.ConcurrentHashMap

internal class SubscriptionManager<T>(private val emitOnSubscribe: Boolean = true) :
    Stream<T> {
    override fun latest(): T? {
        return currentData
    }

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
        if (emitOnSubscribe) observer.invoke(currentData)
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

/**
* Applies the given function on the same thread to each value emitted by source stream and returns stream, which emits resulting values.
*/
internal fun <X, Y> Stream<X>.map(applyTransformation: (x: X) -> Y): Stream<Y> {

    val out = SubscriptionManager<Y>()
    this.subscribe(out.hashCode()) {
        it?.let {
            out.onNext(applyTransformation(it))
        }
    }

    return out
}

internal fun <T> SubscriptionManager<T>.debounce(duration: Long = 1000L): SubscriptionManager<T> = SubscriptionManager<T>().let { mgr ->
    val source = this
    val handler = Handler(Looper.getMainLooper())
    var running = false

    fun runnable(): Runnable {
        return Runnable {
            running = false
            mgr.onNext(source.currentData)
        }
    }

    source.subscribe(source::class.java.simpleName) {
        if (!running) {
            running = true
            handler.removeCallbacks(runnable())
            handler.postDelayed(runnable(), duration)
        }
    }

    return mgr
}
