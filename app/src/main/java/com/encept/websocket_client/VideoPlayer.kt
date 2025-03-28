package com.encept.websocket_client

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import android.view.Surface
import android.view.SurfaceView

class VideoPlayer(
    private val surfaceView: SurfaceView, private val webSocketClient: ChatWebSocketClient
) {
    private var decoder: MediaCodec? = null
    private var surface: Surface? = null

    fun initializeDecoder() {
        surface = surfaceView.holder.surface

        val format = MediaFormat.createVideoFormat("video/avc", 640, 480)
        decoder = MediaCodec.createDecoderByType("video/avc").apply {
            configure(format, surface, null, 0)
            start()
        }
    }

    fun decodeVideoFrame(byteArray: ByteArray) {
        try {
            decoder?.let { codec ->
                val inputIndex = codec.dequeueInputBuffer(10000)
                if (inputIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputIndex)
                    inputBuffer?.clear()
                    inputBuffer?.put(byteArray)

                    codec.queueInputBuffer(
                        inputIndex, 0, byteArray.size, System.nanoTime() / 1000, 0
                    )
                }

                val bufferInfo = MediaCodec.BufferInfo()
                var outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)

                while (outputIndex >= 0) {
                    codec.releaseOutputBuffer(outputIndex, true)
                    outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
                }
            }
        } catch (exp: Exception) {
            Log.e("data", "decodeVideoFrame: " + exp.message)
        }
    }

    fun startReceivedVideo() {
        webSocketClient.let { itSoc ->
            if (itSoc.isWebSocketConnected()) {
                itSoc.setMessageListener(RootJson::class.java) { itUserM ->
                    Log.e("data", "startReceivedVideo: ")
                    decodeVideoFrame(
                        if (itUserM.data?.audioBytes != null) {
                            itUserM.data?.audioBytes!!
                        } else {
                            byteArrayOf()
                        }
                    )
                }
            } else {
                itSoc.connect()
                itSoc.setMessageListener(RootJson::class.java) { itUserM ->
                    Log.e("data", "startReceivedVideo: ")
                    decodeVideoFrame(
                        if (itUserM.data?.audioBytes != null) {
                            itUserM.data?.audioBytes!!
                        } else {
                            byteArrayOf()
                        }
                    )
                }
            }
        }
    }

    fun stopReceivedVideo() {
        decoder?.stop()
        decoder?.release()
        decoder = null
    }
}