package com.mml.mmlengagementsdk.widgets

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v4.content.ContextCompat
import android.view.View
import com.bumptech.glide.Glide
import com.example.mmlengagementsdk.R
import com.livelike.engagementsdk.widget.widgetModel.AlertWidgetModel
import com.mml.mmlengagementsdk.widgets.utils.getFormattedTime
import com.mml.mmlengagementsdk.widgets.utils.parseDuration
import com.mml.mmlengagementsdk.widgets.utils.setCustomFontWithTextStyle
import kotlinx.android.synthetic.main.mml_alert_widget.view.btn_link
import kotlinx.android.synthetic.main.mml_alert_widget.view.img_alert
import kotlinx.android.synthetic.main.mml_alert_widget.view.time_bar
import kotlinx.android.synthetic.main.mml_alert_widget.view.txt_description
import kotlinx.android.synthetic.main.mml_alert_widget.view.txt_time
import kotlinx.android.synthetic.main.mml_alert_widget.view.txt_title
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay

class MMLAlertWidget(context: Context) : ConstraintLayout(context) {
    private val job = SupervisorJob()
    private val uiScope = CoroutineScope(Dispatchers.Main + job)
    var isTimeLine: Boolean = false
    lateinit var alertModel: AlertWidgetModel

    init {
        inflate(context, R.layout.mml_alert_widget, this)
    }


    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        alertModel.widgetData.let { liveLikeWidget ->

            txt_title.text = liveLikeWidget.title
            setCustomFontWithTextStyle(txt_title, "fonts/RingsideExtraWide-Black.otf")
            txt_description.text = liveLikeWidget.text
            setCustomFontWithTextStyle(txt_description, "fonts/RingsideRegular-Book.otf")
            liveLikeWidget.imageUrl?.let {
                img_alert.visibility = View.VISIBLE
                Glide.with(context)
                    .load(it)
                    .into(img_alert)
            }
            liveLikeWidget.createdAt?.let {
                setCustomFontWithTextStyle(txt_time, "fonts/RingsideRegular-Book.otf")
                txt_time.text = getFormattedTime(it)
            }
            liveLikeWidget.linkLabel?.let {
                btn_link.visibility = View.VISIBLE
                btn_link.text = it
                liveLikeWidget.linkUrl?.let {
                    btn_link.setOnClickListener {
                        val universalLinkIntent =
                            Intent(Intent.ACTION_VIEW, Uri.parse(liveLikeWidget.linkUrl)).setFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK
                            )
                        if (universalLinkIntent.resolveActivity(context.packageManager) != null) {
                            ContextCompat.startActivity(context, universalLinkIntent, Bundle.EMPTY)
                        }
                    }
                }
            }
            if (isTimeLine) {
                time_bar.visibility = View.INVISIBLE
            } else {
                val timeMillis = liveLikeWidget.timeout?.parseDuration() ?: 5000
                time_bar.visibility = View.VISIBLE
                time_bar.startTimer(timeMillis)
                uiScope.async {
                    delay(timeMillis)
                    alertModel.finish()
                }
            }
        }
    }


}