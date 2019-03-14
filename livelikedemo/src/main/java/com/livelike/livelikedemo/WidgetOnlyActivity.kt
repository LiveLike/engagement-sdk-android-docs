package com.livelike.livelikedemo

import android.content.Intent
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_widget_only.*
import kotlinx.android.synthetic.main.widget_only_row_element.view.*

class WidgetOnlyActivity : AppCompatActivity() {
    companion object {
        const val WIDGET_TYPE = "widgetType"
        const val WIDGET_VARIANCE = "widgetVariance"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_widget_only)

        toolbar.apply {
            title = "Widgets"
            setNavigationIcon(R.drawable.ic_close_black_24dp)
            setBackgroundColor(Color.parseColor("#00ae8b"))
            setNavigationOnClickListener {
                startActivity(Intent(context, MainActivity::class.java))
            }
        }

        val availableWidgetTypeList = mutableListOf<WidgetType>()
        updateWidgetTypeList(availableWidgetTypeList)

        val linearLayoutManager = LinearLayoutManager(baseContext)
        widget_only.layoutManager = linearLayoutManager
        widget_only.adapter = WidgetAdapter(availableWidgetTypeList)
    }

    private fun updateWidgetTypeList(availableWidgetTypeList: MutableList<WidgetType>) {
        availableWidgetTypeList.add(WidgetType("POLL", "Text", "Image", null))
        availableWidgetTypeList.add(WidgetType("QUIZ", "Text", "Image", null))
        availableWidgetTypeList.add(WidgetType("PREDICTION", "Text", "Image", null))
        availableWidgetTypeList.add(WidgetType("CHEER_METER", "Clapping", null, null))
        availableWidgetTypeList.add(WidgetType("EMOJI", "Poll", "Slider", null))
        availableWidgetTypeList.add(WidgetType("ALERT", "NEWS", "STATS", "SPONSOR"))
        availableWidgetTypeList.add(WidgetType("INFORMATION", "Weather", "Twitter", null))
    }

    inner class WidgetAdapter(private val widgetList: MutableList<WidgetType>) : RecyclerView.Adapter<WidgetTypeViewHolder>() {

        override fun onBindViewHolder(holder: WidgetTypeViewHolder, position: Int) {
            val widget = widgetList[position]
            holder.widgetLabelTextView.text = widget.label
            val intent = Intent(baseContext, WidgetCommandActivity::class.java)
            intent.putExtra(Companion.WIDGET_TYPE, widget.label)

            updateView(widget.variance1, holder.widgetVarianceButton1, null)
            updateView(widget.variance2, holder.widgetVarianceButton2, holder.divider1)
            updateView(widget.variance3, holder.widgetVarianceButton3, holder.divider)
        }

        private fun updateView(varianceText: String?, widgetVarianceButton: Button, divider: View?) {
            if (varianceText != null) updateButton(widgetVarianceButton, varianceText)
             else visibilityGone(widgetVarianceButton, divider)
        }

        private fun updateButton(widgetVarianceButton: Button, varianceText: String?) {
            widgetVarianceButton.text = varianceText
            widgetVarianceButton.setOnClickListener {
            }
        }

        private fun visibilityGone(vararg view: View?) {
            view.forEach { v -> v?.visibility = View.GONE }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WidgetTypeViewHolder {
            return WidgetTypeViewHolder(
                LayoutInflater.from(baseContext).inflate(
                    R.layout.widget_only_row_element,
                    parent,
                    false
                )
            )
        }

        override fun getItemCount(): Int {
            return widgetList.size
        }
    }

    class WidgetTypeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val widgetLabelTextView : TextView = view.widget_type_label
        val widgetVarianceButton1 : Button = view.widgets_variance_1
        val widgetVarianceButton2 : Button = view.widgets_variance_2
        val widgetVarianceButton3 : Button = view.widgets_variance_3
        val divider1 : View = view.standalone_divider
        val divider : View = view.standalone_divider_2
    }
}

data class WidgetType(val label: String, val variance1: String?, val variance2: String?, val variance3: String?)