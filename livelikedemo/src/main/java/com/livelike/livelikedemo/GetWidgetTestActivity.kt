package com.livelike.livelikedemo

import android.content.DialogInterface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import com.livelike.engagementsdk.LiveLikeWidget
import com.livelike.engagementsdk.WidgetStatus
import com.livelike.engagementsdk.WidgetsRequestParameters
import com.livelike.engagementsdk.chat.data.remote.LiveLikePagination
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.widget.UnsupportedWidgetType
import com.livelike.engagementsdk.widget.WidgetType
import kotlinx.android.synthetic.main.activity_get_widget_test.get_widget_status_filters
import kotlinx.android.synthetic.main.activity_get_widget_test.get_widget_type_filters
import kotlinx.android.synthetic.main.activity_get_widget_test.run_filter_button

class GetWidgetTestActivity : AppCompatActivity() {

    val stateToViewLookup: MutableMap<WidgetStatus, SwitchCompat> = mutableMapOf()
    val typeToViewLookup: MutableMap<WidgetType, SwitchCompat> = mutableMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_get_widget_test)

        var channelManager = (application as LiveLikeApplication).channelManager
        var session = (application as LiveLikeApplication).createPublicSession(
            channelManager.selectedChannel.llProgram.toString(), allowTimeCodeGetter = false
        )

        WidgetStatus.values().forEach {
            val switch = SwitchCompat(this)
            switch.text = it.parameterValue
            get_widget_status_filters.addView( switch )
            stateToViewLookup[it] = switch
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
                    stateToViewLookup.filterValues { it.isChecked }.keys
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
                    "type: ${widget.getWidgetType()}, status: ${widget.status}"
                }.toTypedArray(), DialogInterface.OnClickListener { _, _ ->  })
                .create()
                .show()
        }
    }
}