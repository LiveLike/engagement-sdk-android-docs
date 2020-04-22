package com.livelike.livelikedemo

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.google.gson.JsonParser
import kotlinx.android.synthetic.main.activity_widget_framework.input_widget_json
import kotlinx.android.synthetic.main.activity_widget_framework.move_to_next_state
import kotlinx.android.synthetic.main.activity_widget_framework.show_widget
import kotlinx.android.synthetic.main.activity_widget_framework.widget_view

class WidgetFrameworkActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_widget_framework)

        var json = "{\"event\":\"text-poll-created\",\"payload\":{\"id\":\"6605ee55-8d52-49cb-9dfb-ccaadcd1b2bf\",\"url\":\"https://cf-blast-staging.livelikecdn.com/api/v1/text-polls/6605ee55-8d52-49cb-9dfb-ccaadcd1b2bf/\",\"kind\":\"text-poll\",\"program_id\":\"b4dd284b-45bf-45c7-ba95-10416e370cea\",\"created_at\":\"2020-04-20T07:54:35.153Z\",\"published_at\":null,\"scheduled_at\":null,\"question\":\"ss\",\"timeout\":\"P0DT00H00M10S\",\"options\":[{\"id\":\"9fd8eb52-0933-4513-9ec7-da1ea035a815\",\"description\":\"q\",\"vote_count\":0,\"vote_url\":\"https://blastrt-staging-us-east-1.livelikecdn.com/services/interactions/text-poll/6605ee55-8d52-49cb-9dfb-ccaadcd1b2bf/eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJvcHRpb25zIjpbIjlmZDhlYjUyLTA5MzMtNDUxMy05ZWM3LWRhMWVhMDM1YTgxNSIsIjk0MTJkNDBjLWY1YzItNDYxMy05MzhlLTc3ZGY3YzNjNTMxMyJdLCJvcHRpb25fdXVpZCI6IjlmZDhlYjUyLTA5MzMtNDUxMy05ZWM3LWRhMWVhMDM1YTgxNSIsImlzcyI6ImJsYXN0IiwiaWF0IjoxNTg3MzY5Mjc3fQ.2LWWa5ziRXkbBjZzVMhSHINEfYsEe1DFLUWrjyvnwpU\",\"translations\":{},\"translatable_fields\":[\"description\"]},{\"id\":\"9412d40c-f5c2-4613-938e-77df7c3c5313\",\"description\":\"wq\",\"vote_count\":0,\"vote_url\":\"https://blastrt-staging-us-east-1.livelikecdn.com/services/interactions/text-poll/6605ee55-8d52-49cb-9dfb-ccaadcd1b2bf/eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJvcHRpb25zIjpbIjlmZDhlYjUyLTA5MzMtNDUxMy05ZWM3LWRhMWVhMDM1YTgxNSIsIjk0MTJkNDBjLWY1YzItNDYxMy05MzhlLTc3ZGY3YzNjNTMxMyJdLCJvcHRpb25fdXVpZCI6Ijk0MTJkNDBjLWY1YzItNDYxMy05MzhlLTc3ZGY3YzNjNTMxMyIsImlzcyI6ImJsYXN0IiwiaWF0IjoxNTg3MzY5Mjc3fQ.UqI1IQNAVcIbxWcwqHc-hfdfWGXyS8_7GE4C4_sn0cg\",\"translations\":{},\"translatable_fields\":[\"description\"]}],\"subscribe_channel\":\"text_poll_6605ee55_8d52_49cb_9dfb_ccaadcd1b2bf\",\"program_date_time\":\"2020-04-20T07:54:17.123000Z\",\"publish_delay\":\"P0DT00H00M00S\",\"interaction_url\":\"https://cf-blast-staging.livelikecdn.com/api/v1/text-polls/6605ee55-8d52-49cb-9dfb-ccaadcd1b2bf/interactions/\",\"impression_url\":\"https://s93qlqrqw5.execute-api.us-east-1.amazonaws.com/services/impressions/text-poll/6605ee55-8d52-49cb-9dfb-ccaadcd1b2bf\",\"impression_count\":0,\"unique_impression_count\":0,\"engagement_count\":0,\"engagement_percent\":\"0.000\",\"status\":\"pending\",\"created_by\":{\"id\":\"0cdffb74-e382-41f2-bb81-a5a9dba67888\",\"name\":\"Shivansh Mittal\",\"image_url\":\"https://cf-blast-storage-staging.livelikecdn.com/assets/e1ad9075-b59d-4825-9d23-17cdfb351a9a.png\"},\"schedule_url\":\"https://cf-blast-staging.livelikecdn.com/api/v1/text-polls/6605ee55-8d52-49cb-9dfb-ccaadcd1b2bf/schedule/\",\"rewards_url\":null,\"translations\":{},\"translatable_fields\":[\"question\"],\"reactions\":[],\"custom_data\":null}}"

        input_widget_json.setText(json)
        show_widget.setOnClickListener {
            try {
                val jsonObject = JsonParser.parseString(input_widget_json.text.toString()).asJsonObject
                widget_view.displayWidget((application as LiveLikeApplication).sdk, jsonObject)
            } catch (ex: Exception) {
                Toast.makeText(this, "Invalid json", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
        }

        move_to_next_state.setOnClickListener {
            widget_view.moveToNextState()
        }
    }
}
