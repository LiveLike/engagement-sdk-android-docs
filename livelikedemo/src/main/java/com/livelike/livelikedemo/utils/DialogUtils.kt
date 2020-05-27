package com.livelike.livelikedemo.utils

import android.content.Context
import com.github.angads25.filepicker.controller.DialogSelectionListener
import com.github.angads25.filepicker.model.DialogProperties
import com.github.angads25.filepicker.view.FilePickerDialog

object DialogUtils {

    fun showFilePicker(context: Context, callback: DialogSelectionListener) {
        val properties = DialogProperties()
        val dialog = FilePickerDialog(context, properties)
        dialog.setTitle("Select JSON File")
        dialog.setDialogSelectionListener(callback)
        dialog.show()
    }
}
