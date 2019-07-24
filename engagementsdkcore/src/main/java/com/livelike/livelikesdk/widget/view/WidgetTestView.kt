package com.livelike.livelikesdk.widget.view

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.widget.FrameLayout
import com.google.gson.JsonObject
import com.livelike.engagementsdkapi.WidgetInfos
import com.livelike.livelikesdk.EngagementSDK
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.services.analytics.MockAnalyticsService
import com.livelike.livelikesdk.utils.gson
import com.livelike.livelikesdk.widget.viewModel.AlertWidgetViewModel
import com.livelike.livelikesdk.widget.viewModel.PollViewModel
import com.livelike.livelikesdk.widget.viewModel.PredictionViewModel
import com.livelike.livelikesdk.widget.viewModel.QuizViewModel
import kotlinx.android.synthetic.main.widget_alert.view.alertWidget
import kotlinx.android.synthetic.main.widget_test_view.view.buttonRefresh
import kotlinx.android.synthetic.main.widget_test_view.view.testFirst
import kotlinx.android.synthetic.main.widget_test_view.view.testFourth
import kotlinx.android.synthetic.main.widget_test_view.view.testSecond
import kotlinx.android.synthetic.main.widget_test_view.view.testThird

class WidgetTestView(context: Context, attr: AttributeSet) : FrameLayout(context, attr) {

    private val mockConfig = EngagementSDK.SdkConfiguration("", "", "","", "", "","", "", "","", "")

    private val textLabels = listOf(
        "NEW RECORD",
        "SPONSOR",
        "UPCOMING",
        "",
        "NBA")

    private val textTitle = listOf(
        "IS THIS ELI MANNING'S LAST GAME WITH THE GIANTS?",
        "WHO IS YOUR MVP THIS YEAR?",
        "WHICH WAS YOUR TEAM LAST YEAR?",
        "WHAT KIND OF FAN ARE YOU?",
        "WHO WILL BE THE MAN OF THE MATCH?",
        "WHO HOLDS THE RECORD FOR MOST TOUCHDOWNS IN NFL HISTORY?")

    private val textOptions = listOf(
        "Who? Is he still playing?",
        "For sure! He's done.",
        "Tom Brady",
        "For sure! He's done.",
        "He still has a lot left in the tank!",
        "I'm not interested...")

    private val bodyOptions = listOf(
        "Super Bowl halftime show left shark by Katy Perry",
        "",
        "Dirk Koetter stays with Ryan Fitzpatrick to save season, but is it realistic?",
        "Manning reaches 500 TD passes in Broncos' 38-24 win")

    private val linkOptions = listOf(
        "",
        "https://google.com")

    private val linkLabelOptions= listOf(
        "Click Here",
        "Enjoy more Here",
        "Do you want more?")

    private val imageUrlOption= listOf(
        "https://picsum.photos/150/150?",
        "")

    var imageUrl : ()->String = {""}


    private val dataAlert  =
        {"""
            {"timeout": "P0DT00H00M10S",
              "kind": "alert",
              "program_date_time": null,
              "title": "${textLabels.random()}",
              "text": "${bodyOptions.random()}",
              "image_url": "https://picsum.photos/150/100?${java.util.UUID.randomUUID()}",
              "link_url": "${linkOptions.random()}",
              "link_label": "${linkLabelOptions.random()}"}
        """}

    private val pollTextData =
        {"""{"timeout":"P0DT00H00M10S","kind":"text-poll","program_date_time":null,"subscribe_channel":"","question":"${textTitle.random()}","options":[{"description":"${textOptions.random()}","image_url":"${imageUrl()}","vote_count":0,"vote_url":""},{"description":"${textOptions.random()}","image_url":"${imageUrl()}","vote_count":0,"vote_url":""}]}"""}

    private val quizTextData  =
        {"""{"timeout":"P0DT00H00M10S","kind":"text-quiz","program_date_time":null,"subscribe_channel":"","question":"${textTitle.random()}","choices":[{"image_url":"${imageUrl()}", "description":"${textOptions.random()}","is_correct":false,"answer_url":"","answer_count":0},{"image_url":"${imageUrl()}", "description":"${textOptions.random()}","is_correct":false,"answer_url":"","answer_count":0}]}"""}

    private val  predictionTextData =
        {"""{"timeout":"P0DT00H00M10S","kind":"text-prediction","program_date_time":null,"subscribe_channel":"","question":"${textTitle.random()}","confirmation_message":"${textOptions.random()}","options":[{"image_url":"${imageUrl()}", "url":"","description":"${textOptions.random()}","is_correct":false,"vote_count":0,"vote_url":""},{"image_url":"${imageUrl()}", "url":"","description":"${textOptions.random()}","is_correct":false,"vote_count":0,"vote_url":""}]}"""}

    init {
        ConstraintLayout.inflate(context, R.layout.widget_test_view, this)

        buttonRefresh.setOnClickListener {
            cleanupViews()
            addWidgetViews()
        }
    }

    private fun cleanupViews(){
        testFirst.removeAllViews()
        testSecond.removeAllViews()
        testThird.removeAllViews()
        testFourth.removeAllViews()
    }

    private fun addWidgetViews(){
        val randomImage = imageUrlOption.random()
        imageUrl = {if(randomImage.isEmpty()) randomImage else randomImage+java.util.UUID.randomUUID()}
        val viewAlert = AlertWidgetView(context).apply {
            val info = WidgetInfos("alert-created", gson.fromJson(dataAlert(), JsonObject::class.java) , "120571e0-d665-4e9b-b497-908cf8422a64")
            widgetViewModel = AlertWidgetViewModel(info, {}, MockAnalyticsService())
        }
        val viewPoll = PollView(context).apply {
            val info = WidgetInfos("text-poll-created", gson.fromJson(pollTextData(), JsonObject::class.java) , "120571e0-d665-4e9b-b497-908cf8422a64")
            widgetViewModel = PollViewModel(info, {}, MockAnalyticsService(), mockConfig)
        }
        val viewQuiz = QuizView(context).apply {
            val info = WidgetInfos("text-quiz-created", gson.fromJson(quizTextData(), JsonObject::class.java) , "120571e0-d665-4e9b-b497-908cf8422a64")
            widgetViewModel = QuizViewModel(info, {}, MockAnalyticsService(), mockConfig, context)
        }
        val viewPrediction = PredictionView(context).apply {
            val info = WidgetInfos("text-prediction-created", gson.fromJson(predictionTextData(), JsonObject::class.java) , "120571e0-d665-4e9b-b497-908cf8422a64")
            widgetViewModel = PredictionViewModel(info, {}, context, MockAnalyticsService())
        }

        testFirst.addView(viewPrediction)
        testSecond.addView(viewPoll)
        testThird.addView(viewQuiz)
        testFourth.addView(viewAlert)
    }
}