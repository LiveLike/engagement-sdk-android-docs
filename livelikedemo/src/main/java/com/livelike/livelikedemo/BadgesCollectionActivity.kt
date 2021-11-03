package com.livelike.livelikedemo

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.livelike.engagementsdk.chat.data.remote.LiveLikePagination
import com.livelike.engagementsdk.core.data.models.LLPaginatedResult
import com.livelike.engagementsdk.gamification.models.Badge
import com.livelike.engagementsdk.gamification.models.BadgeProgress
import com.livelike.engagementsdk.gamification.models.ProfileBadge
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import kotlinx.android.synthetic.main.activity_badges_collection.badges_list_rv
import kotlinx.android.synthetic.main.activity_badges_collection.fetch_application_badge
import kotlinx.android.synthetic.main.activity_badges_collection.fetch_badges_profile
import kotlinx.android.synthetic.main.activity_badges_collection.load_more
import kotlinx.android.synthetic.main.activity_badges_collection.profile_id_tv
import kotlinx.android.synthetic.main.activity_badges_collection.progress_bar

var isProfileBadges = false

class BadgesCollectionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_badges_collection)
// 0c141991-daea-47ad-93fc-4b4a48738929
// profile id with more than 20 badges in qa env.
//        profile_id_tv.setText("909c060d-1a92-47d6-b91a-f3e4911529f8")

        val badgeListAdapter = BadgeListAdapter()
        badges_list_rv.layoutManager = LinearLayoutManager(this)
        badges_list_rv.adapter = badgeListAdapter

        val badgesClient = (applicationContext as LiveLikeApplication).sdk.badges()

        badgeListAdapter.badgeClickListener = { badge ->

            progress_bar.visibility = View.VISIBLE
            badgesClient.getProfileBadgeProgress(
                profile_id_tv.text.toString(),
                mutableListOf(badge.id),
                object : LiveLikeCallback<List<BadgeProgress>>() {
                    override fun onResponse(result: List<BadgeProgress>?, error: String?) {
                        runOnUiThread {
                            result?.let {
                                if (result.isNotEmpty()) {
                                    showBadgeProgressDialog(this@BadgesCollectionActivity, it.get(0))
                                }
                            }
                            progress_bar.visibility = View.GONE
                            error?.let {
                                Toast.makeText(
                                    applicationContext,
                                    error,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }
            )
        }

        fetch_badges_profile.setOnClickListener {
            isProfileBadges = true
            progress_bar.visibility = View.VISIBLE
            badgesClient.getProfileBadges(
                profile_id_tv.text.toString(),
                LiveLikePagination.FIRST,
                liveLikeCallback = object :
                    LiveLikeCallback<LLPaginatedResult<ProfileBadge>>() {
                    override fun onResponse(
                        result: LLPaginatedResult<ProfileBadge>?,
                        error: String?
                    ) {
                        runOnUiThread {
                            progress_bar.visibility = View.GONE
                            error?.let {
                                Toast.makeText(
                                    applicationContext,
                                    error,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            result?.let {
                                badgeListAdapter.badges.clear()
                                val elements: List<ProfileBadge> =
                                    result.results ?: mutableListOf()
                                badgeListAdapter.badges.addAll(elements.map { it.badge })
                                badgeListAdapter.notifyDataSetChanged()
                            }
                        }
                    }
                }
            )
        }

        fetch_application_badge.setOnClickListener {
            isProfileBadges = false
            progress_bar.visibility = View.VISIBLE
            badgesClient.getApplicationBadges(
                LiveLikePagination.FIRST,
                object :
                    LiveLikeCallback<LLPaginatedResult<Badge>>() {
                    override fun onResponse(result: LLPaginatedResult<Badge>?, error: String?) {
                        badgeListAdapter.badges.clear()
                        addBadgesToAdapter(error, result, badgeListAdapter)
                    }
                }
            )
        }

        load_more.setOnClickListener {
            progress_bar.visibility = View.VISIBLE
            if (isProfileBadges) {
                badgesClient.getProfileBadges(
                    profile_id_tv.text.toString(),
                    LiveLikePagination.NEXT,
                    liveLikeCallback = object :
                        LiveLikeCallback<LLPaginatedResult<ProfileBadge>>() {
                        override fun onResponse(
                            result: LLPaginatedResult<ProfileBadge>?,
                            error: String?
                        ) {
                            runOnUiThread {
                                progress_bar.visibility = View.GONE
                                error?.let {
                                    Toast.makeText(applicationContext, error, Toast.LENGTH_LONG)
                                        .show()
                                }
                                result?.let {
                                    val elements: List<ProfileBadge> =
                                        result.results ?: mutableListOf()
                                    badgeListAdapter.badges.addAll(elements.map { it.badge })
                                    badgeListAdapter.notifyDataSetChanged()
                                }
                            }
                        }
                    }
                )
            } else {
                badgesClient.getApplicationBadges(
                    LiveLikePagination.NEXT,
                    object :
                        LiveLikeCallback<LLPaginatedResult<Badge>>() {
                        override fun onResponse(result: LLPaginatedResult<Badge>?, error: String?) {
                            addBadgesToAdapter(error, result, badgeListAdapter)
                        }
                    }
                )
            }
        }
    }

    private fun addBadgesToAdapter(
        error: String?,
        result: LLPaginatedResult<Badge>?,
        badgeListAdapter: BadgeListAdapter
    ) {
        runOnUiThread {
            progress_bar.visibility = View.GONE
            error?.let {
                Toast.makeText(
                    applicationContext,
                    error,
                    Toast.LENGTH_LONG
                ).show()
            }
            result?.let {
                val elements: List<Badge> =
                    result.results ?: mutableListOf()
                badgeListAdapter.badges.addAll(elements)
                badgeListAdapter.notifyDataSetChanged()
            }
        }
    }

    fun showBadgeProgressDialog(context: Context, badgeProgress: BadgeProgress) {

        AlertDialog.Builder(context).apply {
            setTitle("Progress towards ${badgeProgress.badge.name}")
            val progression = badgeProgress.progressionList.get(0)
            val progress: String =
                progression.currentRewardAmount.toString() + " out of " + progression.rewardItemThreshold + " " + progression.rewardItemName
            setMessage(progress)
        }.show()
    }

    class BadgeListAdapter : RecyclerView.Adapter<BadgeListAdapter.BadgeVH>() {

        lateinit var badgeClickListener: (badge: Badge) -> Unit
        internal val badges = mutableListOf<Badge>()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BadgeVH {
            return BadgeVH(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.activity_badges_collection_list_item, null)
            )
        }

        override fun onBindViewHolder(holder: BadgeVH, position: Int) {
            holder.badgeName.text = badges.get(position).name
            Glide.with(holder.itemView.context).load(badges.get(position).badgeIconUrl)
                .into(holder.badgeIcon)
            if (isProfileBadges) {
                holder.tickIcon.visibility = View.VISIBLE
            } else {
                holder.tickIcon.visibility = View.GONE
            }

            holder.itemView.setOnClickListener {
                badgeClickListener.invoke(badges.get(position))
            }
        }

        override fun getItemCount(): Int {
            return badges.size
        }

        class BadgeVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val badgeIcon: ImageView = itemView.findViewById(R.id.badge_ic)
            val badgeName: TextView = itemView.findViewById(R.id.badge_name_tv)
            val tickIcon: ImageView = itemView.findViewById(R.id.tick_ic)
        }
    }
}
