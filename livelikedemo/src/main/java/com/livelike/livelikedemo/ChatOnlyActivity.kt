package com.livelike.livelikedemo

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.EpochTime
import com.livelike.engagementsdk.chat.ChatRoom
import com.livelike.engagementsdk.chat.LiveLikeChatSession
import com.livelike.engagementsdk.core.utils.isNetworkConnected
import com.livelike.engagementsdk.publicapis.ErrorDelegate
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import kotlinx.android.synthetic.main.activity_chat_only.btn_change
import kotlinx.android.synthetic.main.activity_chat_only.btn_create
import kotlinx.android.synthetic.main.activity_chat_only.chat_view
import kotlinx.android.synthetic.main.activity_chat_only.ed_chat_room_title
import kotlinx.android.synthetic.main.activity_chat_only.prg_create
import kotlinx.android.synthetic.main.activity_chat_only.txt_chat_room_id
import kotlinx.android.synthetic.main.activity_chat_only.txt_chat_room_title

class ChatOnlyActivity : AppCompatActivity() {
    private lateinit var privateGroupChatsession: LiveLikeChatSession
    private var chatRoomIds: MutableSet<String> = mutableSetOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_only)
        getSharedPreferences(PREFERENCES_APP_ID, Context.MODE_PRIVATE).apply {
            chatRoomIds = getStringSet(CHAT_ROOM_LIST, mutableSetOf()) ?: mutableSetOf()
        }
        btn_create.setOnClickListener {
            val title = ed_chat_room_title.text.toString()

            prg_create.visibility = View.VISIBLE
            (application as LiveLikeApplication).sdk.createChatRoom(
                title,
                object : LiveLikeCallback<ChatRoom>() {
                    override fun onResponse(result: ChatRoom?, error: String?) {
                        val response = when {
                            result != null -> "${result.title ?: "No Title"}(${result.id})"
                            else -> error
                        }
                        response?.let { it1 -> showToast(it1) }
                        result?.let {
                            chatRoomIds.add(it.id)
                            getSharedPreferences(PREFERENCES_APP_ID, Context.MODE_PRIVATE)
                                .edit().apply {
                                    putStringSet(CHAT_ROOM_LIST, chatRoomIds).apply()
                                }
                        }
                        ed_chat_room_title.setText("")
                        prg_create.visibility = View.INVISIBLE
                    }
                })
        }
        btn_change.setOnClickListener {
            AlertDialog.Builder(this).apply {
                setTitle("Select a private group")
                setItems(chatRoomIds.toTypedArray()) { _, which ->
                    // On change of theme we need to create the session in order to pass new attribute of theme to widgets and chat
                    (application as LiveLikeApplication).removePrivateSession()

                    //Copy to clipboard
                    val clipboard: ClipboardManager =
                        getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("label", chatRoomIds.elementAt(which))
                    clipboard.primaryClip = clip
                    showToast("Room Id Copy To Clipboard")

                    changeChatRoom(chatRoomIds.elementAt(which))
                }
                create()
            }.show()
        }
    }

    private fun changeChatRoom(chatRoomId: String) {
        privateGroupChatsession =
            (application as LiveLikeApplication).createPrivateSession(
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
        privateGroupChatsession.enterChatRoom(chatRoomId)
        txt_chat_room_id.visibility = View.VISIBLE
        txt_chat_room_title.visibility = View.VISIBLE
        (application as LiveLikeApplication).sdk.getChatRoom(
            chatRoomId,
            object : LiveLikeCallback<ChatRoom>() {
                override fun onResponse(result: ChatRoom?, error: String?) {
                    result?.let {
                        txt_chat_room_title.text = it.title ?: "No Title"
                        txt_chat_room_id.text = it.id
                    }
                }
            })
        chat_view.setSession(privateGroupChatsession)
    }

    private fun checkForNetworkToRecreateActivity() {
        chat_view.postDelayed({
            if (isNetworkConnected()) {
                chat_view.post {
                    startActivity(intent)
                    finish()
                }
            } else {
                checkForNetworkToRecreateActivity()
            }
        }, 1000)
    }

    fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
