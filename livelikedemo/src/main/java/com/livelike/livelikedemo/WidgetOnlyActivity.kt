package com.livelike.livelikedemo

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import com.google.gson.Gson
import com.livelike.engagementsdk.LiveLikeContentSession
import com.livelike.livelikedemo.channel.ChannelManager
import kotlinx.android.synthetic.main.activity_each_widget_type_with_variance.progress_view
import kotlinx.android.synthetic.main.activity_each_widget_type_with_variance.rcyl_view
import kotlinx.android.synthetic.main.activity_each_widget_type_with_variance.widget_view
import kotlinx.android.synthetic.main.rcyl_item_header.view.textView
import kotlinx.android.synthetic.main.rcyl_list_item.view.button
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response


class WidgetOnlyActivity : AppCompatActivity() {
    private lateinit var session: LiveLikeContentSession
    private lateinit var channelManager: ChannelManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_each_widget_type_with_variance)
        channelManager = (application as LiveLikeApplication).channelManager
        session = (application as LiveLikeApplication).createPublicSession(
            channelManager.selectedChannel.llProgram.toString(), allowTimeCodeGetter = false
        )
        val adapter = HeaderAdapter(
            progress_view,
            channelManager.selectedChannel.llProgram.toString(),
            listOf(
                HeaderAdapter.PostType("Text Quiz", true),
                HeaderAdapter.PostType("2 Options", false, "text-quizzes", 2),
                HeaderAdapter.PostType("4 Options", false, "text-quizzes", 4),

                HeaderAdapter.PostType("Image Quiz", true),
                HeaderAdapter.PostType("2 Options", false, "image-quizzes", 2),
                HeaderAdapter.PostType("4 Options", false, "image-quizzes", 4),

                HeaderAdapter.PostType("Text Poll", true),
                HeaderAdapter.PostType("2 Options", false, "text-polls", 2),
                HeaderAdapter.PostType("4 Options", false, "text-polls", 4),

                HeaderAdapter.PostType("Image Poll", true),
                HeaderAdapter.PostType("2 Options", false, "image-polls", 2),
                HeaderAdapter.PostType("4 Options", false, "image-polls", 4),

                HeaderAdapter.PostType("Text Prediction", true),
                HeaderAdapter.PostType("2 Options", false, "text-predictions", 2),
                HeaderAdapter.PostType("4 Options", false, "text-predictions", 4),

                HeaderAdapter.PostType("Image Prediction", true),
                HeaderAdapter.PostType("2 Options", false, "image-predictions", 2),
                HeaderAdapter.PostType("4 Options", false, "image-predictions", 4),

                HeaderAdapter.PostType("Image Slider", true),
                HeaderAdapter.PostType("1 Options", false, "emoji-sliders", 1),
                HeaderAdapter.PostType("2 Options", false, "emoji-sliders", 2),
                HeaderAdapter.PostType("3 Options", false, "emoji-sliders", 3),
                HeaderAdapter.PostType("4 Options", false, "emoji-sliders", 4),
                HeaderAdapter.PostType("5 Options", false, "emoji-sliders", 5),

                HeaderAdapter.PostType("Alert", true),
                HeaderAdapter.PostType("Text Only", false, "alerts", 1),
                HeaderAdapter.PostType("Image Only", false, "alerts", 2),
                HeaderAdapter.PostType("Text and Image", false, "alerts", 3),
                HeaderAdapter.PostType("Text,Image and URL", false, "alerts", 4),

                HeaderAdapter.PostType("Cheer Meter", true),
                HeaderAdapter.PostType("2 Options", false, "cheer-meters", 2)
            )
        )
        rcyl_view.adapter = adapter
        widget_view.setSession(session)
    }
}

class HeaderAdapter(
    val progressBar: ProgressBar,
    val programId: String,
    private val data: List<PostType>
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    val question = "Who will win?"

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        if (viewType == TYPE_ITEM) {
            //inflate your layout and pass it to view holder
            return VHItem(
                LayoutInflater.from(parent.context).inflate(R.layout.rcyl_list_item, parent, false)
            )
        } else if (viewType == TYPE_HEADER) {
            //inflate your layout and pass it to view holder
            return VHHeader(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.rcyl_item_header, parent, false)
            )
        }
        throw RuntimeException("there is no type that matches the type $viewType + make sure your using types correctly")
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        val dataItem = getItem(position)
        if (holder is VHItem) {
            holder.button.text = dataItem.title
        } else if (holder is VHHeader) {
            holder.title.text = dataItem.title
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (isPositionHeader(position)) TYPE_HEADER else TYPE_ITEM
    }

    private fun isPositionHeader(position: Int): Boolean {
        return data[position].isHeader
    }

    private fun getItem(position: Int): PostType {
        return data[position]
    }

    internal inner class VHItem(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var button: Button = itemView.button
        private val client = OkHttpClient().newBuilder()
            .build()
        private val mediaType: MediaType? = MediaType.parse("application/json")
        private val images = arrayListOf<String>(
            "https://cf-blast-storage-staging.livelikecdn.com/assets/e38f0089-00be-4236-830a-6989e8298b50.jpg",
            "https://cf-blast-storage-staging.livelikecdn.com/assets/3c4db954-216b-40a8-ac34-9d43036b009d.jpg",
            "https://cf-blast-storage-staging.livelikecdn.com/assets/d223c74b-c29c-44a0-a3ce-1b8301971cf3.jpg",
            "https://cf-blast-storage-staging.livelikecdn.com/assets/84af0879-cfe9-4e37-8f38-adfe0a51ee57.jpg",
            "https://cf-blast-storage-staging.livelikecdn.com/assets/0cd1f9ad-c92c-4f6d-ada5-bab926df175f.jpg"
        )

        init {
            button.setOnClickListener {
                val type = getItem(adapterPosition)
                val options: ArrayList<Option> = ArrayList()
                val choices: ArrayList<Choice> = ArrayList()
                val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
                scope.launch(Dispatchers.IO) {
                    when (type.url) {
                        "text-quizzes" -> {
                            for (i in 0 until type.count) {
                                choices.add(Choice(description = "Choice ${i + 1}"))
                            }
                        }
                        "image-quizzes" -> {
                            for (i in 0 until type.count) {
                                choices.add(
                                    Choice(
                                        description = "Choice ${i + 1}",
                                        image_url = images[i]
                                    )
                                )
                            }
                        }
                        "text-polls", "text-predictions" -> {
                            for (i in 0 until type.count) {
                                options.add(Option(description = "Option $i"))
                            }
                        }
                        "image-polls", "image-predictions", "cheer-meters", "emoji-sliders" -> {
                            for (i in 0 until type.count) {
                                options.add(
                                    Option(
                                        description = "Option $i",
                                        image_url = images[i]
                                    )
                                )
                            }
                        }
                    }

                    val request = when (type.url) {
                        "alerts" -> when (type.count) {
                            1 -> AlertRequest(
                                title = "Alert",
                                text = type.title,
                                program_id = programId,
                                timeout = "PT10S"
                            )
                            2 -> AlertRequest(
                                title = "Alert",
                                image_url = images[0],
                                program_id = programId,
                                timeout = "PT10S"
                            )
                            3 -> AlertRequest(
                                title = "Alert",
                                text = type.title,
                                image_url = images[0], program_id = programId, timeout = "PT10S"
                            )
                            4 -> AlertRequest(
                                title = "Alert",
                                text = type.title,
                                image_url = images[0],
                                link_url = "https://www.google.com",
                                link_label = "Google", program_id = programId, timeout = "PT10S"
                            )
                            else -> null
                        }
                        "text-quizzes", "image-quizzes" -> QuizRequest(
                            choices,
                            null,
                            programId,
                            question,
                            "PT10S"
                        )
                        "text-polls", "image-polls" -> PollRequest(
                            options,
                            null,
                            programId,
                            question,
                            "PT10S"
                        )
                        "text-predictions", "image-predictions" -> PredictionRequest(
                            "The confirmation Message",
                            options,
                            null,
                            programId,
                            question,
                            "PT10S"
                        )
                        "emoji-sliders" -> EmojiSliderRequest(
                            0.0,
                            options,
                            null,
                            programId,
                            question,
                            "PT10S"
                        )
                        "cheer-meters" -> CheerMeterRequest(
                            "tap",
                            options,
                            null,
                            programId,
                            question,
                            "PT10S"
                        )
                        else -> null
                    }
                    scope.launch {
                        progressBar.visibility = View.VISIBLE
                    }
                    val responseString = postAPI(type.url!!, request)
                    val gson = Gson()
                    val response = when (type.url) {
                        "alerts" -> gson.fromJson(responseString, AlertResponse::class.java)
                        "text-quizzes", "image-quizzes" -> gson.fromJson(
                            responseString,
                            QuizResponse::class.java
                        )
                        "text-polls", "image-polls" -> gson.fromJson(
                            responseString,
                            PollResponse::class.java
                        )
                        "text-predictions", "image-predictions" -> gson.fromJson(
                            responseString,
                            PredictionResponse::class.java
                        )
                        "emoji-sliders" -> gson.fromJson(
                            responseString,
                            EmojiSliderResponse::class.java
                        )
                        "cheer-meters" -> gson.fromJson(
                            responseString, CheerMeterResponse::class.java
                        )
                        else -> null
                    }
                    response?.let {
                        when (it) {
                            is AlertResponse -> {
                                putAPI(it.schedule_url)
                            }
                            is PollResponse -> {
                                putAPI(it.schedule_url)
                            }
                            is QuizResponse -> {
                                putAPI(it.schedule_url)
                            }
                            is PredictionResponse -> {
                                putAPI(it.schedule_url)
                                scope.launch {
                                    progressBar.visibility = View.GONE
                                }
                                delay(10000)
                                for (i in it.options.indices) {
                                    val option = it.options[i]
                                    if (i == 0) {
                                        option.is_correct = true
                                        option.vote_count = 0
                                        val body = patchAPI(option.url, option)
                                    } else if (i == 1) {
                                        option.is_correct = false
                                        option.vote_count = 1
                                    }
                                    it.options[i] = option
                                }
                                delay(5000)
                                val followRequest = FollowUpRequest(
                                    it.options,
                                    it.program_date_time,
                                    it.question,
                                    it.scheduled_at,
                                    "P0DT00H00M07S"
                                )
                                val res =
                                    patchAPI(
                                        "${it.follow_up_url}${it.follow_ups[0].id}",
                                        followRequest
                                    )
                                val resp = gson.fromJson(res, FollowUpResponse::class.java)
                                resp?.let { r ->
                                    putAPI(r.schedule_url)
                                }
                            }
                            is CheerMeterResponse -> {
                                putAPI(it.schedule_url)
                            }
                            is EmojiSliderResponse -> {
                                putAPI(it.schedule_url)
                            }
                            else -> {
                            }
                        }
                        scope.launch {
                            progressBar.visibility = View.GONE
                        }
                    }
                }
            }
        }

        private suspend fun putAPI(url: String): Boolean {
            val request: Request = Request.Builder()
                .url(url)
                .method("PUT", RequestBody.create(mediaType, ""))
                .addHeader(
                    "Authorization",
                    "Bearer db1GX0KrnGWwSOplsMTLJpFBbLds15TbULIxr6J189sabhDdbsrKoA"
                )
                .addHeader("Content-Type", "application/json")
                .build()
            val response: Response = client.newCall(request).execute()
            return response.isSuccessful
        }

        private suspend fun patchAPI(url: String, data: Any? = null): String? {
            val body = RequestBody.create(
                mediaType,
                Gson().toJson(data)
            )
            val request: Request = Request.Builder()
                .url(url)
                .method("PATCH", body)
                .addHeader(
                    "Authorization",
                    "Bearer db1GX0KrnGWwSOplsMTLJpFBbLds15TbULIxr6J189sabhDdbsrKoA"
                )
                .addHeader("Content-Type", "application/json")
                .build()
            val response: Response = client.newCall(request).execute()
            return response.body()?.string()
        }

        private suspend fun postAPI(
            url: String,
            post: Any? = null
        ): String? {
            val body = RequestBody.create(
                mediaType,
                Gson().toJson(post)
            )
            val request: Request = Request.Builder()
                .url("https://cf-blast-staging.livelikecdn.com/api/v1/$url/")
                .method("POST", body)
                .addHeader(
                    "Authorization",
                    "Bearer db1GX0KrnGWwSOplsMTLJpFBbLds15TbULIxr6J189sabhDdbsrKoA"
                )
                .addHeader("Content-Type", "application/json")
                .build()
            val response: Response = client.newCall(request).execute()
            return response.body()?.string()
        }
    }

    internal inner class VHHeader(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var title: TextView = itemView.textView
    }


    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    data class PostType(
        val title: String,
        val isHeader: Boolean = false,
        val url: String? = null,
        val count: Int = 0
    )

    data class QuizRequest(
        val choices: List<Choice>? = null,
        val program_date_time: String? = null,
        val program_id: String? = null,
        val question: String? = null,
        val timeout: String? = null
    )

    data class QuizResponse(
        val choices: List<Choice>,
        val created_at: String,
        val created_by: CreatedBy,
        val custom_data: Any,
        val engagement_count: Int,
        val engagement_percent: String,
        val id: String,
        val impression_count: Int,
        val impression_url: String,
        val interaction_url: String,
        val kind: String,
        val program_date_time: String,
        val program_id: String,
        val publish_delay: String,
        val published_at: Any,
        val question: String,
        val reactions: List<Any>,
        val rewards_url: Any,
        val schedule_url: String,
        val scheduled_at: Any,
        val status: String,
        val subscribe_channel: String,
        val timeout: String,
        val translatable_fields: List<String>,
        val translations: TranslationsX,
        val unique_impression_count: Int,
        val url: String
    )

    data class Choice(
        val answer_count: Int? = null,
        val answer_url: String? = null,
        val description: String,
        val id: String? = null,
        val is_correct: Boolean? = null,
        val translatable_fields: List<String>? = null,
        val translations: Translations? = null,
        val image_url: String? = null
    )

    data class CreatedBy(
        val id: String,
        val image_url: String,
        val name: String
    )

    class TranslationsX

    class Translations

    data class PollRequest(
        val options: List<Option>,
        val program_date_time: String? = null,
        val program_id: String,
        val question: String,
        val timeout: String
    )

    data class PollResponse(
        val created_at: String,
        val created_by: CreatedBy,
        val custom_data: Any,
        val engagement_count: Int,
        val engagement_percent: String,
        val id: String,
        val impression_count: Int,
        val impression_url: String,
        val interaction_url: String,
        val kind: String,
        val options: List<Option>,
        val program_date_time: String,
        val program_id: String,
        val publish_delay: String,
        val published_at: Any,
        val question: String,
        val reactions: List<Any>,
        val rewards_url: Any,
        val schedule_url: String,
        val scheduled_at: Any,
        val status: String,
        val subscribe_channel: String,
        val timeout: String,
        val translatable_fields: List<String>,
        val translations: TranslationsX,
        val unique_impression_count: Int,
        val url: String
    )

    data class Option(
        val description: String,
        val id: String? = null,
        val translatable_fields: List<String>? = null,
        val translations: Translations? = null,
        val vote_count: Int? = null,
        val vote_url: String? = null,
        val image_url: String? = null,
        val is_correct: Boolean? = null,
        val url: String? = null
    )

    data class PredictionRequest(
        val confirmation_message: String,
        val options: List<Option>,
        val program_date_time: String? = null,
        val program_id: String,
        val question: String,
        val timeout: String
    )

    data class PredictionResponse(
        val confirmation_message: String,
        val created_at: String,
        val created_by: CreatedBy,
        val custom_data: Any,
        val engagement_count: Int,
        val engagement_percent: String,
        val follow_up_url: String,
        val follow_ups: List<FollowUp>,
        val id: String,
        val impression_count: Int,
        val impression_url: String,
        val interaction_url: String,
        val kind: String,
        val options: ArrayList<OptionX>,
        val program_date_time: String,
        val program_id: String,
        val publish_delay: String,
        val published_at: Any,
        val question: String,
        val reactions: List<Any>,
        val rewards_url: Any,
        val schedule_url: String,
        val scheduled_at: Any,
        val status: String,
        val subscribe_channel: String,
        val timeout: String,
        val translatable_fields: List<String>,
        val translations: TranslationsXXX,
        val unique_impression_count: Int,
        val url: String
    )

    data class FollowUpRequest(
        val options: List<OptionX>?,
        val program_date_time: String?,
        val question: String?,
        val scheduled_at: Any?,
        val timeout: String?
    )

    data class FollowUp(
        val correct_option_id: Any,
        val created_at: String,
        val created_by: CreatedByX,
        val custom_data: Any,
        val engagement_count: Any,
        val engagement_percent: Any,
        val id: String,
        val image_prediction_id: String,
        val image_prediction_url: String,
        val impression_count: Int,
        val impression_url: String,
        val interaction_url: String,
        val kind: String,
        val options: List<Option>,
        val program_date_time: Any,
        val program_id: String,
        val publish_delay: String,
        val published_at: Any,
        val question: String,
        val reactions: List<Any>,
        val rewards_url: Any,
        val schedule_url: String,
        val scheduled_at: Any,
        val status: String,
        val subscribe_channel: String,
        val timeout: String,
        val translatable_fields: List<Any>,
        val translations: TranslationsX,
        val unique_impression_count: Int,
        val url: String
    )

    data class OptionX(
        val description: String,
        val id: String,
        val image_url: String,
        var is_correct: Boolean,
        val translatable_fields: List<String>,
        val translations: TranslationsXX,
        val url: String,
        var vote_count: Int,
        val vote_url: String
    )

    data class FollowUpResponse(
        val correct_option_id: String,
        val created_at: String,
        val created_by: CreatedBy,
        val custom_data: Any,
        val engagement_count: Any,
        val engagement_percent: Any,
        val id: String,
        val image_prediction_id: String,
        val image_prediction_url: String,
        val impression_count: Int,
        val impression_url: String,
        val interaction_url: String,
        val kind: String,
        val options: List<Option>,
        val program_date_time: String,
        val program_id: String,
        val publish_delay: String,
        val published_at: String,
        val question: String,
        val reactions: List<Any>,
        val rewards_url: Any,
        val schedule_url: String,
        val scheduled_at: String,
        val status: String,
        val subscribe_channel: String,
        val timeout: String,
        val translatable_fields: List<Any>,
        val translations: TranslationsX,
        val unique_impression_count: Int,
        val url: String
    )

    class TranslationsXXX

    data class CreatedByX(
        val id: String,
        val image_url: String,
        val name: String
    )

    class TranslationsXX

    data class AlertRequest(
        val image_url: String? = null,
        val link_label: String? = null,
        val link_url: String? = null,
        val program_date_time: String? = null,
        val program_id: String,
        val text: String? = null,
        val timeout: String,
        val title: String? = null
    )

    data class AlertResponse(
        val created_at: String,
        val created_by: CreatedBy,
        val custom_data: Any,
        val engagement_count: Any,
        val engagement_percent: Any,
        val id: String,
        val image_url: String,
        val impression_count: Int,
        val impression_url: String,
        val interaction_url: String,
        val kind: String,
        val link_label: String,
        val link_url: String,
        val program_date_time: String,
        val program_id: String,
        val publish_delay: String,
        val published_at: Any,
        val reactions: List<Any>,
        val rewards_url: Any,
        val schedule_url: String,
        val scheduled_at: Any,
        val status: String,
        val subscribe_channel: String,
        val text: String,
        val timeout: String,
        val title: String,
        val translatable_fields: List<String>,
        val translations: Translations,
        val unique_impression_count: Int,
        val url: String
    )

    data class EmojiSliderRequest(
        val initial_magnitude: Double,
        val options: List<Option>,
        val program_date_time: String? = null,
        val program_id: String,
        val question: String,
        val timeout: String
    )

    data class EmojiSliderResponse(
        val average_magnitude: String,
        val created_at: String,
        val created_by: CreatedBy,
        val custom_data: Any,
        val engagement_count: Int,
        val engagement_percent: String,
        val id: String,
        val impression_count: Int,
        val impression_url: String,
        val initial_magnitude: String,
        val interaction_url: String,
        val kind: String,
        val options: List<Option>,
        val program_date_time: String,
        val program_id: String,
        val publish_delay: String,
        val published_at: Any,
        val question: String,
        val reactions: List<Any>,
        val rewards_url: Any,
        val schedule_url: String,
        val scheduled_at: Any,
        val status: String,
        val subscribe_channel: String,
        val timeout: String,
        val translatable_fields: List<String>,
        val translations: TranslationsX,
        val unique_impression_count: Int,
        val url: String,
        val vote_url: String
    )

    data class CheerMeterRequest(
        val cheer_type: String,
        val options: List<Option>,
        val program_date_time: String? = null,
        val program_id: String,
        val question: String,
        val timeout: String
    )

    data class CheerMeterResponse(
        val cheer_type: String,
        val created_at: String,
        val created_by: CreatedBy,
        val custom_data: Any,
        val engagement_count: Int,
        val engagement_percent: String,
        val id: String,
        val impression_count: Int,
        val impression_url: String,
        val interaction_url: String,
        val kind: String,
        val options: List<Option>,
        val program_date_time: String,
        val program_id: String,
        val publish_delay: String,
        val published_at: Any,
        val question: String,
        val reactions: List<Any>,
        val rewards_url: Any,
        val schedule_url: String,
        val scheduled_at: Any,
        val status: String,
        val subscribe_channel: String,
        val timeout: String,
        val translatable_fields: List<String>,
        val translations: TranslationsX,
        val unique_impression_count: Int,
        val url: String
    )
}