import android.content.Context
import android.view.Display
import android.view.WindowManager
import org.mockito.Mockito
import org.mockito.kotlin.whenever
import org.robolectric.shadows.ShadowApplication
import java.io.InputStream

fun InputStream.readAll() : String{
    val bufferedReader = this.bufferedReader()
    val content = StringBuilder()
    bufferedReader.use { br ->
        var line = br.readLine()
        while (line != null) {
            content.append(line)
            line = br.readLine()
        }
    }
    return content.toString()
}



fun mockingAndroidServicesUsedByMixpanel() {
    val mock = Mockito.mock(WindowManager::class.java)
    // try shadowOf(context as Application)
    ShadowApplication.getInstance().setSystemService(
        Context.WINDOW_SERVICE,
        mock
    )
    whenever(mock.defaultDisplay).thenReturn(Mockito.mock(Display::class.java))
}
