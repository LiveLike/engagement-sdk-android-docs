package com.livelike.livelikedemo

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.livelike.engagementsdk.publicapis.LiveLikeCallback
import com.livelike.engagementsdk.sponsorship.Sponsor
import com.livelike.engagementsdk.sponsorship.SponsorModel
import kotlinx.android.synthetic.main.activity_sponsor_test.fetch_sponsor
import kotlinx.android.synthetic.main.activity_sponsor_test.program_id_et
import kotlinx.android.synthetic.main.activity_sponsor_test.progress_bar
import kotlinx.android.synthetic.main.activity_sponsor_test.sponsor_result

class SponsorTestActivity : AppCompatActivity() {

    private lateinit var sponsor: Sponsor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sponsor = (application as LiveLikeApplication).sdk.sponsor()

        setContentView(R.layout.activity_sponsor_test)
        program_id_et.setText("0fddc166-b8c3-4ce9-990e-848bde12188b")
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
    }
}
