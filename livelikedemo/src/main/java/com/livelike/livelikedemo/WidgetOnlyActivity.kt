package com.livelike.livelikedemo

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_widget_only)

        toolbar.apply {
            title = "Widgets"
            setNavigationIcon(R.drawable.ic_close_black_24dp)
            setBackgroundColor(Color.parseColor("#00ae8b"))
            setNavigationOnClickListener {
            }
        }

        val widgetsList = mutableListOf<Widgets>()

        widgetsList.add(Widgets("POLL", "Text", "Image", null))
        widgetsList.add(Widgets("QUIZ", "Text", "Image", null))
        widgetsList.add(Widgets("PREDICTION", "Text", "Image", null))
        widgetsList.add(Widgets("CHEER_METER", "Clapping", null, null))
        widgetsList.add(Widgets("EMOJI", "Poll", "Slider", null))
        widgetsList.add(Widgets("ALERT", "NEWS", "STATS", "SPONSOR"))
        widgetsList.add(Widgets("INFORMATION", "Weather", "Twitter", null))

        val linearLayoutManager = LinearLayoutManager(baseContext)
        widget_only.layoutManager = linearLayoutManager
        widget_only.adapter = WidgetAdapter(widgetsList)

    }

    inner class WidgetAdapter(private val widgetList: MutableList<Widgets>) : RecyclerView.Adapter<ViewHolder>() {
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val option = widgetList[position]
            holder.type.text = option.type

            if (option.option1 != null) holder.button1.text = option.option1
            else holder.button1.visibility = View.GONE

            if (option.option2 != null) holder.button2.text = option.option2
            else {
                holder.divider1.visibility = View.GONE
                holder.button2.visibility = View.GONE
            }

            if (option.option3 != null) holder.button3.text = option.option3
            else  {
                holder.divider.visibility = View.GONE
                holder.button3.visibility = View.GONE
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(
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

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val type : TextView = view.standalone_label
        val button1 : Button = view.widgets_only_button_1
        val button2 : Button = view.widgets_only_button_2
        val button3 : Button = view.widgets_only_button_3
        val divider1 : View = view.standalone_divider
        val divider : View = view.standalone_divider_2
    }
}

data class Widgets(val type: String, val option1: String?, val option2: String?, val option3: String?)
