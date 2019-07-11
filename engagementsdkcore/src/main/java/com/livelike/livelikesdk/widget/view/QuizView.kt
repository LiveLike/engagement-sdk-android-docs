package com.livelike.livelikesdk.widget.view

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.support.v7.widget.LinearLayoutManager
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.utils.AndroidResource
import com.livelike.livelikesdk.utils.logDebug
import com.livelike.livelikesdk.widget.DismissAction
import com.livelike.livelikesdk.widget.SpecifiedWidgetView
import com.livelike.livelikesdk.widget.adapters.WidgetOptionsViewAdapter
import com.livelike.livelikesdk.widget.model.Resource
import com.livelike.livelikesdk.widget.util.SpanningLinearLayoutManager
import com.livelike.livelikesdk.widget.viewModel.QuizViewModel
import com.livelike.livelikesdk.widget.viewModel.QuizWidget
import com.livelike.livelikesdk.widget.viewModel.WidgetViewModel
import kotlinx.android.synthetic.main.widget_image_option_selection.view.imageEggTimer
import kotlinx.android.synthetic.main.widget_text_option_selection.view.followupAnimation
import kotlinx.android.synthetic.main.widget_text_option_selection.view.textEggTimer
import kotlinx.android.synthetic.main.widget_text_option_selection.view.textRecyclerView
import kotlinx.android.synthetic.main.widget_text_option_selection.view.titleView

class QuizView(context: Context, attr: AttributeSet? = null) : SpecifiedWidgetView(context, attr) {

    override var dismissFunc: ((action: DismissAction) -> Unit)? = { viewModel?.dismissWidget(it) }

    private var viewModel : QuizViewModel? = null

    override var widgetViewModel: WidgetViewModel? = null
        get() = super.widgetViewModel
        set(value) {
            field = value
            viewModel = value as QuizViewModel
            viewModel?.data?.subscribe(javaClass) {
                resourceObserver(it)
            }
            viewModel?.results?.subscribe(javaClass){ resultsObserver(it) }
            viewModel?.state?.subscribe(javaClass){ stateObserver(it)}
            viewModel?.currentVoteId?.subscribe(javaClass){ onClickObserver(it)}
        }

    private var viewManager: LinearLayoutManager =
        LinearLayoutManager(context).apply { orientation = LinearLayout.VERTICAL }
    private var inflated = false

    private fun onClickObserver(it: String?) {
        viewModel?.onOptionClicked(it)
    }

    private fun resourceObserver(widget: QuizWidget?) {
        widget?.apply {
            val optionList = resource.getMergedOptions() ?: return
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

            viewModel?.adapter = viewModel?.adapter ?: WidgetOptionsViewAdapter(optionList, {
                viewModel?.adapter?.apply {
                    val currentSelectionId = myDataset[selectedPosition]
                    viewModel?.currentVoteId?.onNext(currentSelectionId.id)
                }
            }, type)

            textRecyclerView.apply {
                this.layoutManager = viewManager
                this.adapter = viewModel?.adapter
                setHasFixedSize(true)
            }

            viewModel?.startDismissTimout(resource.timeout, context.applicationContext)

            val animationLength = AndroidResource.parseDuration(resource.timeout).toFloat()
            if (viewModel?.animationEggTimerProgress!! < 1f) {
                listOf(textEggTimer, imageEggTimer).forEach { v ->
                    v?.startAnimationFrom(viewModel?.animationEggTimerProgress ?: 0f, animationLength, {
                        viewModel?.animationEggTimerProgress = it
                    }) {
                        viewModel?.dismissWidget(it)
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

    private fun stateObserver(state:String?) {
        when (state) {
            "results" -> {
                listOf(textEggTimer, imageEggTimer).forEach { v -> v?.showCloseButton() {
                    viewModel?.dismissWidget(it)
                } }
                viewModel?.adapter?.userSelectedOptionId = viewModel?.adapter?.myDataset?.find { it.is_correct }?.id ?: ""
                viewModel?.adapter?.correctOptionId = viewModel?.adapter?.selectedPosition?.let { it1 ->
                    viewModel?.adapter?.myDataset?.get(it1)?.id
                } ?: ""

                textRecyclerView.swapAdapter(viewModel?.adapter, false)
                textRecyclerView.adapter?.notifyItemChanged(0)

                followupAnimation.apply {
                    setAnimation(viewModel?.animationPath)
                    progress = viewModel?.animationProgress ?: 0f
                    logDebug { "Animation: ${viewModel?.animationPath}" }
                    addAnimatorUpdateListener { valueAnimator ->
                        viewModel?.animationProgress = valueAnimator.animatedFraction
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
