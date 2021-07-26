import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.livelike.livelikedemo.LiveLikeApplication

class EngagementViewModelFactory(
    private val application: Application
) : ViewModelProvider.AndroidViewModelFactory(application) {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        require(modelClass == widgetViewModel::class.java) { "Unknown ViewModel class" }
        return widgetViewModel(
            application as LiveLikeApplication
        ) as T
    }
}
