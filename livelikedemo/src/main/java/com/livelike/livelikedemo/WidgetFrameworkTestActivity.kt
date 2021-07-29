package com.livelike.livelikedemo

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.livelike.engagementsdk.LiveLikeWidget
import com.livelike.engagementsdk.chat.data.remote.LiveLikePagination
import com.livelike.engagementsdk.core.services.messaging.proxies.LiveLikeWidgetEntity
import com.livelike.engagementsdk.core.services.messaging.proxies.WidgetLifeCycleEventsListener
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.widget.viewModel.WidgetStates
import com.livelike.livelikedemo.utils.DialogUtils
import com.livelike.livelikedemo.utils.ThemeRandomizer
import kotlinx.android.synthetic.main.activity_widget_framework.current_state_text_view
import kotlinx.android.synthetic.main.activity_widget_framework.input_widget_json
import kotlinx.android.synthetic.main.activity_widget_framework.move_to_next_state
import kotlinx.android.synthetic.main.activity_widget_framework.radio_finished
import kotlinx.android.synthetic.main.activity_widget_framework.radio_interaction
import kotlinx.android.synthetic.main.activity_widget_framework.radio_ready
import kotlinx.android.synthetic.main.activity_widget_framework.radio_result
import kotlinx.android.synthetic.main.activity_widget_framework.show_my_widget
import kotlinx.android.synthetic.main.activity_widget_framework.show_widget
import kotlinx.android.synthetic.main.activity_widget_framework.txt_widget_interact_listener
import kotlinx.android.synthetic.main.activity_widget_framework.widget_view

class WidgetFrameworkTestActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_widget_framework)

        var json =
            "{ \"id\": \"65301dde-17cc-4d13-95b9-989fb20842cb\", \"url\": \"https://cf-blast-staging.livelikecdn.com/api/v1/text-polls/65301dde-17cc-4d13-95b9-989fb20842cb/\", \"kind\": \"text-poll\", \"program_id\": \"b4dd284b-45bf-45c7-ba95-10416e370cea\", \"created_at\": \"2020-05-01T07:21:19.578Z\", \"published_at\": null, \"scheduled_at\": null, \"question\": \"sas\", \"timeout\": \"P0DT00H00M25S\", \"options\": [{ \"id\": \"ea859e7b-1c44-4b0c-aeef-154efe9d2af9\", \"description\": \"sas\", \"vote_count\": 0, \"vote_url\": \"https://blastrt-staging-us-east-1.livelikecdn.com/services/interactions/text-poll/65301dde-17cc-4d13-95b9-989fb20842cb/eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJvcHRpb25zIjpbImVhODU5ZTdiLTFjNDQtNGIwYy1hZWVmLTE1NGVmZTlkMmFmOSIsImU5ZDBhZTk2LWJmODItNDkzOC1hZTMwLWFkMWM2NDMzMTQ4NiJdLCJvcHRpb25fdXVpZCI6ImVhODU5ZTdiLTFjNDQtNGIwYy1hZWVmLTE1NGVmZTlkMmFmOSIsInJld2FyZHMiOltdLCJpc3MiOiJibGFzdCIsImlhdCI6MTU4ODMxNzY4MX0.OpEoVL4hg6cmTnKX_Xme5FELkd5LjgJY0Kq0k27RDMk\", \"translations\": {}, \"translatable_fields\": [\"description\"] }, { \"id\": \"e9d0ae96-bf82-4938-ae30-ad1c64331486\", \"description\": \"sas\", \"vote_count\": 0, \"vote_url\": \"https://blastrt-staging-us-east-1.livelikecdn.com/services/interactions/text-poll/65301dde-17cc-4d13-95b9-989fb20842cb/eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJvcHRpb25zIjpbImVhODU5ZTdiLTFjNDQtNGIwYy1hZWVmLTE1NGVmZTlkMmFmOSIsImU5ZDBhZTk2LWJmODItNDkzOC1hZTMwLWFkMWM2NDMzMTQ4NiJdLCJvcHRpb25fdXVpZCI6ImU5ZDBhZTk2LWJmODItNDkzOC1hZTMwLWFkMWM2NDMzMTQ4NiIsInJld2FyZHMiOltdLCJpc3MiOiJibGFzdCIsImlhdCI6MTU4ODMxNzY4MX0.bJIRnWgmrqZ0LP0sXFNTR1tTjJI1zdbpkKpr6s3B2ZU\", \"translations\": {}, \"translatable_fields\": [\"description\"] }], \"subscribe_channel\": \"widget.text-poll.65301dde-17cc-4d13-95b9-989fb20842cb\", \"program_date_time\": \"2020-05-01T07:21:03.396000Z\", \"publish_delay\": \"P0DT00H00M00S\", \"interaction_url\": \"https://cf-blast-staging.livelikecdn.com/api/v1/text-polls/65301dde-17cc-4d13-95b9-989fb20842cb/interactions/\", \"impression_url\": \"https://s93qlqrqw5.execute-api.us-east-1.amazonaws.com/services/impressions/text-poll/65301dde-17cc-4d13-95b9-989fb20842cb\", \"impression_count\": 0, \"unique_impression_count\": 0, \"engagement_count\": 0, \"engagement_percent\": \"0.000\", \"status\": \"pending\", \"created_by\": { \"id\": \"0cdffb74-e382-41f2-bb81-a5a9dba67888\", \"name\": \"Shivansh Mittal\", \"image_url\": \"https://cf-blast-storage-staging.livelikecdn.com/assets/e1ad9075-b59d-4825-9d23-17cdfb351a9a.png\" }, \"schedule_url\": \"https://cf-blast-staging.livelikecdn.com/api/v1/text-polls/65301dde-17cc-4d13-95b9-989fb20842cb/schedule/\", \"rewards_url\": null, \"translations\": {}, \"translatable_fields\": [\"question\"], \"reactions\": [], \"custom_data\": null }"

        input_widget_json.setText(json)

        val myWidgetsList: ArrayList<LiveLikeWidget> = GsonBuilder().create()
            .fromJson(
                getSharedPreferences(PREFERENCES_APP_ID, Context.MODE_PRIVATE).getString(
                    PREF_MY_WIDGETS,
                    null
                ),
                object : TypeToken<List<LiveLikeWidget>>() {}.type
            ) ?: arrayListOf()
        show_my_widget.setOnClickListener {
            val channelManager = (application as LiveLikeApplication).channelManager
            val channel = channelManager.selectedChannel
            val session = (application as LiveLikeApplication).createPublicSession(
                channel.llProgram.toString(),
                null
            )
            session.getPublishedWidgets(
                LiveLikePagination.FIRST,
                object : LiveLikeCallback<List<LiveLikeWidget>>() {
                    override fun onResponse(
                        result: List<LiveLikeWidget>?,
                        error: String?
                    ) {
                        error?.let {
                            Toast.makeText(applicationContext, "$it", Toast.LENGTH_SHORT).show()
                        }
                        result?.map { it!! }?.let { list ->
                            DialogUtils.showMyWidgetsDialog(
                                this@WidgetFrameworkTestActivity,
                                (application as LiveLikeApplication).sdk,
                                ArrayList(list),
                                object : LiveLikeCallback<LiveLikeWidget>() {
                                    override fun onResponse(
                                        result: LiveLikeWidget?,
                                        error: String?
                                    ) {
                                        result?.let {
                                            widget_view.displayWidget(
                                                (application as LiveLikeApplication).sdk,
                                                result
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            )
        }
        show_widget.setOnClickListener {
            try {
                val jsonObject =
                    JsonParser.parseString(input_widget_json.text.toString()).asJsonObject
                widget_view.displayWidget((application as LiveLikeApplication).sdk, jsonObject)
            } catch (ex: Exception) {
                Toast.makeText(this, "Invalid json", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
        }

        move_to_next_state.setOnClickListener {
            widget_view.moveToNextState()
        }
        widget_view.enableDefaultWidgetTransition = false
        widget_view.widgetLifeCycleEventsListener = object : WidgetLifeCycleEventsListener() {
            override fun onWidgetPresented(widgetData: LiveLikeWidgetEntity) {
            }

            override fun onWidgetInteractionCompleted(widgetData: LiveLikeWidgetEntity) {
            }

            override fun onWidgetDismissed(widgetData: LiveLikeWidgetEntity) {
            }

            override fun onWidgetStateChange(
                state: WidgetStates,
                widgetData: LiveLikeWidgetEntity
            ) {
                current_state_text_view.text = "Current State : ${state.name}"
                when (state) {
                    WidgetStates.FINISHED -> {
                        radio_finished.isChecked = true
                    }
                    WidgetStates.RESULTS -> {
                        radio_result.isChecked = true
                    }
                    WidgetStates.INTERACTING -> {
                        radio_interaction.isChecked = true
                    }
                    WidgetStates.READY -> {
                        radio_ready.isChecked = true
                    }
                }
            }
            var count = 0
            override fun onUserInteract(widgetData: LiveLikeWidgetEntity) {
                count++
                txt_widget_interact_listener.append("$count, ${widgetData.kind}\n")
            }
        }

        radio_ready.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                widget_view.setState(WidgetStates.READY)
            }
        }
        radio_interaction.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                widget_view.setState(WidgetStates.INTERACTING)
            }
        }
        radio_result.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                widget_view.setState(WidgetStates.RESULTS)
            }
        }
        radio_finished.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                widget_view.setState(WidgetStates.FINISHED)
            }
        }
        if (ThemeRandomizer.themesList.size > 0) {
            widget_view.applyTheme(ThemeRandomizer.themesList.last())
        }
    }
}
