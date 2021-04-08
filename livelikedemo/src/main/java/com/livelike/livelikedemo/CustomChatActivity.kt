package com.livelike.livelikedemo

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.livelike.livelikedemo.customchat.ChatFragment
import com.livelike.livelikedemo.customchat.HomeChat
import com.livelike.livelikedemo.customchat.HomeFragment
import java.util.Calendar

class CustomChatActivity : AppCompatActivity() {

    var selectedHomeChat: HomeChat? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_chat)
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, HomeFragment.newInstance())
            .commit()
    }

    fun showChatScreen(homeChat: HomeChat) {
        selectedHomeChat = homeChat
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, ChatFragment.newInstance())
            .addToBackStack(homeChat.toString())
            .commit()
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            selectedHomeChat?.let {
                val sharedPref =
                    application.getSharedPreferences(PREFERENCES_APP_ID, Context.MODE_PRIVATE)
                sharedPref.edit().putLong(
                    "msg_time_${it.channel.llProgram}",
                    Calendar.getInstance().timeInMillis
                ).apply()
            }
            supportFragmentManager.popBackStack()
        } else {
            super.onBackPressed();
        }
    }
}