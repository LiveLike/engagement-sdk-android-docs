package com.livelike.livelikedemo.customwidgets

import android.content.Context
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
import kotlinx.android.synthetic.main.custom_cheer_meter.view.speed_view_1
import kotlinx.android.synthetic.main.custom_cheer_meter.view.speed_view_2
import org.threeten.bp.Duration
import org.threeten.bp.format.DateTimeParseException

/**
 * TODO: document your custom view class.
 */
class CustomCheerMeter : ConstraintLayout {

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
            val op1 = it?.choices?.get(0)
            val op2 = it?.choices?.get(1)
            val vt1 = op1?.vote_count ?: 0
            val vt2 = op2?.vote_count ?: 0
            val total = vt1 + vt2
            if (total > 0) {
                val perVt1 = (vt1.toFloat() / total.toFloat()) * 100
                val perVt2 = (vt2.toFloat() / total.toFloat()) * 100
                speed_view_1.setSpeedAt(perVt1)
                speed_view_2.setSpeedAt(perVt2)
            }
        }
        cheerMeterWidgetModel?.widgetData?.let { livelikeWidget ->

            Glide.with(context)
                .load(livelikeWidget.options?.get(0)?.imageUrl)
                .into(btn_1)

            Glide.with(context)
                .load(livelikeWidget.options?.get(1)?.imageUrl)
                .into(btn_2)

            btn_1.setOnClickListener {
                cheerMeterWidgetModel?.submitVote(livelikeWidget.options?.get(0)?.id!!)
            }
            btn_2.setOnClickListener {
                cheerMeterWidgetModel?.submitVote(livelikeWidget.options?.get(1)?.id!!)
            }

            println("CustomCheerMeter.onAttachedToWindow->${livelikeWidget.timeout}")

            val handler = Handler()
            handler.postDelayed({
                cheerMeterWidgetModel?.dismissWidget(DismissAction.TIMEOUT)
            }, parseDuration(livelikeWidget.timeout ?: ""))
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
