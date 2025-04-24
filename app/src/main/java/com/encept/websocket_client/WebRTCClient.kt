package com.encept.websocket_client

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoCapturer
import org.webrtc.VideoTrack

class WebRTCClient(
    private val context: Context,
    private val localView: SurfaceViewRenderer,
    private val remoteView: SurfaceViewRenderer,
    private val wsChatSocket: ChatWebSocketClient
) {
    private lateinit var peerConnection: PeerConnection
    private lateinit var localVideoTrack: VideoTrack
    private lateinit var localAudioTrack: AudioTrack
    private lateinit var videoCapturer: VideoCapturer

    private val eglBase = EglBase.create()

    private val peerConnectionFactory: PeerConnectionFactory by lazy {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true).setFieldTrials("WebRTC-IntelVP8/Enabled/")
            .createInitializationOptions()

        PeerConnectionFactory.initialize(options)

        PeerConnectionFactory.builder()
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .createPeerConnectionFactory()
    }

    fun init() {
        initViews()
        initLocalStream()
        createPeerConnection()
    }

    private fun initViews() {
        localView.init(eglBase.eglBaseContext, null)
        localView.setZOrderMediaOverlay(true)
        remoteView.init(eglBase.eglBaseContext, null)
    }

    private fun initLocalStream() {
        val videoSource = peerConnectionFactory.createVideoSource(false)
        videoCapturer = createCameraCapturer()
        videoCapturer.initialize(
            SurfaceTextureHelper.create(
                "CaptureThread", eglBase.eglBaseContext
            ), context, videoSource.capturerObserver
        )

        localVideoTrack = peerConnectionFactory.createVideoTrack("LOCAL_VIDEO_TRACK", videoSource)
        localVideoTrack.addSink(localView)
        videoCapturer.startCapture(640, 480, 30)

        val audioSource = peerConnectionFactory.createAudioSource(MediaConstraints())
        localAudioTrack = peerConnectionFactory.createAudioTrack("LOCAL_AUDIO_TRACK", audioSource)
    }

    private fun createPeerConnection() {
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:172.19.141.81:3478").createIceServer(),
            PeerConnection.IceServer.builder("turn:172.19.141.81:3478")
                .setUsername("testuser")
                .setPassword("testpass")
                .createIceServer()
        )

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN

        peerConnection =
            peerConnectionFactory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
                override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
                    Log.e("WRTCC", "onSignalingChange" + Gson().toJson(p0))
                }

                override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
                    Log.e("WRTCC", "onIceConnectionChange" + Gson().toJson(p0))
                }

                override fun onIceConnectionReceivingChange(p0: Boolean) {
                    Log.e("WRTCC", "onIceConnectionReceivingChange" + Gson().toJson(p0))
                }

                override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
                    Log.e("WRTCC", "onIceGatheringChange" + Gson().toJson(p0))
                }

                override fun onIceCandidate(candidate: IceCandidate) {
                    wsChatSocket.sendIceCandidate(candidate)
                    Log.e("WRTCC", "onIceCandidate" + Gson().toJson(candidate))
                }

                override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {
                    Log.e("WRTCC", "onIceCandidatesRemoved" + Gson().toJson(p0))
                }

                override fun onAddStream(stream: MediaStream) {
                    stream.videoTracks.firstOrNull()?.addSink(remoteView)
                }

                override fun onRemoveStream(p0: MediaStream?) {
                    Log.e("WRTCC", "onRemoveStream" + Gson().toJson(p0))
                }

                override fun onDataChannel(p0: DataChannel?) {
                    Log.e("WRTCC", "onDataChannel" + Gson().toJson(p0))
                }

                override fun onRenegotiationNeeded() {
                    Log.e("WRTCC", "onRenegotiationNeeded")
                }

                override fun onTrack(transceiver: RtpTransceiver?) {
                    val track = transceiver?.receiver?.track() as? VideoTrack
                    if (track is VideoTrack) {
                        track.addSink(remoteView)
                    }
                }
            })!!

        val stream = peerConnectionFactory.createLocalMediaStream("LOCAL_STREAM")
        stream.addTrack(localVideoTrack)
        stream.addTrack(localAudioTrack)

        peerConnection.addTrack(localVideoTrack)
        peerConnection.addTrack(localAudioTrack)

        peerConnection.getStats { report ->
            for (stat in report.statsMap.values) {
                Log.d("WebRTC-Stats", stat.toString())
            }
        }
    }

    private fun createCameraCapturer(): VideoCapturer {
        val enumerator = Camera2Enumerator(context)
        for (deviceName in enumerator.deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                return enumerator.createCapturer(deviceName, null)
            }
        }
        throw IllegalStateException("No front camera found")
    }

    fun createOffer() {
        val constraints = MediaConstraints()
        peerConnection.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sessionDescription: SessionDescription) {
                peerConnection.setLocalDescription(this, sessionDescription)
                wsChatSocket.sendOffer(sessionDescription)
            }

            override fun onSetSuccess() {
            }

            override fun onCreateFailure(p0: String?) {
            }

            override fun onSetFailure(p0: String?) {
            }
        }, constraints)
    }

    fun onRemoteSessionReceived(sessionDescription: SessionDescription) {
        peerConnection.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {
                Log.d("WebRTC", "Remote SDP set successfully")

                // Now create the answer
                peerConnection.createAnswer(object : SdpObserver {
                    override fun onCreateSuccess(desc: SessionDescription?) {
                        Log.d("WebRTC", "Answer created: $desc")
                        // Set local description
                        peerConnection.setLocalDescription(object : SdpObserver {
                            override fun onSetSuccess() {
                                Log.d("WebRTC", "Local description set successfully (answer)")
                                // Send this answer to remote peer via WebSocket
                                wsChatSocket.sendAnswer(desc?.description!!)
                            }

                            override fun onSetFailure(error: String?) {
                                Log.e("WebRTC", "Failed to set local description (answer): $error")
                            }

                            override fun onCreateSuccess(p0: SessionDescription?) {}
                            override fun onCreateFailure(p0: String?) {}
                        }, desc)
                    }

                    override fun onCreateFailure(error: String?) {
                        Log.e("WebRTC", "Failed to create answer: $error")
                    }

                    override fun onSetSuccess() {}
                    override fun onSetFailure(p0: String?) {}
                }, MediaConstraints()) // optional constraints
            }

            override fun onSetFailure(p0: String?) {
                Log.e("WebRTC", "Failed to set remote SDP: $p0")
            }

            override fun onCreateSuccess(p0: SessionDescription?) {
                Log.e("WebRTC", "onCreateSuccess: $p0")
            }

            override fun onCreateFailure(p0: String?) {
                Log.e("WebRTC", "onCreateFailure: $p0")
            }
        }, sessionDescription)
    }

    fun addIceCandidate(candidate: IceCandidate) {
        peerConnection.addIceCandidate(candidate)
    }
}