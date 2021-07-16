package com.livelike.engagementsdk.widget.view.components

import android.content.Context
import android.content.Intent
import android.graphics.Outline
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
import com.livelike.engagementsdk.widget.utils.toAnalyticsString
import com.livelike.engagementsdk.widget.viewModel.BaseViewModel
import com.livelike.engagementsdk.widget.viewModel.VideoWidgetViewModel
import com.livelike.engagementsdk.widget.viewModel.WidgetStates
import kotlinx.android.synthetic.main.video_widget.view.bodyText
import kotlinx.android.synthetic.main.video_widget.view.ic_play
import kotlinx.android.synthetic.main.video_widget.view.ic_sound
import kotlinx.android.synthetic.main.video_widget.view.labelText
import kotlinx.android.synthetic.main.video_widget.view.linkArrow
import kotlinx.android.synthetic.main.video_widget.view.linkBackground
import kotlinx.android.synthetic.main.video_widget.view.linkText
import kotlinx.android.synthetic.main.video_widget.view.mute_tv
import kotlinx.android.synthetic.main.video_widget.view.playbackErrorTv
import kotlinx.android.synthetic.main.video_widget.view.playerView
import kotlinx.android.synthetic.main.video_widget.view.progress_bar
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
        bodyText.text = resourceAlert.text
        labelText.text = resourceAlert.title
        linkText.text = resourceAlert.link_label

        if (!resourceAlert.videoUrl.isNullOrEmpty()) {
            setFrameThumbnail(resourceAlert.videoUrl)
            initializePlayer(resourceAlert.videoUrl)
        }

        if (!resourceAlert.link_url.isNullOrEmpty()) {
            linkBackground.setOnClickListener {
                openBrowser(context, resourceAlert.link_url)
            }
        } else {
            linkArrow.visibility = View.GONE
            linkBackground.visibility = View.GONE
            linkText.visibility = View.GONE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                setPlayerViewCornersRound(isOnlyBottomCornersToBeRounded = true)
            }
        }

        if (resourceAlert.title.isNullOrEmpty()) {
            labelText.visibility = GONE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                setPlayerViewCornersRound(isOnlyBottomCornersToBeRounded = false)
            }
            val params = widgetContainer.layoutParams as LayoutParams
            params.topMargin = AndroidResource.dpToPx(0)
            widgetContainer.requestLayout()
        } else {
            val params = widgetContainer.layoutParams as LayoutParams
            widgetContainer.layoutParams = params
            widgetContainer.requestLayout()
        }

        if (!resourceAlert.text.isNullOrEmpty()) {
            bodyText.visibility = View.VISIBLE
            bodyText.text = resourceAlert.text
        } else {
            bodyText.visibility = View.GONE
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


    private fun setOnClickListeners() {
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

        ic_play.setOnClickListener {
            if (mPlayer?.isPlaying == true) {
                pause()
            } else {
                play()
            }
        }

        widgetContainer.setOnClickListener {
            if (mPlayer?.isPlaying == true) {
                pause()
            } else {
                play()
            }
        }
    }


    /** initialize the exoplayer */
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
                    progress_bar.visibility = View.GONE
                     playbackErrorTv.visibility = View.GONE
                    sound_view.visibility = VISIBLE
                    thumbnailView.visibility = GONE
                    playerView.visibility = VISIBLE
                    ic_play.visibility = GONE
                    ic_sound.visibility = VISIBLE

                    logDebug { "onPlayerStateChanged: media is actually playing" }

                } else if (playbackState == Player.STATE_ENDED) {
                    mPlayer?.pause()
                    sound_view.visibility = GONE
                    mPlayer?.seekTo(0)
                    setFrameThumbnail(videoUrl)
                    logDebug { "onPlayerStateChanged: media playing ended" }

                } else if (playWhenReady && playbackState == Player.STATE_BUFFERING) {
                    ic_play.visibility = GONE
                    progress_bar.visibility = View.VISIBLE
                    logDebug { "onPlayerStateChanged: buffering" }
                } else {
                    // player paused in any state
                    logDebug { "onPlayerStateChanged: paused" }
                }

            }

            override fun onPlayerError(error: ExoPlaybackException) {
                progress_bar.visibility = GONE
                ic_play.visibility = GONE
                playerView.visibility = INVISIBLE
                playbackErrorTv.visibility = VISIBLE
                sound_view.visibility = GONE


                when (error.type) {
                    ExoPlaybackException.TYPE_SOURCE -> logError {
                        "TYPE_SOURCE: " + error.sourceException.message
                    }

                    ExoPlaybackException.TYPE_RENDERER -> logError {
                        "TYPE_RENDERER: " + error.rendererException.message
                    }
                    ExoPlaybackException.TYPE_UNEXPECTED -> logError {
                        "TYPE_UNEXPECTED: " + error.unexpectedException.message
                    }
                    ExoPlaybackException.TYPE_REMOTE -> logError {
                        "TYPE_REMOTE: " + error.message
                    }
                }
            }
        })
        mPlayer?.prepare()
    }

    /** responsible for playing the video */
    private fun play() {
        viewModel?.data?.latest()?.program_id?.let {
            viewModel?.currentWidgetType?.toAnalyticsString()?.let { widgetType ->
                viewModel?.analyticsService?.trackVideoAlertPlayed(
                    widgetType,
                    widgetId,
                    it,
                    viewModel?.data?.latest()?.videoUrl.toString()
                )
            }
        }
        sound_view.visibility = VISIBLE
        mPlayer?.playWhenReady = true
        mPlayer?.play()
    }

    /** responsible for stopping the video */
    private fun pause() {
        mPlayer?.pause()
        sound_view.visibility = GONE
        ic_play.visibility = View.VISIBLE
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
        playerView.visibility = INVISIBLE
        if (videoUrl.isNotEmpty()) {
            Glide.with(context.applicationContext)
                .asBitmap()
                .load(videoUrl)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .thumbnail(0.1f)
                .into(thumbnailView)
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
        }
    }

     @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
     private fun setPlayerViewCornersRound(isOnlyBottomCornersToBeRounded:Boolean){
         playerView.outlineProvider = object : ViewOutlineProvider() {
             override fun getOutline(view: View, outline: Outline) {
                 val corner = 20f
                 if(isOnlyBottomCornersToBeRounded) {
                     outline.setRoundRect(0, -corner.toInt(), view.width, view.height, corner)
                 }else{
                     outline.setRoundRect(0, 0, view.width, view.height, 10f) // for making all corners rounded
                 }
             }
         }

         playerView.clipToOutline = true
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


