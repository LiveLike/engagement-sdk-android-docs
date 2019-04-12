package com.livelike.livelikesdk.widget.view.util

import android.content.Context
import android.support.v7.content.res.AppCompatResources
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import com.airbnb.lottie.LottieAnimationView
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.animation.ViewAnimation
import com.livelike.livelikesdk.util.AndroidResource
import com.livelike.livelikesdk.widget.model.VoteOption

internal class WidgetResultDisplayUtil(val context: Context, private val viewAnimation: ViewAnimation) {
    private var lottieAnimationPath = ""
    companion object {
        const val correctAnswerLottieFilePath = "correctAnswer"
        const val wrongAnswerLottieFilePath = "wrongAnswer"
    }
    fun updateViewDrawable(optionId: String,
                           progressBar: ProgressBar,
                           optionButton: View,
                           percentage: Int,
                           correctOption: String?,
                           selectedOption: String?) {
        setViewOutline(optionButton,selectedOption == optionId, correctOption == optionId )
        setProgressBarColor(progressBar, selectedOption == optionId, correctOption == optionId, percentage)
    }

    private fun setViewOutline(view: View, isUserSelected: Boolean, isCorrect: Boolean) {
        if (isCorrect) updateViewBackground(view, R.drawable.button_correct_answer_outline)
        else if (isUserSelected) updateViewBackground(view, R.drawable.button_wrong_answer_outline )

    }

    private fun setProgressBarColor(progressBar: ProgressBar, isUserSelected: Boolean, isCorrect: Boolean, percentage: Int) {
        when {
            isCorrect -> updateProgressBar(progressBar, R.drawable.progress_bar_user_correct, percentage)
            isUserSelected -> updateProgressBar(progressBar, R.drawable.progress_bar_user_selection_wrong, percentage)
            else -> updateProgressBar(progressBar, R.drawable.progress_bar_wrong_option, percentage)
        }
    }

    fun startResultAnimation(
        isCorrect: Boolean,
        prediction_result: LottieAnimationView
    ) {
        lottieAnimationPath = findResultAnimationPath(isCorrect)
        viewAnimation.startResultAnimation(lottieAnimationPath, context, prediction_result)
    }

    private fun findResultAnimationPath(isCorrect: Boolean): String {
        return if (isCorrect)
            correctAnswerLottieFilePath
        else wrongAnswerLottieFilePath
    }

    private fun updateProgressBar(progressBar: ProgressBar, drawable: Int, percentage: Int) {
        progressBar.apply {
            progressDrawable = AppCompatResources.getDrawable(context, drawable)
            visibility = View.VISIBLE
            progress = percentage
        }
    }

    private fun updateViewBackground(view: View, drawable: Int) {
        view.background = AppCompatResources.getDrawable(context, drawable)
    }

    fun updatePercentageText(percentageText: TextView, option: VoteOption) {
        percentageText.apply {
            visibility = View.VISIBLE
            text = option.votePercentage.toString().plus("%")
        }
    }

    fun setImageViewMargin(option: VoteOption, optionList: List<VoteOption>, view: View) {
        if (option == optionList.last()) {
            val params = view.layoutParams as RecyclerView.LayoutParams
            view.layoutParams = params
        } else {
            val params = view.layoutParams as RecyclerView.LayoutParams
            params.marginEnd = AndroidResource.dpToPx(5)
            view.layoutParams = params
        }
    }

    fun setImageItemWidth(optionList: List<VoteOption>, view: View, parentWidth: Int) {
        if (optionList.size > 2)
            view.layoutParams.width = ((parentWidth / 2.5).toInt())
        else view.layoutParams.width = parentWidth / 2
    }
}