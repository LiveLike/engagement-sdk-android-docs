package com.livelike.livelikedemo

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.livelike.livelikesdk.LiveLikeSDK
import com.livelike.livelikesdk.util.registerLogsHandler
import com.livelike.livelikesdk.widget.WidgetType
import kotlinx.android.synthetic.main.activity_standalone_widget.log_label
import kotlinx.android.synthetic.main.activity_standalone_widget.toolbar
import kotlinx.android.synthetic.main.activity_standalone_widget.widget_command
import kotlinx.android.synthetic.main.widget_command_row_element.view.command
import java.io.IOException
import java.nio.charset.Charset

class WidgetStandaloneActivity : AppCompatActivity() {
    private val commandList = mutableListOf<String>()
    private val windowTitle = "Widgets"
    private val showCommand = "Show"
    private val hideCommand = "Hide"
    private val correctAnswerCommand = "Correct Answer"
    private val wrongAnswerCommand = "Wrong Answer"
    private val displayResults = "Display Results"
    private val quizDisplayHandler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_standalone_widget)
        // This is needed so SharedPreference class gets a context.
        LiveLikeSDK("standalone", baseContext)
        val (type, variance) = getWidgetTypeWithVariance()
        updateToolbar()
        registerLogHandler()
        addCommands()
        createAdapter(type, variance)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val id = item?.itemId
        if (id == R.id.action_add_new) {
            startActivity(Intent(baseContext, WidgetOnlyActivity::class.java))
        }

        return super.onOptionsItemSelected(item)
    }

    private fun getWidgetTypeWithVariance(): Pair<String?, String?> {
        val extras = intent?.extras
        val type = extras?.getString(getString(R.string.widget_type))
        val variance = extras?.getString(getString(R.string.widget_variance))
        return Pair(type, variance)
    }

    private fun updateToolbar() {
        setSupportActionBar(toolbar)
        toolbar.apply {
            setNavigationIcon(R.drawable.ic_arrow_back_white_24dp)
            setBackgroundColor(Color.parseColor("#00ae8b"))
            setNavigationOnClickListener {
                startActivity(Intent(context, MainActivity::class.java))
            }
        }
        supportActionBar?.title = windowTitle
    }

    private fun createAdapter(type: String?, variance: String?) {
        val linearLayoutManager = LinearLayoutManager(baseContext)
        widget_command.layoutManager = linearLayoutManager
        widget_command.adapter = WidgetAdapter(type, variance)
    }

    private fun addCommands() {
        val (type, variance) = getWidgetTypeWithVariance()
        when (type) {
            this.getString(R.string.quiz) -> {
                commandList.add(showCommand)
            }
            getString(R.string.poll) -> {
                commandList.add(showCommand)
                commandList.add(displayResults)
            }
            else -> {
                commandList.add(showCommand)
                commandList.add(hideCommand)
                commandList.add(correctAnswerCommand)
                commandList.add(wrongAnswerCommand)
            }
        }
    }

    private fun registerLogHandler() {
        registerLogsHandler(object : (String) -> Unit {
            override fun invoke(text: String) {
                val labelText = "$text \n\n ${log_label.text}"
                log_label.text = labelText
            }
        })
    }

    private fun getPayload(fileName: String): JsonObject {
        return JsonParser().parse(loadJsonFile(fileName)).asJsonObject
    }

    private fun loadJsonFile(fileName: String): String? {
        val json: String
        try {
            val inputStream = assets.open(fileName)
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            json = String(buffer, Charset.defaultCharset())
        } catch (e: IOException) {
            return null
        }
        return json
    }

    inner class WidgetAdapter(type: String?, private val variance: String?) : RecyclerView.Adapter<ViewHolder>() {
        private val predictionType = type == getString(R.string.prediction)
        private val pollType = type == getString(R.string.poll)
        private val quizType = type == getString(R.string.quiz)
        private val cheerMeterType = type == getString(R.string.cheerMeter)
        private val emojiType = type == getString(R.string.emoji)
        private val alertType = type == getString(R.string.alert)
        private val informationType = type == getString(R.string.information)
        private var payload = JsonObject()
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val command = commandList[position]
            holder.commandButton.apply {
                text = command
                setOnClickListener {
                    val commandSelected = commandList[holder.adapterPosition]
                    when (commandSelected) {
                        showCommand -> {
                            // TODO: Once we start to implement other widget types, this logic can be either collated by moving this
                            // into it's own class.
                            when {
                                predictionType -> showPredictionQuestionWidget()
                                pollType -> {
                                    showPollWidget()
                                }
                                quizType -> {
                                    commandList.remove(showCommand)
                                    updateCommandAdapter()
                                    showQuizWidget()
                                }
                                cheerMeterType -> {
                                }
                                emojiType -> {
                                }
                                alertType -> {
                                }
                                informationType -> {
                                }
                            }
                        }
                        hideCommand -> Log.i(this::class.java.simpleName, "Hide Command")
                        correctAnswerCommand -> {
                            when {
                                predictionType -> {
                                    showPredictionResultWidgetAs(correctAnswerCommand)
                                }
                                pollType -> {
                                }
                                cheerMeterType -> {
                                }
                                emojiType -> {
                                }
                                alertType -> {
                                }
                                informationType -> {
                                }
                            }
                        }
                        wrongAnswerCommand -> {
                            if (predictionType) {
                                showPredictionResultWidgetAs(wrongAnswerCommand)
                            }
                        }

                        displayResults -> {
                            when {
                                quizType -> {
                                    commandList.remove(displayResults)
                                    updateCommandAdapter()
                                    if (isVariance(getString(R.string.text))) {
                                        showWidget(
                                            WidgetType.TEXT_QUIZ_RESULT,
                                            getPayload("quiz/text/quiz_text_result.json")
                                        )
                                    } else if (isVariance(getString(R.string.image))) {
                                        showWidget(
                                            WidgetType.IMAGE_QUIZ_RESULT,
                                            getPayload("quiz/image/quiz_result.json")
                                        )
                                    }
                                }
                                pollType -> {
                                    showPollWidgetResults()
                                }
                            }
                        }
                    }
                }
            }
        }

        private fun updateCommandAdapter() {
            val (type, variance) = getWidgetTypeWithVariance()
            createAdapter(type, variance)
        }

        private fun showQuizWidget() {
            quizDisplayHandler.postDelayed({
                commandList.add(displayResults)
                updateCommandAdapter()
            }, 7500L)
//            widget_view.widgetListener = object : WidgetEventListener {
//                override fun subscribeForResults(channel: String) {}
//
//                override fun onOptionVoteUpdate(
//                    oldVoteUrl: String,
//                    newVoteId: String,
//                    channel: String,
//                    voteUpdateCallback: ((String) -> Unit)?
//                ) {
//                }
//
//                override fun onWidgetDisplayed(impressionUrl: String) {}
//
//                override fun onOptionVote(voteUrl: String, channel: String, voteUpdateCallback: ((String) -> Unit)?) {}
//
//                override fun onFetchingQuizResult(answerUrl: String) {}
//
//                override fun onWidgetEvent(event: WidgetEvent) {
//                    if (event == WidgetEvent.WIDGET_DISMISS) {
//                        quizDisplayHandler.removeCallbacksAndMessages(null)
//                        commandList.clear()
//                        commandList.add(showCommand)
//                        updateCommandAdapter()
//                    }
//                }
//            }

            if (isVariance(getString(R.string.text))) {
                showWidget(WidgetType.TEXT_QUIZ, getPayload("quiz/text/quiz_text_widget.json"))
            } else if (isVariance(getString(R.string.image))) {
                showWidget(WidgetType.IMAGE_QUIZ, getPayload("quiz/image/quiz_widget.json"))
            }
        }

        private fun showPredictionQuestionWidget() {
            if (isVariance(getString(R.string.text))) {
                showWidget(WidgetType.TEXT_PREDICTION, getPayload("prediction/text/prediction_question_text.json"))
            } else if (isVariance(getString(R.string.image))) {
                showWidget(
                    WidgetType.IMAGE_PREDICTION,
                    getPayload("prediction/image/prediction_question_image.json")
                )
            }
        }

        private fun showPredictionResultWidgetAs(testTag: String) {
            if (isVariance(getString(R.string.text))) {
                updatePayload(testTag)
                showWidget(WidgetType.TEXT_PREDICTION_RESULTS, payload)
            } else if (isVariance(getString(R.string.image))) {
                payload = getPayload("prediction/image/prediction_question_image_result.json")
                payload.addProperty("testTag", testTag)
                showWidget(WidgetType.IMAGE_PREDICTION_RESULTS, payload)
            }
        }

        private fun showPollWidget() {
            if (isVariance(getString(R.string.text))) {
                payload = getPayload("poll/poll_text_widget.json")
                showWidget(WidgetType.TEXT_POLL, payload)
            } else if (isVariance(getString(R.string.image))) {
                payload = getPayload("poll/poll_image_widget.json")
                showWidget(WidgetType.IMAGE_POLL, payload)
            }
        }

        private fun showPollWidgetResults() {
            if (isVariance(getString(R.string.text))) {
                payload = getPayload("poll/poll_text_widget_results.json")
                showWidget(WidgetType.TEXT_POLL_RESULT, payload)
            } else if (isVariance(getString(R.string.image))) {
                payload = getPayload("poll/poll_image_widget_results.json")
                showWidget(WidgetType.IMAGE_POLL_RESULT, payload)
            }
        }

        private fun updatePayload(testTag: String) {
            payload = getPayload("prediction/text/prediction_question_text_result.json")
            payload.addProperty("testTag", testTag)
        }

        private fun isVariance(variance: String?) = this.variance == variance

        private fun showWidget(widgetType: WidgetType, payload: JsonObject) {
//            widget_view.displayWidget(
//                widgetType.value,
//                payload
//            )
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(
                LayoutInflater.from(baseContext).inflate(
                    R.layout.widget_command_row_element,
                    parent,
                    false
                )
            )
        }

        override fun getItemCount(): Int {
            return commandList.size
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val commandButton: Button = view.command
    }
}