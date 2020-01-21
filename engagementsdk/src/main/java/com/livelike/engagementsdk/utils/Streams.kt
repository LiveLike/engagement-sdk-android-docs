package com.livelike.engagementsdk.utils

import android.os.Handler
import android.os.Looper
import com.livelike.engagementsdk.Stream
import com.livelike.engagementsdk.core.exceptionhelpers.safeCodeBlockCall
import java.util.concurrent.ConcurrentHashMap

internal class SubscriptionManager<T>(private val emitOnSubscribe: Boolean = true,val file:Any?=null) :
    Stream<T> {
    override fun latest(): T? {
        return currentData
    }

    private val observerMap = ConcurrentHashMap<Any, (T?) -> Unit>()
    var currentData: T? = null
        private set

    override fun onNext(data1: T?) {
        // TODO add debug log with class name appended
        println("--->>>SubscriptionManager.onNext-->$file-> $data1 <<><><> ${observerMap.size}")
        safeCodeBlockCall({
            println("--->>>SubscriptionManager.onNext->> ${observerMap.size}")
            observerMap.forEach {
                println("--->>>SubscriptionManager.onNext>>>>> ${it.key} -> $data1")
                it.value.invoke(data1)
            }
        })
        currentData = data1
    }

    override fun subscribe(key: Any, observer: (T?) -> Unit) {
        println("--->>>SubscriptionManager.subscribe-->$file--> $key")
        observerMap[key] = observer
        if (emitOnSubscribe) observer.invoke(currentData)
    }

    override fun unsubscribe(key: Any) {
        println("--->>>SubscriptionManager.unsubscribe-->$file--> $key")
        observerMap.remove(key)
    }

    override fun clear() {
        println("--->>>SubscriptionManager.clear-->$file-->")
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

/**
 * combine the latest from 2 streams only once, so the stream out will be single RX
 */
internal fun <X, Y> Stream<X>.combineLatestOnce(other: Stream<Y>): Stream<Pair<X, Y>> {
    val pairedStream: Stream<Pair<X, Y>> = SubscriptionManager()
    this.subscribe(other.hashCode()) {
        it?.let { x ->
            this.unsubscribe(other.hashCode())
            other.subscribe(this.hashCode()) { y ->
                y?.let {
                    other.unsubscribe(this.hashCode())
                    pairedStream.onNext(Pair(x, y))
                }
            }
        }
    }
    return pairedStream
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
