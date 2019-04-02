package com.livelike.livelikesdk.messaging.proxies

import android.content.Context
import android.graphics.drawable.Drawable
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.gson.JsonObject
import com.livelike.livelikesdk.messaging.ClientMessage
import com.livelike.livelikesdk.messaging.MessagingClient
import com.livelike.livelikesdk.util.logVerbose


internal class ImagePreloaderMessagingClient(
    upstream: MessagingClient,
    val context: Context
) :
    MessagingClientProxy(upstream) {

    private val processingList = mutableListOf<ImageMessage>()

    class ImageMessage(
        val clientMessage: ClientMessage,
        val messagingClient: MessagingClient,
        val imageCount: Int,
        var imagePreloaded: Int = 0
    ) {
        override fun equals(other: Any?): Boolean {
            other as ImageMessage
            return other.clientMessage == this.clientMessage
        }
    }

    override fun onClientMessageEvent(client: MessagingClient, event: ClientMessage) {
        val imageList = getImagesFromJson(event.message, mutableListOf())

        if (imageList.isEmpty()) {
            logVerbose { "No images in this widget." }
            listener?.onClientMessageEvent(client, event)
            return
        }

        val currentImageMessage = ImageMessage(event, client, imageList.size)

        processingList.add(currentImageMessage)

        imageList.forEach {
            Glide.with(context)
                .load(it)
                .addListener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?, model: Any?, target: Target<Drawable>?,
                        isFirstResource: Boolean
                    )
                            : Boolean {
                        updateProcessingList(currentImageMessage)
                        return true
                    }

                    override fun onResourceReady(
                        resource: Drawable?, model: Any?, target: Target<Drawable>?,
                        dataSource: DataSource?, isFirstResource: Boolean
                    )
                            : Boolean {
                        updateProcessingList(currentImageMessage)
                        return true
                    }
                })
                .preload()
        }
    }

    fun updateProcessingList(imageMessage: ImageMessage) {
        val msg = processingList.first { msg -> msg == imageMessage }
        processingList.remove(msg)
        msg.imagePreloaded++
        if (msg.imageCount == msg.imagePreloaded) {
            listener?.onClientMessageEvent(imageMessage.messagingClient, imageMessage.clientMessage)

        } else {
            processingList.add(msg)
        }
    }


    private fun getImagesFromJson(jsonObject: JsonObject, imagesList: MutableList<String>): MutableList<String> {
        val elements = jsonObject.entrySet()
        elements.forEach { element ->
            when {
                element.key == "image_url" -> imagesList.add(element.value.asString)
                element.value.isJsonObject -> getImagesFromJson(element.value.asJsonObject, imagesList)
                element.value.isJsonArray -> element.value.asJsonArray.forEach {
                    if (it.isJsonObject) {
                        getImagesFromJson(it.asJsonObject, imagesList)
                    }
                }
            }
        }
        return imagesList
    }
}


internal fun MessagingClient.withPreloader(
    context: Context
): ImagePreloaderMessagingClient {
    return ImagePreloaderMessagingClient(this, context)
}