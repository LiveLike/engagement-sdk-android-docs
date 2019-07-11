import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.os.Handler
import android.os.Looper
import com.livelike.engagementsdkapi.Stream
import com.livelike.livelikesdk.SubscriptionManager
import com.livelike.livelikesdk.utils.logError
import kotlin.coroutines.CoroutineContext

fun <T> LiveData<T>.debounce(duration: Long = 1000L) = MediatorLiveData<T>().also { mld ->
    val source = this
    val handler = Handler(Looper.getMainLooper())

    val runnable = Runnable {
        mld.value = source.value
    }

    mld.addSource(source) {
        handler.removeCallbacks(runnable)
        handler.postDelayed(runnable, duration)
    }
}

internal fun <T> SubscriptionManager<T>.debounce(duration: Long = 1000L) = SubscriptionManager<T>().also { mgr ->
    val source = this
    val handler = Handler(Looper.getMainLooper())

    val runnable = Runnable {
        mgr.onNext(source.currentData)
        logError { "On next vote for ${source.currentData}" }
    }

    mgr.subscribe(source) {
        handler.removeCallbacks(runnable)
        handler.postDelayed(runnable, duration)
    }
}
