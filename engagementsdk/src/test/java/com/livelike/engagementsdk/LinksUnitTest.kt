package com.livelike.engagementsdk

import com.livelike.engagementsdk.chat.stickerKeyboard.countMatches
import org.junit.Test


open class LinksUnitTest {

    @Test
    fun testLinks() {
        val linksRegex = "^(http:\\/\\/www\\.|https:\\/\\/www\\.|http:\\/\\/|https:\\/\\/)?[a-z0-9]+([\\-\\.]{1}[a-z0-9]+)\\.[a-z]{2,5}(:[0-9]{1,5})?(\\/.)?\$".toRegex()
        val list = arrayListOf(
            "www.livelike.com",
            "http://www.livelike.com",
            "https://www.livelike.com",
            "livelike.com",
            "bit.ly/livelike",
            "https://www.livelike.com?a=abc",
            "www.livelike.com :LeeHype::LeeHype: livelike.com"
        )
        list.forEach {
            val result = linksRegex.toPattern().matcher(it)
            println("Check-> $it -> ${result.countMatches()}")
        }
    }
}