package com.livelike.livelikedemo.customwidgets

import android.content.Context
import android.graphics.Color
import android.support.constraint.ConstraintLayout
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.bumptech.glide.Glide
import com.livelike.engagementsdk.OptionsItem
import com.livelike.engagementsdk.widget.widgetModel.QuizWidgetModel
import com.livelike.livelikedemo.R
import kotlinx.android.synthetic.main.custom_quiz_widget.view.imageView2
import kotlinx.android.synthetic.main.custom_quiz_widget.view.rcyl_quiz_list
import kotlinx.android.synthetic.main.quiz_image_list_item.view.imageButton2
import kotlinx.android.synthetic.main.quiz_list_item.view.button4

class CustomQuizWidget : ConstraintLayout {
    var quizWidgetModel: QuizWidgetModel? = null
    var isImage = false
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
            liveLikeWidget.choices?.let {
                val adapter =
                    QuizListAdapter(context, isImage, ArrayList(it.map { item -> item!! }),
                        object : QuizListAdapter.OnSelectListener {
                            override fun onSelectOption(optionsItem: OptionsItem) {
                                quizWidgetModel?.lockInAnswer(optionsItem.id!!)
                            }
                        })
                rcyl_quiz_list.adapter = adapter
                quizWidgetModel?.voteResults?.subscribe(this) { result ->
                    val op =
                        result?.choices?.find { option -> option.id == adapter.getSelectedOption()?.id }
                    op?.let { option ->
                        Toast.makeText(
                            context, when (option.is_correct) {
                                true -> "Correct"
                                else -> "Incorrect"
                            }, Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            imageView2.setOnClickListener {
                quizWidgetModel?.finish()
            }
        }

    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        quizWidgetModel?.voteResults?.unsubscribe(this)
    }
}

class QuizListAdapter(
    private val context: Context,
    private val isImage: Boolean,
    private val list: ArrayList<OptionsItem>,
    private val onSelectListener: OnSelectListener
) :
    RecyclerView.Adapter<QuizListAdapter.QuizListItemViewHolder>() {
    private var selectedIndex = -1

    fun getSelectedOption(): OptionsItem? = when (selectedIndex > -1) {
        true -> list[selectedIndex]
        else -> null
    }

    class QuizListItemViewHolder(view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): QuizListItemViewHolder {
        return QuizListItemViewHolder(
            LayoutInflater.from(p0.context!!).inflate(
                when (isImage) {
                    true -> R.layout.quiz_image_list_item
                    else -> R.layout.quiz_list_item
                }, p0, false
            )
        )
    }

    override fun onBindViewHolder(holder: QuizListItemViewHolder, index: Int) {
        val item = list[index]
        if (isImage) {
            Glide.with(context)
                .load(item.imageUrl)
                .into(holder.itemView.imageButton2)
            if (selectedIndex == index) {
                holder.itemView.imageButton2.setBackgroundColor(Color.BLUE)
            } else {
                holder.itemView.imageButton2.setBackgroundColor(Color.GRAY)
            }
            holder.itemView.imageButton2.setOnClickListener {
                selectedIndex = holder.adapterPosition
                notifyDataSetChanged()
                onSelectListener.onSelectOption(item)
            }
        } else {
            holder.itemView.button4.text = "${item.description}"
            if (selectedIndex == index) {
                holder.itemView.button4.setBackgroundColor(Color.BLUE)
            } else {
                holder.itemView.button4.setBackgroundColor(Color.GRAY)
            }
            holder.itemView.button4.setOnClickListener {
                selectedIndex = holder.adapterPosition
                notifyDataSetChanged()
                onSelectListener.onSelectOption(item)
            }
        }

    }

    override fun getItemCount(): Int = list.size

    interface OnSelectListener {
        fun onSelectOption(optionsItem: OptionsItem)
    }
}
