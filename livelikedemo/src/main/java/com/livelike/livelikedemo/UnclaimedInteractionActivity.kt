package com.livelike.livelikedemo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.livelike.engagementsdk.LiveLikeContentSession
import com.livelike.engagementsdk.LiveLikeWidget
import com.livelike.engagementsdk.chat.data.remote.LiveLikePagination
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.widget.data.models.PredictionWidgetUserInteraction
import com.livelike.livelikedemo.utils.DialogUtils
import kotlinx.android.synthetic.main.activity_unclaimed_interaction.fetch_interaction
import kotlinx.android.synthetic.main.activity_unclaimed_interaction.widget_view


class UnclaimedInteractionActivity: AppCompatActivity() {

    private var session: LiveLikeContentSession? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_unclaimed_interaction)
        session = (application as LiveLikeApplication).createPublicSession((application as LiveLikeApplication)?.channelManager.selectedChannel.llProgram.toString())

        fetch_interaction.setOnClickListener { fetchUnclaimedInteractions() }
    }


    private fun fetchUnclaimedInteractions(){
        var unclaimedWidgetList: ArrayList<LiveLikeWidget> = ArrayList()
        session?.getWidgetInteractionsWithUnclaimedRewards(
            LiveLikePagination.FIRST,
            object : LiveLikeCallback<List<PredictionWidgetUserInteraction>>() {
                override fun onResponse(
                    result: List<PredictionWidgetUserInteraction>?,
                    error: String?)
                  {
                    result?.let {
                      //load widget data from the id and kind received from interactions
                        result.forEach { interaction ->
                            (application as LiveLikeApplication).sdk.
                               fetchWidgetDetails(interaction.widgetId,interaction.widgetKind,object : LiveLikeCallback<LiveLikeWidget>(){
                                   override fun onResponse(
                                       result: LiveLikeWidget?,
                                       error: String?
                                   ) {
                                       if(result!= null){
                                           unclaimedWidgetList.add(result)
                                       }
                                   }
                               })
                        }

                        // show the widgets
                        DialogUtils.showMyWidgetsDialog(this@UnclaimedInteractionActivity,
                            (application as LiveLikeApplication).sdk,
                            unclaimedWidgetList,
                            object : LiveLikeCallback<LiveLikeWidget>() {
                                override fun onResponse(
                                    result: LiveLikeWidget?,
                                    error: String?
                                ) {
                                    result?.let {
                                        widget_view.displayWidget(
                                            (application as LiveLikeApplication).sdk,
                                            result
                                        )

                                    }
                                }
                            })

                    }
                }
            })
    }
}