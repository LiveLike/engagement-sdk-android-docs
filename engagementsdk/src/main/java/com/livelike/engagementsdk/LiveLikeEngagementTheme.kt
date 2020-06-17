package com.livelike.engagementsdk

import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.livelike.engagementsdk.core.services.network.Result
import com.livelike.engagementsdk.core.utils.gson
import com.livelike.engagementsdk.widget.BaseTheme
import com.livelike.engagementsdk.widget.WidgetsTheme
import java.lang.RuntimeException

class LiveLikeEngagementTheme internal constructor(
    val chat: Map<String, Any?>? = null,
    val version: Double,
    val widgets: WidgetsTheme
) : BaseTheme() {

    override fun validate(): String? {
        return widgets.validate()
    }

    companion object {
        @JvmStatic
        fun instanceFrom(themeJson: JsonObject): Result<LiveLikeEngagementTheme> {
            return try {
                val data = gson.fromJson(
                    themeJson,
                    LiveLikeEngagementTheme::class.java
                )
                val errorString = data.validate()
                if (errorString == null) {
                    Result.Success(data)
                } else {
                    Result.Error(RuntimeException(errorString))
                }
            } catch (ex: JsonParseException) {
                Result.Error(ex)
            }
        }
    }
}
