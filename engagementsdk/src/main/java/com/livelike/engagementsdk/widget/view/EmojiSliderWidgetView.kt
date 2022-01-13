package com.livelike.engagementsdk.widget.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.StateListDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import com.bumptech.glide.Glide
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.core.utils.AndroidResource
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.databinding.WidgetEmojiSliderBinding
import com.livelike.engagementsdk.widget.ImageSliderTheme
import com.livelike.engagementsdk.widget.model.ImageSliderEntity
import com.livelike.engagementsdk.widget.view.components.imageslider.ImageSlider
import com.livelike.engagementsdk.widget.view.components.imageslider.ScaleDrawable
import com.livelike.engagementsdk.widget.view.components.imageslider.ThumbDrawable
import com.livelike.engagementsdk.widget.viewModel.EmojiSliderWidgetViewModel
import com.livelike.engagementsdk.widget.viewModel.WidgetState
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.RoundingMode

internal class EmojiSliderWidgetView(context: Context, attr: AttributeSet? = null) :
    GenericSpecifiedWidgetView<ImageSliderEntity, EmojiSliderWidgetViewModel>(context, attr) {

    private lateinit var binding: WidgetEmojiSliderBinding

    override fun lockInteraction() {
        binding.imageSlider.isUserSeekable = false
    }

    override fun unLockInteraction() {
        binding.imageSlider.isUserSeekable = true
        viewModel.markAsInteractive()
    }

    override fun subscribeCalls() {
        super.subscribeCalls()
    }

    override fun unsubscribeCalls() {
        super.unsubscribeCalls()
    }

    override fun stateObserver(widgetState: WidgetState) {
        super.stateObserver(widgetState)
    }

    override fun confirmInteraction() {
        binding.imageSlider.isUserSeekable = false
        onWidgetInteractionCompleted()
    }

    override fun showResults() {
        val didUserVote = viewModel.currentVote.latest().isNullOrEmpty().not()
        val result = when (didUserVote) {
            true -> viewModel.results.latest()
            else -> viewModel.data.latest()
        }
        binding.imageSlider.averageProgress = result?.averageMagnitude ?:  binding.imageSlider.progress
        disableLockButton()
        binding.layLock.labelLock.visibility = View.VISIBLE

        logDebug { "EmojiSlider Widget showing result value:${ binding.imageSlider.averageProgress}" }
    }

    private fun updateTheme(it: ImageSliderTheme?) {
        it?.let { sliderTheme ->
            applyThemeOnTitleView(sliderTheme)
            sliderTheme.header?.padding?.let {
                AndroidResource.setPaddingForView(binding.titleView, sliderTheme.header.padding)
            }
            AndroidResource.createDrawable(sliderTheme.body)?.let {
                binding.layImageSlider.background = it
            }

            // submit button drawables theme
            val submitButtonEnabledDrawable = AndroidResource.createDrawable(
                sliderTheme.submitButtonEnabled
            )
            val submitButtonDisabledDrawable = AndroidResource.createDrawable(
                sliderTheme.submitButtonDisabled
            )
            val state = StateListDrawable()
            state.addState(intArrayOf(android.R.attr.state_enabled), submitButtonEnabledDrawable)
            state.addState(intArrayOf(), submitButtonDisabledDrawable)
            binding.layLock.btnLock?.background = state

            //confirmation label theme
            AndroidResource.updateThemeForView(
                binding.layLock.labelLock,
                sliderTheme.confirmation,
                fontFamilyProvider
            )
            if (sliderTheme.confirmation?.background != null) {
                binding.layLock.labelLock?.background = AndroidResource.createDrawable(sliderTheme.confirmation)
            }
            sliderTheme.confirmation?.padding?.let {
                AndroidResource.setPaddingForView(binding.layLock.labelLock, sliderTheme.confirmation.padding)
            }
        }
    }

    override fun dataModelObserver(entity: ImageSliderEntity?) {
        entity?.let { resource ->
            resource.getMergedOptions() ?: return
            if (!isViewInflated) {
                binding = WidgetEmojiSliderBinding.inflate(LayoutInflater.from(context), this@EmojiSliderWidgetView, true)
                wouldInflateSponsorUi()
                updateTheme(widgetsTheme?.imageSlider)
                binding.titleView.titleViewBinding.titleTextView.gravity = Gravity.START
                binding.titleView.title = resource.question
                if (binding.imageSlider.progress == ImageSlider.INITIAL_POSITION)
                    entity.initialMagnitude?.let {
                        binding.imageSlider.progress = it
                    }
                enableLockButton()
                if (viewModel.getUserInteraction() != null) {
                    isFirstInteraction = true
                    binding.layLock.labelLock.visibility = VISIBLE
                }
                viewModel.currentVote.currentData?.let {
                    binding.imageSlider.progress = it.toFloat()
                }
                val size = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    36f,
                    resources.displayMetrics
                ).toInt()
                viewModel.uiScope.launch {
                    val list = mutableListOf<Deferred<Bitmap>>()
                    withContext(Dispatchers.IO) {
                        resource.options?.forEach {
                            list.add(
                                async {
                                    try {
                                        Glide.with(context.applicationContext)
                                            .asBitmap()
                                            .load(it.image_url)
                                            .centerCrop()
                                            .submit(size, size)
                                            .get()
                                    } catch (e: Exception) {
                                        BitmapFactory.decodeResource(
                                            context.resources,
                                            R.drawable.default_avatar
                                        )
                                    }
                                }
                            )
                        }
                        val drawableList = list.mapNotNull { t -> ScaleDrawable(t.await()) }
                        withContext(Dispatchers.Main) {
                            val drawable = ThumbDrawable(drawableList, .5f)
                            binding.imageSlider.thumbDrawable = drawable
                        }
                    }
                }
                binding.layLock.btnLock.setOnClickListener {
                    viewModel.currentVote.onNext(binding.imageSlider.progress.toString())
                    viewModel.currentVote.currentData?.let {
                        lockVote()
                        viewModel.saveInteraction(it.toFloat(), entity.voteUrl)
                        binding.textEggTimer.visibility = GONE
                    }
                }

                binding.imageSlider.positionListener = { magnitude ->
                    viewModel.currentVote.onNext(
                        "${
                        magnitude.toBigDecimal().setScale(
                            2,
                            RoundingMode.UP
                        ).toFloat()
                        }"
                    )
                }
                viewModel.getUserInteraction()?.run {
                    disableLockButton()
                }
            }
        }
        logDebug { "showing EmojiSliderWidget" }
        super.dataModelObserver(entity)
    }

    private fun lockVote() {
        disableLockButton()
        binding.layLock.labelLock.visibility = View.VISIBLE
        viewModel.run {
            timeOutJob?.cancel()
            onInteractionCompletion {}
        }
    }

    private fun enableLockButton() {
        binding.layLock.layLock.visibility = VISIBLE
        binding.layLock.btnLock.isEnabled = true
        binding.layLock.btnLock.alpha = 1f
    }

    private fun disableLockButton() {
        binding.layLock.layLock.visibility = VISIBLE
        binding.layLock.btnLock.isEnabled = false
        binding.layLock.btnLock.alpha = 0.5f
    }
}
