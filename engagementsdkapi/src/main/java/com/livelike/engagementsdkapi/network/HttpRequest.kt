package com.livelike.livelikesdk.network

import com.google.gson.stream.MalformedJsonException
import com.livelike.livelikesdk.util.logError
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.ByteString
import java.io.IOException
import java.lang.Exception

// TODO: Ask Shane how to merge this into Dataclient class.
class HttpRequest(val client: OkHttpClient) {
    fun get() {}

    fun post(url: String,
             onResponseCallback: (String?) -> Unit,
             onFailureCallback: (Exception?) -> Unit) {
        val request = Request.Builder()
            .url(url)
            .post(RequestBody.create(null, ByteString.EMPTY))
            .build()

        val call = client.newCall(request)
        call.enqueue(object : Callback {
            override fun onResponse(call: Call?, response: Response) {
                val responseData = response.body()?.string()
                try {
                    onResponseCallback.invoke(responseData)
                } catch (e : MalformedJsonException) {
                    logError { e }
                    onFailureCallback.invoke(e)
                }
            }

            override fun onFailure(call: Call?, e: IOException?) {
                logError { e }
                onFailureCallback.invoke(e)
            }
        })
    }
}