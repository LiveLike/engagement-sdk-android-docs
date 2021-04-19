package com.livelike.livelikedemo

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.EpochTime
import com.livelike.engagementsdk.chat.ChatRoomInfo
import com.livelike.engagementsdk.chat.LiveLikeChatSession
import com.livelike.engagementsdk.chat.Visibility
import com.livelike.engagementsdk.core.utils.isNetworkConnected
import com.livelike.engagementsdk.publicapis.ErrorDelegate
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.livelikedemo.chatonly.ChatOnlyFragment
import com.livelike.livelikedemo.chatonly.ChatOnlyHomeFragment
import kotlinx.android.synthetic.main.activity_chat_only.container
import kotlinx.android.synthetic.main.activity_chat_only.prg_chat


class ChatOnlyActivity : AppCompatActivity() {
    internal var privateGroupChatsession: LiveLikeChatSession? = null
    internal var chatRoomInfo: ChatRoomInfo? = null
    internal val sessionMap: HashMap<String, LiveLikeChatSession> = hashMapOf()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_only)
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, ChatOnlyHomeFragment.newInstance())
            .addToBackStack("chatHome")
            .commit()
    }


    internal fun changeChatRoom(
        chatRoomId: String,
        showAvatar: Boolean? = null,
        url: String? = null
    ) {
        prg_chat.visibility = View.VISIBLE
        val session = sessionMap[chatRoomId]
        privateGroupChatsession =
            session ?: (application as LiveLikeApplication).createPrivateSessionForMultiple(
                errorDelegate = object : ErrorDelegate() {
                    override fun onError(error: String) {
                        checkForNetworkToRecreateActivity()
                    }
                }, timecodeGetter = object : EngagementSDK.TimecodeGetter {
                    override fun getTimecode(): EpochTime {
                        return EpochTime(0)
                    }
                }
            )
        showAvatar?.let {
            privateGroupChatsession?.shouldDisplayAvatar = it
        }
        url?.let {
            privateGroupChatsession?.avatarUrl = it
        }
        sessionMap[chatRoomId] = privateGroupChatsession!!
        privateGroupChatsession?.connectToChatRoom(chatRoomId, object : LiveLikeCallback<Unit>() {
            override fun onResponse(result: Unit?, error: String?) {
                println("ChatOnlyActivity.onResponse -> $result -> $error")
            }
        })
        (application as LiveLikeApplication).sdk.getChatRoom(
            chatRoomId,
            object : LiveLikeCallback<ChatRoomInfo>() {
                override fun onResponse(result: ChatRoomInfo?, error: String?) {
                    chatRoomInfo = result
                    prg_chat.visibility = View.INVISIBLE
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.container, ChatOnlyFragment.newInstance())
                        .addToBackStack("chat")
                        .commit()
                }
            })
    }

    private fun checkForNetworkToRecreateActivity() {
        container.postDelayed({
            if (isNetworkConnected()) {
                container.post {
                    startActivity(intent)
                    finish()
                }
            } else {
                checkForNetworkToRecreateActivity()
            }
        }, 1000)
    }

    internal fun selectVisibility(visibilityInterface: ChatOnlyFragment.VisibilityInterface) {
        AlertDialog.Builder(this).apply {
            setTitle("Select Visibility")
            setItems(Visibility.values().map { it.toString() }.toTypedArray()) { _, which ->
                val visibility = Visibility.values()[which]
                visibilityInterface.onSelectItem(visibility)
            }
            create()
        }.show()
    }

}
