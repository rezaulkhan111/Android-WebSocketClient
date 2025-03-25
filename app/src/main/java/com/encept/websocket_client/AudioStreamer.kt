package com.encept.websocket_client

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.gson.Gson
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
    private val audioTrack = AudioTrack(
        AudioManager.STREAM_MUSIC,
        sampleRate,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
        bufferSize,
        AudioTrack.MODE_STREAM
    )

    private var isStreaming = false

    fun startStreaming() {
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
//                    val bufferAudioEncod = Base64.encodeToString(buffer, Base64.NO_WRAP)
//                    val audioByte = Base64.decode(bufferAudioEncod, Base64.NO_WRAP)!!
//                    Log.e("MA", "startAudioPlay: " + Gson().toJson(audioByte))
//                    playAudio(audioByte)
                    webSocketClient.sendAudioData(buffer)
                }
            }
        }
    }

    fun startRecive() {
        webSocketClient.let { itSoc ->
            if (itSoc.isWebSocketConnected()) {
                itSoc.setMessageListener(RootJson::class.java) { itUserM ->
                    playAudio(
                        if (itUserM.data?.audioBytes != null) {
//                            val audioByte =
//                                Base64.decode(itUserM.data?.audioBytes, Base64.NO_WRAP)!!
                            itUserM.data?.audioBytes!!
                        } else {
//                            Base64.decode("", Base64.NO_WRAP)!!
                            byteArrayOf()
                        }
                    )
                }
            } else {
                itSoc.connect()
                itSoc.setMessageListener(RootJson::class.java) { itUserM ->
                    playAudio(
                        if (itUserM.data?.audioBytes != null) {
//                            val audioByte = Base64.decode(itUserM.strAudioBytes!!, Base64.NO_WRAP)!!
//                            Log.e("MA", "startAudioPlay: " + Gson().toJson(audioByte))
//                            audioByte
                            itUserM.data?.audioBytes!!
                        } else {
//                            Base64.decode("", Base64.NO_WRAP)!!
                            byteArrayOf()
                        }
                    )
                }
            }
        }
    }

    fun playAudio(audioData: ByteArray) {
        audioTrack.write(audioData, 0, audioData.size)
        audioTrack.play()
    }

    fun stopAudio() {
        audioTrack.stop()
        audioTrack.release()
    }

    fun stopStreaming() {
        isStreaming = false
        if (audioRecord != null) {
            audioRecord?.stop()
            audioRecord?.release()
        }
    }
}