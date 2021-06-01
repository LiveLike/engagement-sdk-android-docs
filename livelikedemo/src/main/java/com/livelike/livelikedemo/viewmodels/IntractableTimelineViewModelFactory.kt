package com.livelike.livelikedemo.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.livelike.livelikedemo.LiveLikeApplication

class IntractableTimelineViewModelFactory(private val application: Application
) : ViewModelProvider.AndroidViewModelFactory(application)  {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        require(modelClass == NewIntractableTimelineViewModel::class.java) { "Unknown ViewModel class" }
        return NewIntractableTimelineViewModel(
            application as LiveLikeApplication
        ) as T
    }
}