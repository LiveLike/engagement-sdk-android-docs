package com.livelike.livelikedemo.chatonly

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.livelike.engagementsdk.LiveLikeUser
import com.livelike.engagementsdk.chat.ChatRoomInfo
import com.livelike.engagementsdk.chat.Visibility
import com.livelike.engagementsdk.chat.data.remote.LiveLikePagination
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.livelikedemo.ChatOnlyActivity
import com.livelike.livelikedemo.LiveLikeApplication
import com.livelike.livelikedemo.R
import kotlinx.android.synthetic.main.fragment_chat_only.btn_chat_room_members
import kotlinx.android.synthetic.main.fragment_chat_only.chat_view
import kotlinx.android.synthetic.main.fragment_chat_only.prg_members
import kotlinx.android.synthetic.main.fragment_chat_only.prg_visibility
import kotlinx.android.synthetic.main.fragment_chat_only.txt_chat_room_id
import kotlinx.android.synthetic.main.fragment_chat_only.txt_chat_room_members_count
import kotlinx.android.synthetic.main.fragment_chat_only.txt_chat_room_title
import kotlinx.android.synthetic.main.fragment_chat_only.txt_chat_room_visibility


class ChatOnlyFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
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
                (activity?.application as? LiveLikeApplication)?.sdk?.getMembersOfChatRoom(id,
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
                                    AlertDialog.Builder(context).apply {
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
                            })
                    }
                })
            }
        }

        (activity as? ChatOnlyActivity)?.privateGroupChatsession?.let {
            chat_view.setSession(it)
        }
        (activity as? ChatOnlyActivity)?.chatRoomInfo?.let {
            updateData(it)
        }
    }


    private fun updateData(result: ChatRoomInfo?) {
        result?.let {
            txt_chat_room_title.text = it.title ?: "No Title"
            txt_chat_room_id.text = it.id
            txt_chat_room_members_count.text = ""
            txt_chat_room_visibility.text = it.visibility?.name
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