package com.livelike.livelikedemo.chatonly

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.livelike.engagementsdk.ChatRoomListener
import com.livelike.engagementsdk.LiveLikeUser
import com.livelike.engagementsdk.chat.ChatRoomInfo
import com.livelike.engagementsdk.chat.Visibility
import com.livelike.engagementsdk.chat.data.remote.LiveLikePagination
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.livelikedemo.ChatOnlyActivity
import com.livelike.livelikedemo.LiveLikeApplication
import com.livelike.livelikedemo.R
import kotlinx.android.synthetic.main.fragment_chat_only.*

class ChatOnlyFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_chat_only, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        btn_chat_room_members.setOnClickListener {
            val id = txt_chat_room_id.text.toString()
            if (id.isNotEmpty()) {
                prg_members.visibility = View.VISIBLE
                (activity?.application as? LiveLikeApplication)?.sdk?.getMembersOfChatRoom(
                    id,
                    LiveLikePagination.FIRST,
                    object : LiveLikeCallback<List<LiveLikeUser>>() {
                        override fun onResponse(result: List<LiveLikeUser>?, error: String?) {
                            if (result?.isNotEmpty() == true)
                                txt_chat_room_members_count?.text = "Members: ${result.size}"
                            else
                                txt_chat_room_members_count?.text = ""
                            prg_members.visibility = View.INVISIBLE
                            result?.let { list ->
                                if (list.isNotEmpty()) {
                                    AlertDialog.Builder(context).apply {
                                        setTitle("Room Members")
                                        setItems(
                                            list.map { "${it.nickname} (${it.id})" }
                                                .toTypedArray()
                                        ) { _, which ->
                                            // On change of theme we need to create the session in order to pass new attribute of theme to widgets and chat
                                            val item = list[which]
                                            item.let {
                                                val clipboard =
                                                    context?.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                                val clip = ClipData.newPlainText("copied User Id", item.id)
                                                clipboard?.setPrimaryClip(clip)
                                            }
                                        }
                                        create()
                                    }.show()
                                }
                            }
                        }
                    }
                )
            } else {
                showToast("Select Room")
            }
        }

        txt_chat_room_visibility.setOnClickListener {
            if (txt_chat_room_id.text.isNotEmpty()) {
                (activity as? ChatOnlyActivity)?.selectVisibility(object : VisibilityInterface {
                    override fun onSelectItem(visibility: Visibility) {
                        prg_visibility.visibility = View.VISIBLE
                        (activity?.application as? LiveLikeApplication)?.sdk?.updateChatRoom(
                            txt_chat_room_id.text.toString(),
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
                            }
                        )
                    }
                })
            }
        }

        (activity as? ChatOnlyActivity)?.privateGroupChatsession?.let {
            chat_view.setSession(it)
            it.setChatRoomListener(object : ChatRoomListener {
                override fun onChatRoomUpdate(chatRoom: ChatRoomInfo) {
                    (activity as? ChatOnlyActivity)?.chatRoomInfo = chatRoom
                    txt_chat_room_id?.post {
                        updateData(chatRoom)
                    }
                }
            })
        }
        (activity as? ChatOnlyActivity)?.chatRoomInfo?.let {
            txt_chat_room_id.post {
                updateData(it)
            }
        }
        txt_chat_room_id.setOnClickListener {
            val clipboard =
                context?.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText("copied ChatRoom Id", txt_chat_room_id.text.toString())
            clipboard?.setPrimaryClip(clip)
        }

        btn_chat_room_1.setOnClickListener {
            (activity as? ChatOnlyActivity)?.privateGroupChatsession?.connectToChatRoom("564a120f-b3d9-41a0-8f21-8827dcb79866")
        }
        btn_chat_room_2.setOnClickListener {
            (activity as? ChatOnlyActivity)?.privateGroupChatsession?.connectToChatRoom("af5e6e1e-ea65-4195-9f15-ccbaa8a1ffbe")
        }
    }

    private fun updateData(result: ChatRoomInfo?) {
        result?.let {
            txt_chat_room_title.text =
                "Title: ${it.title}\nVisibility: ${it.visibility?.name}\nContent Filter: ${it.contentFilter}\nCustom Data: ${it.customData}"
            txt_chat_room_id.text = it.id
            txt_chat_room_members_count.text = ""
            txt_chat_room_visibility.text = it.visibility?.name
            chat_view.isChatInputVisible = (it.contentFilter == "producer").not()
        }
    }

    fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    interface VisibilityInterface {
        fun onSelectItem(visibility: Visibility)
    }

    companion object {
        @JvmStatic
        fun newInstance() = ChatOnlyFragment()
    }
}
