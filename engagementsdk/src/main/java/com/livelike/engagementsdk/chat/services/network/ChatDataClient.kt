package com.livelike.engagementsdk.chat.services.network

import com.livelike.engagementsdk.LiveLikeUser
import com.livelike.engagementsdk.chat.ChatMessage
import com.livelike.engagementsdk.core.services.network.EngagementDataClientImpl
import com.livelike.engagementsdk.core.services.network.RequestType
import com.livelike.engagementsdk.core.services.network.Result
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody

internal interface ChatDataClient {
    suspend fun reportMessage(remoteUrl: String, message: ChatMessage, accessToken: String?)
    suspend fun uploadImage(remoteUrl: String, accessToken: String?, image: ByteArray): String
}

internal class ChatDataClientImpl : EngagementDataClientImpl(), ChatDataClient {
    override suspend fun uploadImage(
        remoteUrl: String,
        accessToken: String?,
        image: ByteArray
    ): String {

        val formBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "image", "image.png",
                RequestBody.create(MediaType.parse("image/png"), image)
            )
            .build()

        val response = remoteCall<ImageResource>(
            remoteUrl,
            RequestType.POST,
            formBody,
            accessToken
        )

        when (response) {
            is Result.Success -> return response.data.image_url
            is Result.Error -> throw response.exception
        }
    }

    data class ImageResource(val id: String, val image_url: String)

    override suspend fun reportMessage(
        remoteUrl: String,
        message: ChatMessage,
        accessToken: String?
    ) {
        val result = remoteCall<LiveLikeUser>(
            remoteUrl,
            RequestType.POST,
            RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"), message.toReportMessageJson()
            ),
            accessToken
        )
        println("Reprt->${result}")
    }


}