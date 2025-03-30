package com.encept.websocket_client

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.encept.websocket_client.databinding.ActivityMainBinding
import okhttp3.OkHttpClient

/*
Created By Encept Ltd (https://encept.co)
*/

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private var wsSendClient: ChatWebSocketClient? = null
    private var wsReciverClient: ChatWebSocketClient? = null

    private var audioStreamer: AudioStreamer? = null
    private var vaStreamer: VAStreamer? = null
    private var vAPlayer: VideoAudioPlayer? = null


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
            btnStartVideoStream.setOnClickListener {
                if (wsSendClient != null) {
                    vAStreamer = VideoAudioStreamer(this@MainActivity, wsSendClient!!, pvCameraView)
                    checkAVPermissions()
                }
            }

            btnStopVideoStream.setOnClickListener {
                if (vAStreamer != null && wsSendClient != null) {
                    vAStreamer?.stopAudioVideoStreaming()
                }
            }

            btnStartVideoReceived.setOnClickListener {
                if (wsReciverClient != null) {
                    vAPlayer = VideoAudioPlayer(binding.svVideoView, wsReciverClient!!)
                    vAPlayer?.initializeDecoder()
                }
            }

            btnStopVideoReceived.setOnClickListener {
                if (wsReciverClient != null && vAPlayer != null) {
                    vAPlayer?.stopReceivedVideo()
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
}