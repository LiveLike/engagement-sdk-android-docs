package com.livelike.livelikedemo.utils

import com.livelike.engagementsdk.LiveLikeEngagementTheme
import java.util.Random

/**
 * Created by Shivansh Mittal on 16/06/20.
 */

class ThemeRandomizer {

    companion object {

        var themesList = mutableListOf<LiveLikeEngagementTheme>()
        var mLastindex = 1

        fun nextTheme(): LiveLikeEngagementTheme? {
            if (themesList.size > 1) {
                var nextRandomIndex = mLastindex
                while (mLastindex == nextRandomIndex) {
                    nextRandomIndex = Random().nextInt(themesList.size)
                }
                mLastindex = nextRandomIndex
                return themesList[nextRandomIndex]
            }
            return null
        }
    }
}
