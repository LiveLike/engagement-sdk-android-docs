package com.livelike.livelikedemo.channel

import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.ColorDrawable
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import com.livelike.livelikedemo.R
import kotlinx.android.synthetic.main.channel_select_bottom.view.channel_list

class ChannelSelectionView : LinearLayout {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        LayoutInflater.from(context).inflate(R.layout.channel_select_bottom, this)
        orientation = LinearLayout.VERTICAL
    }

    var channelSelectListener: ((Channel) -> Unit)? = null

    var channelList: MutableList<Channel>? = null
        set(channels) {
        this.channel_list.removeAllViews()
            for (channel: Channel in channels ?: return) {
            val channelOption = LayoutInflater.from(context).inflate(R.layout.test_app_button, null) as Button
            channelOption.text = channel.name
            channelOption.setOnClickListener {
                channelSelectListener?.invoke(channel)
            }
            this.channel_list.addView(channelOption)
            val divider = LayoutInflater.from(context).inflate(R.layout.test_app_divider, null)
            val dividerParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1))
            dividerParams.marginStart = dpToPx(16)
            divider.layoutParams = dividerParams
            divider.background = ColorDrawable(ContextCompat.getColor(context, R.color.colorDivider))
            this.channel_list.addView(divider)
        }
        field = channels
    }

    fun dpToPx(dp: Int): Int {
        val scale = Resources.getSystem().displayMetrics.density
        return (dp * scale + 0.5f).toInt()
    }
}