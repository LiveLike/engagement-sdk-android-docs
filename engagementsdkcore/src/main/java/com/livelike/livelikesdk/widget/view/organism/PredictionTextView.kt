package com.livelike.livelikesdk.widget.view.organism

import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.support.constraint.ConstraintLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.util.AttributeSet
import android.widget.LinearLayout
import com.livelike.livelikesdk.widget.view.molecule.TextViewAdapter
import com.livelike.livelikesdk.widget.view.prediction.text.TextOptionWidgetViewModel
import kotlinx.android.synthetic.main.organism_text_prediction.view.textRecyclerView

class PredictionTextView(context: Context, attr: AttributeSet? = null) : ConstraintLayout(context, attr) {

    private var textOptionWidgetViewModel =
        ViewModelProviders.of(context as AppCompatActivity).get(TextOptionWidgetViewModel::class.java)

    init {
        inflate(context, com.livelike.livelikesdk.R.layout.organism_text_prediction, this)

        val viewManager = LinearLayoutManager(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        val viewAdapter = TextViewAdapter(textOptionWidgetViewModel.data.options)

        textRecyclerView.apply {
            layoutManager = viewManager
            adapter = viewAdapter
        }
    }
}