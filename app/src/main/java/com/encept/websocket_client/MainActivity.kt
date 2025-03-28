package com.encept.websocket_client

import android.content.pm.PackageManager
import android.os.Bundle
import android.Manifest
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.encept.websocket_client.databinding.ActivityMainBinding
import okhttp3.OkHttpClient
import java.util.*

/*
Created By Encept Ltd (https://encept.co)
*/

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private var wsSendClient: ChatWebSocketClient? = null
    private var wsReciverClient: ChatWebSocketClient? = null

    private var audioStreamer: AudioStreamer? = null
    private var vaStreamer: VAStreamer? = null
    private var videoPlayer: VideoPlayer? = null


    private var vAStreamer: VideoAudioStreamer? = null

    //    private var videoStreamer: VideoStreamer? = null
//    private var audioReceiver: AudioReceiver? = null

    private var mSenderToket =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJVc2VySWQiOiJkYTVmNWUyOC02OTU5LTQ2MDUtYjViMS1hNGVhZTI1YmZkNjEiLCJFbWFpbCI6InJlemF1bEB0ZXN0LmNvbSIsIk1vYmlsZU51bWJlciI6IjAxNzc3NTUwMjg3IiwiVXNlclJvbGVOYW1lIjoiRGlzdHJpY3QgT2ZmaWNlciIsIlVzZXJSb2xlSWQiOiI2MTEzNjdmNi0yZmFhLTQyOTctOTMxMi04OTc3ZTUwOTQwYjgiLCJqdGkiOiI1NDMxNjFjMi1hYWNkLTRiN2YtYjM5YS1mN2RiOTE1MDhiOTYiLCJleHAiOjE3NDIwNTI0NTksImlzcyI6Im1hdGlycHJhbiIsImF1ZCI6IlVzZXIifQ.xRO5GM8Pc7qhmf8WGhvjJuAX_BTsAs2Zy5uA9YBJuHA"

    private var mReciverToket =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJVc2VySWQiOiI5YTc2NGY0ZS00YzdmLTRmZDUtYWNlZi0xOTE1YWUxOGUzMjUiLCJFbWFpbCI6IiIsIk1vYmlsZU51bWJlciI6IjAxNjg1NTE1MDg2IiwiVXNlclJvbGVOYW1lIjoiRmFybWVyIiwiVXNlclJvbGVJZCI6ImFjODhmZmIzLWI5MWYtNDQzMi04NjlkLWJhODBmMTNmNDU3NiIsImp0aSI6IjMyMjgwMTc2LTUxMzEtNDc5My04YTdlLTVjY2Y1OTAwMjAyYSIsImV4cCI6MTc0MTkwNzk3OCwiaXNzIjoibWF0aXJwcmFuIiwiYXVkIjoiVXNlciJ9.AEhmbu7o8eekrrd3dp-g75xDBewiL5ZY1-WBMCY1p14"

    // load server url from strings.xml
//    val serverUri = "ws://127.0.0.1:44324/sendMessage"
    val serverUri = "ws://192.168.0.110:9090/sendMessage"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        wsSendClient = ChatWebSocketClient(
            serverUri, HashMap<String, String>().apply {
                put("Authorization", mSenderToket)
                put("Content-Type", "application/json")
            }, OkHttpClient()
        )

        wsReciverClient = ChatWebSocketClient(
            serverUri, HashMap<String, String>().apply {
                put("Authorization", mReciverToket)
                put("Content-Type", "application/json")
            }, OkHttpClient()
        )

        binding.apply {
            btnStartAudioStream.setOnClickListener {
                if (wsSendClient != null) {
                    wsSendClient?.connect()
                    checkRequestAudioPermissions()
                }
            }

            btnStopAudioStream.setOnClickListener {
                stopAudioStreaming()
            }

            btnStartReceivedAudio.setOnClickListener {
                if (wsReciverClient != null) {
                    wsReciverClient?.connect()
                    startReceivedAudio()
                }
            }

            btnStopReceivedAudio.setOnClickListener {
                if (wsReciverClient != null) {
                    wsReciverClient?.disconnect()
                    stopReceivedAudio()
                }
            }

            btnStartVideoStream.setOnClickListener {
                if (wsSendClient != null) {
                    wsSendClient?.connect()
                    vAStreamer = VideoAudioStreamer(this@MainActivity, wsSendClient!!, pvCamera)
//                    vaStreamer = VAStreamer(
//                        this@MainActivity, wsSendClient!!, pvCamera
//                    )
                    checkAVPermissions()
//                    checkRequestVideoPermissions()
//                    checkRequestAudioPermissions()
                }
            }

            btnStopVideoStream.setOnClickListener {
                if (vAStreamer != null && wsSendClient != null) {
                    wsSendClient?.disconnect()
                    vAStreamer?.stopAudioVideoStreaming()
//                    stopAudioStreaming()
                }
            }

            btnStartVideoReceived.setOnClickListener {
                if (wsReciverClient != null) {
                    wsReciverClient?.connect()
                    videoPlayer = VideoPlayer(binding.svVideoView, wsReciverClient!!)
                    videoPlayer?.initializeDecoder()

                    videoPlayer?.startReceivedVideo()

                    startReceivedAudio()
                }
            }

            btnStopVideoReceived.setOnClickListener {
                if (wsReciverClient != null && videoPlayer != null) {
                    wsReciverClient?.disconnect()
                    videoPlayer?.stopReceivedVideo()

                    stopReceivedAudio()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (wsSendClient != null) {
            wsSendClient?.disconnect()
        }

        if (wsReciverClient != null) {
            wsReciverClient?.disconnect()
        }
//        VideoPlayer.releaseDecoder()
    }

    private fun startAudioStreaming() {
        if (wsSendClient != null) {
            audioStreamer = AudioStreamer(this, wsSendClient!!)
            audioStreamer?.startAudioStreaming()
            Toast.makeText(this, "Audio streaming started!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopAudioStreaming() {
        if (audioStreamer != null && wsSendClient != null) {
            audioStreamer?.stopAudioStreaming()
            wsSendClient?.disconnect()
        }
        Toast.makeText(this, "Audio streaming stopped!", Toast.LENGTH_SHORT).show()
    }

    private fun startReceivedAudio() {
        if (wsReciverClient != null) {
            audioStreamer = AudioStreamer(this, wsReciverClient!!)
            audioStreamer?.startReceivedAudio()
            Toast.makeText(this, "Audio Play started!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopReceivedAudio() {
        if (audioStreamer != null) {
            audioStreamer?.stopAudioReceived()
        }
    }

    private fun checkAVPermissions() {
        val requiredPermissions = arrayOf(
            Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO
        )
        if (!requiredPermissions.all {
                ContextCompat.checkSelfPermission(
                    this, it
                ) == PackageManager.PERMISSION_GRANTED
            }) {
            ActivityCompat.requestPermissions(this, requiredPermissions, 101)
        } else {
            if (vAStreamer != null) {
                vAStreamer?.startAudioVideoStreaming()
            }
        }
    }

    private fun checkRequestVideoPermissions() {
        val requiredPermissions = arrayOf(
            Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO
        )
        if (!requiredPermissions.all {
                ContextCompat.checkSelfPermission(
                    this, it
                ) == PackageManager.PERMISSION_GRANTED
            }) {
            ActivityCompat.requestPermissions(this, requiredPermissions, 101)
        } else {
            if (vaStreamer != null) {
                vaStreamer?.startVideoStreaming()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (vaStreamer != null) {
                vaStreamer?.startVideoStreaming()
            }
        }
    }

    private fun checkRequestAudioPermissions() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startAudioStreaming()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startAudioStreaming()
            } else {
                Toast.makeText(this, "Microphone permission denied!", Toast.LENGTH_SHORT).show()
            }
        }

    /*    private val requestWritePermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
    //                startAudioStreaming()
                } else {
                    Toast.makeText(this, "Microphone permission denied!", Toast.LENGTH_SHORT).show()
                }
            }*/

    /*    fun checkPermission(activity: Activity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                true
            } else {
                activity.requestPermissions(
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 100
                )
//                requestWritePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                false
            }
        } else {
            true
        }
    }*/
}