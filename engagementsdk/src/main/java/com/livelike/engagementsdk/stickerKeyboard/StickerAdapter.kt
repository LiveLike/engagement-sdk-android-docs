@file:Suppress("UNCHECKED_CAST")

package com.livelike.engagementsdk.stickerKeyboard

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.util.DiffUtil
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.utils.liveLikeSharedPrefs.addRecentSticker
import com.livelike.engagementsdk.utils.liveLikeSharedPrefs.getRecentStickers
import kotlinx.android.synthetic.main.livelike_sticker_keyboard_item.view.itemImage
import kotlinx.android.synthetic.main.livelike_sticker_keyboard_rv.empty_recent_text
import kotlinx.android.synthetic.main.livelike_sticker_keyboard_rv.rvStickers

// Since this is an object collection, use a FragmentStatePagerAdapter,
// and NOT a FragmentPagerAdapter.
class StickerCollectionPagerAdapter(
    fm: FragmentManager,
    private val stickerPacks: List<StickerPack>,
    val programId : String,
    val onClickCallback: (Sticker) -> Unit
) : FragmentStatePagerAdapter(fm) {

    private val RECENT_STICKERS_POSITION = 0
    private var recentStickerView: RecentStickerFragment? = null

    override fun getCount(): Int = stickerPacks.size + 1

    override fun getItem(i: Int): Fragment {
        return if (i == RECENT_STICKERS_POSITION) {
            recentStickerView = RecentStickerFragment().apply {
                setOnClickListener(object : FragmentClickListener {
                    override fun onClick(sticker: Sticker) {
                        onClickCallback(sticker)
                    }
                })
                arguments = Bundle().apply {
                    putString(ARG_PROGRAM_ID, programId)
                }
                updateRecentStickers(programId)
            }
            return recentStickerView as Fragment
        } else {
            StickerObjectFragment().apply {
                setOnClickListener(object : FragmentClickListener {
                    override fun onClick(sticker: Sticker) {
                        onClickCallback(sticker)
                    }
                })
                arguments = Bundle().apply {
                    putParcelableArray(ARG_OBJECT, stickerPacks[i - 1].stickers.toTypedArray())
                }
            }
        }
    }

    fun refreshRecents() {
        recentStickerView?.updateRecentStickers(programId)
    }
}

private const val ARG_OBJECT = "stickerList"
private const val ARG_PROGRAM_ID = "programID"

interface FragmentClickListener {
    fun onClick(sticker: Sticker)
}

class RecentStickerFragment : Fragment() {

    private var listener: FragmentClickListener? = null
    private val adapter = StickerAdapter { sticker -> listener?.onClick(sticker) }

    fun setOnClickListener(listener: FragmentClickListener) {
        this.listener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.livelike_sticker_keyboard_rv, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val programId = arguments?.takeIf { it.containsKey(ARG_PROGRAM_ID) }?.getString(ARG_PROGRAM_ID)
            ?: ""
        rvStickers.layoutManager = GridLayoutManager(context, 6)
        rvStickers.adapter = adapter
        super.onViewCreated(view, savedInstanceState)
        updateRecentStickers(programId)
    }

    fun updateRecentStickers(programId : String) {
        val stickers = getRecentStickers(programId)
        empty_recent_text?.visibility = if (stickers.isEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }
        adapter.submitList(stickers)
    }
}

class StickerObjectFragment : Fragment() {
    private var listener: FragmentClickListener? = null
    private var stickers: Array<Sticker>? = null

    fun setOnClickListener(listener: FragmentClickListener) {
        this.listener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.livelike_sticker_keyboard_rv, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        stickers = arguments?.takeIf { it.containsKey(ARG_OBJECT) }?.let {
            it.getParcelableArray(ARG_OBJECT) as Array<Sticker>
        }

        if (stickers.isNullOrEmpty() && savedInstanceState != null) {
            stickers = savedInstanceState.getParcelableArray(ARG_OBJECT) as Array<Sticker>
        }

        rvStickers.layoutManager = GridLayoutManager(context, 6)
        val adapter = StickerAdapter { sticker -> listener?.onClick(sticker) }
        rvStickers.adapter = adapter
        adapter.submitList(stickers?.toList())
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelableArray(ARG_OBJECT, stickers)
    }
}

class StickerDiffCallback : DiffUtil.ItemCallback<Sticker>() {
    override fun areItemsTheSame(oldItem: Sticker, newItem: Sticker): Boolean {
        return oldItem.shortcode == newItem.shortcode
    }

    override fun areContentsTheSame(oldItem: Sticker, newItem: Sticker): Boolean {
        return oldItem.shortcode == newItem.shortcode
    }
}

class StickerAdapter(private val onClick: (Sticker) -> Unit) : ListAdapter<Sticker, StickerAdapter.StickerViewHolder>(StickerDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StickerViewHolder {
        return StickerViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.livelike_sticker_keyboard_item, parent, false))
    }

    override fun onBindViewHolder(holder: StickerViewHolder, position: Int) {
        holder.onBind(getItem(position), onClick)
    }

    class StickerViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        fun onBind(sticker: Sticker, onClick: (Sticker) -> Unit) {
            Glide.with(view).load(sticker.file).diskCacheStrategy(DiskCacheStrategy.ALL).into(view.itemImage)
            view.itemImage.setOnClickListener {
                onClick(sticker)
                addRecentSticker(sticker)
            }
        }
    }
}
