package com.encept.websocket_client

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.encept.websocket_client.databinding.ActivityWebRtcBinding
import com.google.gson.Gson
import okhttp3.OkHttpClient
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpTransceiver
import org.webrtc.SessionDescription
import org.webrtc.VideoTrack

class WebRtcActivity : AppCompatActivity(),
    SignalingListener {

    private lateinit var binding: ActivityWebRtcBinding

    private var wsSendClient: ChatWebSocketClient? = null
    private lateinit var webRTCClient: WebRTCClient

    private var mSenderToket =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJVc2VySWQiOiJkYTVmNWUyOC02OTU5LTQ2MDUtYjViMS1hNGVhZTI1YmZkNjEiLCJFbWFpbCI6InJlemF1bEB0ZXN0LmNvbSIsIk1vYmlsZU51bWJlciI6IjAxNzc3NTUwMjg3IiwiVXNlclJvbGVOYW1lIjoiRGlzdHJpY3QgT2ZmaWNlciIsIlVzZXJSb2xlSWQiOiI2MTEzNjdmNi0yZmFhLTQyOTctOTMxMi04OTc3ZTUwOTQwYjgiLCJqdGkiOiI1NDMxNjFjMi1hYWNkLTRiN2YtYjM5YS1mN2RiOTE1MDhiOTYiLCJleHAiOjE3NDIwNTI0NTksImlzcyI6Im1hdGlycHJhbiIsImF1ZCI6IlVzZXIifQ.xRO5GM8Pc7qhmf8WGhvjJuAX_BTsAs2Zy5uA9YBJuHA"
    val serverUri = "ws://192.168.0.110:9090/sendMessage"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebRtcBinding.inflate(layoutInflater)
        setContentView(binding.root)

        wsSendClient = ChatWebSocketClient(
            serverUri, HashMap<String, String>().apply {
                put("Authorization", mSenderToket)
                put("Content-Type", "application/json")
            }, OkHttpClient(), this
        )
        webRTCClient = WebRTCClient(this, binding.localView, binding.remoteView, wsSendClient!!)
        wsSendClient?.connect()

        webRTCClient.init()
    }

    override fun onRemoteSessionReceived(sdp: SessionDescription) {
        webRTCClient.onRemoteSessionReceived(sdp)
    }

    override fun onIceCandidateReceived(iceCandidate: IceCandidate) {
        webRTCClient.addIceCandidate(iceCandidate)
        Log.e("WebRTC", "onIceCandidateReceived: " + Gson().toJson(iceCandidate))
    }
}