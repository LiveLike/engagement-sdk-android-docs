package com.livelike.livelikesdk.widget.view.prediction.text

import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import android.widget.ImageView
import com.livelike.livelikesdk.R
import com.livelike.livelikesdk.animation.ViewAnimation
import com.livelike.livelikesdk.widget.model.VoteOption
import kotlinx.android.synthetic.main.confirm_message.view.*

internal class PredictionTextFollowUpWidgetView :
    TextOptionWidgetBase {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)


    private lateinit var viewAnimation: ViewAnimation
    private var timeout: Long = 7000

    override fun initialize(dismiss: () -> Unit, timeout: Long, parentWidth: Int) {
        super.initialize(dismiss, timeout, parentWidth)
        showResults = true
        buttonClickEnabled = false
        pieTimerViewStub.layoutResource = R.layout.cross_image
        pieTimerViewStub.inflate()
        val imageView = findViewById<ImageView>(R.id.prediction_followup_image_cross)
        imageView.setImageResource(R.mipmap.widget_ic_x)
        imageView.setOnClickListener { dismissWidget() }
        viewAnimation = ViewAnimation(this)
        this.timeout = timeout
    }

    override fun optionListUpdated(
        voteOptions: List<VoteOption>,
        optionSelectedCallback: (String?) -> Unit,
        correctOptionWithUserSelection: Pair<String?, String?>) {
        super.optionListUpdated(voteOptions, optionSelectedCallback, correctOptionWithUserSelection)
        super.showResultsAnimation(correctOptionWithUserSelection)
        Handler().postDelayed({ dismissWidget?.invoke() }, timeout)
    }
}
