package com.livelike.livelikedemo.customwidgets

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import com.livelike.engagementsdk.LiveLikeWidget
import com.livelike.engagementsdk.widget.viewModel.LiveLikeWidgetMediator
import com.livelike.livelikedemo.R

/**
 * TODO: document your custom view class.
 */
class CustomCheerMeter : ConstraintLayout {


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
        inflate(context, R.layout.custom_cheer_meter, this@CustomCheerMeter)
    }

    fun setLiveLikeData(
        widgetData: LiveLikeWidget,
        widgetMediator: LiveLikeWidgetMediator
    ) {

    }

}
