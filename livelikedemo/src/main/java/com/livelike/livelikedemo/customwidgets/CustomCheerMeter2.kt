package com.livelike.livelikedemo.customwidgets

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import com.livelike.engagementsdk.widget.viewModel.CheerMeterWidgetmodel
import com.livelike.livelikedemo.R
import kotlinx.android.synthetic.main.custom_cheer_meter.view.btn_1
import kotlinx.android.synthetic.main.custom_cheer_meter.view.btn_2
import kotlinx.android.synthetic.main.custom_cheer_meter.view.img_close
import kotlinx.android.synthetic.main.custom_cheer_meter.view.speed_view_1
import kotlinx.android.synthetic.main.custom_cheer_meter.view.speed_view_2
import kotlinx.android.synthetic.main.custom_cheer_meter.view.txt_team1
import kotlinx.android.synthetic.main.custom_cheer_meter.view.txt_team2

class CustomCheerMeter2 : ConstraintLayout {

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
        inflate(context, R.layout.custom_cheer_meter, this)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        cheerMeterWidgetModel?.voteResults?.subscribe(this.javaClass) {
            // update results
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
                txt_team1.text = "$perVt1"
                txt_team2.text = "$perVt2"
            }
        }
        cheerMeterWidgetModel?.widgetData?.let { livelikeWidget ->
            val option1 = livelikeWidget.options?.get(0)
            val option2 = livelikeWidget.options?.get(1)
            btn_1.setOnClickListener {
                cheerMeterWidgetModel?.submitVote(option1?.id!!)
            }
            btn_2.setOnClickListener {
                cheerMeterWidgetModel?.submitVote(option2?.id!!)
            }
            img_close.setOnClickListener {
                cheerMeterWidgetModel?.finish()
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cheerMeterWidgetModel?.voteResults?.unsubscribe(this.javaClass)
    }
}