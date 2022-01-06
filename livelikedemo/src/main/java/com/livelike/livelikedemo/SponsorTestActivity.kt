package com.livelike.livelikedemo

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.livelike.engagementsdk.chat.data.remote.LiveLikePagination
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.sponsorship.ISponsor
import com.livelike.engagementsdk.sponsorship.SponsorModel
import kotlinx.android.synthetic.main.activity_sponsor_test.*

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
                    LiveLikePagination.FIRST,
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
        fetch_sponsor_program_next.setOnClickListener {
            if (program_id_et.text.toString().isNotEmpty()) {
                progress_bar.visibility = View.VISIBLE
                sponsor.fetchByProgramId(
                    program_id_et.text.toString(),
                    LiveLikePagination.NEXT,
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

        fetch_sponsor_program_previous.setOnClickListener {
            if (program_id_et.text.toString().isNotEmpty()) {
                progress_bar.visibility = View.VISIBLE
                sponsor.fetchByProgramId(
                    program_id_et.text.toString(),
                    LiveLikePagination.PREVIOUS,
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
                    LiveLikePagination.FIRST,
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

        button5_next.setOnClickListener {
            if (editTextTextPersonName3.text.toString().isNotEmpty()) {
                progress_bar.visibility = View.VISIBLE
                sponsor.fetchByChatRoomId(editTextTextPersonName3.text.toString(),
                    LiveLikePagination.NEXT,
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

        button5_previous.setOnClickListener {
            if (editTextTextPersonName3.text.toString().isNotEmpty()) {
                progress_bar.visibility = View.VISIBLE
                sponsor.fetchByChatRoomId(editTextTextPersonName3.text.toString(),
                    LiveLikePagination.PREVIOUS,
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
            sponsor.fetchForApplication(LiveLikePagination.FIRST,
                object : LiveLikeCallback<List<SponsorModel>>() {
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
        button6_next.setOnClickListener {
            progress_bar.visibility = View.VISIBLE
            sponsor.fetchForApplication(LiveLikePagination.NEXT,
                object : LiveLikeCallback<List<SponsorModel>>() {
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
        button6_previous.setOnClickListener {
            progress_bar.visibility = View.VISIBLE
            sponsor.fetchForApplication(LiveLikePagination.PREVIOUS,
                object : LiveLikeCallback<List<SponsorModel>>() {
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
