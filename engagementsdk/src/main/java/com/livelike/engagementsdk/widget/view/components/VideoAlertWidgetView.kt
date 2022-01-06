package com.livelike.engagementsdk.widget.view.components

import android.content.Context
import android.content.Intent
import android.graphics.Outline
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.core.utils.AndroidResource
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.core.utils.logError
import com.livelike.engagementsdk.databinding.VideoWidgetBinding
import com.livelike.engagementsdk.widget.SpecifiedWidgetView
import com.livelike.engagementsdk.widget.WidgetType
import com.livelike.engagementsdk.widget.WidgetsTheme
import com.livelike.engagementsdk.widget.model.Alert
import com.livelike.engagementsdk.widget.viewModel.BaseViewModel
import com.livelike.engagementsdk.widget.viewModel.VideoWidgetViewModel
import com.livelike.engagementsdk.widget.viewModel.WidgetStates


internal class VideoAlertWidgetView : SpecifiedWidgetView {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private var inflated = false
    private lateinit var binding: VideoWidgetBinding
    var viewModel: VideoWidgetViewModel? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isMuted: Boolean = false
    private var playedAtLeastOnce: Boolean = false
    private var stopPosition: Int = 0

    override var dismissFunc: ((action: DismissAction) -> Unit)? =
        {
            viewModel?.dismissWidget(it)
            removeAllViews()
        }

    override var widgetViewModel: BaseViewModel? = null
        set(value) {
            field = value
            viewModel = value as VideoWidgetViewModel
        }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewModel?.data?.subscribe(javaClass) {
            logDebug { "showing the Video WidgetView" }
            it?.let {
                inflate(context, it)
            }
        }
        viewModel?.widgetState?.subscribe(javaClass) { widgetStates ->
            logDebug { "Current State: $widgetStates" }
            widgetStates?.let {
                if (widgetStates == WidgetStates.INTERACTING) {
                    // will only be fired if link is available in alert widget
                    viewModel?.markAsInteractive()
                }
                if (viewModel?.enableDefaultWidgetTransition == true) {
                    defaultStateTransitionManager(widgetStates)
                }
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        viewModel?.data?.unsubscribe(javaClass)
        viewModel?.widgetState?.unsubscribe(javaClass)
        release()
    }

    override fun moveToNextState() {
        super.moveToNextState()
        if (widgetViewModel?.widgetState?.latest() == WidgetStates.INTERACTING) {
            widgetViewModel?.widgetState?.onNext(WidgetStates.FINISHED)
        } else {
            super.moveToNextState()
        }
    }

    private fun inflate(context: Context, resourceAlert: Alert) {
        if (!inflated) {
            inflated = true
            binding = VideoWidgetBinding.inflate(LayoutInflater.from(context), this@VideoAlertWidgetView, true)
        }
        binding.bodyText.text = resourceAlert.text
        binding.labelText.text = resourceAlert.title
        binding.linkText.text = resourceAlert.link_label
        binding.soundView.visibility = View.GONE
        binding.playbackErrorView.visibility = View.GONE

        if (!resourceAlert.videoUrl.isNullOrEmpty()) {
            setFrameThumbnail(resourceAlert.videoUrl)
        }

        if (!resourceAlert.link_url.isNullOrEmpty()) {
            binding.linkBackground.setOnClickListener {
                openBrowser(context, resourceAlert.link_url)
            }
        } else {
            binding.linkArrow.visibility = View.GONE
            binding.linkBackground.visibility = View.GONE
            binding.linkText.visibility = View.GONE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                setPlayerViewCornersRound(isOnlyBottomCornersToBeRounded = true)
            }
        }

        if (resourceAlert.title.isNullOrEmpty()) {
            binding.labelText.visibility = GONE
            binding.widgetContainer.setBackgroundResource(R.drawable.video_alert_all_rounded_corner)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                setPlayerViewCornersRound(isOnlyBottomCornersToBeRounded = false)
            }
            val params = binding.widgetContainer.layoutParams as ConstraintLayout.LayoutParams
            params.topMargin = AndroidResource.dpToPx(0)
            binding.widgetContainer.requestLayout()
        } else {
            binding.widgetContainer.setBackgroundResource(R.drawable.video_alert_rounded_corner_black_background)
            val params = binding.widgetContainer.layoutParams as ConstraintLayout.LayoutParams
            binding.widgetContainer.layoutParams = params
            binding.widgetContainer.requestLayout()
        }

        if (!resourceAlert.text.isNullOrEmpty()) {
            binding.bodyText.visibility = View.VISIBLE
            binding.bodyText.text = resourceAlert.text
        } else {
            binding.bodyText.visibility = View.GONE
        }

        setOnClickListeners()

        widgetsTheme?.let {
            applyTheme(it)
        }
    }

    override fun applyTheme(theme: WidgetsTheme) {
        super.applyTheme(theme)
        viewModel?.data?.latest()?.let { _ ->
            theme.getThemeLayoutComponent(WidgetType.VIDEO_ALERT)?.let { themeComponent ->
                AndroidResource.updateThemeForView(
                    binding.labelText,
                    themeComponent.title,
                    fontFamilyProvider
                )
                if (themeComponent.header?.background != null) {
                    binding.labelText.background = AndroidResource.createDrawable(themeComponent.header)
                }
                themeComponent.header?.padding?.let {
                    AndroidResource.setPaddingForView(binding.labelText, themeComponent.header.padding)
                }

                binding.widgetContainer.background =
                    AndroidResource.createDrawable(themeComponent.body)

                AndroidResource.updateThemeForView(
                    binding.linkText,
                    themeComponent.body,
                    fontFamilyProvider
                )
            }
        }
    }

    /** sets the listeners */
    private fun setOnClickListeners() {
        binding.soundView.setOnClickListener {
            if (isMuted) {
                unMute()
            } else {
                mute()
            }
        }

        binding.widgetContainer.setOnClickListener {
            if (binding.playerView.isPlaying) {
                pause()
            } else {
                if (stopPosition > 0) { // already running
                    resume()
                } else {
                    play()
                }
            }
        }
    }

    /** sets the video view */
    private fun initializePlayer(videoUrl: String) {
        try {
            val uri = Uri.parse(videoUrl)
            binding.playerView.setVideoURI(uri)
            // playerView.seekTo(stopPosition)
            binding.playerView.requestFocus()
            binding.playerView.start()
            unMute()

            // perform set on prepared listener event on video view
            try {
                binding.playerView.setOnPreparedListener { mp ->
                    // do something when video is ready to play
                    this.mediaPlayer = mp
                    playedAtLeastOnce = true
                    binding.progressBar.visibility = View.GONE
                    binding.playbackErrorView.visibility = View.GONE
                    binding.soundView.visibility = VISIBLE
                    binding.icSound.visibility = VISIBLE
                }

                binding.playerView.setOnCompletionListener {
                    binding.playerView.stopPlayback()
                    binding.soundView.visibility = GONE
                    setFrameThumbnail(videoUrl)
                }

                binding.playerView.setOnErrorListener { _, _, _ ->
                    logError { "Error on playback" }
                    binding.progressBar.visibility = GONE
                    binding.icPlay.visibility = GONE
                    binding.playerView.visibility = INVISIBLE
                    binding.playbackErrorView.visibility = VISIBLE
                    binding.soundView.visibility = GONE
                    true
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = GONE
                binding.playbackErrorView.visibility = VISIBLE
                e.printStackTrace()
            }
        } catch (e: Exception) {
            binding.progressBar.visibility = GONE
            binding.playbackErrorView.visibility = VISIBLE
            e.printStackTrace()
        }
    }

    /** responsible for playing the video */
    private fun play() {
        binding.progressBar.visibility = View.VISIBLE
        viewModel?.registerPlayStarted()
        binding.icPlay.visibility = View.GONE
        binding.playbackErrorView.visibility = View.GONE
        binding.thumbnailView.visibility = View.GONE
        binding.playerView.visibility = View.VISIBLE
        viewModel?.data?.latest()?.videoUrl?.let { initializePlayer(it) }
    }

    /** responsible for resuming the video from where it was stopped */
    private fun resume() {
        binding.soundView.visibility = VISIBLE
        binding.playbackErrorView.visibility = GONE
        binding.progressBar.visibility = GONE
        binding.icPlay.visibility = GONE
        binding.playerView.seekTo(stopPosition)
        if (binding.playerView.currentPosition == 0) {
            play()
        } else {
            binding.playerView.start()
        }
    }

    /** responsible for stopping the video */
    private fun pause() {
        stopPosition = binding.playerView.currentPosition
        binding.playerView.pause()
        binding.soundView.visibility = GONE
        binding.icPlay.visibility = View.VISIBLE
        binding.playbackErrorView.visibility = View.GONE
        binding.icPlay.setImageResource(R.drawable.ic_play_button)
    }

    /** responsible for stopping the player and releasing it */
    private fun release() {
        try {
            playedAtLeastOnce = false
            if (binding.playerView != null && binding.playerView.isPlaying) {
                binding.playerView.stopPlayback()
                binding.playerView.seekTo(0)
                stopPosition = 0
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
            }
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    /** checks if the player is paused */
    fun isPaused(): Boolean {
        return !mediaPlayer!!.isPlaying && playedAtLeastOnce
    }

    /** mutes the video */
    private fun mute() {
        try {
            isMuted = true
            mediaPlayer?.setVolume(0f, 0f)
            binding.icSound.setImageResource(R.drawable.ic_volume_on)
            binding.muteTv.text = context.resources.getString(R.string.livelike_unmute_label)
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    /** unmute the video */
    private fun unMute() {
        try {
            isMuted = false
            mediaPlayer?.setVolume(1f, 1f)
            binding.icSound.setImageResource(R.drawable.ic_volume_off)
            binding.muteTv.text = context.resources.getString(R.string.livelike_mute_label)
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    /** extract thumbnail from the video url */
    private fun setFrameThumbnail(videoUrl: String) {
        binding.thumbnailView.visibility = VISIBLE
        binding.icPlay.visibility = VISIBLE
        binding.progressBar.visibility = GONE
        binding.playbackErrorView.visibility = GONE
        binding.icPlay.setImageResource(R.drawable.ic_play_button)
        binding.playerView.visibility = INVISIBLE
        var requestOptions = RequestOptions()

        if (videoUrl.isNotEmpty()) {
            requestOptions = if (viewModel?.data?.latest()?.title.isNullOrEmpty()) {
                requestOptions.transform(CenterCrop(), GranularRoundedCorners(16f, 16f, 16f, 16f))
            } else {
                requestOptions.transform(CenterCrop(), GranularRoundedCorners(0f, 0f, 16f, 16f))
            }
            Glide.with(context.applicationContext)
                .asBitmap()
                .load(videoUrl)
                .apply(requestOptions)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .thumbnail(0.1f)
                .into(binding.thumbnailView)
        }
    }

    private fun defaultStateTransitionManager(widgetStates: WidgetStates?) {
        when (widgetStates) {
            WidgetStates.READY -> {
                viewModel?.widgetState?.onNext(WidgetStates.INTERACTING)
            }
            WidgetStates.INTERACTING -> {
                viewModel?.data?.latest()?.let {
                    viewModel?.startDismissTimeout(it.timeout) {
                        viewModel?.widgetState?.onNext(WidgetStates.FINISHED)
                    }
                }
            }
            WidgetStates.FINISHED -> {
                removeAllViews()
                parent?.let { (it as ViewGroup).removeAllViews() }
            }
            WidgetStates.RESULTS -> {
                 // not required
            }
            null -> {
                //not required
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun setPlayerViewCornersRound(isOnlyBottomCornersToBeRounded: Boolean) {
        binding.playerView.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                val corner = 20f
                if (isOnlyBottomCornersToBeRounded) {
                    outline.setRoundRect(0, -corner.toInt(), view.width, view.height, corner)
                } else {
                    outline.setRoundRect(
                        0,
                        0,
                        view.width,
                        view.height,
                        corner
                    ) // for making all corners rounded
                }
            }
        }

        binding.playerView.clipToOutline = true
    }

    private fun openBrowser(context: Context, linkUrl: String) {
        viewModel?.onVideoAlertClickLink(linkUrl)
        val universalLinkIntent =
            Intent(Intent.ACTION_VIEW, Uri.parse(linkUrl)).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (universalLinkIntent.resolveActivity(context.packageManager) != null) {
            ContextCompat.startActivity(context, universalLinkIntent, Bundle.EMPTY)
        }
    }
}
