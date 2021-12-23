package com.livelike.livelikedemo

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.sponsorship.ISponsor
import com.livelike.engagementsdk.sponsorship.SponsorModel
import kotlinx.android.synthetic.main.activity_sponsor_test.*
import kotlinx.android.synthetic.main.activity_widget_json.*

class SponsorTestActivity : AppCompatActivity() {

    private lateinit var sponsor: ISponsor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sponsor = (application as LiveLikeApplication).sdk.sponsor()

        setContentView(R.layout.activity_sponsor_test)
        program_id_et.setText("0fddc166-b8c3-4ce9-990e-848bde12188b")
        editTextTextPersonName3.setText("b6bbcbd4-1d25-40bc-9cb5-c67cceb923d1")
        fetch_sponsor.setOnClickListener {
            if (program_id_et.text.toString().isNotEmpty()) {
                progress_bar.visibility = View.VISIBLE
                sponsor.fetchByProgramId(
                    program_id_et.text.toString(),
                    object : LiveLikeCallback<List<SponsorModel>>() {
                        override fun onResponse(result: List<SponsorModel>?, error: String?) {
                            runOnUiThread {
                                progress_bar.visibility = View.GONE
                                error?.let {
                                    sponsor_result.text = it
                                }
                                result?.let {
                                    for (sponsor in it) {
                                        sponsor_result.text = "${sponsor_result.text} $sponsor \n "
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }

        button5.setOnClickListener {
            if (editTextTextPersonName3.text.toString().isNotEmpty()) {
                progress_bar.visibility = View.VISIBLE
                sponsor.fetchByChatRoomId(editTextTextPersonName3.text.toString(),
                    object : LiveLikeCallback<List<SponsorModel>>() {
                        override fun onResponse(result: List<SponsorModel>?, error: String?) {
                            runOnUiThread {
                                progress_bar.visibility = View.GONE
                                result?.let {
                                    for (sponsor in it) {
                                        textView9.text = "${textView9.text} $sponsor \n "
                                    }
                                }
                                error?.let {
                                    textView9.text = it
                                }
                            }
                        }

                    })
            }
        }

        button6.setOnClickListener {
            progress_bar.visibility = View.VISIBLE
            sponsor.fetchForApplication(object : LiveLikeCallback<List<SponsorModel>>() {
                override fun onResponse(result: List<SponsorModel>?, error: String?) {
                    runOnUiThread {
                        progress_bar.visibility = View.GONE
                        result?.let {
                            for (sponsor in it) {
                                textView10.text = "${textView10.text} $sponsor \n "
                            }
                        }
                        error?.let {
                            textView10.text = it
                        }
                    }
                }
            })

        }
    }
}
