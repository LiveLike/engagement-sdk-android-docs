package com.livelike.livelikedemo.mml.widgets

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.View
import com.bumptech.glide.Glide
import com.livelike.engagementsdk.OptionsItem
import com.livelike.engagementsdk.widget.widgetModel.CheerMeterWidgetmodel
import com.livelike.livelikedemo.R
import com.livelike.livelikedemo.mml.widgets.utils.getFormattedTime
import com.livelike.livelikedemo.mml.widgets.utils.parseDuration
import com.livelike.livelikedemo.mml.widgets.utils.setCustomFontWithTextStyle
import kotlinx.android.synthetic.main.mml_cheer_meter.view.cheer_result_team
import kotlinx.android.synthetic.main.mml_cheer_meter.view.frame_cheer_team_1
import kotlinx.android.synthetic.main.mml_cheer_meter.view.frame_cheer_team_2
import kotlinx.android.synthetic.main.mml_cheer_meter.view.img_cheer_team_1
import kotlinx.android.synthetic.main.mml_cheer_meter.view.img_cheer_team_2
import kotlinx.android.synthetic.main.mml_cheer_meter.view.img_winner_anim
import kotlinx.android.synthetic.main.mml_cheer_meter.view.img_winner_team
import kotlinx.android.synthetic.main.mml_cheer_meter.view.prg_cheer_team_1
import kotlinx.android.synthetic.main.mml_cheer_meter.view.prg_cheer_team_2
import kotlinx.android.synthetic.main.mml_cheer_meter.view.time_bar
import kotlinx.android.synthetic.main.mml_cheer_meter.view.txt_time
import kotlinx.android.synthetic.main.mml_cheer_meter.view.txt_title
import kotlinx.android.synthetic.main.mml_cheer_meter.view.vs_anim
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay

class MMLCheerMeterWidget : ConstraintLayout {

    lateinit var cheerMeterWidgetModel: CheerMeterWidgetmodel
    var winnerOptionItem: OptionsItem? = null
    var isTimeLine = false
    private val job = SupervisorJob()
    private val uiScope = CoroutineScope(Dispatchers.Main + job)

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
        inflate(context, R.layout.mml_cheer_meter, this)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        cheerMeterWidgetModel.widgetData.let { liveLikeWidget ->

            if (isTimeLine) {
                time_bar.visibility = View.INVISIBLE
                val op1 = liveLikeWidget.options?.get(0)
                val op2 = liveLikeWidget.options?.get(1)
                val vt1 = op1?.voteCount ?: 0
                val vt2 = op2?.voteCount ?: 0
                val total = vt1 + vt2
                if (total > 0) {
                    val perVt1 = (vt1.toFloat() / total) * 100
                    val perVt2 = (vt2.toFloat() / total) * 100
                    prg_cheer_team_1.progress = perVt1.toInt()
                    prg_cheer_team_2.progress = perVt2.toInt()
                    winnerOptionItem = if (perVt1 > perVt2) {
                        liveLikeWidget.options?.get(0)
                    } else {
                        liveLikeWidget.options?.get(1)
                    }
                    showWinnerAnimation()
                }
            } else {
                val timeMillis = liveLikeWidget.timeout?.parseDuration() ?: 5000
                time_bar.startTimer(timeMillis)
                uiScope.async {
                    delay(timeMillis)
                    showWinnerAnimation()
                    delay(5000)
                    cheerMeterWidgetModel.finish()
                }
            }

            setCustomFontWithTextStyle(txt_title,"fonts/RingsideExtraWide-Black.otf")
            txt_title.text = liveLikeWidget.question
            liveLikeWidget.createdAt?.let {
                setCustomFontWithTextStyle(txt_time, "fonts/RingsideRegular-Book.otf")
                txt_time.text = getFormattedTime(it)
            }
            vs_anim.setAnimation("mml/vs-1-light.json")
            vs_anim.playAnimation()
            liveLikeWidget.options?.let { options ->
                if (options.size == 2) {
                    options[0]?.let { op ->
                        Glide.with(context)
                            .load(op.imageUrl)
                            .into(img_cheer_team_1)
                        if (!isTimeLine)
                            frame_cheer_team_1.setOnClickListener {
                                cheerMeterWidgetModel.submitVote(op.id!!)
                            }
                    }
                    options[1]?.let { op ->
                        Glide.with(context)
                            .load(op.imageUrl)
                            .into(img_cheer_team_2)
                        if (!isTimeLine)
                            frame_cheer_team_2.setOnClickListener {
                                cheerMeterWidgetModel.submitVote(op.id!!)
                            }
                    }
                    cheerMeterWidgetModel.voteResults.subscribe(this) {
                        it?.let {
                            val op1 = it.choices?.get(0)
                            val op2 = it.choices?.get(1)
                            val vt1 = op1?.vote_count ?: 0
                            val vt2 = op2?.vote_count ?: 0
                            val total = vt1 + vt2
                            if (total > 0) {
                                val perVt1 = (vt1.toFloat() / total) * 100
                                val perVt2 = (vt2.toFloat() / total) * 100
                                prg_cheer_team_1.progress = perVt1.toInt()
                                prg_cheer_team_2.progress = perVt2.toInt()
                                winnerOptionItem = if (perVt1 > perVt2) {
                                    options[0]
                                } else {
                                    options[1]
                                }
                            }
                        }
                    }
                }
            }

        }
    }

    private fun showWinnerAnimation() {
        winnerOptionItem?.let { op ->
            cheer_result_team.visibility = View.VISIBLE
            Glide.with(this.context)
                .load(op.imageUrl)
                .into(img_winner_team)
            img_winner_anim.setAnimation("mml/winner_animation.json")
            img_winner_anim.playAnimation()
        }
    }
}