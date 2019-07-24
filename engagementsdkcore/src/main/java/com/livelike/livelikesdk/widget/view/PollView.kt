package com.livelike.livelikesdk.widget.view

import android.arch.lifecycle.Observer
import android.content.Context
import android.util.AttributeSet
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.utils.AndroidResource
import com.livelike.livelikesdk.widget.DismissAction
import com.livelike.livelikesdk.widget.SpecifiedWidgetView
import com.livelike.livelikesdk.widget.adapters.WidgetOptionsViewAdapter
import com.livelike.livelikesdk.widget.model.Resource
import com.livelike.livelikesdk.widget.viewModel.PollViewModel
import com.livelike.livelikesdk.widget.viewModel.PollWidget
import com.livelike.livelikesdk.widget.viewModel.WidgetViewModel
import kotlinx.android.synthetic.main.widget_text_option_selection.view.textEggTimer
import kotlinx.android.synthetic.main.widget_text_option_selection.view.textRecyclerView
import kotlinx.android.synthetic.main.widget_text_option_selection.view.titleView

class PollView(context: Context, attr: AttributeSet? = null) : SpecifiedWidgetView(context, attr) {
    override var dismissFunc: ((DismissAction) -> Unit)? = { viewModel?.dismissWidget(it) }

    private var viewModel: PollViewModel? = null

    private var inflated = false

    override var widgetViewModel: WidgetViewModel? = null
        get() = super.widgetViewModel
        set(value) {
            field = value
            viewModel = value as PollViewModel
            viewModel?.data?.subscribe(javaClass) { resourceObserver(it) }
            viewModel?.results?.subscribe(javaClass) { resultsObserver(it) }
            viewModel?.currentVoteId?.subscribe(javaClass) { clickedOptionObserver() }
        }

    private fun clickedOptionObserver() = Observer<String?> {
        viewModel?.onOptionClicked()
    }

    // Refresh the view when re-attached to the activity
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewModel?.data?.subscribe(javaClass) { resourceObserver(it) }
        viewModel?.results?.subscribe(javaClass) { resultsObserver(it) }
        viewModel?.currentVoteId?.subscribe(javaClass) { clickedOptionObserver() }
    }

    private fun resourceObserver(widget: PollWidget?) {
        widget?.apply {
            val optionList = resource.getMergedOptions() ?: return
            if (!inflated) {
                inflated = true
                inflate(context, R.layout.widget_text_option_selection, this@PollView)
            }

            titleView.title = resource.question
            titleView.background = R.drawable.header_rounded_corner_poll

            viewModel?.adapter = viewModel?.adapter ?: WidgetOptionsViewAdapter(optionList, {
                val selectedId = viewModel?.adapter?.myDataset?.get(viewModel?.adapter?.selectedPosition ?: -1)?.id ?: ""
                viewModel?.currentVoteId?.onNext(selectedId)
            }, type)

            textRecyclerView.apply {
                this.adapter = viewModel?.adapter
            }

            viewModel?.startDismissTimout(resource.timeout)

            val animationLength = AndroidResource.parseDuration(resource.timeout).toFloat()
            if (viewModel?.animationEggTimerProgress!! < 1f) {
                listOf(textEggTimer).forEach { v ->
                    viewModel?.animationEggTimerProgress?.let {
                        v?.startAnimationFrom(it, animationLength, {
                            viewModel?.animationEggTimerProgress = it
                        }, {
                            viewModel?.dismissWidget(it)
                        })
                    }
                }
            }
        }

        if (widget == null) {
            inflated = false
        }
    }

    private fun resultsObserver(resource: Resource?) {
        resource?.apply {
            val optionResults = resource.getMergedOptions() ?: return
            val totalVotes = optionResults.sumBy { it.getMergedVoteCount().toInt() }
            val options = viewModel?.data?.currentData?.resource?.getMergedOptions() ?: return
            options.forEach { opt ->
                optionResults.find {
                    it.id == opt.id
                }?.apply {
                    opt.updateCount(this)
                    opt.percentage = opt.getPercent(totalVotes.toFloat())
                }
            }
            viewModel?.adapter?.myDataset = options
            textRecyclerView.swapAdapter(viewModel?.adapter, false)
        }
    }
}
