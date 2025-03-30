package com.encept.websocket_client

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaRecorder
import android.util.Log
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class VideoAudioStreamer(
    private val context: Context,
    private val webSocketClient: ChatWebSocketClient?,
    val pvCameraView: PreviewView
) {
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var encoder: MediaCodec? = null
    private var inputSurface: Surface? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var isVideoStreaming = false

    private val sampleRate = 44100
    private val audioBufferSize = AudioRecord.getMinBufferSize(
        sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
    )
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var isAudioStreaming = false

    fun startAudioVideoStreaming() {
        val requiredPermissions = arrayOf(
            Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO
        )
        if (!requiredPermissions.all { itPermissions ->
                ContextCompat.checkSelfPermission(
                    context, itPermissions
                ) == PackageManager.PERMISSION_GRANTED
            }) {
            Log.e("VideoStreamer", "Camera and audio permission not granted")
            return
        }

        webSocketClient?.connect()
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            audioBufferSize
        )

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().apply {
                surfaceProvider = pvCameraView.surfaceProvider
            }

            val imageAnalysis =
                ImageAnalysis.Builder().setTargetResolution(android.util.Size(640, 480))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                val buffer = imageProxy.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                imageProxy.close()
            }

            cameraProvider?.bindToLifecycle(
                context as LifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalysis
            )

            isVideoStreaming = true
            initializeEncoder()
            //if used this. Video Preview not show
            preview.setSurfaceProvider { request ->
                inputSurface?.let { surface ->
                    request.provideSurface(surface, cameraExecutor) {}
                }
            }

        }, ContextCompat.getMainExecutor(context))
    }

    private fun initializeEncoder() {
        val format = MediaFormat.createVideoFormat("video/avc", 640, 480).apply {
            setInteger(
                MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
            )
            setInteger(MediaFormat.KEY_BIT_RATE, 500_000)
            setInteger(MediaFormat.KEY_FRAME_RATE, 30)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        encoder = MediaCodec.createEncoderByType("video/avc").apply {
            configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = createInputSurface()

            start()
        }

        isAudioStreaming = true
        audioRecord?.startRecording()

        if (encoder != null) {
            CoroutineScope(Dispatchers.IO).launch {
//            Thread {
                try {
                    val bufferInfo = MediaCodec.BufferInfo()
                    while (isVideoStreaming && isAudioStreaming) {
                        val videoOutputIndex = encoder?.dequeueOutputBuffer(bufferInfo, 10000) ?: -1

                        val audioBuffer = ByteArray(audioBufferSize)
                        val audioReadBytes =
                            audioRecord?.read(audioBuffer, 0, audioBuffer.size) ?: -1

                        if (videoOutputIndex >= 0 && audioReadBytes > 0) {
                            val videoOutputBuffer = encoder?.getOutputBuffer(videoOutputIndex)
                            val videoEncodedData = ByteArray(bufferInfo.size)
                            videoOutputBuffer?.get(videoEncodedData)

                            webSocketClient?.sendAudioOrVideoData(UserChat().apply {
                                audioBytes = audioBuffer
                                videoBytes = videoEncodedData
                            })
                            // Send encoded frame over WebSocket
                            encoder?.releaseOutputBuffer(videoOutputIndex, false)
                        } else {
                            Log.e("VAStreamer", "startEncoding No output buffer available.")
                        }
//                    Thread.sleep(10)
                    }
                } catch (exp: Exception) {
                    Log.e("VAStreamer2", "exp: " + exp.message)
                }
//            }.start()
            }
        }
    }

    fun stopAudioVideoStreaming() {
        isAudioStreaming = false
        isVideoStreaming = false

        if (cameraExecutor != null && cameraProvider != null && encoder != null && audioRecord != null && webSocketClient!=null) {
            webSocketClient.disconnect()

            cameraExecutor.shutdown()
            cameraProvider?.unbindAll()
            encoder?.stop()
            encoder?.release()

            audioRecord?.stop()
            audioRecord?.release()

            audioRecord = null
            encoder = null
        }
    }
}