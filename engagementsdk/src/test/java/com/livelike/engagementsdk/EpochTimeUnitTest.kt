package com.livelike.engagementsdk

import org.junit.Test


class EpochTimeUnitTest {

    @Test
    fun testISOFormats() {
        assert("2020-09-04T16:40:53.000Z".parseISODateTime() != null)
        assert("2020-09-04T16:40:53.000000Z".parseISODateTime() != null)
        assert("2020-09-04T16:40:53+05:30".parseISODateTime() != null)
        assert("2020-09-04T16:40:53.000000+00:00".parseISODateTime() != null)
        assert("2020-09-04T16:40:53Z".parseISODateTime() != null)
    }
}