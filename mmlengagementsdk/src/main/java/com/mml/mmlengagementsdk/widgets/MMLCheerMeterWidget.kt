package com.mml.mmlengagementsdk.widgets

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.view.View
import com.bumptech.glide.Glide
import com.example.mmlengagementsdk.R
import com.livelike.engagementsdk.OptionsItem
import com.livelike.engagementsdk.widget.widgetModel.CheerMeterWidgetmodel
import com.mml.mmlengagementsdk.widgets.timeline.TimelineWidgetResource
import com.mml.mmlengagementsdk.widgets.utils.getFormattedTime
import com.mml.mmlengagementsdk.widgets.utils.parseDuration
import com.mml.mmlengagementsdk.widgets.utils.setCustomFontWithTextStyle
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
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlin.math.max

class MMLCheerMeterWidget(context: Context) : ConstraintLayout(context) {

    lateinit var cheerMeterWidgetModel: CheerMeterWidgetmodel
    var winnerOptionItem: OptionsItem? = null
    private val job = SupervisorJob()
    private val uiScope = CoroutineScope(Dispatchers.Main + job)
    var timelineWidgetResource: TimelineWidgetResource? = null

    init {
        inflate(context, R.layout.mml_cheer_meter, this)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        cheerMeterWidgetModel.widgetData.let { liveLikeWidget ->

            if (timelineWidgetResource?.isActive == false) {
                time_bar.visibility = View.INVISIBLE
                val op1 = liveLikeWidget.options?.get(0)
                val op2 = liveLikeWidget.options?.get(1)
                val vt1 = timelineWidgetResource?.liveLikeWidgetResult?.choices?.get(0)?.vote_count
                    ?: op1?.voteCount ?: 0
                val vt2 =
                    timelineWidgetResource?.liveLikeWidgetResult?.choices?.get(1)?.vote_count
                        ?: op2?.voteCount
                        ?: 0
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
                if (timelineWidgetResource?.startTime == null) {
                    timelineWidgetResource?.startTime = Calendar.getInstance().timeInMillis
                }
                val timeMillis = liveLikeWidget.timeout?.parseDuration() ?: 5000
                val timeDiff =
                    Calendar.getInstance().timeInMillis - (timelineWidgetResource?.startTime ?: 0L)
                val remainingTimeMillis = max(0, timeMillis - timeDiff)
                time_bar.visibility = View.VISIBLE
                time_bar.startTimer(timeMillis, remainingTimeMillis)

                uiScope.async {
                    delay(remainingTimeMillis)
                    showWinnerAnimation()
                    frame_cheer_team_1.setOnClickListener(null)
                    frame_cheer_team_2.setOnClickListener(null)
                    frame_cheer_team_1.setBackgroundResource(R.drawable.mml_cheer_meter_background_stroke_drawable)
                    frame_cheer_team_2.setBackgroundResource(R.drawable.mml_cheer_meter_background_stroke_drawable)
                    timelineWidgetResource?.isActive = false
                    cheerMeterWidgetModel.voteResults.unsubscribe(this@MMLCheerMeterWidget)
                }
            }

            setCustomFontWithTextStyle(txt_title, "fonts/RingsideExtraWide-Black.otf")
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
                        if (timelineWidgetResource?.isActive == true)
                            frame_cheer_team_1.setOnClickListener {
                                cheerMeterWidgetModel.submitVote(op.id!!)
                            }
                    }
                    options[1]?.let { op ->
                        Glide.with(context)
                            .load(op.imageUrl)
                            .into(img_cheer_team_2)
                        if (timelineWidgetResource?.isActive == true)
                            frame_cheer_team_2.setOnClickListener {
                                cheerMeterWidgetModel.submitVote(op.id!!)
                            }
                    }
                    if (timelineWidgetResource?.isActive == true)
                        cheerMeterWidgetModel.voteResults.subscribe(this@MMLCheerMeterWidget) {
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
                                timelineWidgetResource?.liveLikeWidgetResult = it
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

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (timelineWidgetResource?.isActive == true) {
            job.cancel()
            uiScope.cancel()
            cheerMeterWidgetModel.voteResults.unsubscribe(this)
            cheerMeterWidgetModel.finish()
        }
    }
}