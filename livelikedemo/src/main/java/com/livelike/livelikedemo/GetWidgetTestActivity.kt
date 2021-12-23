package com.livelike.livelikedemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.RadioButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import com.livelike.engagementsdk.EarnableReward
import com.livelike.engagementsdk.LiveLikeWidget
import com.livelike.engagementsdk.OptionReward
import com.livelike.engagementsdk.WidgetStatus
import com.livelike.engagementsdk.WidgetsRequestOrdering
import com.livelike.engagementsdk.WidgetsRequestParameters
import com.livelike.engagementsdk.chat.data.remote.LiveLikePagination
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.widget.UnsupportedWidgetType
import com.livelike.engagementsdk.widget.WidgetType
import kotlinx.android.synthetic.main.activity_get_widget_test.*

class GetWidgetTestActivity : AppCompatActivity() {

    val orderingToViewLookup: MutableMap<WidgetsRequestOrdering, RadioButton> = mutableMapOf()
    val stateToViewLookup: MutableMap<WidgetStatus?, RadioButton> = mutableMapOf()
    val typeToViewLookup: MutableMap<WidgetType, SwitchCompat> = mutableMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_get_widget_test)

        var channelManager = (application as LiveLikeApplication).channelManager
        var session = (application as LiveLikeApplication).createPublicSession(
            channelManager.selectedChannel.llProgram.toString(), allowTimeCodeGetter = false
        )

        stateToViewLookup[null] = RadioButton(this).apply {
            text = "Any"
        }
        get_widget_status_filters.addView( stateToViewLookup[null] )

        WidgetStatus.values().forEach {
            val radioButton = RadioButton(this)
            radioButton.text = it.parameterValue
            get_widget_status_filters.addView( radioButton )
            stateToViewLookup[it] = radioButton
        }

        WidgetsRequestOrdering.values().forEach {
            val radioButton = RadioButton(this)
            radioButton.text = it.name
            get_widget_ordering.addView(radioButton)
            orderingToViewLookup[it] = radioButton
        }

        WidgetType.values().filter {
            //remove unsupported types
            it.declaringClass.getField(it.name).getAnnotation(UnsupportedWidgetType::class.java) == null
        }.forEach {
            val switch = SwitchCompat(this)
            switch.text = it.event
                .replace("-created", "")
                .replace("-updated", "")
            get_widget_type_filters.addView(switch)
            typeToViewLookup[it] = switch
        }

        run_filter_button.setOnClickListener {

            session.getWidgets(
                LiveLikePagination.FIRST,
                WidgetsRequestParameters(
                    typeToViewLookup.filterValues { it.isChecked }.keys,
                    stateToViewLookup.filterValues { it.isChecked }.keys.let {
                        if ( it.isEmpty() ){
                            null
                        } else {
                            it.first()
                        }
                    },
                    orderingToViewLookup.filterValues { it.isChecked }.keys.let{
                        if ( !it.isEmpty() ){
                            it.first()
                        } else {
                            null
                        }
                    },
                    interactive = radio_btn_true.isChecked
                ),
                object : LiveLikeCallback<List<LiveLikeWidget>>() {
                    override fun onResponse(result: List<LiveLikeWidget>?, error: String?) {
                        buildResultDialog(result, error)
                    }
                }
            )
        }

    }

    private fun buildResultDialog(result: List<LiveLikeWidget>?, error: String?) {
        error?.let {
            AlertDialog.Builder( this)
                .setMessage(it)
                .create()
                .show()
        }
        result?.let{
            AlertDialog.Builder ( this )
                .setItems( it.map { widget ->
                    "type: ${widget.getWidgetType()}, status: ${widget.status}, interactiveUntil : ${widget.interactiveUntil}"
                }.toTypedArray()) { dialog, which ->
                    dialog.dismiss()
                    showEarnedRewards(result[which])
                }
                .create()
                .show()
        }
    }

    private fun showEarnedRewards(likeLikeWidget: LiveLikeWidget) {
        val items: Array<String> = likeLikeWidget.earnableRewards
            ?.map(EarnableReward::toString)
            ?.toTypedArray()
            ?.plus(
                likeLikeWidget.options?.flatMap { option ->
                    option?.earnableRewards?.map(OptionReward::toString) ?: emptyList()
                } ?: emptyList()
            ) ?: emptyArray()

        AlertDialog.Builder( this)
            .setItems( items ){ _,_ -> }
            .create()
            .show()
    }
}