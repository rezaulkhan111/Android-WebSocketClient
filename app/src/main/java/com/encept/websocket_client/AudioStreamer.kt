package com.encept.websocket_client

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AudioStreamer(
    private val context: Context, private val webSocketClient: ChatWebSocketClient
) {
    private val sampleRate = 44100
    private val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
    )

    private var audioRecord: AudioRecord? = null

    private var audioTrack: AudioTrack? = null

    private var isStreaming = false

    fun startAudioStreaming() {
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(
                context, "Permission denied! Cannot start audio recording.", Toast.LENGTH_SHORT
            ).show()
            return
        }

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        isStreaming = true
        audioRecord?.startRecording()

        CoroutineScope(Dispatchers.IO).launch {
            val buffer = ByteArray(bufferSize)
            while (isStreaming) {
                val readBytes = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                if (readBytes > 0) {
                    webSocketClient.sendAudioOrVideoData(UserChat().apply {
                        audioBytes = buffer
                    })
                }
            }
        }
    }

    fun startReceivedAudio() {
        audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize,
            AudioTrack.MODE_STREAM
        )

        webSocketClient.let { itSoc ->
            if (itSoc.isWebSocketConnected()) {
                itSoc.setMessageListener(RootJson::class.java) { itUserM ->
                    Log.e("data", "startReceivedAudio: ")
                    playAudio(
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
                    Log.e("data", "startReceivedAudio: ")
                    playAudio(
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

    private fun playAudio(audioData: ByteArray) {
        if (audioTrack != null) {
            audioTrack?.write(audioData, 0, audioData.size)
            audioTrack?.play()
        }
    }

    fun stopAudioReceived() {
        if (audioTrack != null) {
            audioTrack?.stop()
            audioTrack?.release()

            audioTrack = null
        }
    }

    fun stopAudioStreaming() {
        isStreaming = false
        if (audioRecord != null) {
            audioRecord?.stop()
            audioRecord?.release()

            audioRecord = null
        }
    }
}