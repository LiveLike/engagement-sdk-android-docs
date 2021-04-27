
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import android.view.View
import com.bumptech.glide.Glide
import com.livelike.engagementsdk.widget.timeline.TimelineWidgetResource
import com.livelike.engagementsdk.widget.viewModel.WidgetStates
import com.livelike.engagementsdk.widget.widgetModel.AlertWidgetModel
import com.livelike.livelikedemo.R
import com.livelike.livelikedemo.customwidgets.parseDuration
import kotlinx.android.synthetic.main.mml_alert_widget.view.btn_link
import kotlinx.android.synthetic.main.mml_alert_widget.view.img_alert
import kotlinx.android.synthetic.main.mml_alert_widget.view.time_bar
import kotlinx.android.synthetic.main.mml_alert_widget.view.txt_description
import kotlinx.android.synthetic.main.mml_alert_widget.view.txt_title
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay

class MMLAlertWidget(
    context: Context,
    private var alertModel: AlertWidgetModel,
    var timelineWidgetResource: TimelineWidgetResource? = null,
    private val isActive : Boolean = false
) : ConstraintLayout(context) {

    private val job = SupervisorJob()
    private val uiScope = CoroutineScope(Dispatchers.Main + job)

    init {
        inflate(context, R.layout.mml_alert_widget, this)
        initView()
    }


    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
    }

    private fun initView() {
        alertModel.widgetData.let { liveLikeWidget ->
            txt_title.text = liveLikeWidget.title
            liveLikeWidget.text?.let {
                txt_description.visibility = View.VISIBLE
                txt_description.text = liveLikeWidget.text
            }
            liveLikeWidget.imageUrl?.let {
                img_alert.visibility = View.VISIBLE
                Glide.with(context)
                    .load(it)
                    .into(img_alert)
            }
            liveLikeWidget.linkLabel?.let {
                btn_link.visibility = View.VISIBLE
                btn_link.text = it
                liveLikeWidget.linkUrl?.let { url ->
                    btn_link.setOnClickListener {
                        alertModel.alertLinkClicked(url)
                        val universalLinkIntent =
                            Intent(Intent.ACTION_VIEW, Uri.parse(liveLikeWidget.linkUrl)).setFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK
                            )
                        if (universalLinkIntent.resolveActivity(context.packageManager) != null) {
                            ContextCompat.startActivity(context, universalLinkIntent, Bundle.EMPTY)
                        }
                    }
                }
            }
            if (!isActive) {
                time_bar.visibility = View.GONE
            } else {
                val timeMillis = liveLikeWidget.timeout?.parseDuration() ?: 5000

                time_bar.visibility = View.VISIBLE
                time_bar.startTimer(timeMillis, timeMillis)
                uiScope.async {
                    delay(timeMillis)
                    timelineWidgetResource?.widgetState = WidgetStates.RESULTS
                    if (timelineWidgetResource == null) {
                        alertModel.finish()
                    } else {
                        time_bar.visibility = View.GONE
                    }
                }
            }
        }
    }
}