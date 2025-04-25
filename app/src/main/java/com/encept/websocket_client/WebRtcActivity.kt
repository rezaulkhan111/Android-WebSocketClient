package com.encept.websocket_client

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.encept.websocket_client.databinding.ActivityWebRtcBinding
import com.google.gson.Gson
import okhttp3.OkHttpClient
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription

class WebRtcActivity : AppCompatActivity(), SignalingListener, RTCCallBack {

    private lateinit var binding: ActivityWebRtcBinding

    private var wsSendClient: ChatWebSocketClient? = null
    private lateinit var webRTCClient: WebRTCClient

    private var mAnyTokenData: String? = null
    private var mReciverId: String? = null
    val serverUri = "ws://192.168.0.110:9090/sendMessage"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebRtcBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (intent.extras != null) {
            val mCallType = intent.extras?.getString(Utils.CALL_TYPE_KEY)
            if (mCallType == "SEND") {
                val mLocalSenderToken = intent.extras?.getString(Utils.CALL_TYPE_SEND_KEY)

                mReciverId = intent.extras?.getString(Utils.CALL_RECIVER_KEY)
                mAnyTokenData = mLocalSenderToken
            } else {
                val mLocalReciveToken = intent.extras?.getString(Utils.CALL_TYPE_RECIVED_KEY)

                mReciverId = intent.extras?.getString(Utils.CALL_RECIVER_KEY)
                mAnyTokenData = mLocalReciveToken
            }

            Log.e("WRA", "onCreate: reciver " + mReciverId)
            if (!mAnyTokenData.isNullOrEmpty()) {
                wsSendClient = ChatWebSocketClient(
                    serverUri, HashMap<String, String>().apply {
                        put("Authorization", mAnyTokenData!!)
                        put("Content-Type", "application/json")
                    }, OkHttpClient(), this
                )

                webRTCClient = WebRTCClient(
                    this, binding.localView, binding.remoteView, wsSendClient!!, mReciverId!!, this
                )
                wsSendClient?.connect()

                webRTCClient.init()

                binding.apply {
                    btnCallOffer.setOnClickListener {
                        webRTCClient.createOffer()
                    }
                }
            }
        }
    }

    override fun setRemoteDescription(sdp: SessionDescription) {
        webRTCClient.setRemoteDescription(sdp)
    }

    override fun onIceCandidateReceived(iceCandidate: IceCandidate) {
        webRTCClient.addIceCandidate(iceCandidate)
    }

    override fun onIceState(state: PeerConnection.IceConnectionState) {
        if (state == PeerConnection.IceConnectionState.DISCONNECTED) {
//            if (webRTCClient != null) {
//                webRTCClient.release()
//            }
            finish()
        }
    }
}