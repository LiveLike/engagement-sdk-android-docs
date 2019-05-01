package com.livelike.livelikesdk.chat

import android.graphics.Color
import android.graphics.Typeface

/**
 *  Builder class for Chat branding. Customize various UI properties of the Chat widget using this class.
 *
 *  TODO: this class can be shortened using kotlin Builder DSL.
 */
internal class ChatTheme(
    var font: Typeface? = Typeface.MONOSPACE,
    var backgroundColor: Int = Color.BLUE,
    var foregroundColor: Int = Color.RED,
    var cellFont: Typeface? = Typeface.SANS_SERIF,
    var cellBackgroundColor: Int = Color.CYAN,
    var cellForegroundColor: Int = Color.GREEN
) {
    private constructor(builder: Builder) : this(
        builder.font,
        builder.backgroundColor,
        builder.foregroundColor,
        builder.cellFont,
        builder.cellBackgroundColor,
        builder.cellForegroundColor
    )

    class Builder {
        var font: Typeface? = null
            private set
        var backgroundColor: Int = 0
            private set

        var foregroundColor: Int = 0
            private set

        var cellFont: Typeface? = null
            private set

        var cellBackgroundColor: Int = 0
            private set

        var cellForegroundColor: Int = 0
            private set

        fun font(font: Typeface) = apply { this.font = font }
        fun backgroundColor(backgroundColor: Int) = apply { this.backgroundColor = backgroundColor }
        fun foregroundColor(foregroundColor: Int) = apply { this.foregroundColor = foregroundColor }
        fun cellFont(cellFont: Typeface) = apply { this.cellFont = cellFont }
        fun cellBackgroundColor(cellBackgroundColor: Int) = apply { this.cellBackgroundColor = cellBackgroundColor }
        fun cellForegroundColor(cellForegroundColor: Int) = apply { this.cellForegroundColor = cellForegroundColor }

        fun build() = ChatTheme(this)
    }
}
