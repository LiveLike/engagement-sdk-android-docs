package com.livelike.livelikedemo.mml.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.livelike.engagementsdk.widget.LiveLikeWidgetViewFactory
import com.livelike.engagementsdk.widget.widgetModel.AlertWidgetModel
import com.livelike.engagementsdk.widget.widgetModel.CheerMeterWidgetmodel
import com.livelike.engagementsdk.widget.widgetModel.FollowUpWidgetViewModel
import com.livelike.engagementsdk.widget.widgetModel.ImageSliderWidgetModel
import com.livelike.engagementsdk.widget.widgetModel.PollWidgetModel
import com.livelike.engagementsdk.widget.widgetModel.PredictionWidgetViewModel
import com.livelike.engagementsdk.widget.widgetModel.QuizWidgetModel
import com.livelike.livelikedemo.R
import com.livelike.livelikedemo.mml.MMLActivity
import com.livelike.livelikedemo.mml.widgets.MMLAlertWidget
import com.livelike.livelikedemo.mml.widgets.MMLCheerMeterWidget
import com.livelike.livelikedemo.mml.widgets.MMLImageSliderWidget
import com.livelike.livelikedemo.mml.widgets.MMLPollWidget
import com.livelike.livelikedemo.mml.widgets.MMLQuizWidget
import kotlinx.android.synthetic.main.fragment_widgets.widget_view

class WidgetsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_widgets, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        (activity as MMLActivity).session.let {
            println("WidgetsFragment.onActivityCreated>>>>")
            widget_view.setSession(it)
            widget_view.widgetViewFactory = object : LiveLikeWidgetViewFactory {
                override fun createCheerMeterView(cheerMeterWidgetModel: CheerMeterWidgetmodel): View? {
                    return MMLCheerMeterWidget(context!!).apply {
                        this.cheerMeterWidgetModel = cheerMeterWidgetModel
                    }
                }

                override fun createAlertWidgetView(alertWidgetModel: AlertWidgetModel): View? {
                    return MMLAlertWidget(context!!).apply {
                        this.alertModel = alertWidgetModel
                    }
                }

                override fun createQuizWidgetView(
                    quizWidgetModel: QuizWidgetModel,
                    isImage: Boolean
                ): View? {
                    return MMLQuizWidget(context!!).apply {
                        this.quizWidgetModel = quizWidgetModel
                    }
                }

                override fun createPredictionWidgetView(
                    predictionViewModel: PredictionWidgetViewModel,
                    isImage: Boolean
                ): View? {
                    return null
                }

                override fun createPredictionFollowupWidgetView(
                    followUpWidgetViewModel: FollowUpWidgetViewModel,
                    isImage: Boolean
                ): View? {
                    return null
                }

                override fun createPollWidgetView(
                    pollWidgetModel: PollWidgetModel,
                    isImage: Boolean
                ): View? {
                    return MMLPollWidget(context!!).apply {
                        this.isImage = isImage
                        this.pollWidgetModel = pollWidgetModel
                    }
                }

                override fun createImageSliderWidgetView(imageSliderWidgetModel: ImageSliderWidgetModel): View? {
                    return MMLImageSliderWidget(context!!).apply {
                        this.imageSliderWidgetModel = imageSliderWidgetModel
                    }
                }
            }
        }
    }

}