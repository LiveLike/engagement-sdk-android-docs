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