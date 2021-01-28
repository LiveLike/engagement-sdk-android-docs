package com.mml.mmlengagementsdk.widgets

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.support.v7.widget.GridLayoutManager
import android.view.View
import com.example.mmlengagementsdk.R
import com.livelike.engagementsdk.widget.model.LiveLikeWidgetResult
import com.livelike.engagementsdk.widget.widgetModel.QuizWidgetModel
import com.mml.mmlengagementsdk.widgets.adapter.QuizListAdapter
import com.mml.mmlengagementsdk.widgets.model.LiveLikeWidgetOption
import com.mml.mmlengagementsdk.widgets.utils.getFormattedTime
import com.mml.mmlengagementsdk.widgets.utils.parseDuration
import com.mml.mmlengagementsdk.widgets.utils.setCustomFontWithTextStyle
import kotlinx.android.synthetic.main.mml_quiz_widget.view.lottie_animation_view
import kotlinx.android.synthetic.main.mml_quiz_widget.view.quiz_rv
import kotlinx.android.synthetic.main.mml_quiz_widget.view.quiz_title
import kotlinx.android.synthetic.main.mml_quiz_widget.view.time_bar
import kotlinx.android.synthetic.main.mml_quiz_widget.view.txt_time
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MMLQuizWidget(context: Context) : ConstraintLayout(context) {
    lateinit var quizWidgetModel: QuizWidgetModel
    private lateinit var adapter: QuizListAdapter
    private var quizAnswerJob: Job? = null
    var isTimeLine = false
    var livelikeWidgetResult: LiveLikeWidgetResult? = null
    private val job = SupervisorJob()
    private val uiScope = CoroutineScope(Dispatchers.Main + job)

    init {
        inflate(context, R.layout.mml_quiz_widget, this)
    }

    private fun showResultAnimation() {
        lottie_animation_view?.apply {
            if (adapter.selectedOptionItem?.isCorrect == false) {
                setAnimation(
                    "mml/quiz_incorrect.json"
                )
            } else {
                setAnimation(
                    "mml/quiz_correct.json"
                )
            }
            playAnimation()
            visibility = View.VISIBLE
        }
    }


    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        quizWidgetModel.widgetData.let { liveLikeWidget ->
            liveLikeWidget.choices?.let {
                adapter =
                    QuizListAdapter(
                        context,
                        ArrayList(it.map { item ->
                            LiveLikeWidgetOption(
                                item?.id!!,
                                item.description ?: "",
                                item.isCorrect ?: false,
                                item.imageUrl,
                                item.answerCount
                            )
                        })
                    ) { option ->
                        // TODO change sdk apis to have non-nullable option item ids
                        // 1000ms debounce added, TODO To discuss whether sdk should have inbuilt debounce to optimize sdk api calls
                        quizAnswerJob?.cancel()
                        quizAnswerJob = uiScope.launch {
                            delay(1000)
                            quizWidgetModel.lockInAnswer(option.id ?: "")
                        }
                    }
                quiz_rv.layoutManager = GridLayoutManager(context, 2)
                quiz_rv.adapter = adapter
            }
            liveLikeWidget.createdAt?.let {
                setCustomFontWithTextStyle(txt_time, "fonts/RingsideRegular-Book.otf")
                txt_time.text = getFormattedTime(it)
            }
            quiz_title.text = liveLikeWidget.question
            setCustomFontWithTextStyle(quiz_title, "fonts/RingsideExtraWide-Black.otf")
            // TODO  change sdk api for duration, it should passes duration in millis, parsing should be done at sdk side.
            if (isTimeLine) {
                time_bar.visibility = View.INVISIBLE
                val totalVotes = liveLikeWidget.choices?.sumBy { it?.answerCount ?: 0 } ?: 0
                adapter.isResultState = true
                adapter.isResultAvailable = true
                liveLikeWidget.choices?.zip(adapter.list)?.let { options ->
                    adapter.list = ArrayList(options.map { item ->
                        LiveLikeWidgetOption(
                            item.second.id,
                            item.second.description,
                            item.first?.isCorrect ?: false,
                            item.second.imageUrl,
                            when (totalVotes > 0) {
                                true -> (((item.first?.answerCount ?: 0) * 100) / totalVotes)
                                else -> 0
                            }
                        )
                    })
                }
                livelikeWidgetResult?.choices?.zip(adapter.list)?.let { options ->
                    adapter.isResultAvailable = true
                    adapter.list = ArrayList(options.map { item ->
                        LiveLikeWidgetOption(
                            item.second.id,
                            item.second.description ?: "",
                            item.first.is_correct,
                            item.second.imageUrl,
                            (((item.first.answer_count ?: 0) * 100) / totalVotes)
                        )
                    })
                }
                adapter.notifyDataSetChanged()
            } else {
                val timeMillis = liveLikeWidget.timeout?.parseDuration() ?: 5000
                time_bar.startTimer(timeMillis)
                subscribeToVoteResults()
                uiScope.async {
                    delay(timeMillis)
                    adapter.isResultState = true
                    adapter.notifyDataSetChanged()
                    adapter.selectedOptionItem?.let {
                        showResultAnimation()
                        delay(2000)
                        isTimeLine = true
                        quizWidgetModel.voteResults.unsubscribe(this@MMLQuizWidget)
                    }
                }
            }
        }
    }

    private fun subscribeToVoteResults() {
        quizWidgetModel.voteResults.subscribe(this@MMLQuizWidget) { result ->
            val totalVotes = result?.choices?.sumBy { it.answer_count ?: 0 } ?: 0
            result?.choices?.zip(adapter.list)?.let { options ->
                adapter.isResultAvailable = true
                adapter.list = ArrayList(options.map { item ->
                    LiveLikeWidgetOption(
                        item.second.id,
                        item.second.description ?: "",
                        item.first.is_correct,
                        item.second.imageUrl,
                        (((item.first.answer_count ?: 0) * 100) / totalVotes)
                    )
                })
                adapter.notifyDataSetChanged()
            }
            livelikeWidgetResult = result
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (isTimeLine) {
            quizWidgetModel.voteResults.unsubscribe(this)
            quizWidgetModel.finish()
        }
    }


}