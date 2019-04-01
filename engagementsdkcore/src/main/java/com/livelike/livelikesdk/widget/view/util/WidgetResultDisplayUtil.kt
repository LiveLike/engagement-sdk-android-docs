package com.livelike.livelikesdk.widget.view.util

import android.content.Context
import android.support.v7.content.res.AppCompatResources
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import com.airbnb.lottie.LottieAnimationView
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.animation.ViewAnimation
import com.livelike.livelikesdk.util.AndroidResource
import com.livelike.livelikesdk.widget.model.VoteOption
import com.livelike.livelikesdk.widget.view.prediction.text.PredictionTextFollowUpWidgetView
import com.livelike.livelikesdk.widget.view.quiz.QuizImageWidget

internal class WidgetResultDisplayUtil(val context: Context, private val viewAnimation: ViewAnimation) {
    private var lottieAnimationPath = ""

    fun updateViewDrawable(option: VoteOption,
                           progressBar: ProgressBar,
                           optionButton: ImageButton,
                           percentage: Int,
                           correctOption: String?,
                           selectedOption: String?,
                           prediction_result: LottieAnimationView) {

        startResultAnimation(correctOption, selectedOption, prediction_result)

        if (hasUserSelectedCorrectOption(selectedOption, correctOption)) {
            if (isCurrentButtonSameAsCorrectOption(correctOption, option.id)) {
                updateProgressBar(progressBar, R.drawable.progress_bar_user_correct, percentage)
                updateImageButton(optionButton, R.drawable.button_correct_answer_outline)
            } else {
                updateProgressBar(progressBar, R.drawable.progress_bar_wrong_option, percentage)
            }
        } else {
            when {
                isCurrentButtonSameAsCorrectOption(correctOption, option.id) -> {
                    updateProgressBar(progressBar, R.drawable.progress_bar_user_correct, percentage)
                    updateImageButton(optionButton, R.drawable.button_correct_answer_outline)
                }
                isCurrentButtonSameAsCorrectOption(selectedOption, option.id) -> {
                    updateProgressBar(progressBar, R.drawable.progress_bar_user_selection_wrong, percentage)
                    updateImageButton(optionButton, R.drawable.button_wrong_answer_outline)
                }
                else -> {
                    updateProgressBar(progressBar, R.drawable.progress_bar_wrong_option, percentage)
                }
            }
        }
    }

    private fun startResultAnimation(
        correctOption: String?,
        selectedOption: String?,
        prediction_result: LottieAnimationView
    ) {
        lottieAnimationPath = findResultAnimationPath(correctOption, selectedOption)
        viewAnimation.startResultAnimation(lottieAnimationPath, context, prediction_result)
    }

    private fun hasUserSelectedCorrectOption(userSelectedOption: String?, correctOption: String?) =
        userSelectedOption == correctOption

    private fun findResultAnimationPath(correctOption: String?, userSelectedOption: String?): String {
        return if (hasUserSelectedCorrectOption(correctOption, userSelectedOption))
            PredictionTextFollowUpWidgetView.correctAnswerLottieFilePath
        else PredictionTextFollowUpWidgetView.wrongAnswerLottieFilePath
    }

    private fun updateProgressBar(progressBar: ProgressBar, drawable: Int, percentage: Int) {
        progressBar.apply {
            progressDrawable = AppCompatResources.getDrawable(context, drawable)
            visibility = View.VISIBLE
            progress = percentage
        }
    }

    private fun updateImageButton(button: ImageButton, drawable: Int) {
        button.background = AppCompatResources.getDrawable(context, drawable)
        button.setOnClickListener(null)
    }

    private fun isCurrentButtonSameAsCorrectOption(correctOption: String?, buttonId: String?) =
        buttonId == correctOption

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
}