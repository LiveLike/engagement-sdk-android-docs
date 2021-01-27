package com.example.mmlengagementsdk.widgets

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.support.v7.widget.GridLayoutManager
import android.util.AttributeSet
import android.view.View
import com.example.mmlengagementsdk.R
import com.livelike.engagementsdk.widget.widgetModel.QuizWidgetModel
import com.example.mmlengagementsdk.widgets.adapter.QuizListAdapter
import com.example.mmlengagementsdk.widgets.model.LiveLikeWidgetOption
import com.example.mmlengagementsdk.widgets.utils.getFormattedTime
import com.example.mmlengagementsdk.widgets.utils.parseDuration
import com.example.mmlengagementsdk.widgets.utils.setCustomFontWithTextStyle
import kotlinx.android.synthetic.main.mml_quiz_widget.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MMLQuizWidget : ConstraintLayout {
    lateinit var quizWidgetModel: QuizWidgetModel
    private lateinit var adapter: QuizListAdapter
    private var quizAnswerJob: Job? = null
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
                liveLikeWidget.choices?.zip(adapter.list)?.let { options ->
                    adapter.isResultState = true
                    adapter.isResultAvailable = true
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
                    adapter.notifyDataSetChanged()
                }
            } else {
                val timeMillis = liveLikeWidget.timeout?.parseDuration() ?: 5000
                time_bar.startTimer(timeMillis)

                uiScope.async {
                    delay(timeMillis)
                    adapter.isResultState = true
                    adapter.notifyDataSetChanged()
                    adapter.selectedOptionItem?.let {
                        showResultAnimation()
                        delay(2000)
                    }
                    quizWidgetModel.finish()
                }
            }
        }
        subscribeToVoteResults()
    }

    private fun subscribeToVoteResults() {
        quizWidgetModel.voteResults.subscribe(this) { result ->
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
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        quizWidgetModel.voteResults.unsubscribe(this)
        quizWidgetModel?.finish()
    }


}