package com.livelike.engagementsdk.widget.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import com.bumptech.glide.Glide
import com.livelike.engagementsdk.DismissAction
import com.livelike.engagementsdk.R
import com.livelike.engagementsdk.core.utils.AndroidResource
import com.livelike.engagementsdk.core.utils.logDebug
import com.livelike.engagementsdk.databinding.WidgetCheerMeterBinding
import com.livelike.engagementsdk.widget.SpecifiedWidgetView
import com.livelike.engagementsdk.widget.ViewStyleProps
import com.livelike.engagementsdk.widget.model.Resource
import com.livelike.engagementsdk.widget.viewModel.BaseViewModel
import com.livelike.engagementsdk.widget.viewModel.CheerMeterViewModel
import com.livelike.engagementsdk.widget.viewModel.CheerMeterWidget
import com.livelike.engagementsdk.widget.viewModel.WidgetStates
import kotlin.math.max

class CheerMeterView(context: Context, attr: AttributeSet? = null) :
    SpecifiedWidgetView(context, attr) {

    private var mShowTeamResults: Boolean = false
    private var lastResult: Resource? = null
    private var viewModel: CheerMeterViewModel? = null

    private var inflated = false
    private var binding: WidgetCheerMeterBinding? = null

    override var dismissFunc: ((DismissAction) -> Unit)? = { viewModel?.dismissWidget(it) }

    override var widgetViewModel: BaseViewModel? = null
        set(value) {
            field = value
            viewModel = value as CheerMeterViewModel
        }

    // Refresh the view when re-attached to the activity
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewModel?.data?.subscribe(this.hashCode()) { resourceObserver(it) }
        viewModel?.widgetState?.subscribe(this.hashCode()) { stateObserver(it) }
        viewModel?.results?.subscribe(this.hashCode()) { resultObserver(it) }
        viewModel?.voteEnd?.subscribe(this.hashCode()) { endObserver(it) }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        viewModel?.data?.unsubscribe(this.hashCode())
        viewModel?.widgetState?.unsubscribe(this.hashCode())
        viewModel?.results?.unsubscribe(this.hashCode())
        viewModel?.voteEnd?.unsubscribe(this.hashCode())
    }

    private fun stateObserver(widgetStates: WidgetStates?) {
        when (widgetStates) {
            WidgetStates.READY -> {
                lockInteraction()
            }
            WidgetStates.INTERACTING -> {
                unLockInteraction()
                showResultAnimation = true

                // show timer while widget interaction mode
                viewModel?.data?.latest()?.resource?.timeout?.let { timeout ->
                    showTimer(
                        timeout, binding?.textEggTimer,
                        {
                            viewModel?.animationEggTimerProgress = it
                        },
                        {
                            viewModel?.dismissWidget(it)
                        }
                    )
                }
            }
            WidgetStates.RESULTS, WidgetStates.FINISHED -> {
                lockInteraction()
                onWidgetInteractionCompleted()
                if ((
                            viewModel?.totalVoteCount
                                ?: 0
                            ) > 0 || viewModel?.enableDefaultWidgetTransition == false
                ) {
                    viewModel?.voteEnd?.onNext(true)
                    viewModel?.voteEnd()
                }
            }
        }
        if (viewModel?.enableDefaultWidgetTransition == true) {
            defaultStateTransitionManager(widgetStates)
        }
    }

    private fun lockInteraction() {
        binding?.viewRipple?.isClickable = false
        binding?.viewRipple2?.isClickable = false
    }

    private fun unLockInteraction() {
        binding?.viewRipple?.isClickable = true
        binding?.viewRipple2?.isClickable = true
        viewModel?.isWidgetInteractedEventLogged = false
        viewModel?.markAsInteractive()
    }

    private fun defaultStateTransitionManager(widgetStates: WidgetStates?) {
        when (widgetStates) {
            WidgetStates.READY -> {
                moveToNextState()
            }
            WidgetStates.INTERACTING -> {
                viewModel?.data?.latest()?.let {
                    viewModel?.startDismissTimout(it.resource.timeout)
                }
            }
            WidgetStates.RESULTS -> {
                //not required
            }
            WidgetStates.FINISHED -> {
                resourceObserver(null)
            }
        }
    }

    private fun resultObserver(resource: Resource?) {
        (resource ?: viewModel?.data?.latest()?.resource)?.let {
            lastResult = it
            val options = it.options ?: return
            if (options.size == 2) {
                val team1 = options[0]
                val team2 = options[1]
                val vote1 = max(team1.vote_count ?: 0, 1)

                val vote2 = max(team2.vote_count ?: 0, 1)

                val totalCount = max(vote1 + vote2, 1)

                binding?.llCheerMeterTeams?.weightSum = totalCount.toFloat()
                binding?.llCheerMeterTeams?.orientation = LinearLayout.HORIZONTAL

                binding?.txtCheerMeterTeam1?.layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    vote1.toFloat()
                )

                binding?.txtCheerMeterTeam2?.layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    vote2.toFloat()
                )
            }
            if (mShowTeamResults) {
                mShowTeamResults = false
                showTeamResults(it)
            }
        }
    }

    private var angle = false

    private fun resourceObserver(widget: CheerMeterWidget?) {
        widget?.apply {
            val optionList = resource.getMergedOptions() ?: return
            if (!inflated) {
                inflated = true
                binding = WidgetCheerMeterBinding.inflate(
                    LayoutInflater.from(context),
                    this@CheerMeterView,
                    true
                )
            }
            widgetsTheme?.cheerMeter?.let { cheerMeterTheme ->
                AndroidResource.updateThemeForView(
                    binding?.txtCheerMeterTitle,
                    cheerMeterTheme.title,
                    fontFamilyProvider
                )
                cheerMeterTheme.header?.let {
                    binding?.layCheerMeterHeader?.background = AndroidResource.createDrawable(it)
                }
                AndroidResource.updateThemeForView(
                    binding?.txtCheerMeterTeam1,
                    cheerMeterTheme.sideABar,
                    fontFamilyProvider
                )
                AndroidResource.updateThemeForView(
                    binding?.txtCheerMeterTeam2,
                    cheerMeterTheme.sideBBar,
                    fontFamilyProvider
                )
                binding?.txtCheerMeterTeam1?.background =
                    AndroidResource.createDrawable(cheerMeterTheme.sideABar)

                binding?.txtCheerMeterTeam2?.background =
                    AndroidResource.createDrawable(cheerMeterTheme.sideBBar)
                widgetsTheme?.cheerMeter?.sideAButton?.let {
                    updateRippleView(binding?.viewRipple!!, it)
                }
                widgetsTheme?.cheerMeter?.sideBButton?.let {
                    updateRippleView(binding?.viewRipple2!!, it)
                }
                widgetsTheme?.cheerMeter?.body?.let { props ->
                    AndroidResource.createDrawable(props)?.let {
                        binding?.layCheerMeterBackground?.background = it
                    }
                }
            }
            binding?.txtCheerMeterTitle?.text = resource.question
            if (optionList.size == 2) {
                binding?.txtCheerMeterTeam1?.text = optionList[0].description
                Glide.with(context.applicationContext)
                    .load(optionList[0].image_url)
                    .into(binding?.imgLogoTeam1!!)

                binding?.txtCheerMeterTeam2?.text = optionList[1].description

                Glide.with(context.applicationContext)
                    .load(optionList[1].image_url)
                    .into(binding?.imgLogoTeam2!!)

                binding?.imgLogoTeam1?.startAnimation(
                    AnimationUtils.loadAnimation(
                        context,
                        R.anim.scale_animation
                    ).apply {
                        repeatMode = Animation.REVERSE
                        repeatCount = Animation.INFINITE
                    }
                )
                binding?.imgLogoTeam2?.startAnimation(
                    AnimationUtils.loadAnimation(
                        context,
                        R.anim.scale_reverse_animation
                    ).apply {
                        repeatMode = Animation.REVERSE
                        repeatCount = Animation.INFINITE
                    }
                )

                setupTeamCheerRipple(binding?.viewRipple!!, binding?.imgLogoTeam1!!, 0)
                setupTeamCheerRipple(binding?.viewRipple2!!, binding?.imgLogoTeam2!!, 1)
            }

            if ((viewModel?.totalVoteCount ?: 0) > 0) {
                clearStartingAnimations()
                binding?.txtMyScore?.visibility = View.VISIBLE
                binding?.txtMyScore?.text = "${viewModel?.totalVoteCount}"
            } else {
                binding?.lottieVsAnimation?.apply {
                    setAnimation("vs_animation.json")
                    progress = viewModel?.animationProgress ?: 0f
                    repeatCount = 0
                    playAnimation()
                }
            }

            logDebug { "Showing CheerMeter Widget" }
            if (widgetViewModel?.widgetState?.latest() == null)
                widgetViewModel?.widgetState?.onNext(WidgetStates.READY)
        }

        if (widget == null) {
            inflated = false
            removeAllViews()
            parent?.let { (it as ViewGroup).removeAllViews() }
        }
    }

    // this method setup the ripple view which animates on tapping up/cheering the team
    @SuppressLint("ClickableViewAccessibility")
    private fun setupTeamCheerRipple(viewRipple: View, teamView: View, teamIndex: Int) {
        viewRipple.setOnTouchListener(object : OnTouchListener {
            var handler = Handler(Looper.getMainLooper())
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                // when tapped for first time
                if (v.isClickable) {
                    this@CheerMeterView.clearStartingAnimations()
                }
                when (event.action) {
                    MotionEvent.ACTION_UP -> {
                        if (v.isClickable) {
                            if (angle) {
                                angle = false
                                teamView.animate().rotation(0F).setDuration(50)
                                    .start()
                                if (teamIndex == 0) {
                                    binding?.txtCheerMeterTeam1!!.animate().alpha(1F)
                                        .setDuration(30).start()
                                } else {
                                    binding?.txtCheerMeterTeam2!!.animate().alpha(1F)
                                        .setDuration(30).start()
                                }
                                handler.removeCallbacksAndMessages(null)
                                handler.postDelayed(
                                    {
                                        // txt_my_score.visibility = View.INVISIBLE
                                        binding?.txtMyScore?.visibility = View.VISIBLE
                                        binding?.txtMyScore?.text = "${viewModel?.totalVoteCount}"
                                    },
                                    500
                                )
                            }
                        }
                        return false
                    }
                    MotionEvent.ACTION_DOWN -> {
                        if (v.isClickable) {
                            if (!angle) {
                                angle = true
                                var txtTeamView = if (teamIndex == 0) {
                                    binding?.txtCheerMeterTeam1
                                } else {
                                    binding?.txtCheerMeterTeam2
                                }
                                teamView.animate().rotation(35F).setDuration(50)
                                    .start()
                                val listener = object : AnimatorListenerAdapter() {
                                    override fun onAnimationEnd(animation: Animator?) {
                                        txtTeamView?.animate()?.alpha(1F)
                                            ?.setDuration(30)
                                            ?.start()
                                    }
                                }
                                txtTeamView?.animate()?.alpha(0F)?.setDuration(30)
                                    ?.setListener(listener)
                                    ?.start()

                                viewModel?.incrementVoteCount(
                                    teamIndex,
                                    viewModel?.data?.latest()?.resource?.getMergedOptions()
                                        ?.get(teamIndex)?.id
                                )
                                binding?.txtMyScore?.visibility = View.VISIBLE
                                binding?.txtMyScore?.text = "${viewModel?.totalVoteCount}"
                            }
                        }
                        return false
                    }
                    else -> return false
                }
            }
        }
        )
    }

    // all animations which run before user start interactions
    private fun clearStartingAnimations() {
        binding?.imgLogoTeam1?.clearAnimation()
        binding?.imgLogoTeam2?.clearAnimation()
        binding?.lottieVsAnimation?.visibility = View.GONE
//        collapse(lottie_vs_animation, 500, 0)
    }

    private fun updateRippleView(viewRipple: View, component: ViewStyleProps) {
        val drawable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            viewRipple.background as? RippleDrawable
        } else {
            viewRipple.background as? GradientDrawable
        }
        drawable?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (drawable is RippleDrawable) {
                    var drawable2 = drawable.findDrawableByLayerId(android.R.id.mask)
                    if (drawable2 is GradientDrawable) {
                        drawable2 = AndroidResource.createDrawable(component, drawable2)
                        drawable.setDrawableByLayerId(android.R.id.mask, drawable2)
                        viewRipple.background = drawable
                    }
                }
            } else {
                if (drawable is GradientDrawable) {
                    viewRipple.background = AndroidResource.createDrawable(component, drawable)
                }
            }
        }
    }

    private fun endObserver(it: Boolean?) {
        if (it == true) {
            this@CheerMeterView.clearStartingAnimations()
            binding?.txtCheerMeterTeam1?.alpha = 1F
            binding?.txtCheerMeterTeam2?.alpha = 1F
            if (lastResult == null) {
                mShowTeamResults = true
            } else {
                showTeamResults(lastResult!!)
            }
        }
    }

    private fun showTeamResults(resource: Resource): Boolean {
        val options = viewModel?.data?.latest()?.resource?.options ?: return true
        if (options.size == 2) {

            val team1 = options[0]
            val team2 = options[1]
            team1.vote_count =
                resource.options?.find { option -> option.id == team1.id }?.vote_count
            team2.vote_count =
                resource.options?.find { option -> option.id == team2.id }?.vote_count

            // viewModel?.voteEnd()
            binding?.flResultTeam?.visibility = View.VISIBLE
            logDebug { "CheerMeter voting stop,result: Team1:${team1.vote_count},Team2:${team2.vote_count}" }
            binding?.flResultTeam?.postDelayed(
                {
                    if (team1.vote_count == team2.vote_count) {
                        playDrawAnimation()
                        return@postDelayed
                    }

                    var winnerTeam = if (team1.vote_count ?: 0 > team2.vote_count ?: 0) {
                        team1
                    } else {
                        team2
                    }
                    binding?.imgLogoTeam2?.visibility = View.GONE
                    binding?.imgLogoTeam1?.visibility = View.GONE
                    val animation =
                        AnimationUtils.loadAnimation(
                            context,
                            R.anim.cheer_meter_winner_scale_animation
                        )
                    binding?.imgWinnerTeam?.visibility = View.VISIBLE
                    Glide.with(context.applicationContext)
                        .load(winnerTeam.image_url)
                        .into(binding?.imgWinnerTeam!!)
                    animation.setAnimationListener(object :
                        Animation.AnimationListener {
                        override fun onAnimationRepeat(animation: Animation?) {
                            //not required
                        }

                        override fun onAnimationStart(animation: Animation?) {
                            //not required
                        }

                        override fun onAnimationEnd(animation: Animation?) {
                            playWinnerAnimation()
                        }
                    })
                    binding?.imgWinnerTeam!!.startAnimation(animation)
                },
                500
            )
        }
        return false
    }

    private fun playLoserAnimation() {
        logDebug { "CheerMeter user lose" }
        viewModel?.animationProgress = 0f
        binding?.imgWinnerAnim?.apply {
            val rootPath = widgetViewThemeAttributes.widgetLoseAnimation
            val animationPath = AndroidResource.selectRandomLottieAnimation(rootPath, context) ?: ""
            setAnimation(animationPath)
            progress = viewModel?.animationProgress ?: 0f
            repeatCount = 0
            addAnimatorListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    viewModel?.dismissWidget(DismissAction.TIMEOUT)
                }
            })
            playAnimation()
        }
    }

    private fun playWinnerAnimation() {
        logDebug { "CheerMeter user win" }
        viewModel?.animationProgress = 0f
        binding?.imgWinnerAnim?.apply {
            val rootPath = widgetViewThemeAttributes.widgetWinAnimation
            val animationPath = AndroidResource.selectRandomLottieAnimation(rootPath, context) ?: ""
            setAnimation(animationPath)
            progress = viewModel?.animationProgress ?: 0f
            repeatCount = 0
            addAnimatorListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    if (viewModel?.enableDefaultWidgetTransition == true)
                        viewModel?.dismissWidget(DismissAction.TIMEOUT)
                }
            })
            playAnimation()
        }
    }

    private fun playDrawAnimation() {
        logDebug { "CheerMeter user draw" }
        viewModel?.animationProgress = 0f
        binding?.imgWinnerAnim?.apply {
            val rootPath = widgetViewThemeAttributes.widgetDrawAnimation
            val animationPath = AndroidResource.selectRandomLottieAnimation(rootPath, context) ?: ""
            setAnimation(animationPath)
            progress = viewModel?.animationProgress ?: 0f
            repeatCount = 0
            addAnimatorListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    if (viewModel?.enableDefaultWidgetTransition == true)
                        viewModel?.dismissWidget(DismissAction.TIMEOUT)
                }
            })
            playAnimation()
        }
    }

    fun collapse(v: View, duration: Int, targetHeight: Int) {
        val prevHeight = v.width
        val valueAnimator = ValueAnimator.ofInt(prevHeight, targetHeight)
        valueAnimator.interpolator = DecelerateInterpolator()
        valueAnimator.addUpdateListener { animation ->
            v.layoutParams = LinearLayout.LayoutParams(animation.animatedValue as Int, v.height)
        }
        valueAnimator.interpolator = DecelerateInterpolator()
        valueAnimator.duration = duration.toLong()
        valueAnimator.start()
    }
}
