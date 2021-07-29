package com.livelike.livelikedemo.customchat

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.livelike.engagementsdk.LiveLikeContentSession
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.livelikedemo.CustomChatActivity
import com.livelike.livelikedemo.LiveLikeApplication
import com.livelike.livelikedemo.PREFERENCES_APP_ID
import com.livelike.livelikedemo.R
import com.livelike.livelikedemo.channel.Channel
import kotlinx.android.synthetic.main.fragment_home.lay_swipe
import kotlinx.android.synthetic.main.fragment_home.rcyl_chats
import kotlinx.android.synthetic.main.home_chat_item.view.txt_msg_count
import kotlinx.android.synthetic.main.home_chat_item.view.txt_name
import java.util.Calendar

class HomeFragment : Fragment() {

    private val adapter = HomeAdapter(object : ItemClickListener {
        override fun itemClick(homeChat: HomeChat) {
            homeChat.msgCount = 0
            (activity as? CustomChatActivity)?.showChatScreen(homeChat)
        }
    })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        rcyl_chats.adapter = adapter
        val dividerItemDecoration = DividerItemDecoration(
            rcyl_chats.context,
            (rcyl_chats.layoutManager as LinearLayoutManager).orientation
        )
        rcyl_chats.addItemDecoration(dividerItemDecoration)
        (activity?.application as? LiveLikeApplication)?.let { application ->
            if (adapter.sessionsList.isEmpty()) {
                val channelManager = application.channelManager
                val sdk = application.sdk
                val channels = channelManager.getChannels()
                val sessions =
                    ArrayList(
                        channels.map { channel ->
                            HomeChat(
                                channel,
                                sdk.createContentSession(channel.llProgram.toString())
                            )
                        }
                    )
                adapter.sessionsList.addAll(sessions)
            }
            adapter.notifyDataSetChanged()
            // fetching old messages
            lay_swipe.setOnRefreshListener {
                loadUnreadCount()
            }
            lay_swipe?.postDelayed({ loadUnreadCount() }, 5000L)
        }
    }

    override fun onDestroy() {
        adapter.sessionsList.forEach { it.session.close() }
        super.onDestroy()
    }

    private fun loadUnreadCount() {
        val sharedPref =
            activity?.application?.getSharedPreferences(PREFERENCES_APP_ID, Context.MODE_PRIVATE)
        sharedPref?.let {
            lay_swipe?.isRefreshing = true
            adapter.sessionsList.forEachIndexed { index, homeChat ->
                val time = sharedPref.getLong("msg_time_${homeChat.channel.llProgram}", 0L)
                println("HomeFragment.loadUnreadCount TIME->${homeChat.channel.name} ->$time")
                homeChat.session.chatSession.getMessageCount(
                    when (time) {
                        0L -> Calendar.getInstance().timeInMillis
                        else -> time
                    },
                    object : LiveLikeCallback<Byte>() {
                        override fun onResponse(result: Byte?, error: String?) {
                            println("HomeFragment.onResponse-- MSGCOUNT>${homeChat.channel.name} ->${result?.toInt()}")
                            homeChat.msgCount = result?.toInt() ?: 0
                            adapter.sessionsList[index] = homeChat
                            activity?.runOnUiThread {
                                lay_swipe?.isRefreshing = false
                                adapter.notifyItemChanged(index)
                            }
                        }
                    }
                )
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = HomeFragment()
    }
}

class HomeAdapter(private val itemClickListener: ItemClickListener) :
    RecyclerView.Adapter<HomeViewHolder>() {

    val sessionsList = arrayListOf<HomeChat>()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeViewHolder {
        return HomeViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.home_chat_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: HomeViewHolder, position: Int) {
        val homeChat = sessionsList[position]
        holder.itemView.txt_name.text = homeChat.channel.name
        holder.itemView.txt_msg_count.text = homeChat.msgCount.toString()
        holder.itemView.setOnClickListener {
            itemClickListener.itemClick(homeChat)
        }
    }

    override fun getItemCount(): Int = sessionsList.size
}

interface ItemClickListener {
    fun itemClick(homeChat: HomeChat)
}

class HomeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

class HomeChat(val channel: Channel, val session: LiveLikeContentSession) {
    var msgCount = 0
}
