package com.livelike.livelikesdk.widget.view

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.support.constraint.ConstraintLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.utils.logDebug
import com.livelike.livelikesdk.widget.adapters.WidgetOptionsViewAdapter
import com.livelike.livelikesdk.widget.model.Resource
import com.livelike.livelikesdk.widget.util.SpanningLinearLayoutManager
import com.livelike.livelikesdk.widget.viewModel.QuizViewModel
import com.livelike.livelikesdk.widget.viewModel.QuizWidget
import kotlinx.android.synthetic.main.organism_text_prediction.view.followupAnimation
import kotlinx.android.synthetic.main.organism_text_prediction.view.textRecyclerView
import kotlinx.android.synthetic.main.organism_text_prediction.view.titleView

class QuizView(context: Context, attr: AttributeSet? = null) : ConstraintLayout(context, attr) {

    private var viewModel =
        ViewModelProviders.of(context as AppCompatActivity).get(QuizViewModel::class.java)

    private var viewManager: LinearLayoutManager =
        LinearLayoutManager(context).apply { orientation = LinearLayout.VERTICAL }
    private var inflated = false

    init {
        context as AppCompatActivity
        viewModel.data.observe(context, resourceObserver())
        viewModel.results.observe(context, resultsObserver())
        viewModel.state.observe(context, stateObserver())
    }

    private fun resourceObserver() = Observer<QuizWidget> { widget ->
        widget?.apply {
            val optionList = resource.getMergedOptions() ?: return@Observer
            if (!inflated) {
                inflated = true
                inflate(context, R.layout.organism_text_prediction, this@QuizView)
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
            titleView.background = R.drawable.quiz_textview_rounded_corner

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

    private fun stateObserver() = Observer<String> { state ->
        when (state) {
            "results" -> {
                viewModel.adapter?.userSelectedOptionId = viewModel.adapter?.myDataset?.find { it.is_correct }?.id ?: ""
                viewModel.adapter?.correctOptionId = viewModel.adapter?.selectedPosition?.let { it1 ->
                    viewModel.adapter?.myDataset?.get(it1)?.id
                } ?: ""

                textRecyclerView.swapAdapter(viewModel.adapter, false)
                textRecyclerView.adapter?.notifyItemChanged(0)

                followupAnimation.apply {
                    setAnimation(viewModel.animationPath)
                    progress = viewModel.animationProgress
                    logDebug { "Animation: ${viewModel.animationPath}" }
                    addAnimatorUpdateListener { valueAnimator ->
                        viewModel.animationProgress = valueAnimator.animatedFraction
                    }
                    if (progress != 1f) {
                        resumeAnimation()
                    }
                    visibility = View.VISIBLE
                }
            }
        }
    }
}
