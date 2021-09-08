package com.livelike.livelikedemo

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.livelike.engagementsdk.LiveLikeWidget
import com.livelike.engagementsdk.core.services.messaging.proxies.LiveLikeWidgetEntity
import com.livelike.engagementsdk.core.services.messaging.proxies.WidgetLifeCycleEventsListener
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.widget.LiveLikeWidgetViewFactory
import com.livelike.engagementsdk.widget.data.models.CheerMeterUserInteraction
import com.livelike.engagementsdk.widget.data.models.EmojiSliderUserInteraction
import com.livelike.engagementsdk.widget.data.models.PollWidgetUserInteraction
import com.livelike.engagementsdk.widget.data.models.PredictionWidgetUserInteraction
import com.livelike.engagementsdk.widget.data.models.QuizWidgetUserInteraction
import com.livelike.engagementsdk.widget.data.models.WidgetUserInteractionBase
import com.livelike.engagementsdk.widget.viewModel.WidgetStates
import com.livelike.engagementsdk.widget.widgetModel.AlertWidgetModel
import com.livelike.engagementsdk.widget.widgetModel.CheerMeterWidgetmodel
import com.livelike.engagementsdk.widget.widgetModel.FollowUpWidgetViewModel
import com.livelike.engagementsdk.widget.widgetModel.ImageSliderWidgetModel
import com.livelike.engagementsdk.widget.widgetModel.NumberPredictionWidgetModel
import com.livelike.engagementsdk.widget.widgetModel.PollWidgetModel
import com.livelike.engagementsdk.widget.widgetModel.PredictionWidgetViewModel
import com.livelike.engagementsdk.widget.widgetModel.QuizWidgetModel
import com.livelike.engagementsdk.widget.widgetModel.TextAskWidgetModel
import com.livelike.engagementsdk.widget.widgetModel.VideoAlertWidgetModel
import com.livelike.livelikedemo.customwidgets.CustomNumberPredictionWidget
import com.livelike.livelikedemo.utils.ThemeRandomizer
import kotlinx.android.synthetic.main.activity_widget_framework.btn_fetch
import kotlinx.android.synthetic.main.activity_widget_framework.current_state_text_view
import kotlinx.android.synthetic.main.activity_widget_framework.ed_widget_id
import kotlinx.android.synthetic.main.activity_widget_framework.ed_widget_kind
import kotlinx.android.synthetic.main.activity_widget_framework.input_widget_json
import kotlinx.android.synthetic.main.activity_widget_framework.move_to_next_state
import kotlinx.android.synthetic.main.activity_widget_framework.radio_finished
import kotlinx.android.synthetic.main.activity_widget_framework.radio_interaction
import kotlinx.android.synthetic.main.activity_widget_framework.radio_ready
import kotlinx.android.synthetic.main.activity_widget_framework.radio_result
import kotlinx.android.synthetic.main.activity_widget_framework.show_my_widget
import kotlinx.android.synthetic.main.activity_widget_framework.show_widget
import kotlinx.android.synthetic.main.activity_widget_framework.txt_result
import kotlinx.android.synthetic.main.activity_widget_framework.txt_widget_interact_listener
import kotlinx.android.synthetic.main.activity_widget_framework.widget_view
import kotlinx.android.synthetic.main.activity_widget_framework.widget_view_fetch

class WidgetFrameworkTestActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_widget_framework)

        /* var json =
             "{ \"id\": \"65301dde-17cc-4d13-95b9-989fb20842cb\", \"url\": \"https://cf-blast-staging.livelikecdn.com/api/v1/text-polls/65301dde-17cc-4d13-95b9-989fb20842cb/\", \"kind\": \"text-poll\", \"program_id\": \"b4dd284b-45bf-45c7-ba95-10416e370cea\", \"created_at\": \"2020-05-01T07:21:19.578Z\", \"published_at\": null, \"scheduled_at\": null, \"question\": \"sas\", \"timeout\": \"P0DT00H00M25S\", \"options\": [{ \"id\": \"ea859e7b-1c44-4b0c-aeef-154efe9d2af9\", \"description\": \"sas\", \"vote_count\": 0, \"vote_url\": \"https://blastrt-staging-us-east-1.livelikecdn.com/services/interactions/text-poll/65301dde-17cc-4d13-95b9-989fb20842cb/eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJvcHRpb25zIjpbImVhODU5ZTdiLTFjNDQtNGIwYy1hZWVmLTE1NGVmZTlkMmFmOSIsImU5ZDBhZTk2LWJmODItNDkzOC1hZTMwLWFkMWM2NDMzMTQ4NiJdLCJvcHRpb25fdXVpZCI6ImVhODU5ZTdiLTFjNDQtNGIwYy1hZWVmLTE1NGVmZTlkMmFmOSIsInJld2FyZHMiOltdLCJpc3MiOiJibGFzdCIsImlhdCI6MTU4ODMxNzY4MX0.OpEoVL4hg6cmTnKX_Xme5FELkd5LjgJY0Kq0k27RDMk\", \"translations\": {}, \"translatable_fields\": [\"description\"] }, { \"id\": \"e9d0ae96-bf82-4938-ae30-ad1c64331486\", \"description\": \"sas\", \"vote_count\": 0, \"vote_url\": \"https://blastrt-staging-us-east-1.livelikecdn.com/services/interactions/text-poll/65301dde-17cc-4d13-95b9-989fb20842cb/eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJvcHRpb25zIjpbImVhODU5ZTdiLTFjNDQtNGIwYy1hZWVmLTE1NGVmZTlkMmFmOSIsImU5ZDBhZTk2LWJmODItNDkzOC1hZTMwLWFkMWM2NDMzMTQ4NiJdLCJvcHRpb25fdXVpZCI6ImU5ZDBhZTk2LWJmODItNDkzOC1hZTMwLWFkMWM2NDMzMTQ4NiIsInJld2FyZHMiOltdLCJpc3MiOiJibGFzdCIsImlhdCI6MTU4ODMxNzY4MX0.bJIRnWgmrqZ0LP0sXFNTR1tTjJI1zdbpkKpr6s3B2ZU\", \"translations\": {}, \"translatable_fields\": [\"description\"] }], \"subscribe_channel\": \"widget.text-poll.65301dde-17cc-4d13-95b9-989fb20842cb\", \"program_date_time\": \"2020-05-01T07:21:03.396000Z\", \"publish_delay\": \"P0DT00H00M00S\", \"interaction_url\": \"https://cf-blast-staging.livelikecdn.com/api/v1/text-polls/65301dde-17cc-4d13-95b9-989fb20842cb/interactions/\", \"impression_url\": \"https://s93qlqrqw5.execute-api.us-east-1.amazonaws.com/services/impressions/text-poll/65301dde-17cc-4d13-95b9-989fb20842cb\", \"impression_count\": 0, \"unique_impression_count\": 0, \"engagement_count\": 0, \"engagement_percent\": \"0.000\", \"status\": \"pending\", \"created_by\": { \"id\": \"0cdffb74-e382-41f2-bb81-a5a9dba67888\", \"name\": \"Shivansh Mittal\", \"image_url\": \"https://cf-blast-storage-staging.livelikecdn.com/assets/e1ad9075-b59d-4825-9d23-17cdfb351a9a.png\" }, \"schedule_url\": \"https://cf-blast-staging.livelikecdn.com/api/v1/text-polls/65301dde-17cc-4d13-95b9-989fb20842cb/schedule/\", \"rewards_url\": null, \"translations\": {}, \"translatable_fields\": [\"question\"], \"reactions\": [], \"custom_data\": null }"
 */
        var json = "{\n" +
                "  \"id\": \"079cabc4-b35b-492c-8f1a-ad3091d2b8fb\",\n" +
                "  \"url\": \"https://cf-blast-storage-iconic.livelikecdn.com/api/v1/text-number-predictions/079cabc4-b35b-492c-8f1a-ad3091d2b8fb/\",\n" +
                "  \"kind\": \"text-number-prediction\",\n" +
                "  \"program_id\": \"c0e5e64c-bf08-45a1-919b-b213c1936d08\",\n" +
                "  \"client_id\": \"LT9lUmrzSqXvAL66rSWSGK0weclpFNbHANUTxW9O\",\n" +
                "  \"created_at\": \"2021-09-07T15:51:57.436848Z\",\n" +
                "  \"published_at\": \"2021-09-07T15:52:02.818150Z\",\n" +
                "  \"scheduled_at\": \"2021-09-07T15:51:57.744223Z\",\n" +
                "  \"follow_up_url\": \"https://cf-blast-iconic.livelikecdn.com/api/v1/text-number-prediction-follow-ups/\",\n" +
                "  \"follow_ups\": [\n" +
                "    {\n" +
                "      \"id\": \"14be9af1-a8e7-4695-a4e9-f938e18051a8\",\n" +
                "      \"url\": \"https://cf-blast-iconic.livelikecdn.com/api/v1/text-number-prediction-follow-ups/14be9af1-a8e7-4695-a4e9-f938e18051a8/\",\n" +
                "      \"program_id\": \"c0e5e64c-bf08-45a1-919b-b213c1936d08\",\n" +
                "      \"client_id\": \"LT9lUmrzSqXvAL66rSWSGK0weclpFNbHANUTxW9O\",\n" +
                "      \"text_prediction_id\": \"079cabc4-b35b-492c-8f1a-ad3091d2b8fb\",\n" +
                "      \"text_prediction_url\": \"https://cf-blast-storage-iconic.livelikecdn.com/api/v1/text-number-predictions/079cabc4-b35b-492c-8f1a-ad3091d2b8fb/\",\n" +
                "      \"timeout\": \"P0DT00H00M30S\",\n" +
                "      \"published_at\": null,\n" +
                "      \"scheduled_at\": null,\n" +
                "      \"created_at\": \"2021-09-07T15:51:57.530033Z\",\n" +
                "      \"subscribe_channel\": \"program.widget.079cabc4-b35b-492c-8f1a-ad3091d2b8fb\",\n" +
                "      \"kind\": \"text-number-prediction-follow-up\",\n" +
                "      \"options\": [\n" +
                "        {\n" +
                "          \"id\": \"60e57fc0-7158-4294-a41d-f55a319b16a1\",\n" +
                "          \"url\": \"https://cf-blast-iconic.livelikecdn.com/api/v1/text-number-prediction-options/60e57fc0-7158-4294-a41d-f55a319b16a1/\",\n" +
                "          \"description\": \"1st Place Score\",\n" +
                "          \"correct_number\": null,\n" +
                "          \"translations\": {},\n" +
                "          \"translatable_fields\": [\n" +
                "            \"description\"\n" +
                "          ]\n" +
                "        },\n" +
                "        {\n" +
                "          \"id\": \"a83d3ab4-fa40-4155-bd6c-25220de6f881\",\n" +
                "          \"url\": \"https://cf-blast-iconic.livelikecdn.com/api/v1/text-number-prediction-options/a83d3ab4-fa40-4155-bd6c-25220de6f881/\",\n" +
                "          \"description\": \"2nd Place Score\",\n" +
                "          \"correct_number\": null,\n" +
                "          \"translations\": {},\n" +
                "          \"translatable_fields\": [\n" +
                "            \"description\"\n" +
                "          ]\n" +
                "        }\n" +
                "      ],\n" +
                "      \"vote_url\": \"https://blastrt-iconic-us-east-1.livelikecdn.com/api/v1/widget-interactions/text-number-prediction/079cabc4-b35b-492c-8f1a-ad3091d2b8fb/votes/eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJlbmNyeXB0ZWQiOiJnQUFBQUFCaE40cjVJZERZWmZaS0Y4N1BoYTZRV2ZvZ2x5YnVZdlJCT0Y1RXdRMW5pN3p6dTZLR0hWcHlmZXVacV9WTDNQdWowbDBkTG9CeGpxN1BQV3g1ZmpKM3RSZFBhZz09IiwiaXNzIjoiYmxhc3QiLCJpYXQiOjE2MzEwMzAwMDl9.4WByoNCFN-mhLk1hB9XC1gbj2mv_0ht2XIunwAd2j-A/\",\n" +
                "      \"question\": \"What will the final two highest scores be?\",\n" +
                "      \"program_date_time\": null,\n" +
                "      \"publish_delay\": \"P0DT00H00M00S\",\n" +
                "      \"interaction_url\": \"https://cf-blast-iconic.livelikecdn.com/api/v1/text-number-prediction-follow-ups/14be9af1-a8e7-4695-a4e9-f938e18051a8/interactions/\",\n" +
                "      \"impression_url\": \"https://blastrt-iconic-us-east-1.livelikecdn.com/api/v1/widget-impressions/text-number-prediction-follow-up/14be9af1-a8e7-4695-a4e9-f938e18051a8/\",\n" +
                "      \"impression_count\": 0,\n" +
                "      \"unique_impression_count\": 0,\n" +
                "      \"engagement_count\": null,\n" +
                "      \"engagement_percent\": null,\n" +
                "      \"status\": \"pending\",\n" +
                "      \"created_by\": {\n" +
                "        \"id\": \"8ee9168e-00ba-4206-a870-6eff6a052cf6\",\n" +
                "        \"name\": null,\n" +
                "        \"image_url\": null\n" +
                "      },\n" +
                "      \"schedule_url\": \"https://cf-blast-iconic.livelikecdn.com/api/v1/text-number-prediction-follow-ups/14be9af1-a8e7-4695-a4e9-f938e18051a8/schedule/\",\n" +
                "      \"rewards_url\": null,\n" +
                "      \"translations\": {},\n" +
                "      \"translatable_fields\": [],\n" +
                "      \"reactions\": [],\n" +
                "      \"custom_data\": null,\n" +
                "      \"claim_url\": \"https://blastrt-iconic-us-east-1.livelikecdn.com/api/v1/widget-interaction-claims/text-number-prediction/079cabc4-b35b-492c-8f1a-ad3091d2b8fb/claims/eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJlbmNyeXB0ZWQiOiJnQUFBQUFCaE40cjVBbzVEU0o2REhuNUtNUXdFcUxXTjkyd2h1M3NXM2tTR1RxQXlWNGM3MVZRWkZnbHpUa3BEby1scWZaWEVrRXM3NUYyMlFqYkdadmJUUUVDVVQtY0F4U0FYLU02UkpIMmVzLTE0TWVVZUprWWRPT1RQelhndVlQcG9oNFlCai14U2d4SmNiNmFZQk9tY09qeUs1ZGNmc2lIM3hpYWU0M0dNZk14RUllRlVHb3dOemZlOERxZ2JTVmJ3WG5YSHhFdE4iLCJpc3MiOiJibGFzdCIsImlhdCI6MTYzMTAyOTkxN30.5k2fafOKB7eaAuqwjVYp6OXd2_R3RxCg6lXFw1tr10o/\",\n" +
                "      \"sponsors\": [],\n" +
                "      \"widget_interactions_url_template\": \"https://cf-blast-iconic.livelikecdn.com/api/v1/profiles/{profile_id}/widget-interactions/?text_number_prediction_id=079cabc4-b35b-492c-8f1a-ad3091d2b8fb\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"question\": \"What will the final two highest scores be?\",\n" +
                "  \"confirmation_message\": \"Alright! Stay tuned to see the result.\",\n" +
                "  \"timeout\": \"P0DT00H00M30S\",\n" +
                "  \"options\": [\n" +
                "    {\n" +
                "      \"id\": \"60e57fc0-7158-4294-a41d-f55a319b16a1\",\n" +
                "      \"url\": \"https://cf-blast-iconic.livelikecdn.com/api/v1/text-number-prediction-options/60e57fc0-7158-4294-a41d-f55a319b16a1/\",\n" +
                "      \"description\": \"1st Place Score\",\n" +
                "      \"correct_number\": null,\n" +
                "      \"translations\": {},\n" +
                "      \"translatable_fields\": [\n" +
                "        \"description\"\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"id\": \"a83d3ab4-fa40-4155-bd6c-25220de6f881\",\n" +
                "      \"url\": \"https://cf-blast-iconic.livelikecdn.com/api/v1/text-number-prediction-options/a83d3ab4-fa40-4155-bd6c-25220de6f881/\",\n" +
                "      \"description\": \"2nd Place Score\",\n" +
                "      \"correct_number\": null,\n" +
                "      \"translations\": {},\n" +
                "      \"translatable_fields\": [\n" +
                "        \"description\"\n" +
                "      ]\n" +
                "    }\n" +
                "  ],\n" +
                "  \"vote_url\": \"https://blastrt-iconic-us-east-1.livelikecdn.com/api/v1/widget-interactions/text-number-prediction/079cabc4-b35b-492c-8f1a-ad3091d2b8fb/votes/eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJlbmNyeXB0ZWQiOiJnQUFBQUFCaE40cjVJZERZWmZaS0Y4N1BoYTZRV2ZvZ2x5YnVZdlJCT0Y1RXdRMW5pN3p6dTZLR0hWcHlmZXVacV9WTDNQdWowbDBkTG9CeGpxN1BQV3g1ZmpKM3RSZFBhZz09IiwiaXNzIjoiYmxhc3QiLCJpYXQiOjE2MzEwMzAwMDl9.4WByoNCFN-mhLk1hB9XC1gbj2mv_0ht2XIunwAd2j-A/\",\n" +
                "  \"subscribe_channel\": \"program.widget.079cabc4-b35b-492c-8f1a-ad3091d2b8fb\",\n" +
                "  \"program_date_time\": null,\n" +
                "  \"publish_delay\": \"P0DT00H00M00S\",\n" +
                "  \"interaction_url\": \"https://cf-blast-storage-iconic.livelikecdn.com/api/v1/text-number-predictions/079cabc4-b35b-492c-8f1a-ad3091d2b8fb/interactions/\",\n" +
                "  \"impression_url\": \"https://blastrt-iconic-us-east-1.livelikecdn.com/api/v1/widget-impressions/text-number-prediction/079cabc4-b35b-492c-8f1a-ad3091d2b8fb/\",\n" +
                "  \"impression_count\": 0,\n" +
                "  \"unique_impression_count\": 0,\n" +
                "  \"engagement_count\": 0,\n" +
                "  \"engagement_percent\": \"0.000\",\n" +
                "  \"status\": \"published\",\n" +
                "  \"created_by\": {\n" +
                "    \"id\": \"8ee9168e-00ba-4206-a870-6eff6a052cf6\",\n" +
                "    \"name\": null,\n" +
                "    \"image_url\": null\n" +
                "  },\n" +
                "  \"schedule_url\": \"https://cf-blast-storage-iconic.livelikecdn.com/api/v1/text-number-predictions/079cabc4-b35b-492c-8f1a-ad3091d2b8fb/schedule/\",\n" +
                "  \"rewards_url\": null,\n" +
                "  \"translations\": {},\n" +
                "  \"translatable_fields\": [\n" +
                "    \"question\",\n" +
                "    \"confirmation_message\"\n" +
                "  ],\n" +
                "  \"reactions\": [],\n" +
                "  \"custom_data\": null,\n" +
                "  \"sponsors\": [],\n" +
                "  \"widget_interactions_url_template\": \"https://cf-blast-iconic.livelikecdn.com/api/v1/profiles/{profile_id}/widget-interactions/?text_number_prediction_id=079cabc4-b35b-492c-8f1a-ad3091d2b8fb\"\n" +
                "}"
        input_widget_json.setText(json)

        val myWidgetsList: ArrayList<LiveLikeWidget> = GsonBuilder().create()
            .fromJson(
                getSharedPreferences(PREFERENCES_APP_ID, Context.MODE_PRIVATE).getString(
                    PREF_MY_WIDGETS,
                    null
                ),
                object : TypeToken<List<LiveLikeWidget>>() {}.type
            ) ?: arrayListOf()
        val channelManager = (application as LiveLikeApplication).channelManager
        val channel = channelManager.selectedChannel
        val session = (application as LiveLikeApplication).createPublicSession(
            channel.llProgram.toString(),
            null
        )
        show_my_widget.setOnClickListener {
//            session.getPublishedWidgets(
//                LiveLikePagination.FIRST,
//                object : LiveLikeCallback<List<LiveLikeWidget>>() {
//                    override fun onResponse(
//                        result: List<LiveLikeWidget>?,
//                        error: String?
//                    ) {
//                        error?.let {
//                            Toast.makeText(applicationContext, "$it", Toast.LENGTH_SHORT).show()
//                        }
//                        result?.map { it!! }?.let { list ->
//                            DialogUtils.showMyWidgetsDialog(
//                                this@WidgetFrameworkTestActivity,
//                                (application as LiveLikeApplication).sdk,
//                                ArrayList(list),
//                                object : LiveLikeCallback<LiveLikeWidget>() {
//                                    override fun onResponse(
//                                        result: LiveLikeWidget?,
//                                        error: String?
//                                    ) {
//                                        result?.let {
//                                            widget_view.displayWidget(
//                                                (application as LiveLikeApplication).sdk,
//                                                result
//                                            )
//                                        }
//                                    }
//                                }
//                            )
//                        }
//                    }
//                }
//            )
            (application as LiveLikeApplication).sdk.fetchWidgetDetails("4d034738-9e62-4a5e-8547-4c560f034ea2",
                "text-prediction",
                object : LiveLikeCallback<LiveLikeWidget>() {
                    override fun onResponse(result: LiveLikeWidget?, error: String?) {
                        result?.let {
                            widget_view.displayWidget(
                                (application as LiveLikeApplication).sdk,
                                result, showWithInteractionData = true
                            )
                            widget_view.postDelayed({
                                widget_view.setState(WidgetStates.RESULTS)
                            }, 5000)
                        }
                        error?.let {
                            Toast.makeText(applicationContext, it, Toast.LENGTH_SHORT).show()
                        }
                    }

                })
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
        widget_view.widgetViewFactory = object : LiveLikeWidgetViewFactory{
            override fun createCheerMeterView(cheerMeterWidgetModel: CheerMeterWidgetmodel): View? {
                return null
            }

            override fun createAlertWidgetView(alertWidgetModel: AlertWidgetModel): View? {
                return null
            }

            override fun createQuizWidgetView(
                quizWidgetModel: QuizWidgetModel,
                isImage: Boolean
            ): View? {
                return null
            }

            override fun createPredictionWidgetView(
                predictionViewModel: PredictionWidgetViewModel,
                isImage: Boolean
            ): View? {
                return null
            }

            override fun createPredictionFollowupWidgetView(
                followUpWidgetViewModel: FollowUpWidgetViewModel,
                isImage: Boolean
            ): View? {
                return null
            }

            override fun createPollWidgetView(
                pollWidgetModel: PollWidgetModel,
                isImage: Boolean
            ): View? {
                return null
            }

            override fun createImageSliderWidgetView(imageSliderWidgetModel: ImageSliderWidgetModel): View? {
                return null
            }

            override fun createVideoAlertWidgetView(videoAlertWidgetModel: VideoAlertWidgetModel): View? {
                return null
            }

            override fun createTextAskWidgetView(imageSliderWidgetModel: TextAskWidgetModel): View? {
                return null
            }

            override fun createNumberPredictionWidgetView(numberPredictionWidgetModel: NumberPredictionWidgetModel): View? {
                return CustomNumberPredictionWidget(this@WidgetFrameworkTestActivity).apply {
                    this.numberPredictionWidgetViewModel = numberPredictionWidgetModel
                }
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
        ed_widget_id.setText("80b728be-4839-4a99-8cc1-b0d66b3c3818")
        ed_widget_kind.setText("text-prediction-follow-up")
        btn_fetch.setOnClickListener {
            val id = ed_widget_id.text.toString()
            val kind = ed_widget_kind.text.toString()
            (application as LiveLikeApplication).sdk.let { sdk ->
                sdk.fetchWidgetDetails(id, kind,
                    object : LiveLikeCallback<LiveLikeWidget>() {
                        override fun onResponse(result: LiveLikeWidget?, error: String?) {
                            result?.let { widget ->
                                session.getWidgetInteraction(
                                    widgetId = widget.id!!,
                                    widgetKind = widget.kind!!,
                                    widgetInteractionUrl = widget.widgetInteractionUrl!!,
                                    liveLikeCallback = object :
                                        LiveLikeCallback<WidgetUserInteractionBase>() {
                                        override fun onResponse(
                                            result: WidgetUserInteractionBase?,
                                            error: String?
                                        ) {
                                            result?.let {
                                                txt_result.text = it.let {
                                                    when (it) {
                                                        is PredictionWidgetUserInteraction -> "User Correct: ${it.isCorrect}"
                                                        is PollWidgetUserInteraction -> "User Selected OptionId :${it.optionId}"
                                                        is QuizWidgetUserInteraction -> "User Selected: ${it.choiceId}"
                                                        is EmojiSliderUserInteraction -> "User data: ${it.magnitude}"
                                                        is CheerMeterUserInteraction -> "Total Score: ${it.totalScore}"
                                                        else -> "No Response"
                                                    }
                                                }
                                            }
                                        }
                                    }
                                )

                                widget_view_fetch.enableDefaultWidgetTransition = false
                                widget_view_fetch.setState(WidgetStates.RESULTS)

                                widget_view_fetch.displayWidget(
                                    sdk,
                                    widget,
                                    showWithInteractionData = true
                                )

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
