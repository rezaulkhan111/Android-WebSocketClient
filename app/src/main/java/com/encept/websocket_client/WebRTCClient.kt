package com.encept.websocket_client

import android.content.Context
import android.media.AudioManager
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
import org.webrtc.audio.JavaAudioDeviceModule

class WebRTCClient(
    private val contextRtc: Context,
    private val localView: SurfaceViewRenderer,
    private val remoteView: SurfaceViewRenderer,
    private val wsChatSocket: ChatWebSocketClient,
    private val mReciverId: String,
    private val mRtcCallback: RTCCallBack
) {
    private lateinit var peerConnection: PeerConnection
    private lateinit var localVideoTrack: VideoTrack
    private lateinit var localAudioTrack: AudioTrack
    private lateinit var videoCapturer: VideoCapturer
    private var remoteAudioTrack: AudioTrack? = null

    private val eglBase = EglBase.create()

    private val peerConnectionFactory: PeerConnectionFactory by lazy {
        val options = PeerConnectionFactory.InitializationOptions.builder(contextRtc)
            .setEnableInternalTracer(true).setFieldTrials("WebRTC-IntelVP8/Enabled/")
            .createInitializationOptions()

        PeerConnectionFactory.initialize(options)

        val audioDeviceModule = JavaAudioDeviceModule.builder(contextRtc)
            .setUseHardwareAcousticEchoCanceler(true)
            .setUseHardwareNoiseSuppressor(true)
            .createAudioDeviceModule()

        PeerConnectionFactory.builder()
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .createPeerConnectionFactory()
    }

    fun init() {
        initViews()
        initLocalStream()
        createPeerConnection()

        val audioManager = contextRtc.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = true
    }

    fun release() {
        try {
            videoCapturer.stopCapture()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        videoCapturer.dispose()
        localView.release()
        remoteView.release()
        eglBase.release()
        peerConnection.dispose()
        peerConnectionFactory.dispose()
    }

    private fun initViews() {
        localView.init(eglBase.eglBaseContext, null)
        localView.setZOrderMediaOverlay(true)
        localView.setMirror(true)
        localView.setEnableHardwareScaler(true)

        remoteView.init(eglBase.eglBaseContext, null)
        remoteView.setZOrderMediaOverlay(true)
        remoteView.setMirror(true)
        remoteView.setEnableHardwareScaler(true)
    }

    private fun initLocalStream() {
        val videoSource = peerConnectionFactory.createVideoSource(false)
        videoCapturer = createCameraCapturer()
        videoCapturer.initialize(
            SurfaceTextureHelper.create(
                "CaptureThread",
                eglBase.eglBaseContext
            ), contextRtc,
            videoSource.capturerObserver
        )

        localVideoTrack = peerConnectionFactory.createVideoTrack("LOCAL_VIDEO_TRACK", videoSource)
        localVideoTrack.addSink(localView)

        videoCapturer.startCapture(640, 480, 30)

        val audioSource = peerConnectionFactory.createAudioSource(MediaConstraints())
        localAudioTrack = peerConnectionFactory.createAudioTrack("LOCAL_AUDIO_TRACK", audioSource)

        localAudioTrack.setEnabled(true)
    }

    private fun createPeerConnection() {
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.ourcodeworld.com:5349").createIceServer(),
            PeerConnection.IceServer.builder("turn:turn.ourcodeworld.com:5349")
                .setUsername("brucewayne").setPassword("12345").createIceServer()
        )

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN

        peerConnection =
            peerConnectionFactory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
                override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
                }

                override fun onIceConnectionChange(connState: PeerConnection.IceConnectionState?) {
                    mRtcCallback.onIceState(connState!!)
                }

                override fun onIceConnectionReceivingChange(p0: Boolean) {
                }

                override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
                }

                override fun onIceCandidate(candidate: IceCandidate) {
                    wsChatSocket.sendIceCandidate(candidate, mReciverId)
                }

                override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {
                }

                override fun onAddStream(stream: MediaStream) {
                }

                override fun onRemoveStream(p0: MediaStream?) {

                }

                override fun onDataChannel(p0: DataChannel?) {
                }

                override fun onRenegotiationNeeded() {
                    Log.e("WebRTC", "Renegotiation needed")
                }

                override fun onTrack(transceiver: RtpTransceiver?) {
                    val trackRece = transceiver?.receiver
                    if (trackRece != null) {
                        val mTrackType = trackRece.track()

                        if (mTrackType is VideoTrack) {
                            mTrackType.addSink(remoteView)
                        } else if (mTrackType is AudioTrack) {
                            remoteAudioTrack = mTrackType

                            remoteAudioTrack?.setEnabled(true)
                        }
                    }
                }
            })!!

//        val mediaStream = peerConnectionFactory.createLocalMediaStream("LOCAL_STREAM")
//        mediaStream.addTrack(localVideoTrack)
//        mediaStream.addTrack(localAudioTrack)

        peerConnection.addTrack(localVideoTrack, listOf("ARDAMS"))
        peerConnection.addTrack(localAudioTrack, listOf("ARDAMS"))

//        peerConnection.getStats { report ->
//            for (stat in report.statsMap.values) {
//                Log.e("WebRTC-Stats", stat.toString())
//            }
//        }
    }

    private fun createCameraCapturer(): VideoCapturer {
        val enumerator = Camera2Enumerator(contextRtc)
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

            override fun onCreateFailure(error: String?) {
                Log.e("WebRTC", "CreateOffer failed: $error")
            }

            override fun onSetFailure(p0: String?) {
            }
        }, constraints)
    }

    private val pendingIceCandidates = mutableListOf<IceCandidate>()
    private var remoteSdpSet = false

    fun setRemoteDescription(sessionDescription: SessionDescription) {
        Log.e("WebRTC", "setRemoteDescription" + Gson().toJson(sessionDescription))
        peerConnection.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {
                Log.e("WebRTC", "Remote SDP set successfully")

                remoteSdpSet = true
                for (candidate in pendingIceCandidates) {
                    peerConnection.addIceCandidate(candidate)
                }
                pendingIceCandidates.clear()

                peerConnection.createAnswer(object : SdpObserver {
                    override fun onCreateSuccess(desc: SessionDescription) {
                        Log.e("WebRTC", "Answer created: " + Gson().toJson(desc))

                        peerConnection.setLocalDescription(this, desc)
                        wsChatSocket.sendAnswer(desc)
                    }

                    override fun onCreateFailure(error: String?) {
                        Log.e("WebRTC", "Failed to create answer: $error")
                    }

                    override fun onSetSuccess() {}
                    override fun onSetFailure(p0: String?) {}
                }, MediaConstraints())
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
        if (remoteSdpSet) {
            peerConnection.addIceCandidate(candidate)
        } else {
            pendingIceCandidates.add(candidate)
        }
    }
}

interface RTCCallBack {
    fun onIceState(state: PeerConnection.IceConnectionState)
}