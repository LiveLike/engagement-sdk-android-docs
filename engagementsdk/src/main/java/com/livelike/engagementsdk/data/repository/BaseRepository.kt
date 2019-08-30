package com.livelike.engagementsdk.data.repository

import com.livelike.engagementsdk.services.network.EngagementDataClientImpl

internal abstract class BaseRepository {

    protected val dataClient = EngagementDataClientImpl()
}
