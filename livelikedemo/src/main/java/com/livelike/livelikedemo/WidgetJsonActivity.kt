package com.livelike.livelikedemo

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.livelike.engagementsdk.LiveLikeWidget
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.widget.widgetModel.PollWidgetModel
import com.livelike.livelikedemo.customwidgets.CustomPollWidget
import kotlinx.android.synthetic.main.activity_widget_json.button3
import kotlinx.android.synthetic.main.activity_widget_json.editTextTextPersonName
import kotlinx.android.synthetic.main.activity_widget_json.editTextTextPersonName2
import kotlinx.android.synthetic.main.activity_widget_json.frame_widget

class WidgetJsonActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_widget_json)

        (application as LiveLikeApplication).channelManager.let { manager ->
            val session = (application as LiveLikeApplication).createPublicSession(
                manager.selectedChannel.llProgram.toString()
            )
            editTextTextPersonName.setText("1bc4353a-d189-4acc-b1cb-e0874348850a")

            button3.setOnClickListener {
                val widgetId = editTextTextPersonName.text.toString()
                val widgetKind = editTextTextPersonName2.text.toString()
                (application as LiveLikeApplication).sdk.fetchWidgetDetails(widgetId,
                    widgetKind,
                    object : LiveLikeCallback<LiveLikeWidget>() {
                        override fun onResponse(result: LiveLikeWidget?, error: String?) {
                            result?.let {
                                val widgetModel = session.getWidgetModelFromLiveLikeWidget(it)
                                val widget = CustomPollWidget(applicationContext)
                                widget.pollWidgetModel = widgetModel as PollWidgetModel
                                frame_widget.removeAllViews()
                                frame_widget.addView(widget)
                            }
                            error?.let {
                                Toast.makeText(applicationContext, it, Toast.LENGTH_SHORT).show()
                            }
                        }
                    })
            }
        }
    }
}