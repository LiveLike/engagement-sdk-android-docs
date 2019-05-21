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
import com.livelike.livelikesdk.utils.AndroidResource
import com.livelike.livelikesdk.utils.logDebug
import com.livelike.livelikesdk.widget.adapters.WidgetOptionsViewAdapter
import com.livelike.livelikesdk.widget.model.Resource
import com.livelike.livelikesdk.widget.util.SpanningLinearLayoutManager
import com.livelike.livelikesdk.widget.view.components.EggTimerView
import com.livelike.livelikesdk.widget.viewModel.QuizViewModel
import com.livelike.livelikesdk.widget.viewModel.QuizWidget
import kotlinx.android.synthetic.main.widget_image_option_selection.view.imageCloseButton
import kotlinx.android.synthetic.main.widget_image_option_selection.view.imageEggTimer
import kotlinx.android.synthetic.main.widget_text_option_selection.view.followupAnimation
import kotlinx.android.synthetic.main.widget_text_option_selection.view.textCloseButton
import kotlinx.android.synthetic.main.widget_text_option_selection.view.textEggTimer
import kotlinx.android.synthetic.main.widget_text_option_selection.view.textRecyclerView
import kotlinx.android.synthetic.main.widget_text_option_selection.view.titleView

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
                if (optionList.isNotEmpty() && !optionList[0].image_url.isNullOrEmpty()) {
                    inflate(context, R.layout.widget_image_option_selection, this@QuizView)
                } else {
                    inflate(context, R.layout.widget_text_option_selection, this@QuizView)
                }
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

            val animationLength = AndroidResource.parseDuration(resource.timeout).toFloat()
            if (viewModel.animationEggTimerProgress < 1f) {
                imageEggTimer?.setupEggTimer(animationLength)
                textEggTimer?.setupEggTimer(animationLength)
            }
        }

        if (widget == null) {
            inflated = false
        }
    }

    private fun EggTimerView.setupEggTimer(animationLength: Float) {
        visibility = View.VISIBLE
        startAnimationFrom(viewModel.animationEggTimerProgress, animationLength) {
            viewModel.animationEggTimerProgress = it
            if (it >= 1) {
                textCloseButton?.visibility = View.VISIBLE
                imageCloseButton?.visibility = View.VISIBLE
                textEggTimer?.visibility = View.VISIBLE
                imageEggTimer?.visibility = View.VISIBLE
            }
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
