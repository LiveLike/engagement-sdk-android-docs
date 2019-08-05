package android.support.test.runner.screenshot

import android.os.Environment.DIRECTORY_PICTURES
import android.os.Environment.getExternalStoragePublicDirectory
import java.io.File

class CustomScreenCaptureProcessor : BasicScreenCaptureProcessor(
    File(getExternalStoragePublicDirectory(DIRECTORY_PICTURES), "livelike")
)