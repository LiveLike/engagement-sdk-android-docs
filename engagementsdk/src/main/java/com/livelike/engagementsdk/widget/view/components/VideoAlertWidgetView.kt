package com.livelike.engagementsdk.widget.view.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.core.utils.AndroidResource
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.core.utils.logError
import com.livelike.engagementsdk.widget.SpecifiedWidgetView
import com.livelike.engagementsdk.widget.WidgetType
import com.livelike.engagementsdk.widget.WidgetsTheme
import com.livelike.engagementsdk.widget.model.Alert
import com.livelike.engagementsdk.widget.viewModel.BaseViewModel
import com.livelike.engagementsdk.widget.viewModel.VideoWidgetViewModel
import com.livelike.engagementsdk.widget.viewModel.WidgetStates
import kotlinx.android.synthetic.main.video_widget.view.ic_play
import kotlinx.android.synthetic.main.video_widget.view.ic_sound
import kotlinx.android.synthetic.main.video_widget.view.labelText
import kotlinx.android.synthetic.main.video_widget.view.linkArrow
import kotlinx.android.synthetic.main.video_widget.view.linkBackground
import kotlinx.android.synthetic.main.video_widget.view.linkText
import kotlinx.android.synthetic.main.video_widget.view.mute_tv
import kotlinx.android.synthetic.main.video_widget.view.playerView
import kotlinx.android.synthetic.main.video_widget.view.sound_view
import kotlinx.android.synthetic.main.video_widget.view.thumbnailView
import kotlinx.android.synthetic.main.video_widget.view.widgetContainer



internal class VideoAlertWidgetView : SpecifiedWidgetView {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private var inflated = false
    var viewModel: VideoWidgetViewModel? = null
    private var mPlayer: SimpleExoPlayer? = null


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


    private fun defaultStateTransitionManager(widgetStates: WidgetStates?) {
        when (widgetStates) {
            WidgetStates.READY -> {
                viewModel?.widgetState?.onNext(WidgetStates.INTERACTING)
            }
            WidgetStates.INTERACTING -> {
                viewModel?.data?.latest()?.let {
                    viewModel?.startDismissTimout(it.timeout) {
                        viewModel?.widgetState?.onNext(WidgetStates.FINISHED)
                    }
                }
            }
            WidgetStates.FINISHED -> {
                removeAllViews()
                parent?.let { (it as ViewGroup).removeAllViews() }
            }
        }
    }

    override fun applyTheme(theme: WidgetsTheme) {
        super.applyTheme(theme)
        viewModel?.data?.latest()?.let { _ ->
            theme.getThemeLayoutComponent(WidgetType.VIDEO_ALERT)?.let { themeComponent ->
                AndroidResource.updateThemeForView(
                    labelText,
                    themeComponent.title,
                    fontFamilyProvider
                )
                if (themeComponent.header?.background != null) {
                    labelText?.background = AndroidResource.createDrawable(themeComponent.header)
                }
                themeComponent.header?.padding?.let {
                    AndroidResource.setPaddingForView(labelText, themeComponent.header.padding)
                }

                widgetContainer?.background =
                    AndroidResource.createDrawable(themeComponent.body)

                AndroidResource.updateThemeForView(
                    linkText,
                    themeComponent.body,
                    fontFamilyProvider
                )
            }
        }
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
            LayoutInflater.from(context)
                .inflate(R.layout.video_widget, this, true) as ConstraintLayout
        }
        labelText.text = resourceAlert.title
        linkText.text = resourceAlert.link_label

        if (!resourceAlert.link_url.isNullOrEmpty()) {
            linkBackground.setOnClickListener {
                openBrowser(context, resourceAlert.link_url)
            }
        } else {
            linkArrow.visibility = View.GONE
            linkBackground.visibility = View.GONE
            linkText.visibility = View.GONE
        }

        if (resourceAlert.title.isNullOrEmpty()) {
            labelText.visibility = GONE
            val params = widgetContainer.layoutParams as LayoutParams
            params.topMargin = AndroidResource.dpToPx(0)
            widgetContainer.requestLayout()
        } else {
            val params = widgetContainer.layoutParams as LayoutParams
            widgetContainer.layoutParams = params
            widgetContainer.requestLayout()
        }

        if (resourceAlert.video_url.isNotEmpty()){
            initializePlayer(resourceAlert.video_url)
            setFrameThumbnail(resourceAlert.video_url)
        }

        ic_play.setOnClickListener {
            if (mPlayer?.isPlaying == true) {
                pause()
            } else {
                play()
            }
        }

        sound_view.setOnClickListener {
            val currentVolume = mPlayer?.volume
            if (currentVolume != null) {
                if (currentVolume == 0f) {
                    unMute()
                } else {
                    mute()
                }
            }
        }

        widgetsTheme?.let {
            applyTheme(it)
        }
    }


    // initialize the exoplayer
    private fun initializePlayer(videoUrl: String) {
        if (mPlayer == null) {
            mPlayer = SimpleExoPlayer.Builder(context).build()

        }
        playerView.player = mPlayer
        playerView.useController = false
        val mediaItem = MediaItem.fromUri(Uri.parse(videoUrl))
        mPlayer?.setMediaItem(mediaItem)
        unMute()
        mPlayer?.addListener(object : Player.Listener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                if (playWhenReady && playbackState == Player.STATE_READY) {
                    // media actually playing
                    sound_view.visibility = VISIBLE
                    thumbnailView.visibility = GONE
                    playerView.visibility = VISIBLE
                    ic_play.visibility = VISIBLE
                    ic_play.setImageResource(R.drawable.ic_pause_button)
                    ic_sound.visibility = VISIBLE

                    logDebug { "onPlayerStateChanged: media is actually playing" }

                } else if (playbackState == Player.STATE_ENDED) {
                    setFrameThumbnail(videoUrl)
                    initializePlayer(videoUrl)
                    logDebug { "onPlayerStateChanged: media playing ended" }

                } else if (playWhenReady) {
                    // might be idle (plays after prepare()),
                    // buffering (plays when data available)
                    // or ended (plays when seek away from end)
                    //setFrameThumbnail(videoUrl)
                    logDebug { "onPlayerStateChanged: buffering" }
                } else {
                    // player paused in any state
                    logDebug { "onPlayerStateChanged: paused" }
                }

            }

            override fun onPlayerError(error: ExoPlaybackException) {
                when (error.type) {
                    ExoPlaybackException.TYPE_SOURCE -> logError {
                        "TYPE_SOURCE: " + error.sourceException.message}

                    ExoPlaybackException.TYPE_RENDERER -> logError {
                        "TYPE_RENDERER: " + error.rendererException.message
                    }
                    ExoPlaybackException.TYPE_UNEXPECTED -> logError {
                        "TYPE_UNEXPECTED: " + error.unexpectedException.message
                    }

                }
            }
        })
        mPlayer?.prepare()
    }

    /** responsible for playing the video */
    private fun play() {
        mPlayer?.playWhenReady = true
        mPlayer?.play()
    }

    /** responsible for stopping the video */
    private fun pause() {
        mPlayer?.pause()
        ic_play.setImageResource(R.drawable.ic_play_button)
    }


    /** responsible for stopping the player and releasing it */
    private fun release() {
        mPlayer?.stop()
        mPlayer?.release()
        mPlayer?.setVideoSurfaceHolder(null)
        mPlayer = null
    }

    /** mutes the video */
    private fun mute() {
        mPlayer?.volume = 0f
        ic_sound.setImageResource(R.drawable.ic_volume_on)
        mute_tv.text = context.resources.getString(R.string.livelike_unmute_label)
    }

    /** unmute the video */
    private fun unMute() {
        mPlayer?.volume = 4f
        ic_sound.setImageResource(R.drawable.ic_volume_off)
        mute_tv.text = context.resources.getString(R.string.livelike_mute_label)
    }


    /** extract thumbnail fro the video url */
    private fun setFrameThumbnail(videoUrl: String) {
        thumbnailView.visibility = VISIBLE
        ic_play.visibility = VISIBLE
        ic_play.setImageResource(R.drawable.ic_play_button)
        playerView.visibility = GONE
        if (videoUrl.isNotEmpty()) {
            Glide.with(context.applicationContext)
                .asBitmap()
                .load(videoUrl)
                .into(thumbnailView)
        }
    }


    private fun openBrowser(context: Context, linkUrl: String) {
        viewModel?.onClickLink(linkUrl)
        val universalLinkIntent =
            Intent(Intent.ACTION_VIEW, Uri.parse(linkUrl)).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (universalLinkIntent.resolveActivity(context.packageManager) != null) {
            ContextCompat.startActivity(context, universalLinkIntent, Bundle.EMPTY)
        }
    }


}


