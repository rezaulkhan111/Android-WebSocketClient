package com.encept.websocket_client

import android.content.pm.PackageManager
import android.os.Bundle
import android.Manifest
import android.app.Activity
import android.os.Build
import android.util.Base64
import android.util.Log
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.encept.websocket_client.databinding.ActivityMainBinding
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okio.ByteString
import java.net.URI
import java.util.*

/*
Created By Encept Ltd (https://encept.co)
*/

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var webSocketClient: ChatWebSocketClient
    private var audioStreamer: AudioStreamer? = null
//    private var audioReceiver: AudioReceiver? = null

    private var mSenderToket =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJVc2VySWQiOiJkYTVmNWUyOC02OTU5LTQ2MDUtYjViMS1hNGVhZTI1YmZkNjEiLCJFbWFpbCI6InJlemF1bEB0ZXN0LmNvbSIsIk1vYmlsZU51bWJlciI6IjAxNzc3NTUwMjg3IiwiVXNlclJvbGVOYW1lIjoiRGlzdHJpY3QgT2ZmaWNlciIsIlVzZXJSb2xlSWQiOiI2MTEzNjdmNi0yZmFhLTQyOTctOTMxMi04OTc3ZTUwOTQwYjgiLCJqdGkiOiI1NDMxNjFjMi1hYWNkLTRiN2YtYjM5YS1mN2RiOTE1MDhiOTYiLCJleHAiOjE3NDIwNTI0NTksImlzcyI6Im1hdGlycHJhbiIsImF1ZCI6IlVzZXIifQ.xRO5GM8Pc7qhmf8WGhvjJuAX_BTsAs2Zy5uA9YBJuHA"

    private var mReciverToket =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJVc2VySWQiOiI5YTc2NGY0ZS00YzdmLTRmZDUtYWNlZi0xOTE1YWUxOGUzMjUiLCJFbWFpbCI6IiIsIk1vYmlsZU51bWJlciI6IjAxNjg1NTE1MDg2IiwiVXNlclJvbGVOYW1lIjoiRmFybWVyIiwiVXNlclJvbGVJZCI6ImFjODhmZmIzLWI5MWYtNDQzMi04NjlkLWJhODBmMTNmNDU3NiIsImp0aSI6IjMyMjgwMTc2LTUxMzEtNDc5My04YTdlLTVjY2Y1OTAwMjAyYSIsImV4cCI6MTc0MTkwNzk3OCwiaXNzIjoibWF0aXJwcmFuIiwiYXVkIjoiVXNlciJ9.AEhmbu7o8eekrrd3dp-g75xDBewiL5ZY1-WBMCY1p14"

    // load server url from strings.xml
//    val serverUri = "ws://127.0.0.1:44324/sendMessage"
    val serverUri = "ws://192.168.0.110:9090/sendMessage"

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startAudioStreaming()
            } else {
                Toast.makeText(this, "Microphone permission denied!", Toast.LENGTH_SHORT).show()
            }
        }

    private val requestWritePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startAudioStreaming()
            } else {
                Toast.makeText(this, "Microphone permission denied!", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.apply {
            btnStart.setOnClickListener {
                checkPermission(this@MainActivity)

                webSocketClient = ChatWebSocketClient(
                    serverUri, HashMap<String, String>().apply {
                        put("Authorization", mSenderToket)
                        put("Content-Type", "application/json")
                    }, OkHttpClient()
                )
                webSocketClient.connect()
                checkAndRequestPermissions()
            }

            btnStop.setOnClickListener {
                stopAudioStreaming()
            }

            btnPlayAudio.setOnClickListener {
                webSocketClient = ChatWebSocketClient(
                    serverUri, HashMap<String, String>().apply {
                        put("Authorization", mReciverToket)
                        put("Content-Type", "application/json")
                    }, OkHttpClient()
                )
                webSocketClient.connect()
                startAudioPlay()
            }

            btnStopPlayAudio.setOnClickListener {
                webSocketClient.disconnect()
                stopAudioPlay()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webSocketClient.disconnect()
    }

    private fun checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startAudioStreaming()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startAudioStreaming() {
        audioStreamer = AudioStreamer(this, webSocketClient)
        audioStreamer?.startStreaming()
        Toast.makeText(this, "Audio streaming started!", Toast.LENGTH_SHORT).show()
    }

    private fun startAudioPlay() {
        audioStreamer = AudioStreamer(this, webSocketClient)
        audioStreamer?.startRecive()
        Toast.makeText(this, "Audio Play started!", Toast.LENGTH_SHORT).show()
    }

    private fun stopAudioPlay() {
        audioStreamer = AudioStreamer(this, webSocketClient)
        audioStreamer?.stopAudio()
    }

    private fun stopAudioStreaming() {
        audioStreamer?.stopStreaming()
        webSocketClient.disconnect()
        Toast.makeText(this, "Audio streaming stopped!", Toast.LENGTH_SHORT).show()
    }

    fun checkPermission(activity: Activity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                true
            } else {
                activity.requestPermissions(
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 100
                )
                requestWritePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)

                false
            }
        } else {
            true
        }
    }
}