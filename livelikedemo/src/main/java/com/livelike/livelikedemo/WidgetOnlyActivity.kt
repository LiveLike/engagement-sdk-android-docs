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
import kotlinx.android.synthetic.main.activity_each_widget_type_with_variance.*
import kotlinx.android.synthetic.main.widget_type_with_variance_row_element.view.*

class WidgetOnlyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_each_widget_type_with_variance)

        toolbar.apply {
            title = "Widgets"
            setNavigationIcon(R.drawable.ic_close_white_24dp)
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
        availableWidgetTypeList.add(WidgetType(getString(R.string.poll), getString(R.string.text), getString(R.string.image), null))
        availableWidgetTypeList.add(WidgetType(getString(R.string.quiz), getString(R.string.text), getString(R.string.image), null))
        availableWidgetTypeList.add(WidgetType(getString(R.string.prediction), getString(R.string.text), getString(R.string.image), null))
        availableWidgetTypeList.add(WidgetType(getString(R.string.cheerMeter), getString(R.string.clapping), null, null))
        availableWidgetTypeList.add(WidgetType(getString(R.string.emoji), getString(R.string.poll_lowercase), getString(
                    R.string.slider), null))
        availableWidgetTypeList.add(WidgetType(getString(R.string.alert), getString(R.string.news), getString(R.string.stats), getString(
                    R.string.sponsor)))
        availableWidgetTypeList.add(WidgetType(getString(R.string.information), getString(R.string.weather), getString(R.string.twitter), null))
    }

    inner class WidgetAdapter(private val widgetList: List<WidgetType>) : RecyclerView.Adapter<WidgetTypeViewHolder>() {

        override fun onBindViewHolder(holder: WidgetTypeViewHolder, position: Int) {
            val widget = widgetList[position]
            holder.widgetLabelTextView.text = widget.label
            val intent = Intent(baseContext, WidgetStandaloneActivity::class.java)
            intent.putExtra(getString(R.string.widget_type), widget.label)

            updateView(widget.variance1, holder.widgetVarianceButton1, null, intent)
            updateView(widget.variance2, holder.widgetVarianceButton2, holder.divider1, intent)
            updateView(widget.variance3, holder.widgetVarianceButton3, holder.divider, intent)
        }

        private fun updateView(
            varianceText: String?,
            widgetVarianceButton: Button,
            divider: View?,
            intent: Intent
        ) {
            if (varianceText != null) updateButton(widgetVarianceButton, varianceText, intent)
             else visibilityGone(widgetVarianceButton, divider)
        }

        private fun updateButton(
            widgetVarianceButton: Button,
            varianceText: String?,
            intent: Intent
        ) {
            widgetVarianceButton.text = varianceText
            widgetVarianceButton.setOnClickListener {
                startActivity(varianceText, intent)
            }
        }

        private fun visibilityGone(vararg view: View?) {
            view.forEach { v -> v?.visibility = View.GONE }
        }

        private fun startActivity(variance: String?, intent: Intent) {
            intent.putExtra(getString(R.string.widget_variance), variance)
            startActivity(intent)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WidgetTypeViewHolder {
            return WidgetTypeViewHolder(
                LayoutInflater.from(baseContext).inflate(
                    R.layout.widget_type_with_variance_row_element,
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