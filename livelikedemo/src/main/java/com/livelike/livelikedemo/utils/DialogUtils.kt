package com.livelike.livelikedemo.utils

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.github.angads25.filepicker.controller.DialogSelectionListener
import com.github.angads25.filepicker.model.DialogConfigs
import com.github.angads25.filepicker.model.DialogProperties
import com.github.angads25.filepicker.view.FilePickerDialog
import com.livelike.engagementsdk.EngagementSDK
import com.livelike.engagementsdk.LiveLikeWidget
import com.livelike.engagementsdk.publicapis.LiveLikeCallback

object DialogUtils {

    fun showFilePicker(context: Context, callback: DialogSelectionListener) {
        val properties = DialogProperties()
        properties.selection_mode = DialogConfigs.MULTI_MODE
        val dialog = FilePickerDialog(context, properties)
        dialog.setTitle("Select JSON File")
        dialog.setDialogSelectionListener(callback)
        dialog.show()
    }

    fun showMyWidgetsDialog(
        context: Context,
        sdk: EngagementSDK,
        myWidgetsList: ArrayList<LiveLikeWidget>,
        liveLikeCallback: LiveLikeCallback<LiveLikeWidget>
    ) {
        AlertDialog.Builder(context).apply {
            setTitle("Choose a widget to show!")
            setItems(
                myWidgetsList.map { "${it.id}(${it.kind})\nPublished:${it.publishedAt}\nCreated:${it.createdAt}" }
                    .toTypedArray()
            ) { _, which ->
                val widget = myWidgetsList[which]
                sdk.fetchWidgetDetails(
                    widget.id!!,
                    widget.kind!!, liveLikeCallback
                )
            }
        }.create()
            .apply {
//                listView.divider = ColorDrawable(Color.BLACK) // set color
//                listView.dividerHeight = 2
                show()
            }
    }
}
