package com.livelike.livelikedemo

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
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
import kotlinx.android.synthetic.main.activity_standalone_widget.*
import kotlinx.android.synthetic.main.widget_command_row_element.view.*
import java.io.IOException
import java.nio.charset.Charset

class WidgetStandaloneActivity : AppCompatActivity() {
    private val commandList = mutableListOf<String>()
    private val windowTitle = "Widgets"
    private val showCommand = "Show"
    private val hideCommand = "Hide"
    private val correctAnswerCommand = "Correct Answer"
    private val wrongAnswerCommand = "Wrong Answer"

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
        commandList.add(showCommand)
        commandList.add(hideCommand)
        commandList.add(correctAnswerCommand)
        commandList.add(wrongAnswerCommand)
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
                                }
                                quizType -> {
                                    if (isVariance(getString(R.string.text))) {
                                        //showWidget(WidgetType.TEXT_PREDICTION, getPayload("prediction/quiz/quiz_widget.json"))
                                    } else if (isVariance(getString(R.string.image))) {
                                        showWidget(WidgetType.IMAGE_QUIZ, getPayload("prediction/quiz/quiz_widget.json"))
                                    }
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
                        hideCommand -> widget_view.dismissCurrentWidget()
                        correctAnswerCommand -> {
                            when {
                                predictionType -> {
                                    showPredictionResultWidgetAs(correctAnswerCommand)
                                }
                                pollType -> {
                                }
                                quizType -> {
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
                    }
                }
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

        private fun updatePayload(testTag: String) {
            payload = getPayload("prediction/text/prediction_question_text_result.json")
            payload.addProperty("testTag", testTag)
        }

        private fun isVariance(variance: String?) = this.variance == variance

        private fun showWidget(widgetType: WidgetType, payload: JsonObject) {
            widget_view.displayWidget(
                widgetType.value,
                payload
            )
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