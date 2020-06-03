package com.livelike.engagementsdk.widget.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.util.AttributeSet
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
import com.livelike.engagementsdk.widget.Component
import com.livelike.engagementsdk.widget.SpecifiedWidgetView
import com.livelike.engagementsdk.widget.model.Option
import com.livelike.engagementsdk.widget.model.Resource
import com.livelike.engagementsdk.widget.viewModel.BaseViewModel
import com.livelike.engagementsdk.widget.viewModel.CheerMeterViewModel
import com.livelike.engagementsdk.widget.viewModel.CheerMeterWidget
import kotlin.math.max
import kotlinx.android.synthetic.main.widget_cheer_meter.view.fl_result_team
import kotlinx.android.synthetic.main.widget_cheer_meter.view.img_logo_team_1
import kotlinx.android.synthetic.main.widget_cheer_meter.view.img_logo_team_2
import kotlinx.android.synthetic.main.widget_cheer_meter.view.img_winner_anim
import kotlinx.android.synthetic.main.widget_cheer_meter.view.img_winner_team
import kotlinx.android.synthetic.main.widget_cheer_meter.view.ll_cheer_meter_teams
import kotlinx.android.synthetic.main.widget_cheer_meter.view.lottie_vs_animation
import kotlinx.android.synthetic.main.widget_cheer_meter.view.textEggTimer
import kotlinx.android.synthetic.main.widget_cheer_meter.view.txt_cheer_meter_team_1
import kotlinx.android.synthetic.main.widget_cheer_meter.view.txt_cheer_meter_team_2
import kotlinx.android.synthetic.main.widget_cheer_meter.view.txt_cheer_meter_title
import kotlinx.android.synthetic.main.widget_cheer_meter.view.txt_my_score
import kotlinx.android.synthetic.main.widget_cheer_meter.view.view_ripple
import kotlinx.android.synthetic.main.widget_cheer_meter.view.view_ripple2

class CheerMeterView(context: Context, attr: AttributeSet? = null) :
    SpecifiedWidgetView(context, attr) {

    private var lastResult: Resource? = null
    private lateinit var selectedTeam: Option
    private var viewModel: CheerMeterViewModel? = null

    private var inflated = false

    override var dismissFunc: ((DismissAction) -> Unit)? = { viewModel?.dismissWidget(it) }

    override var widgetViewModel: BaseViewModel? = null
        get() = super.widgetViewModel
        set(value) {
            field = value
            viewModel = value as CheerMeterViewModel
            viewModel?.data?.subscribe(javaClass.simpleName) { resourceObserver(it) }
            viewModel?.results?.subscribe(javaClass.simpleName) { resultObserver(it) }
            viewModel?.voteEnd?.subscribe(javaClass.simpleName) { endObserver(it) }
        }

    // Refresh the view when re-attached to the activity
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewModel?.data?.subscribe(javaClass.simpleName) { resourceObserver(it) }
        viewModel?.results?.subscribe(javaClass.simpleName) { resultObserver(it) }
        viewModel?.voteEnd?.subscribe(javaClass.simpleName) { endObserver(it) }
    }

    private fun resultObserver(resource: Resource?) {
        resource?.let {
            lastResult = it
            val options = resource.options ?: return
            if (options.size == 2) {
                val team1 = options[0]
                val team2 = options[1]
                val vote1 = max(team1.vote_count ?: 0, 1)

                val vote2 = max(team2.vote_count ?: 0, 1)

                val totalCount = max(vote1 + vote2, 1)

                ll_cheer_meter_teams.weightSum = totalCount.toFloat()
                ll_cheer_meter_teams.orientation = LinearLayout.HORIZONTAL

                txt_cheer_meter_team_1.layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    vote1.toFloat()
                )

                txt_cheer_meter_team_2.layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    vote2.toFloat()
                )
            }
        }
    }

    private var angle = false

    private fun resourceObserver(widget: CheerMeterWidget?) {
        widget?.apply {
            val optionList = resource.getMergedOptions() ?: return
            if (!inflated) {
                inflated = true
                inflate(context, R.layout.widget_cheer_meter, this@CheerMeterView)
            }
            widgetsTheme?.cheerMeter?.let { cheerMeterTheme ->
                AndroidResource.updateThemeForView(txt_cheer_meter_title, cheerMeterTheme.title)
                AndroidResource.updateThemeForView(txt_cheer_meter_team_1, cheerMeterTheme.sideABar)
                AndroidResource.updateThemeForView(txt_cheer_meter_team_2, cheerMeterTheme.sideBBar)
                txt_cheer_meter_team_1.background =
                    AndroidResource.createUpdateDrawable(cheerMeterTheme.sideABar)

                txt_cheer_meter_team_2.background =
                    AndroidResource.createUpdateDrawable(cheerMeterTheme.sideBBar)
                widgetsTheme?.cheerMeter?.sideAButton?.let {
                                            updateRippleView(view_ripple, it)
                                        }
                widgetsTheme?.cheerMeter?.sideBButton?.let {
                    updateRippleView(view_ripple2, it)
                }
            }
            txt_cheer_meter_title.text = resource.question
            if (optionList.size == 2) {
                txt_cheer_meter_team_1.text = optionList[0].description
                Glide.with(context)
                    .load(optionList[0].image_url)
                    .into(img_logo_team_1)

                txt_cheer_meter_team_2.text = optionList[1].description

                Glide.with(context)
                    .load(optionList[1].image_url)
                    .into(img_logo_team_2)

                img_logo_team_1.startAnimation(
                    AnimationUtils.loadAnimation(
                        context,
                        R.anim.scale_animation
                    ).apply {
                        repeatMode = Animation.REVERSE
                        repeatCount = Animation.INFINITE
                    }
                )
                img_logo_team_2.startAnimation(
                    AnimationUtils.loadAnimation(
                        context,
                        R.anim.scale_reverse_animation
                    ).apply {
                        repeatMode = Animation.REVERSE
                        repeatCount = Animation.INFINITE
                    }
                )

                setupTeamCheerRipple(view_ripple, img_logo_team_1, 0)
                setupTeamCheerRipple(view_ripple2, img_logo_team_2, 1)
            }

            lottie_vs_animation.apply {
                setAnimation("vs_animation.json")
                progress = viewModel?.animationProgress ?: 0f
                repeatCount = 0
                playAnimation()
            }

            showTimer(resource.timeout, viewModel?.animationEggTimerProgress, textEggTimer, {
                viewModel?.animationEggTimerProgress = it
            }, {
                viewModel?.dismissWidget(it)
            })
//            showEggerView(animationLength)
            viewModel?.startDismissTimout(resource.timeout)
            logDebug { "Showing CheerMeter Widget" }
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
        viewRipple.setOnTouchListener { v, event ->
            // when tapped for first time
            if (viewModel?.localVoteCount == 0) {
                clearStartingAnimations()
            }
            when (event.action) {
                MotionEvent.ACTION_UP -> {
                    if (v.isClickable) {
                        if (angle) {
                            angle = false
                            teamView.animate().rotation(0F).setDuration(50)
                                .start()
                            if (teamIndex == 0) {
                                txt_cheer_meter_team_1.animate().alpha(1F).setDuration(30)
                                    .start()
                            } else {
                                txt_cheer_meter_team_2.animate().alpha(1F).setDuration(30)
                                    .start()
                            }
                            txt_my_score.visibility = View.INVISIBLE // add debounce here as per design instructions by shu
                        }
                    }
                    return@setOnTouchListener false
                }
                MotionEvent.ACTION_DOWN -> {
                    if (v.isClickable) {
                        if (!angle) {
                            angle = true
                            var txtTeamView = if (teamIndex == 0) {
                                txt_cheer_meter_team_1
                            } else {
                                txt_cheer_meter_team_2
                            }
                            teamView.animate().rotation(35F).setDuration(50)
                                .start()
                            val listener = object : AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator?) {
                                    txtTeamView.animate().alpha(1F)
                                        .setDuration(30)
                                        .start()
                                }
                            }
                            txtTeamView.animate().alpha(0F).setDuration(30)
                                .setListener(listener)
                                .start()

                            viewModel?.sendVote("")
                            txt_my_score.visibility = View.VISIBLE
                            txt_my_score.text = "${viewModel?.localVoteCount}"
                        }
                    }
                    return@setOnTouchListener false
                }
                else -> false
            }
        }
    }

    // all animations which run before user start interactions
    private fun clearStartingAnimations() {
        img_logo_team_1.clearAnimation()
        img_logo_team_2.clearAnimation()
        collapse(lottie_vs_animation, 500, 0)
    }

    private fun updateRippleView(viewRipple: View, component: Component) {
        val drawable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            viewRipple.background as? RippleDrawable
        } else {
            viewRipple.background as? GradientDrawable
        }
        drawable?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (drawable is RippleDrawable) {
                    val drawable2 = drawable.findDrawableByLayerId(android.R.id.mask)
                    if (drawable2 is GradientDrawable) {
                        AndroidResource.createUpdateDrawable(component, drawable2)
                    }
                }
            }
        }
    }

    private fun initializaVoting(voteUrl: String, option: Option) {
        viewModel?.sendVote(voteUrl)
        startVoting(voteUrl, option)
    }

    private fun startVoting(voteUrl: String, id: Option) {
//        ll_my_score.visibility = View.VISIBLE
//        txt_my_score.visibility = View.VISIBLE
//        view_ripple.isClickable = true
//        view_ripple2.isClickable = true

        viewModel?.animationEggTimerProgress = 0f
        selectedTeam = id
        txt_my_score.text = "0"
        if (viewModel?.animationEggTimerProgress!! < 1f) {
//            listOf(textEggTimer).forEach { v ->
//                viewModel?.animationEggTimerProgress?.let {
//                    v?.startAnimationFrom(it, 10000F, { t ->
//                        viewModel?.animationEggTimerProgress = t
//                    }, {
//                        // stop voting
//                        // Added in order to get the updated voteCount at the voting end
//                        viewModel?.pushVoteData(0)
//                        stopVoting()
//                        viewModel?.dismissWidget(it)
//                    })
//                }
//            }
//            viewModel?.startDismissTimout(10000.toString(), isVotingStarted = true)
        }
        logDebug { "CheerMeter voting start" }
        viewModel?.sendVote(voteUrl)
    }

    private fun endObserver(it: Boolean?) {
        if (it == true) {
            // stop voting
            // Added in order to get the updated voteCount at the voting end
            viewModel?.pushVoteData(0)
            stopVoting()
        }
    }

    private fun stopVoting() {
        onWidgetInteractionCompleted()
        view_ripple.isClickable = false
        view_ripple2.isClickable = false
        txt_cheer_meter_team_1.alpha = 1F
        txt_cheer_meter_team_2.alpha = 1F
        lastResult?.let {
            val options = it.options ?: return
            if (options.size == 2) {

                val team1 = options[0]
                val team2 = options[1]
                resultObserver(it)

                viewModel?.voteEnd()
                fl_result_team.visibility = View.VISIBLE
                logDebug { "CheerMeter voting stop,result: Team1:${team1.vote_count},Team2:${team2.vote_count}" }
                fl_result_team.postDelayed({
                    img_logo_team_2.visibility = View.GONE
                    img_logo_team_1.visibility = View.GONE
                    val animation =
                        AnimationUtils.loadAnimation(
                            context,
                            R.anim.cheer_meter_winner_scale_animation
                        )
                    var winnerTeam = if (team1.vote_count ?: 0 > team2.vote_count ?: 0) {
                        team1
                    } else {
                        team2
                    }
                    img_winner_team.visibility = View.VISIBLE
                    Glide.with(context)
                        .load(winnerTeam.image_url)
                        .into(img_winner_team)
                    animation.setAnimationListener(object :
                        Animation.AnimationListener {
                        override fun onAnimationRepeat(animation: Animation?) {
                        }

                        override fun onAnimationStart(animation: Animation?) {
                        }

                        override fun onAnimationEnd(animation: Animation?) {
                            playWinnerAnimation()
                        }
                    })
                    img_winner_team.startAnimation(animation)

//                    when (selectedTeam.id) {
//                        team1.id -> when {
//                            (team1.vote_count ?: 0) > (team2.vote_count
//                                ?: 0) -> {
//                                // Winner
//                                img_winner_team.visibility = View.VISIBLE
//                                Glide.with(context)
//                                    .load(selectedTeam.image_url)
//                                    .into(img_winner_team)
//                                animation.setAnimationListener(object :
//                                    Animation.AnimationListener {
//                                    override fun onAnimationRepeat(animation: Animation?) {
//                                    }
//
//                                    override fun onAnimationStart(animation: Animation?) {
//                                    }
//
//                                    override fun onAnimationEnd(animation: Animation?) {
//                                        playWinnerAnimation()
//                                    }
//                                })
//                                img_winner_team.startAnimation(animation)
//                            }
//                            (team1.vote_count ?: 0) < (team2.vote_count
//                                ?: 0) -> {
//                                // Loser
//                                img_winner_team.visibility = View.GONE
//                                playLoserAnimation()
//                            }
//                            else -> {
//                                // Draw
//                                img_winner_team.visibility = View.GONE
//                                playDrawAnimation()
//                            }
//                        }
//                        team2.id -> when {
//                            (team1.vote_count ?: 0) > (team2.vote_count
//                                ?: 0) -> {
//                                // Loser
//                                img_winner_team.visibility = View.GONE
//                                playLoserAnimation()
//                            }
//                            (team1.vote_count ?: 0) < (team2.vote_count
//                                ?: 0) -> {
//                                // Winner
//                                img_winner_team.visibility = View.VISIBLE
//                                Glide.with(context)
//                                    .load(selectedTeam.image_url)
//                                    .into(img_winner_team)
//                                animation.setAnimationListener(object :
//                                    Animation.AnimationListener {
//                                    override fun onAnimationRepeat(animation: Animation?) {
//                                    }
//
//                                    override fun onAnimationStart(animation: Animation?) {
//                                    }
//
//                                    override fun onAnimationEnd(animation: Animation?) {
//                                        playWinnerAnimation()
//                                    }
//                                })
//                                img_winner_team.startAnimation(animation)
//                            }
//                            else -> {
//                                // Draw
//                                img_winner_team.visibility = View.GONE
//                                playDrawAnimation()
//                            }
//                        }
//                        else -> {
//                            logDebug { "CheerMeter: No Result" }
//                        }
//                    }
                }, 500)
            }
        }
    }

    private fun playLoserAnimation() {
        logDebug { "CheerMeter user lose" }
        viewModel?.animationProgress = 0f
        img_winner_anim.apply {
            val rootPath = widgetViewThemeAttributes.widgetLoseAnimation
            val animationPath = AndroidResource.selectRandomLottieAnimation(rootPath, context) ?: ""
            setAnimation(animationPath)
            progress = viewModel?.animationProgress ?: 0f
            repeatCount = 0
            addAnimatorListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {
                }

                override fun onAnimationEnd(animation: Animator?) {
                    viewModel?.dismissWidget(DismissAction.TAP_X)
                }

                override fun onAnimationCancel(animation: Animator?) {
                }

                override fun onAnimationStart(animation: Animator?) {
                }
            })
            playAnimation()
        }
    }

    private fun playWinnerAnimation() {
        logDebug { "CheerMeter user win" }
        viewModel?.animationProgress = 0f
        img_winner_anim.apply {
            val rootPath = widgetViewThemeAttributes.widgetWinAnimation
            val animationPath = AndroidResource.selectRandomLottieAnimation(rootPath, context) ?: ""
            setAnimation(animationPath)
            progress = viewModel?.animationProgress ?: 0f
            repeatCount = 0
            addAnimatorListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {
                }

                override fun onAnimationEnd(animation: Animator?) {
                    viewModel?.dismissWidget(DismissAction.TAP_X)
                }

                override fun onAnimationCancel(animation: Animator?) {
                }

                override fun onAnimationStart(animation: Animator?) {
                }
            })
            playAnimation()
        }
    }

    private fun playDrawAnimation() {
        logDebug { "CheerMeter user draw" }
        viewModel?.animationProgress = 0f
        img_winner_anim.apply {
            val rootPath = widgetViewThemeAttributes.widgetDrawAnimation
            val animationPath = AndroidResource.selectRandomLottieAnimation(rootPath, context) ?: ""
            setAnimation(animationPath)
            progress = viewModel?.animationProgress ?: 0f
            repeatCount = 0
            addAnimatorListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {
                }

                override fun onAnimationEnd(animation: Animator?) {
                    viewModel?.dismissWidget(DismissAction.TAP_X)
                }

                override fun onAnimationCancel(animation: Animator?) {
                }

                override fun onAnimationStart(animation: Animator?) {
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
