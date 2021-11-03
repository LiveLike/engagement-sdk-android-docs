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
import androidx.recyclerview.widget.RecyclerView
import com.livelike.engagementsdk.chat.ChatRoomInfo
import com.livelike.engagementsdk.chat.Visibility
import com.livelike.engagementsdk.chat.data.remote.ChatRoomMembership
import com.livelike.engagementsdk.chat.data.remote.LiveLikePagination
import com.livelike.engagementsdk.publicapis.BlockedData
import com.livelike.engagementsdk.publicapis.ChatRoomAdd
import com.livelike.engagementsdk.publicapis.ChatRoomDelegate
import com.livelike.engagementsdk.publicapis.ChatRoomInvitation
import com.livelike.engagementsdk.publicapis.ChatRoomInvitationStatus
import com.livelike.engagementsdk.publicapis.ChatUserMuteStatus
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.publicapis.LiveLikeEmptyResponse
import com.livelike.engagementsdk.publicapis.LiveLikeUserApi
import com.livelike.livelikedemo.ChatOnlyActivity
import com.livelike.livelikedemo.LiveLikeApplication
import com.livelike.livelikedemo.R
import kotlinx.android.synthetic.main.block_list_item.view.btn_unblock
import kotlinx.android.synthetic.main.block_list_item.view.txt_title
import kotlinx.android.synthetic.main.chat_only_check_box.view.chk_avatar
import kotlinx.android.synthetic.main.chat_only_check_box.view.ed_avatar
import kotlinx.android.synthetic.main.fragment_chat_only_home.btn_add
import kotlinx.android.synthetic.main.fragment_chat_only_home.btn_block
import kotlinx.android.synthetic.main.fragment_chat_only_home.btn_block_list
import kotlinx.android.synthetic.main.fragment_chat_only_home.btn_change
import kotlinx.android.synthetic.main.fragment_chat_only_home.btn_create
import kotlinx.android.synthetic.main.fragment_chat_only_home.btn_delete
import kotlinx.android.synthetic.main.fragment_chat_only_home.btn_invite
import kotlinx.android.synthetic.main.fragment_chat_only_home.btn_invite_by_list
import kotlinx.android.synthetic.main.fragment_chat_only_home.btn_invite_list
import kotlinx.android.synthetic.main.fragment_chat_only_home.btn_invite_list_first
import kotlinx.android.synthetic.main.fragment_chat_only_home.btn_invite_list_next
import kotlinx.android.synthetic.main.fragment_chat_only_home.btn_invite_list_previous
import kotlinx.android.synthetic.main.fragment_chat_only_home.btn_join
import kotlinx.android.synthetic.main.fragment_chat_only_home.btn_mute_status
import kotlinx.android.synthetic.main.fragment_chat_only_home.btn_refresh
import kotlinx.android.synthetic.main.fragment_chat_only_home.btn_visibility
import kotlinx.android.synthetic.main.fragment_chat_only_home.ed_block_profile_id
import kotlinx.android.synthetic.main.fragment_chat_only_home.ed_chat_room_id
import kotlinx.android.synthetic.main.fragment_chat_only_home.ed_chat_room_id_1
import kotlinx.android.synthetic.main.fragment_chat_only_home.ed_chat_room_id_invite_1
import kotlinx.android.synthetic.main.fragment_chat_only_home.ed_chat_room_title
import kotlinx.android.synthetic.main.fragment_chat_only_home.ed_user_id
import kotlinx.android.synthetic.main.fragment_chat_only_home.ed_user_invite_id
import kotlinx.android.synthetic.main.fragment_chat_only_home.prg_add
import kotlinx.android.synthetic.main.fragment_chat_only_home.prg_add_invite
import kotlinx.android.synthetic.main.fragment_chat_only_home.prg_block
import kotlinx.android.synthetic.main.fragment_chat_only_home.prg_create
import kotlinx.android.synthetic.main.fragment_chat_only_home.prg_delete
import kotlinx.android.synthetic.main.fragment_chat_only_home.prg_invite_by_list
import kotlinx.android.synthetic.main.fragment_chat_only_home.prg_invite_list
import kotlinx.android.synthetic.main.fragment_chat_only_home.prg_join
import kotlinx.android.synthetic.main.fragment_chat_only_home.prg_mute
import kotlinx.android.synthetic.main.fragment_chat_only_home.prg_refresh
import kotlinx.android.synthetic.main.fragment_chat_only_home.rcyl_block
import kotlinx.android.synthetic.main.fragment_chat_only_home.rcyl_invite
import kotlinx.android.synthetic.main.invite_list_item.view.btn_accept
import kotlinx.android.synthetic.main.invite_list_item.view.btn_reject
import kotlinx.android.synthetic.main.invite_list_item.view.txt_invitation
import kotlinx.android.synthetic.main.user_list_item.view.txt_name
import java.util.Locale

class ChatOnlyHomeFragment : Fragment() {

    private var chatRoomList: ArrayList<ChatRoomInfo> = arrayListOf()
    private val inviteAdapter =
        InviteListAdapter(
            ::updateInviteStatus
        )

    //    private val adapter = UserAdapter()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_chat_only_home, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val adapter = BlockedListAdapter(::unblock)
        rcyl_block.adapter = adapter
        btn_block_list.setOnClickListener {
            val profileId = ed_block_profile_id.text.toString()
            prg_block.visibility = View.VISIBLE
            (activity?.application as? LiveLikeApplication)?.sdk?.getBlockedProfileList(
                LiveLikePagination.FIRST,
                profileId,
                object : LiveLikeCallback<List<BlockedData>>() {
                    override fun onResponse(result: List<BlockedData>?, error: String?) {
                        prg_block.visibility = View.INVISIBLE
                        error?.let { it1 -> showToast(it1) }
                        result?.let {
                            adapter.blockedList.clear()
                            adapter.blockedList.addAll(it)
                            adapter.notifyDataSetChanged()
                        }
                    }
                })
        }
        btn_block.setOnClickListener {
            val profileId = ed_block_profile_id.text.toString()
            prg_block.visibility = View.VISIBLE
            (activity?.application as? LiveLikeApplication)?.sdk?.blockProfile(
                profileId,
                object : LiveLikeCallback<BlockedData>() {
                    override fun onResponse(result: BlockedData?, error: String?) {
                        prg_block.visibility = View.INVISIBLE
                        result?.let {
                            showToast("BLocked User: ${it.blockedProfileID}")
                        }
                        error?.let { it1 -> showToast(it1) }
                    }
                })
        }
        btn_create.setOnClickListener {
            val title = ed_chat_room_title.text.toString()
            val visibility =
                if (btn_visibility.text.toString().lowercase(Locale.getDefault()).contains("visibility").not())
                    Visibility.valueOf(btn_visibility.text.toString())
                else
                    Visibility.everyone
            prg_create.visibility = View.VISIBLE
            (activity?.application as? LiveLikeApplication)?.sdk?.createChatRoom(
                title,
                visibility,
                object : LiveLikeCallback<ChatRoomInfo>() {
                    override fun onResponse(result: ChatRoomInfo?, error: String?) {
                        val response = when {
                            result != null ->
                                "${
                                    result.title
                                        ?: "No Title"
                                }(${result.id}),  Room Id copy to clipboard"
                            else -> error
                        }
                        var clipboard =
                            context?.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        var clip = ClipData.newPlainText("copied ChatRoomId", result?.id)
                        clipboard?.setPrimaryClip(clip)
                        response?.let { it1 -> showToast(it1) }

                        ed_chat_room_title.setText("")
                        prg_create.visibility = View.INVISIBLE
                    }
                }
            )
        }

        btn_join.setOnClickListener {
            val id = ed_chat_room_id.text.toString()
            if (id.isEmpty()) {
                showToast("Enter Room Id First")
                return@setOnClickListener
            }
            prg_join.visibility = View.VISIBLE
            (activity?.application as? LiveLikeApplication)?.sdk?.addCurrentUserToChatRoom(
                id,
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
                }
            )
        }

        btn_add.setOnClickListener {
            val chatRoomId = ed_chat_room_id_1.text.toString()
            val userId = ed_user_id.text.toString()
            if (chatRoomId.isEmpty() || userId.isEmpty()) {
                showToast("Enter Room Id,User Id First")
                return@setOnClickListener
            }
            prg_add.visibility = View.VISIBLE
            (activity?.application as? LiveLikeApplication)?.sdk?.addUserToChatRoom(chatRoomId,
                userId,
                object : LiveLikeCallback<ChatRoomMembership>() {
                    override fun onResponse(result: ChatRoomMembership?, error: String?) {
                        result?.let {
                            showToast("User Added Successfully")
                        }
                        ed_chat_room_id_1.setText("")
                        ed_user_id.setText("")
                        error?.let {
                            showToast(it)
                        }
                        prg_add.visibility = View.INVISIBLE
                        btn_refresh.callOnClick()
                    }
                })
        }

        btn_invite.setOnClickListener {
            val chatRoomId = ed_chat_room_id_invite_1.text.toString()
            val userId = ed_user_invite_id.text.toString()
            if (chatRoomId.isEmpty() || userId.isEmpty()) {
                showToast("Enter Room Id,User Id First")
                return@setOnClickListener
            }
            prg_add_invite.visibility = View.VISIBLE
            (activity?.application as? LiveLikeApplication)?.sdk?.sendChatRoomInviteToUser(
                chatRoomId,
                userId,
                object : LiveLikeCallback<ChatRoomInvitation>() {
                    override fun onResponse(result: ChatRoomInvitation?, error: String?) {
                        result?.let {
                            showToast("User Invited Successfully")
                        }
                        ed_chat_room_id_invite_1.setText("")
                        ed_user_invite_id.setText("")
                        error?.let {
                            showToast(it)
                        }
                        prg_add_invite.visibility = View.INVISIBLE
                        btn_refresh.callOnClick()
                    }
                })
        }

        btn_mute_status.setOnClickListener {
            val id = ed_chat_room_id.text.toString()
            if (id.isEmpty()) {
                showToast("Enter Room Id First")
                return@setOnClickListener
            }
            prg_join.visibility = View.VISIBLE
            (activity?.application as? LiveLikeApplication)?.sdk?.getChatUserMutedStatus(
                id,
                object : LiveLikeCallback<ChatUserMuteStatus>() {
                    override fun onResponse(result: ChatUserMuteStatus?, error: String?) {
                        result?.let {
                            btn_mute_status.post {
                                showToast("User is ${if (result.isMuted) " " else "not "}muted ")
                            }
                        }
                        error?.let {
                            btn_mute_status.post {
                                showToast(it)
                            }
                        }
                        prg_mute.visibility = View.INVISIBLE
                    }
                }
            )
        }
        btn_refresh.setOnClickListener {
            prg_refresh.visibility = View.VISIBLE
            (activity?.application as? LiveLikeApplication)?.sdk?.getCurrentUserChatRoomList(
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
                }
            )
        }

        btn_change.setOnClickListener {
            AlertDialog.Builder(context).apply {
                setTitle("Select a private group")
                setItems(chatRoomList.map { "${it.id}(${it.title})" }.toTypedArray()) { _, which ->
                    // On change of theme we need to create the session in order to pass new attribute of theme to widgets and chat
//                    (application as LiveLikeApplication).removePrivateSession()
                    val session = (activity as? ChatOnlyActivity)?.sessionMap?.get(
                        chatRoomList.elementAt(which).id
                    )
                    if (session == null) {
                        val checkBoxView =
                            View.inflate(context, R.layout.chat_only_check_box, null)
                        val builder = AlertDialog.Builder(context)
                        builder.setTitle("Avatar")
                            .setView(checkBoxView)
                            .setCancelable(false)
                            .setPositiveButton("Done") { _, _ ->
                                val url = checkBoxView.ed_avatar.text.toString()
                                (activity as? ChatOnlyActivity)?.changeChatRoom(
                                    chatRoomList.elementAt(which).id,
                                    checkBoxView.chk_avatar.isChecked,
                                    url
                                )
                            }
                            .show()
                    } else {
                        (activity as? ChatOnlyActivity)?.changeChatRoom(
                            chatRoomList.elementAt(which).id
                        )
                    }
                }
                create()
            }.show()
        }

        btn_delete.setOnClickListener {
            val id = ed_chat_room_id.text.toString()
            if (id.isNotEmpty()) {
                prg_delete.visibility = View.VISIBLE
                (activity?.application as? LiveLikeApplication)?.sdk?.deleteCurrentUserFromChatRoom(
                    id,
                    object : LiveLikeCallback<LiveLikeEmptyResponse>() {
                        override fun onResponse(result: LiveLikeEmptyResponse?, error: String?) {
                            result?.let {
                                showToast("Deleted ChatRoom")
                                (activity as? ChatOnlyActivity)?.privateGroupChatsession?.close()
                                (activity?.application as? LiveLikeApplication)?.removePrivateSession()
                            }
                            prg_delete.visibility = View.INVISIBLE
                            btn_refresh.callOnClick()
                        }
                    }
                )
            } else {
                showToast("Enter Room ID")
            }
        }

        btn_visibility.setOnClickListener {
            (activity as? ChatOnlyActivity)?.selectVisibility(object :
                ChatOnlyFragment.VisibilityInterface {
                override fun onSelectItem(visibility: Visibility) {
                    btn_visibility.text = visibility.name
                }
            })
        }
        btn_refresh.callOnClick()



        rcyl_invite.adapter = inviteAdapter
        btn_invite_list_first.setOnClickListener {
            inviteList(LiveLikePagination.FIRST)
        }
        btn_invite_list_previous.setOnClickListener {
            inviteList(LiveLikePagination.PREVIOUS)
        }
        btn_invite_list_next.setOnClickListener {
            inviteList(LiveLikePagination.NEXT)
        }
        btn_invite_list.setOnClickListener {
            inviteList(LiveLikePagination.FIRST)
        }

        btn_invite_by_list.setOnClickListener {
            prg_invite_by_list.visibility = View.VISIBLE
            (activity?.application as? LiveLikeApplication)?.sdk?.getInvitationsByCurrentProfileWithInvitationStatus(
                LiveLikePagination.FIRST,
                ChatRoomInvitationStatus.PENDING,
                object : LiveLikeCallback<List<ChatRoomInvitation>>() {
                    override fun onResponse(result: List<ChatRoomInvitation>?, error: String?) {
                        result?.let {
                            AlertDialog.Builder(context).apply {
                                setTitle("List")
                                setItems(it.map { "${it.invited_profile.nickname} => (${it.chat_room.title},${it.chat_room_id})" }
                                    .toTypedArray()) { _, _ ->
                                }
                                create()
                            }.show()
                        }
                        error?.let {
                            showToast(it)
                        }
                        prg_invite_by_list.visibility = View.INVISIBLE
                    }
                })
        }

        (activity?.application as? LiveLikeApplication)?.sdk?.chatRoomDelegate =
            object : ChatRoomDelegate() {
                override fun onNewChatRoomAdded(chatRoomAdd: ChatRoomAdd) {
                    activity?.runOnUiThread {
                        val builder = AlertDialog.Builder(context)
                        builder.setTitle("You are added to Chat Room")
                            .setMessage("Title: ${chatRoomAdd.chatRoomTitle}\nId: ${chatRoomAdd.chatRoomID}\nBy User: ${chatRoomAdd.senderNickname}")
                            .setCancelable(true)
                            .show()
                    }
                }

                override fun onReceiveInvitation(invitation: ChatRoomInvitation) {
                    showToast("Receive invitation from ${invitation.invited_by.nickname} => ${invitation.invited_by.userId}")
                }
            }

//        btn_search.setOnClickListener {
//            search(LiveLikePagination.FIRST)
//        }
//
//        rcyl_members.adapter = adapter
//        btn_first.setOnClickListener {
//            search(LiveLikePagination.FIRST)
//        }
//        btn_next.setOnClickListener {
//            search(LiveLikePagination.NEXT)
//        }
//        btn_previous.setOnClickListener {
//            search(LiveLikePagination.PREVIOUS)
//        }
    }

    private fun inviteList(pagination: LiveLikePagination) {
        prg_invite_list.visibility = View.VISIBLE
        (activity?.application as? LiveLikeApplication)?.sdk?.getInvitationsForCurrentProfileWithInvitationStatus(
            pagination,
            ChatRoomInvitationStatus.PENDING,
            object : LiveLikeCallback<List<ChatRoomInvitation>>() {
                override fun onResponse(result: List<ChatRoomInvitation>?, error: String?) {
                    result?.let {
                        inviteAdapter.inviteList.clear()
                        inviteAdapter.inviteList.addAll(it)
                        inviteAdapter.notifyDataSetChanged()
                        if (it.isEmpty()) {
                            showToast("List Empty")
                        }
                    }
                    error?.let {
                        showToast(it)
                    }
                    prg_invite_list.visibility = View.GONE
                }
            })
    }

    private fun unblock(blockID: String) {
        (activity?.application as? LiveLikeApplication)?.sdk?.unBlockProfile(blockID,
            object : LiveLikeCallback<LiveLikeEmptyResponse>() {
                override fun onResponse(result: LiveLikeEmptyResponse?, error: String?) {
                    error?.let {
                        showToast(it)
                    }
                    result?.let {
                        showToast("Success Unblock")
                        btn_block_list.callOnClick()
                    }
                }
            })
    }

    private fun updateInviteStatus(
        chatRoomInvitation: ChatRoomInvitation,
        chatRoomInvitationStatus: ChatRoomInvitationStatus
    ) {
        (activity?.application as? LiveLikeApplication)?.sdk?.updateChatRoomInviteStatus(
            chatRoomInvitation,
            chatRoomInvitationStatus,
            object : LiveLikeCallback<ChatRoomInvitation>() {
                override fun onResponse(result: ChatRoomInvitation?, error: String?) {
                    result?.let {
                        showToast("Status: ${it.status}")
                    }
                    error?.let {
                        showToast(it)
                    }
                    btn_invite_list_first.callOnClick()
                }
            })
    }


    fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = ChatOnlyHomeFragment()
    }

//    private fun search(pagination: LiveLikePagination) {
//        val search = ed_search.text.toString()
//        (activity?.application as? LiveLikeApplication)?.sdk?.searchUser(search,
//            pagination,
//            object : LiveLikeCallback<List<LiveLikeUserApi>>() {
//                override fun onResponse(result: List<LiveLikeUserApi>?, error: String?) {
//                    error?.let {
//                        showToast(it)
//                    }
//                    result?.let {
//                        adapter.userList.clear()
//                        adapter.userList.addAll(it)
//                        adapter.notifyDataSetChanged()
//                    }
//                }
//
//            })
//    }
}

class InviteListAdapter(
    private val updateStatus: (invitation: ChatRoomInvitation, status: ChatRoomInvitationStatus) -> Unit
) :
    RecyclerView.Adapter<InviteListAdapter.InviteListViewHolder>() {
    inner class InviteListViewHolder(view: View) : RecyclerView.ViewHolder(view)

    val inviteList = arrayListOf<ChatRoomInvitation>()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InviteListViewHolder {
        return InviteListViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.invite_list_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: InviteListViewHolder, position: Int) {
        val item = inviteList[position]
        holder.itemView.txt_invitation.text =
            "ChatRoom: ${item.chat_room.title},${item.chat_room_id} ,By User: ${item.invited_by.nickname}"
        holder.itemView.btn_accept.setOnClickListener {
            updateStatus.invoke(item, ChatRoomInvitationStatus.ACCEPTED)
        }
        holder.itemView.btn_reject.setOnClickListener {
            updateStatus.invoke(item, ChatRoomInvitationStatus.REJECTED)
        }
    }

    override fun getItemCount(): Int = inviteList.size
}

class BlockedListAdapter(
    private val unBlockProfile: (blockId: String) -> Unit
) :
    RecyclerView.Adapter<BlockedListAdapter.BlockListViewHolder>() {
    inner class BlockListViewHolder(view: View) : RecyclerView.ViewHolder(view)

    val blockedList = arrayListOf<BlockedData>()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BlockListViewHolder {
        return BlockListViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.block_list_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: BlockListViewHolder, position: Int) {
        val item = blockedList[position]
        holder.itemView.txt_title.text =
            "${item.blockedProfile.userId}(${item.blockedProfile.nickname})"

        holder.itemView.btn_unblock.setOnClickListener {
            unBlockProfile.invoke(item.id)
        }
    }

    override fun getItemCount(): Int = blockedList.size
}

class UserAdapter() : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    val userList = arrayListOf<LiveLikeUserApi>()

    inner class UserViewHolder(view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        return UserViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.user_list_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]
        holder.itemView.txt_name.text = user.nickname
        holder.itemView.setOnClickListener {
            val clipboard =
                holder.itemView.context?.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText("copied ChatRoomId", user.userId)
            clipboard?.setPrimaryClip(clip)
            Toast.makeText(holder.itemView.context, "User id copied", Toast.LENGTH_LONG).show()
        }
    }

    override fun getItemCount(): Int = userList.size
}
