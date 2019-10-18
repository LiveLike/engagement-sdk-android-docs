package com.livelike.engagementsdk.stickerKeyboard

import android.animation.LayoutTransition
import android.app.Activity
import android.content.Context
import android.support.constraint.ConstraintLayout
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.utils.AndroidResource
import kotlinx.android.synthetic.main.livelike_sticker_keyboard_pager.view.pager
import kotlinx.android.synthetic.main.livelike_sticker_keyboard_pager.view.pager_tab

class StickerKeyboardView(context: Context?, attributes: AttributeSet? = null) : ConstraintLayout(context, attributes) {
    private lateinit var viewModel: StickerKeyboardViewModel

    init {
        LayoutInflater.from(context).inflate(R.layout.livelike_sticker_keyboard_pager, this, true)
        layoutTransition = LayoutTransition()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        if (visibility == View.VISIBLE) {
            val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(changedView.windowToken, 0)
        }
        super.onVisibilityChanged(changedView, visibility)
    }

    private fun createTabItemView(imgUri: String? = null): View {
        val imageView = ImageView(context)
        imageView.layoutParams = ViewGroup.LayoutParams(
            AndroidResource.dpToPx(24),
            AndroidResource.dpToPx(24))
        Glide.with(this).load(imgUri ?: R.drawable.keyboard_ic_recent).into(imageView)

        return imageView
    }

    fun setProgram(stickerPackRepository: StickerPackRepository, onLoaded: ((List<StickerPack>?) -> Unit)? = null) {
        viewModel = StickerKeyboardViewModel(stickerPackRepository)
        viewModel.stickerPacks.subscribe(javaClass) {
            onLoaded?.invoke(it)
            it?.let { stickerPacks ->
                val stickerCollectionPagerAdapter = StickerCollectionPagerAdapter((context as AppCompatActivity).supportFragmentManager, stickerPacks) { s -> listener?.onClick(s) }
                pager.adapter = stickerCollectionPagerAdapter
                pager_tab.setupWithViewPager(pager)
                pager_tab.getTabAt(0)?.customView = createTabItemView()
                for (i in 0 until pager_tab.tabCount) {
                    pager_tab.getTabAt(i + 1)?.customView = createTabItemView(stickerPacks[i].file)
                }
                pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                    override fun onPageScrollStateChanged(state: Int) {
                    }

                    override fun onPageScrolled(
                        position: Int,
                        positionOffset: Float,
                        positionOffsetPixels: Int
                    ) {}

                    override fun onPageSelected(position: Int) {
                        if (position == 0) {
                            stickerCollectionPagerAdapter.refreshRecents()
                        }
                    }
                })
            }
            viewModel.preload(context)
        }
    }

    private var listener: FragmentClickListener? = null

    fun setOnClickListener(listener: FragmentClickListener) {
        this.listener = listener
    }
}
