package com.livelike.livelikedemo.customwidgets

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import com.bumptech.glide.Glide
import com.livelike.engagementsdk.widget.widgetModel.AlertWidgetModel
import com.livelike.livelikedemo.R
import kotlinx.android.synthetic.main.custom_alert_widget.view.bodyImage
import kotlinx.android.synthetic.main.custom_alert_widget.view.bodyText


class CustomAlertWidget : ConstraintLayout{


    lateinit var alertModel: AlertWidgetModel

    constructor(context: Context) : super(context) {
        init(null, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(attrs, defStyle)
    }

    private fun init(attrs: AttributeSet?, defStyle: Int) {
        inflate(context, R.layout.custom_alert_widget, this)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        bodyText.text = alertModel.widgetData.title
        alertModel.widgetData.imageUrl?.let {
            Glide.with(context)
                .load(it)
                .into(bodyImage)
        }
    }



}