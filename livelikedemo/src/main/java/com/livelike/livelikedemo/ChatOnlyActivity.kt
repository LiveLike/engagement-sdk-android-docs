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
import com.livelike.engagementsdk.LiveLikeUser
import com.livelike.engagementsdk.chat.ChatRoomInfo
import com.livelike.engagementsdk.chat.LiveLikeChatSession
import com.livelike.engagementsdk.chat.Visibility
import com.livelike.engagementsdk.chat.data.remote.ChatRoomMembership
import com.livelike.engagementsdk.chat.data.remote.LiveLikePagination
import com.livelike.engagementsdk.core.utils.isNetworkConnected
import com.livelike.engagementsdk.publicapis.ErrorDelegate
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import kotlinx.android.synthetic.main.activity_chat_only.btn_change
import kotlinx.android.synthetic.main.activity_chat_only.btn_chat_room_members
import kotlinx.android.synthetic.main.activity_chat_only.btn_create
import kotlinx.android.synthetic.main.activity_chat_only.btn_delete
import kotlinx.android.synthetic.main.activity_chat_only.btn_join
import kotlinx.android.synthetic.main.activity_chat_only.btn_refresh
import kotlinx.android.synthetic.main.activity_chat_only.btn_visibility
import kotlinx.android.synthetic.main.activity_chat_only.chat_view
import kotlinx.android.synthetic.main.activity_chat_only.ed_chat_room_id
import kotlinx.android.synthetic.main.activity_chat_only.ed_chat_room_title
import kotlinx.android.synthetic.main.activity_chat_only.prg_create
import kotlinx.android.synthetic.main.activity_chat_only.prg_delete
import kotlinx.android.synthetic.main.activity_chat_only.prg_join
import kotlinx.android.synthetic.main.activity_chat_only.prg_members
import kotlinx.android.synthetic.main.activity_chat_only.prg_refresh
import kotlinx.android.synthetic.main.activity_chat_only.prg_visibility
import kotlinx.android.synthetic.main.activity_chat_only.txt_chat_room_id
import kotlinx.android.synthetic.main.activity_chat_only.txt_chat_room_members_count
import kotlinx.android.synthetic.main.activity_chat_only.txt_chat_room_title
import kotlinx.android.synthetic.main.activity_chat_only.txt_chat_room_visibility

class ChatOnlyActivity : AppCompatActivity() {
    private lateinit var privateGroupChatsession: LiveLikeChatSession
    private var chatRoomList: ArrayList<ChatRoomInfo> = arrayListOf()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_only)

        btn_create.setOnClickListener {
            val title = ed_chat_room_title.text.toString()
            val visibility =
                if (btn_visibility.text.toString().toLowerCase().contains("visibility").not())
                    Visibility.valueOf(btn_visibility.text.toString())
                else
                    Visibility.everyone
            prg_create.visibility = View.VISIBLE
            (application as LiveLikeApplication).sdk.createChatRoom(
                title,
                visibility,
                object : LiveLikeCallback<ChatRoomInfo>() {
                    override fun onResponse(result: ChatRoomInfo?, error: String?) {
                        val response = when {
                            result != null -> "${result.title ?: "No Title"}(${result.id}),  Room Id copy to clipboard"
                            else -> error
                        }
                        val clipboard =
                            getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("copied ChatRoomId", result?.id)
                        clipboard.primaryClip = clip
                        response?.let { it1 -> showToast(it1) }

                        ed_chat_room_title.setText("")
                        prg_create.visibility = View.INVISIBLE

                    }
                })
        }
        btn_join.setOnClickListener {
            val id = ed_chat_room_id.text.toString()
            if (id.isEmpty()) {
                showToast("Enter Room Id First")
                return@setOnClickListener
            }
            prg_join.visibility = View.VISIBLE
            (application as LiveLikeApplication).sdk.addCurrentUserToChatRoom(id,
                object : LiveLikeCallback<ChatRoomMembership>() {
                    override fun onResponse(result: ChatRoomMembership?, error: String?) {
                        result?.let {
                            showToast("User Added Successfully")
                        }
                        ed_chat_room_id.setText("")
                        error?.let {
                            showToast(it)
                        }
                        prg_join.visibility = View.INVISIBLE
                        btn_refresh.callOnClick()
                    }
                })
        }
        btn_change.setOnClickListener {
            AlertDialog.Builder(this).apply {
                setTitle("Select a private group")
                setItems(chatRoomList.map { it.id }.toTypedArray()) { _, which ->
                    // On change of theme we need to create the session in order to pass new attribute of theme to widgets and chat
//                    (application as LiveLikeApplication).removePrivateSession()
                    changeChatRoom(chatRoomList.elementAt(which).id)
                }
                create()
            }.show()
        }

        btn_refresh.setOnClickListener {
            prg_refresh.visibility = View.VISIBLE
            (application as LiveLikeApplication).sdk.getCurrentUserChatRoomList(
                LiveLikePagination.FIRST,
                object : LiveLikeCallback<List<ChatRoomInfo>>() {
                    override fun onResponse(result: List<ChatRoomInfo>?, error: String?) {
                        prg_refresh.visibility = View.INVISIBLE
                        chatRoomList.clear()
                        result?.let { it1 ->
                            chatRoomList.addAll(it1)
                        }
                        error?.let {
                            showToast(it)
                        }
                    }
                })
        }
        btn_chat_room_members.setOnClickListener {
            val id = txt_chat_room_id.text.toString()
            if (id.isNotEmpty()) {
                prg_members.visibility = View.VISIBLE
                (application as LiveLikeApplication).sdk.getMembersOfChatRoom(id,
                    LiveLikePagination.FIRST,
                    object : LiveLikeCallback<List<LiveLikeUser>>() {
                        override fun onResponse(result: List<LiveLikeUser>?, error: String?) {
                            if (result?.isNotEmpty() == true)
                                txt_chat_room_members_count?.text = "Members: ${result?.size ?: 0}"
                            else
                                txt_chat_room_members_count?.text = ""
                            prg_members.visibility = View.INVISIBLE
                            result?.let { list ->
                                if (list.isNotEmpty()) {
                                    AlertDialog.Builder(this@ChatOnlyActivity).apply {
                                        setTitle("Room Members")
                                        setItems(list.map { it.nickname }
                                            .toTypedArray()) { _, which ->
                                            // On change of theme we need to create the session in order to pass new attribute of theme to widgets and chat
                                        }
                                        create()
                                    }.show()
                                }
                            }
                        }
                    })
            } else {
                showToast("Select Room")
            }
        }
        btn_delete.setOnClickListener {
            val id = txt_chat_room_id.text.toString()
            if (id.isNotEmpty()) {
                prg_delete.visibility = View.VISIBLE
                (application as LiveLikeApplication).sdk.deleteCurrentUserFromChatRoom(id,
                    object : LiveLikeCallback<Boolean>() {
                        override fun onResponse(result: Boolean?, error: String?) {
                            result?.let {
                                showToast("Deleted ChatRoom")
                                privateGroupChatsession.exitChatRoom(id)
                                privateGroupChatsession.close()
                                (application as LiveLikeApplication).removePrivateSession()
                                chat_view.visibility = View.INVISIBLE
                                txt_chat_room_id.text = ""
                                txt_chat_room_title.text = ""
                                txt_chat_room_members_count.text = ""
                                txt_chat_room_visibility.text = ""
                            }
                            prg_delete.visibility = View.INVISIBLE
                            btn_refresh.callOnClick()
                        }
                    })
            } else {
                showToast("Select Room")
            }
        }
        btn_visibility.setOnClickListener {
            selectVisibility(object : VisibilityInterface {
                override fun onSelectItem(visibility: Visibility) {
                    btn_visibility.text = visibility.name
                }
            })
        }
        txt_chat_room_visibility.setOnClickListener {
            if (txt_chat_room_id.text.isNotEmpty()) {
                selectVisibility(object : VisibilityInterface {
                    override fun onSelectItem(visibility: Visibility) {
                        prg_visibility.visibility = View.VISIBLE
                        (application as LiveLikeApplication).sdk.updateChatRoom(txt_chat_room_id.text.toString(),
                            txt_chat_room_title.text.toString(),
                            visibility,
                            object : LiveLikeCallback<ChatRoomInfo>() {
                                override fun onResponse(result: ChatRoomInfo?, error: String?) {
                                    prg_visibility.visibility = View.INVISIBLE
                                    updateData(result)
                                    error?.let {
                                        showToast(it)
                                    }
                                }
                            })
                    }
                })
            }
        }
        btn_refresh.callOnClick()
    }

    private fun selectVisibility(visibilityInterface: VisibilityInterface) {
        AlertDialog.Builder(this).apply {
            setTitle("Select Visibility")
            setItems(Visibility.values().map { it.toString() }.toTypedArray()) { _, which ->
                val visibility = Visibility.values()[which]
                visibilityInterface.onSelectItem(visibility)
            }
            create()
        }.show()
    }

    interface VisibilityInterface {
        fun onSelectItem(visibility: Visibility)
    }

    private fun updateData(result: ChatRoomInfo?) {
        result?.let {
            txt_chat_room_title.text = it.title ?: "No Title"
            txt_chat_room_id.text = it.id
            txt_chat_room_members_count.text = ""
            txt_chat_room_visibility.text = it.visibility?.name
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
            object : LiveLikeCallback<ChatRoomInfo>() {
                override fun onResponse(result: ChatRoomInfo?, error: String?) {
                    updateData(result)
                }
            })
        chat_view.visibility = View.VISIBLE
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
