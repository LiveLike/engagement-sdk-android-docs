package com.livelike.livelikedemo.utils

import com.livelike.engagementsdk.LiveLikeEngagementTheme
import java.util.Random

/**
 * Created by Shivansh Mittal on 16/06/20.
 */

class ThemeRandomizer {

    companion object {

        fun nextTheme(): LiveLikeEngagementTheme? {
            if (themesList.size > 0) {
                return themesList[Random().nextInt(themesList.size)]
            }
            return null
        }

        var themesList = mutableListOf<LiveLikeEngagementTheme>()
    }
}
