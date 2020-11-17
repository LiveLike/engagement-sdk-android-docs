package com.livelike.livelikedemo.customwidgets

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import com.livelike.engagementsdk.widget.widgetModel.QuizWidgetModel
import com.livelike.livelikedemo.R
import kotlinx.android.synthetic.main.custom_quiz_widget.view.button2
import kotlinx.android.synthetic.main.custom_quiz_widget.view.button3

class CustomQuizWidget : ConstraintLayout {
    var quizWidgetModel: QuizWidgetModel? = null

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
        inflate(context, R.layout.custom_quiz_widget, this@CustomQuizWidget)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        quizWidgetModel?.widgetData?.let { liveLikeWidget ->
            button2.setOnClickListener {
                quizWidgetModel?.lockInAnswer(liveLikeWidget.choices!![0]!!.id!!)
            }
            button3.setOnClickListener {
                quizWidgetModel?.lockInAnswer(liveLikeWidget.choices!![1]!!.id!!)
            }
        }
        quizWidgetModel?.voteResults?.subscribe(this) {
            it?.choices?.forEach {
                println("SD->${it.answer_count} ->${it.vote_count} ->${it.is_correct}")
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        quizWidgetModel?.voteResults?.unsubscribe(this)
    }
}