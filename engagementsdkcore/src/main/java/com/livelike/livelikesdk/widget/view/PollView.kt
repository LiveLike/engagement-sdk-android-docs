package com.livelike.livelikesdk.widget.view

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.support.constraint.ConstraintLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.util.AttributeSet
import android.widget.LinearLayout
import com.livelike.livelikesdk.widget.adapters.WidgetOptionsViewAdapter
import com.livelike.livelikesdk.widget.model.Resource
import com.livelike.livelikesdk.widget.util.SpanningLinearLayoutManager
import com.livelike.livelikesdk.widget.viewModel.PollViewModel
import com.livelike.livelikesdk.widget.viewModel.PollWidget
import kotlinx.android.synthetic.main.organism_text_prediction.view.textRecyclerView
import kotlinx.android.synthetic.main.organism_text_prediction.view.titleView

class PollView(context: Context, attr: AttributeSet? = null) : ConstraintLayout(context, attr) {

    private var viewModel =
        ViewModelProviders.of(context as AppCompatActivity).get(PollViewModel::class.java)

    private var viewManager: LinearLayoutManager =
        LinearLayoutManager(context).apply { orientation = LinearLayout.VERTICAL }
    private var inflated = false

    init {
        context as AppCompatActivity
        viewModel.data.observe(context, resourceObserver())
        viewModel.results.observe(context, resultsObserver())
    }

    private fun resourceObserver() = Observer<PollWidget> { widget ->
        widget?.apply {
            val optionList = resource.getMergedOptions() ?: return@Observer
            if (!inflated) {
                inflated = true
                inflate(context, com.livelike.livelikesdk.R.layout.organism_text_prediction, this@PollView)
            }
            if (optionList.isNotEmpty() && !optionList[0].image_url.isNullOrEmpty()
            ) {
                if (optionList.size > 3) {
                    viewManager =
                        LinearLayoutManager(context).apply { orientation = LinearLayout.HORIZONTAL }
                } else {
                    viewManager =
                        SpanningLinearLayoutManager(context)
                            .apply { orientation = LinearLayout.HORIZONTAL }
                }
            }

            titleView.title = resource.question

            viewModel.adapter = viewModel.adapter ?: WidgetOptionsViewAdapter(optionList, { viewModel.vote() }, type)

            textRecyclerView.apply {
                this.layoutManager = viewManager
                this.adapter = viewModel.adapter
                setHasFixedSize(true)
            }

            viewModel.startDismissTimout(resource.timeout)
        }

        if (widget == null) {
            inflated = false
        }
    }

    private fun resultsObserver() = Observer<Resource> { resource ->
        resource?.apply {
            val optionResults = resource.getMergedOptions() ?: return@Observer
            val totalVotes = optionResults.sumBy { it.getMergedVoteCount().toInt() }
            val options = viewModel.data.value?.resource?.getMergedOptions() ?: return@Observer
            options.forEach { opt ->
                optionResults.find {
                    it.id == opt.id
                }?.apply {
                    opt.updateCount(this)
                    opt.percentage = opt.getPercent(totalVotes.toFloat())
                }
            }
            viewModel.adapter?.myDataset = options
            textRecyclerView.swapAdapter(viewModel.adapter, false)
        }
    }
}
