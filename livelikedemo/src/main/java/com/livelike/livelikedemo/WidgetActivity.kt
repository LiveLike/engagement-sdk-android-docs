package com.livelike.livelikedemo

import EngagementViewModelFactory
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.livelike.engagementsdk.LiveLikeWidget
import com.livelike.engagementsdk.WidgetListener
import kotlinx.android.synthetic.main.activity_widget.widget_view
import widgetViewModel

class WidgetActivity : AppCompatActivity() {

    var mainViewModel: widgetViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_widget)

        // to get intent received from MainActivity

        // This will create an instance of Engagement viewmodel which can be used to creating session and initialization
        mainViewModel = ViewModelProvider(
            this,
            EngagementViewModelFactory(this.application)
        ).get(widgetViewModel::class.java)
        // Check whether chat or widget is selected
        println("WidgetActivity.onCreate->$mainViewModel")
        mainViewModel!!.getSession()?.let { widget_view.setSession(it) }
        widget_view.setWidgetListener(object : WidgetListener {
            override fun onNewWidget(liveLikeWidget: LiveLikeWidget) {
                println("WidgetActivity.onNewWidget->${liveLikeWidget.kind}")
            }
        })
        // Example of Widget Interceptor showing a dialog
//        val interceptor = object : WidgetInterceptor() {
//            override fun widgetWantsToShow(widgetData: LiveLikeWidgetEntity) {
//                AlertDialog.Builder(this@WidgetActivity).apply {
//                    setMessage("You received a Widget, what do you want to do?")
//                    setPositiveButton("Show") { _, _ ->
//                        showWidget() // Releases the widget
//                    }
//                    setNegativeButton("Dismiss") { _, _ ->
//                        dismissWidget() // Discards the widget
//                    }
//                    create()
//                }.show()
//            }
//        }


        // You just need to add it on your session instance
        // mainViewModel?.getSession()?.widgetInterceptor = interceptor

    }

    override fun onPause() {
        super.onPause()
        mainViewModel?.pauseSession()
    }

    override fun onResume() {
        super.onResume()
        mainViewModel?.resumeSession()
    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        mainViewModel?.closeSession()
//    }


}

