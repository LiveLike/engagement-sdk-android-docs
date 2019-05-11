package com.livelike.livelikesdk.widget.view

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.google.gson.JsonObject
import com.livelike.engagementsdkapi.LiveLikeContentSession
import com.livelike.livelikesdk.util.AndroidResource
import com.livelike.livelikesdk.util.gson
import com.livelike.livelikesdk.widget.model.Alert
import com.livelike.livelikesdk.widget.model.Resource

internal class AlertWidgetViewModel(application: Application) : AndroidViewModel(application), WidgetViewBuilder {
    var payload: JsonObject = JsonObject()
        set(value) {
            field = value
            data = gson.fromJson(payload.toString(), Alert::class.java)
                ?: error("hello from json") // could have this in the data layer, doesn't really belong
        }
    var session: LiveLikeContentSession? = null
    var data = Alert()

    override fun createView(): View {
        val timeout = AndroidResource.parseDuration(gson.fromJson(payload, Resource::class.java).timeout)
        Handler().postDelayed({ dismiss() }, timeout)
        return AlertWidgetView(session?.applicationContext as AppCompatActivity)
    }

    fun voteForOption(optionId: String) {
        //
    }

    fun dismiss() {
        // The session should be global to allow the widget to dismiss itself
        // USE DI !!!!!!
        session?.widgetStream?.onNext(null)
    }
}

internal interface WidgetViewBuilder {
    fun createView(): View
}
