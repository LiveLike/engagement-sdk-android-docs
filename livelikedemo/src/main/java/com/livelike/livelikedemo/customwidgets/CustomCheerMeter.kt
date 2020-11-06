package com.livelike.livelikedemo.customwidgets

import android.content.Context
import android.os.CountDownTimer
import android.os.Handler
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.util.Log
import com.bumptech.glide.Glide
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.widget.viewModel.CheerMeterWidgetmodel
import com.livelike.livelikedemo.R
import kotlinx.android.synthetic.main.custom_cheer_meter.view.btn_1
import kotlinx.android.synthetic.main.custom_cheer_meter.view.btn_2
import kotlinx.android.synthetic.main.custom_cheer_meter.view.progress_bar
import kotlinx.android.synthetic.main.custom_cheer_meter.view.speed_view_1
import kotlinx.android.synthetic.main.custom_cheer_meter.view.speed_view_2
import kotlinx.android.synthetic.main.custom_cheer_meter.view.txt_team1
import kotlinx.android.synthetic.main.custom_cheer_meter.view.txt_team2
import org.threeten.bp.Duration
import org.threeten.bp.format.DateTimeParseException
import java.time.format.DateTimeParseException


/**
 * TODO: document your custom view class.
 */
class CustomCheerMeter : ConstraintLayout {

    private lateinit var mCountDownTimer: CountDownTimer
    var cheerMeterWidgetModel: CheerMeterWidgetmodel? = null


    constructor(context: Context) : super(context) {
        init(null, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(attrs, defStyle)
    }

    private fun init(attrs: AttributeSet?, defStyle: Int) {
        inflate(context, R.layout.custom_cheer_meter, this@CustomCheerMeter)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        cheerMeterWidgetModel?.voteResults?.subscribe(this.javaClass) {

        }

        cheerMeterWidgetModel?.widgetData?.let { livelikeWidget ->


        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cheerMeterWidgetModel?.voteResults?.unsubscribe(this.javaClass)
    }

    private fun parseDuration(durationString: String): Long {
        var timeout = 7000L
        try {
            timeout = Duration.parse(durationString).toMillis()
        } catch (e: DateTimeParseException) {
            Log.e("Error", "Duration $durationString can't be parsed.")
        }
        return timeout
    }
}
