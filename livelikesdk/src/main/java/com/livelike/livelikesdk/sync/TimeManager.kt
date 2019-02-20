package com.livelike.livelikesdk.sync

import com.livelike.livelikesdk.messaging.EpochTime

class TimeManager(val playheadPosition: () -> EpochTime)