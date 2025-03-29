package com.encept.websocket_client

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import android.view.Surface
import android.view.SurfaceView

class VideoAudioPlayer(
    private val surfaceView: SurfaceView, private val webSocketClient: ChatWebSocketClient
) {
    private val sampleRate = 44100
    private val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
    )
    private var audioTrack: AudioTrack? = null
    private var decoder: MediaCodec? = null
    private var surface: Surface? = null

    fun initializeDecoder() {
        surface = surfaceView.holder.surface

        val format = MediaFormat.createVideoFormat("video/avc", 640, 480)
        decoder = MediaCodec.createDecoderByType("video/avc").apply {
            configure(format, surface, null, 0)
            start()
        }

        audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize,
            AudioTrack.MODE_STREAM
        )

        startReceivedAudioVideo()
    }

    private fun decodeVideoFrame(byteArray: ByteArray) {
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

    private fun startReceivedAudioVideo() {
        webSocketClient.let { itSoc ->
            if (itSoc.isWebSocketConnected()) {
                itSoc.setMessageListener(RootJson::class.java) { itUserM ->
                    Log.e("data", "startReceivedVideo: ")
                    if (itUserM.data != null) {
                        itUserM.data?.apply {
                            decodeVideoFrame(
                                if (videoBytes != null) {
                                    videoBytes!!
                                } else {
                                    byteArrayOf()
                                }
                            )

                            playAudio(
                                if (audioBytes != null) {
                                    audioBytes!!
                                } else {
                                    byteArrayOf()
                                }
                            )
                        }
                    }
                }
            } else {
                itSoc.connect()
                itSoc.setMessageListener(RootJson::class.java) { itUserM ->
                    Log.e("data", "startReceivedVideo: ")
                    if (itUserM.data != null) {
                        itUserM.data?.apply {
                            decodeVideoFrame(
                                if (videoBytes != null) {
                                    videoBytes!!
                                } else {
                                    byteArrayOf()
                                }
                            )

                            playAudio(
                                if (audioBytes != null) {
                                    audioBytes!!
                                } else {
                                    byteArrayOf()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun playAudio(audioData: ByteArray) {
        if (audioTrack != null) {
            audioTrack?.write(audioData, 0, audioData.size)
            audioTrack?.play()
        }
    }

    fun stopReceivedVideo() {
        if (decoder != null && audioTrack != null) {
            decoder?.stop()
            decoder?.release()

            audioTrack?.stop()
            audioTrack?.release()

            decoder = null
            audioTrack = null
        }
    }
}