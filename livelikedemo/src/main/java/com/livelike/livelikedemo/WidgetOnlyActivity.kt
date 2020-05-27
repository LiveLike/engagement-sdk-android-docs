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
import android.widget.Toast
import com.google.gson.Gson
import com.livelike.engagementsdk.BuildConfig
import com.livelike.engagementsdk.LiveLikeContentSession
import com.livelike.livelikedemo.channel.ChannelManager
import com.livelike.livelikedemo.models.AlertRequest
import com.livelike.livelikedemo.models.AlertResponse
import com.livelike.livelikedemo.models.CheerMeterRequestResponse
import com.livelike.livelikedemo.models.EmojiSliderRequest
import com.livelike.livelikedemo.models.FollowUpRequest
import com.livelike.livelikedemo.models.FollowUpResponse
import com.livelike.livelikedemo.models.PollRequestResponse
import com.livelike.livelikedemo.models.PredictionRequest
import com.livelike.livelikedemo.models.PredictionResponse
import com.livelike.livelikedemo.models.QuizRequest
import com.livelike.livelikedemo.models.QuizResponse
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

    private val twoOptions = "2 Options"
    private val fourOptions = "4 Options"
    private val textQuiz = "text-quizzes"
    private val imgQuiz = "image-quizzes"
    private val txtPoll = "text-polls"
    private val imgPoll = "image-polls"
    private val txtPrediction = "text-predictions"
    private val imgPrediction = "image-predictions"
    private val emojiSlider = "emoji-sliders"
    private val alerts = "alerts"
    private val cheerMeter = "cheer-meters"

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
                PostType("Text Quiz", true),
                PostType(twoOptions, false, textQuiz, 2),
                PostType(fourOptions, false, textQuiz, 4),

                PostType("Image Quiz", true),
                PostType(twoOptions, false, imgQuiz, 2),
                PostType(fourOptions, false, imgQuiz, 4),

                PostType("Text Poll", true),
                PostType(twoOptions, false, txtPoll, 2),
                PostType(fourOptions, false, txtPoll, 4),

                PostType("Image Poll", true),
                PostType(twoOptions, false, imgPoll, 2),
                PostType(fourOptions, false, imgPoll, 4),

                PostType("Text Prediction", true),
                PostType(twoOptions, false, txtPrediction, 2),
                PostType(fourOptions, false, txtPrediction, 4),

                PostType("Image Prediction", true),
                PostType(twoOptions, false, imgPrediction, 2),
                PostType(fourOptions, false, imgPrediction, 4),

                PostType("Image Slider", true),
                PostType("1 Options", false, emojiSlider, 1),
                PostType(twoOptions, false, emojiSlider, 2),
                PostType("3 Options", false, emojiSlider, 3),
                PostType(fourOptions, false, emojiSlider, 4),
                PostType("5 Options", false, emojiSlider, 5),

                PostType("Alert", true),
                PostType("Text Only", false, alerts, 1),
                PostType("Image Only", false, alerts, 2),
                PostType("Text and Image", false, alerts, 3),
                PostType("Text,Image and URL", false, alerts, 4),

                PostType("Cheer Meter", true),
                PostType(twoOptions, false, cheerMeter, 2)
            )
        )
        rcyl_view.adapter = adapter
        val jsonTheme = intent.getStringExtra("jsonTheme")
        if (jsonTheme != null) {
            try {
                widget_view.setTheme(jsonTheme)
            } catch (e: Exception) {
                Toast.makeText(applicationContext, "${e.message}", Toast.LENGTH_LONG).show()
            }
        }
        widget_view.setSession(session)
    }

    override fun onResume() {
        super.onResume()
        session.resume()
    }

    override fun onPause() {
        super.onPause()
        session.pause()
    }

    inner class HeaderAdapter(
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
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.rcyl_list_item, parent, false)
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
            private val contentType = "Content-Type"
            private val applicationJSON: String = "application/json"
            private val mediaType: MediaType? = MediaType.parse(applicationJSON)

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
                    accessToken =
                        when (com.livelike.livelikedemo.BuildConfig.BUILD_TYPE == "release") {
                            true -> "Bearer DblPy3BVfqj_aatq6N5kFpNcH08LKObqEbnlLWc-3NlFInKTfsVxEg"
                            else -> "Bearer db1GX0KrnGWwSOplsMTLJpFBbLds15TbULIxr6J189sabhDdbsrKoA"
                        }
                    scope.launch(Dispatchers.IO) {
                        when (type.url) {
                            textQuiz -> {
                                for (i in 0 until type.count) {
                                    choices.add(
                                        Choice(
                                            description = "Choice ${i + 1}",
                                            is_correct = i == 0
                                        )
                                    )
                                }
                            }
                            imgQuiz -> {
                                for (i in 0 until type.count) {
                                    choices.add(
                                        Choice(
                                            description = "Choice ${i + 1}",
                                            image_url = images[i],
                                            is_correct = i == 0
                                        )
                                    )
                                }
                            }
                            txtPoll, txtPrediction -> {
                                for (i in 0 until type.count) {
                                    options.add(Option(description = "Option $i"))
                                }
                            }
                            imgPoll, imgPrediction, cheerMeter, emojiSlider -> {
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
                            alerts -> when (type.count) {
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
                            textQuiz, imgQuiz -> QuizRequest(
                                choices,
                                null,
                                programId,
                                question,
                                "PT10S"
                            )
                            txtPoll, imgPoll -> PollRequestResponse(
                                options,
                                null,
                                programId,
                                question,
                                "PT10S"
                            )
                            txtPrediction, imgPrediction -> PredictionRequest(
                                "The confirmation Message",
                                options,
                                null,
                                programId,
                                question,
                                "PT10S"
                            )
                            emojiSlider -> EmojiSliderRequest(
                                0.0,
                                options,
                                null,
                                programId,
                                question,
                                "PT10S"
                            )
                            cheerMeter -> CheerMeterRequestResponse(
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
                        var response: Any? = null
                        try {
                            response = when (type.url) {
                                alerts -> gson.fromJson(responseString, AlertResponse::class.java)
                                textQuiz, imgQuiz -> gson.fromJson(
                                    responseString,
                                    QuizResponse::class.java
                                )
                                txtPoll, imgPoll -> gson.fromJson(
                                    responseString,
                                    PollRequestResponse::class.java
                                )
                                txtPrediction, imgPrediction -> gson.fromJson(
                                    responseString,
                                    PredictionResponse::class.java
                                )
                                emojiSlider -> gson.fromJson(
                                    responseString,
                                    PredictionResponse::class.java
                                )
                                cheerMeter -> gson.fromJson(
                                    responseString, CheerMeterRequestResponse::class.java
                                )
                                else -> null
                            }
                        } catch (e: java.lang.Exception) {
                            e.printStackTrace()
                            response = null
                        }
                        response?.let {
                            when (it) {
                                is AlertResponse -> {
                                    it.schedule_url?.let { it1 -> putAPI(it1) }
                                }
                                is PollRequestResponse -> {
                                    it.schedule_url?.let { it1 -> putAPI(it1) }
                                }
                                is QuizResponse -> {
                                    it.schedule_url?.let { it1 -> putAPI(it1) }
                                }
                                is PredictionResponse -> {
                                    it.schedule_url?.let { it1 -> putAPI(it1) }
                                    scope.launch {
                                        progressBar.visibility = View.GONE
                                    }
                                    if (it.kind?.contains("prediction") == true) {
                                        delay(10000)
                                        it.options?.let { list ->
                                            for (i in list.indices) {
                                                val option = list[i]
                                                if (i == 0) {
                                                    option.is_correct = true
                                                    option.vote_count = 0
                                                    patchAPI(option.url, option)
                                                } else if (i == 1) {
                                                    option.is_correct = false
                                                    option.vote_count = 1
                                                }
                                                list[i] = option
                                            }
                                        }

                                        delay(5000)
                                        val followRequest = FollowUpRequest(
                                            it.options,
                                            it.program_date_time,
                                            it.question,
                                            it.scheduled_at,
                                            "P0DT00H00M07S"
                                        )
                                        it.follow_ups?.let { followups ->
                                            val res =
                                                patchAPI(
                                                    "${followups[0].url}",
                                                    followRequest
                                                )
                                            val resp =
                                                gson.fromJson(res, FollowUpResponse::class.java)
                                            resp?.let { r ->
                                                r.schedule_url?.let { it1 -> putAPI(it1) }
                                            }
                                        }
                                    }
                                }
                                is CheerMeterRequestResponse -> {
                                    it.schedule_url?.let { it1 -> putAPI(it1) }
                                }
                                is PredictionResponse -> {
                                    it.schedule_url?.let { it1 -> putAPI(it1) }
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
                        authorization,
                        accessToken
                    )
                    .addHeader(contentType, "application/json")
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
                        authorization,
                        accessToken
                    )
                    .addHeader(contentType, applicationJSON)
                    .build()
                val response: Response = client.newCall(request).execute()
                return response.body()?.string()
            }

            private val authorization = "Authorization"
            private var accessToken: String =
                "Bearer db1GX0KrnGWwSOplsMTLJpFBbLds15TbULIxr6J189sabhDdbsrKoA"

            private suspend fun postAPI(
                url: String,
                post: Any? = null
            ): String? {
                val body = RequestBody.create(
                    mediaType,
                    Gson().toJson(post)
                )
                val request: Request = Request.Builder()
                    .url("${BuildConfig.CONFIG_URL}$url/")
                    .method("POST", body)
                    .addHeader(
                        authorization,
                        accessToken
                    )
                    .addHeader(contentType, applicationJSON)
                    .build()
                val response: Response = client.newCall(request).execute()
                return response.body()?.string()
            }
        }

        internal inner class VHHeader(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var title: TextView = itemView.textView
        }

        private val TYPE_HEADER = 0
        private val TYPE_ITEM = 1
    }

}

data class PostType(
    val title: String,
    val isHeader: Boolean = false,
    val url: String? = null,
    val count: Int = 0
)

data class Choice(
    val answer_count: Int? = null,
    val answer_url: String? = null,
    val description: String,
    val id: String? = null,
    val is_correct: Boolean? = null,
    val translatable_fields: List<String>? = null,
    val image_url: String? = null
)

data class CreatedBy(
    val id: String,
    val image_url: String,
    val name: String
)


data class Option(
    val description: String,
    val id: String? = null,
    val translatable_fields: List<String>? = null,
    val vote_count: Int? = null,
    val vote_url: String? = null,
    val image_url: String? = null,
    val is_correct: Boolean? = null,
    val url: String? = null
)













