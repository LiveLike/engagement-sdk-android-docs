package com.livelike.engagementsdk.widget.data.respository

import android.content.Context
import com.livelike.engagementsdk.widget.data.models.WidgetUserInteractionBase

/**
 * Repository that handles user's widget interaction data. It knows what data sources need to be
 * triggered to get widget interaction and where to store the data.
**/
internal class WidgetInteractionRepository(val context : Context) {


    fun getWidgetInteraction(widgetId: String) : WidgetUserInteractionBase?{
        return null
    }


}