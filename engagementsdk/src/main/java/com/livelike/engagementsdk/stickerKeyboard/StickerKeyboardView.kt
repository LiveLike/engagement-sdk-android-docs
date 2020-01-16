package com.livelike.engagementsdk.stickerKeyboard

import android.animation.LayoutTransition
import android.app.Activity
import android.content.Context
import android.support.constraint.ConstraintLayout
import android.support.design.widget.TabLayout
import android.support.v7.widget.LinearLayoutManager
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.chat.ChatViewThemeAttributes
import com.livelike.engagementsdk.utils.AndroidResource
import kotlinx.android.synthetic.main.livelike_sticker_keyboard_pager.view.pager
import kotlinx.android.synthetic.main.livelike_sticker_keyboard_pager.view.pager_tab


class StickerKeyboardView(context: Context?, attributes: AttributeSet? = null) : ConstraintLayout(context, attributes) {
    private lateinit var viewModel: StickerKeyboardViewModel
    private lateinit var chatViewThemeAttributes:ChatViewThemeAttributes
    init {
        LayoutInflater.from(context).inflate(R.layout.livelike_sticker_keyboard_pager, this, true)
        layoutTransition = LayoutTransition()
    }

    fun initTheme(themeAttributes: ChatViewThemeAttributes) {
        chatViewThemeAttributes = themeAttributes
        themeAttributes.apply {
            pager.background = stickerBackground
            pager_tab.background = stickerTabBackground
            pager_tab.setSelectedTabIndicatorColor(stickerSelectedTabIndicatorColor)
        }
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
        if (imgUri == null)
            imageView.setColorFilter(
                chatViewThemeAttributes.stickerSelectedTabIndicatorColor,
                android.graphics.PorterDuff.Mode.MULTIPLY
            )
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
                val stickerCollectionPagerAdapter = StickerCollectionAdapter(
                    stickerPacks,
                    stickerPackRepository.programId,
                    emptyRecentTextColor = chatViewThemeAttributes.stickerRecentEmptyTextColor
                ) { s -> listener?.onClick(s) }
                pager.layoutManager =
                    LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                pager.adapter = stickerCollectionPagerAdapter
                RVPagerSnapHelperListenable().attachToRecyclerView(
                    pager,
                    object : RVPagerStateListener {
                        override fun onPageScroll(pagesState: List<VisiblePageState>) {

                        }

                        override fun onScrollStateChanged(state: RVPageScrollState) {
                        }

                        override fun onPageSelected(index: Int) {
                            pager_tab.getTabAt(index)?.select()
                        }
                    })

                val listener = object : TabLayout.BaseOnTabSelectedListener<TabLayout.Tab> {
                    override fun onTabReselected(p0: TabLayout.Tab?) {

                    }

                    override fun onTabUnselected(p0: TabLayout.Tab?) {

                    }

                    override fun onTabSelected(p0: TabLayout.Tab?) {
                        p0?.let {
                            pager.smoothScrollToPosition(p0.position)
                        }
                    }

                }
                pager_tab.addOnTabSelectedListener(listener)
                for (i in 0..stickerPacks.size) {
                    val tab = pager_tab.newTab()
                    if (i == 0) {
                        tab.customView = createTabItemView()
                    } else {
                        tab.customView = createTabItemView(stickerPacks[i].file)
                    }
                    pager_tab.addTab(tab)
                }
            }
            viewModel.preload(context)
        }
    }

    private var listener: FragmentClickListener? = null

    fun setOnClickListener(listener: FragmentClickListener) {
        this.listener = listener
    }
}
