package com.encept.websocket_client

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Log
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.Executors

class VAStreamer(
    private val context: Context,
    private val webSocketClient: ChatWebSocketClient,
    val pvCameraView: PreviewView
) {

    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var encoder: MediaCodec? = null
    private var inputSurface: Surface? = null
    private var cameraProvider: ProcessCameraProvider? = null

    fun startVideoStreaming() {
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("VideoStreamer", "Camera permission not granted")
            return
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().apply {
                surfaceProvider = pvCameraView.surfaceProvider // Show camera feed in UI
            }

            preview.setSurfaceProvider { request ->
                inputSurface?.let { surface ->
                    request.provideSurface(surface, cameraExecutor) {}
                }
            }
            initializeEncoder()

            val imageAnalysis =
                ImageAnalysis.Builder().setTargetResolution(android.util.Size(640, 480))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                try {
                    val buffer = imageProxy.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
//                    val encodedFrame = encodeVideoFrame(bytes)
//                    if (encodedFrame != null) {
//                        webSocketClient.sendAudioOrVideoData(encodedFrame)
//                    }
                } finally {
                    imageProxy.close()
                }
            }

            cameraProvider.bindToLifecycle(
                context as LifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalysis
            )

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

        startEncoding()
    }

    private fun startEncoding() {
        Thread {
            val bufferInfo = MediaCodec.BufferInfo()
            Log.e("VAStreamer", "startEncoding Encoding thread started." + bufferInfo.size)
            while (true) {
                val outputIndex = encoder?.dequeueOutputBuffer(bufferInfo, 10000) ?: -1

                if (outputIndex >= 0) {
                    Log.e("VAStreamer", "startEncoding Output buffer found, processing...")

                    val outputBuffer = encoder?.getOutputBuffer(outputIndex)
                    val encodedData = ByteArray(bufferInfo.size)
                    outputBuffer?.get(encodedData)

                    Log.e(
                        "VAStreamer", "startEncoding Encoded data size: ${encodedData.size} bytes"
                    )

                    // Send encoded frame over WebSocket
                    webSocketClient.sendAudioOrVideoData(encodedData)
                    encoder?.releaseOutputBuffer(outputIndex, false)
                } else {
                    Log.e("VAStreamer", "startEncoding No output buffer available.")
                }

                Thread.sleep(10)
            }
        }.start()
    }

/*    private fun encodeVideoFrame(rawFrame: ByteArray): ByteArray? {
        if (encoder != null) {
            try {
                val inputBufferIndex = encoder!!.dequeueInputBuffer(10000)
                var encodedData: ByteArray? = null
                Log.e("VAStreamer", "encodeVideoFrame inputBufferIndex " + inputBufferIndex)
                if (inputBufferIndex >= 0) {
                    val inputBuffer = encoder?.getInputBuffer(inputBufferIndex)
                    inputBuffer?.clear()
                    // Ensure the rawFrame is not empty
                    if (rawFrame.isNotEmpty()) {
                        inputBuffer?.put(rawFrame)
                        encoder?.queueInputBuffer(
                            inputBufferIndex, 0, rawFrame.size, System.nanoTime() / 1000, 0
                        )
                    } else {
                        Log.e(
                            "VAStreamer", "encodeVideoFrame Raw frame is empty, skipping encoding."
                        )
                    }

                    val bufferInfo = MediaCodec.BufferInfo()
                    var outputBufferIndex = encoder?.dequeueOutputBuffer(bufferInfo, 10000) ?: -1

                    while (outputBufferIndex >= 0) {
                        if (bufferInfo.size > 0) {
                            val outputBuffer = encoder?.getOutputBuffer(outputBufferIndex)
                            encodedData = ByteArray(bufferInfo.size)
                            outputBuffer?.get(encodedData)

                            Log.e("VAStreamer", "encodeVideoFrame while: ${encodedData.size}")
                            // Send encoded frame over WebSocket
                            webSocketClient.sendAudioOrVideoData(encodedData)
                            encoder?.releaseOutputBuffer(outputBufferIndex, false)
                        } else {
                            Log.e(
                                "VAStreamer",
                                "encodeVideoFrame Output buffer is empty, size: ${bufferInfo.size}"
                            )
                        }
                        outputBufferIndex = encoder?.dequeueOutputBuffer(bufferInfo, 10000) ?: -1
                    }
                } else {
                    Log.e("VAStreamer", "encodeVideoFrame No input buffer available.")
                }

                return encodedData
            } catch (e: IllegalStateException) {
                Log.e("VideoStreamer", "encodeVideoFrame MediaCodec is in an incorrect state!", e)
            }
        }

        return null
    }*/

    fun stopCamera() {
        if (cameraExecutor != null && cameraProvider != null && encoder != null) {
            cameraProvider?.unbindAll()
            cameraExecutor.shutdown()
            encoder?.stop()
            encoder?.release()
        }
    }
}