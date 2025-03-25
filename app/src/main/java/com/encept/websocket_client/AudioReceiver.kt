package com.encept.websocket_client

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

class AudioReceiver : WebSocketListener() {
    private val sampleRate = 44100
    private val bufferSize = AudioTrack.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )

    private val audioTrack = AudioTrack(
        AudioManager.STREAM_MUSIC,
        sampleRate,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
        bufferSize,
        AudioTrack.MODE_STREAM
    )

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        audioTrack.write(bytes.toByteArray(), 0, bytes.size())
        audioTrack.play()
    }
}