package com.livelike.livelikedemo.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.livelike.livelikedemo.LiveLikeApplication

class LiveBlogModelFactory(
    private val application: Application
) : ViewModelProvider.AndroidViewModelFactory(application)  {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        require(modelClass == LiveBlogViewModel::class.java) { "Unknown ViewModel class" }
        return LiveBlogViewModel(
            application as LiveLikeApplication
        ) as T
    }
}
