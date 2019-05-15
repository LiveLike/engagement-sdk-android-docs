package com.livelike.livelikesdk.widget.view.organism

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.support.constraint.ConstraintLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import com.livelike.livelikesdk.widget.model.Resource
import com.livelike.livelikesdk.widget.view.molecule.TextViewAdapter
import com.livelike.livelikesdk.widget.view.util.SpanningLinearLayoutManager
import kotlinx.android.synthetic.main.atom_widget_confirmation_message.view.confirmMessageAnimation
import kotlinx.android.synthetic.main.organism_text_prediction.view.confirmationMessage
import kotlinx.android.synthetic.main.organism_text_prediction.view.followupAnimation
import kotlinx.android.synthetic.main.organism_text_prediction.view.textRecyclerView
import kotlinx.android.synthetic.main.organism_text_prediction.view.titleView

class PredictionTextView(context: Context, attr: AttributeSet? = null) : ConstraintLayout(context, attr) {

    private var viewModel =
        ViewModelProviders.of(context as AppCompatActivity).get(PredictionTextViewModel::class.java)

    private var viewManager: LinearLayoutManager =
        LinearLayoutManager(context).apply { orientation = LinearLayout.VERTICAL }
    private var inflated = false

    init {
        context as AppCompatActivity
        viewModel.data.observe(context, resourceObserver())
        viewModel.state.observe(context, stateObserver())
    }

    private fun resourceObserver() = Observer<Resource> { resource ->
        if (resource != null) {
            val optionList = resource.getMergedOptions() ?: return@Observer
            if (!inflated) {
                inflated = true
                inflate(context, com.livelike.livelikesdk.R.layout.organism_text_prediction, this@PredictionTextView)
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

            viewModel.adapter = viewModel.adapter ?: TextViewAdapter(optionList)

            textRecyclerView.apply {
                this.layoutManager = viewManager
                this.adapter = viewModel.adapter
                setHasFixedSize(true)
            }

            viewModel.startDismissTimout(resource.timeout, resource.correct_option_id.isNotEmpty())
        } else {
            inflated = false
        }
    }

    private fun stateObserver() = Observer<String> {
        when (it) {
            "confirmation" -> {
                confirmationMessage.apply {
                    text = viewModel.data.value?.confirmation_message ?: ""
                    startAnimation(viewModel.animationPath, viewModel.animationProgress)
                    confirmMessageAnimation.addAnimatorUpdateListener { valueAnimator ->
                        viewModel.animationProgress = valueAnimator.animatedFraction
                    }
                    visibility = View.VISIBLE
                }
            }
            "followup" -> {
                followupAnimation.apply {
                    setAnimation(viewModel.animationPath)
                    progress = viewModel.animationProgress
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
