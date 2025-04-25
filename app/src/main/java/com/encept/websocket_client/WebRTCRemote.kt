package com.encept.websocket_client

import android.content.Context
import android.util.Log
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

class WebRTCRemote {
    private var peerConnection: PeerConnection? = null
    private var remoteVideoTrack: VideoTrack? = null
    private var remoteRenderer: SurfaceViewRenderer? = null

    fun initialize(context: Context, remoteRenderer: SurfaceViewRenderer) {
        this.remoteRenderer = remoteRenderer
    }

    fun onOfferReceived(offerSdp: String) {
        val sdp = SessionDescription(SessionDescription.Type.OFFER, offerSdp)
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {
            }

            override fun onSetSuccess() {
                createAnswer()
            }

            override fun onCreateFailure(p0: String?) {
            }

            override fun onSetFailure(p0: String?) {
            }
        }, sdp)
    }

    private fun createAnswer() {
        val mediaConstraints = MediaConstraints()
        peerConnection?.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription) {
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {

                    }

                    override fun onSetSuccess() {
                        sendAnswer(sdp)
                    }

                    override fun onCreateFailure(p0: String?) {

                    }

                    override fun onSetFailure(p0: String?) {
                    }
                }, sdp)
            }

            override fun onCreateFailure(error: String?) {
                Log.e("WebRTCClient", "Error creating answer: $error")
            }

            override fun onSetSuccess() {}
            override fun onSetFailure(error: String?) {}
        }, mediaConstraints)
    }

    private fun sendAnswer(answerSdp: SessionDescription) {
    }

    private fun initPeerConnection() {
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:172.19.141.81:3478").createIceServer(),
            PeerConnection.IceServer.builder("turn:172.19.141.81:3478").setUsername("testuser")
                .setPassword("testpass").createIceServer()
        )
        val peerConnectionFactory = PeerConnectionFactory.builder().createPeerConnectionFactory()
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)
        peerConnection =
            peerConnectionFactory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
                override fun onIceCandidate(p0: IceCandidate?) {
                }

                override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
                }

                override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
                    if (newState == PeerConnection.IceConnectionState.CONNECTED) {
                        Log.d("WebRTCClient", "ICE connection established")
                    }
                }

                override fun onIceConnectionReceivingChange(p0: Boolean) {
                }

                override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {

                }


                override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {

                }

                override fun onAddStream(mediaStream: MediaStream?) {

                }

                override fun onRemoveStream(p0: MediaStream?) {

                }

                override fun onDataChannel(p0: DataChannel?) {

                }

                override fun onRenegotiationNeeded() {

                }

                override fun onTrack(transceiver: RtpTransceiver?) {
                    val track = transceiver?.receiver?.track() as? VideoTrack
                    if (track is VideoTrack) {
                        track.addSink(remoteRenderer)
                    }
                }
            })
    }
}