import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.livelike.livelikedemo.LiveLikeApplication

class EngagementViewModelFactory(
    private val application: Application
) : ViewModelProvider.AndroidViewModelFactory(application) {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        require(modelClass == WidgetViewModel::class.java) { "Unknown ViewModel class" }
        @Suppress("UNCHECKED_CAST")
        return WidgetViewModel(
            application as LiveLikeApplication
        ) as T
    }
}
