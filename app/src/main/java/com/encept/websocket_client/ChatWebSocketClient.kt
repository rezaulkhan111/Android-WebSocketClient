package com.encept.websocket_client

import android.util.Log
import com.google.gson.Gson
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

/*
Created By Encept Ltd (https://encept.co)
*/
// initialize websocket client
open class ChatWebSocketClient(
    private var wsServerUrl: String,
    private var headersMap: HashMap<String, String>,
    private var okHttpClient: OkHttpClient,
    private val listener: SignalingListener? = null
) {

    private var webSocket: WebSocket? = null
    private var messageListener: ((String) -> Unit)? = null
    private var isConnected: Boolean = false

    fun connect() {
        val headers = Headers.Builder().apply {
            headersMap.forEach { (key, value) ->
                add(key, value)
            }
        }.build()

        val request = Request.Builder().headers(headers).url(wsServerUrl).build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected = true
                Log.d("WebSocket", "Connected to $wsServerUrl")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
//                messageListener?.invoke(text)
                Log.e("WebRTC", "onMessage" + Gson().toJson(text))
                val model = Gson().fromJson(text, RootJson::class.java)
                val mUserData = model.data
                when (mUserData?.messageType) {
                    SDPType.SDP_OFFER.name -> {
                        val sdp = SessionDescription(
                            SessionDescription.Type.OFFER, mUserData.sdpDescription
                        )
                        listener?.setRemoteDescription(sdp)
                    }

                    SDPType.SDP_ANSWER.name -> {
                        val sdp = SessionDescription(
                            SessionDescription.Type.ANSWER, mUserData.sdpDescription
                        )
                        listener?.setRemoteDescription(sdp)
                    }

                    SDPType.ICE_CANDIDATE.name -> {
                        listener?.onIceCandidateReceived(
                            IceCandidate(
                                mUserData.sdpMid, if (mUserData.sdpMLineIndex != null) {
                                    mUserData.sdpMLineIndex!!
                                } else {
                                    0
                                }, mUserData.sdp
                            )
                        )
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                Log.e("WebSocket", "Connection failed", t)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isConnected = false
                Log.d("WebSocket", "Connection closed: $reason")
            }
        })
    }

    fun sendAudioOrVideoData(mData: UserChat) {
        val userData = Gson().toJson(UserChat().apply {
            receiverId = "9a764f4e-4c7f-4fd5-acef-1915ae18e325"
            isSaveMessage = false
            messageText = "No"
            audioBytes = mData.audioBytes
            videoBytes = mData.videoBytes
//            strAudioBytes =
//                Base64.encodeToString(mByteAudio, Base64.NO_WRAP)
        })
//        val s = "jsonStringForPrimaryCompany"
//        val json: String = Gson().toJson(ByteString.of(*finalBytes))
//        val json: String = userData
//        Utils.writeStringToTextFile(json, s)
        webSocket?.send(userData.toString())
    }

    fun <T> setMessageListener(dtoClass: Class<T>, onComplete: (T) -> Unit) {
        messageListener = { message ->
            try {
                val model = Gson().fromJson(message, dtoClass)
                onComplete(model)
            } catch (e: Exception) {
                Log.e("WebSocket", "Error parsing message", e)
            }
        }
    }

    fun isWebSocketConnected(): Boolean {
        return isConnected
    }

    fun disconnect() {
        webSocket?.close(1000, "Connection closed")
        webSocket = null
        isConnected = false
    }

    fun sendOffer(sdpDes: SessionDescription) {
        val userData = Gson().toJson(UserChat().apply {
            receiverId = "9a764f4e-4c7f-4fd5-acef-1915ae18e325"

            messageType = SDPType.SDP_OFFER.name
            sdpType = sdpDes.type.canonicalForm()
            sdpDescription = sdpDes.description.toString()
        })
//        val s = "jsonStringForPrimaryCompany"
//        val json: String = userData
//        Utils.writeStringToTextFile(json, s)
        webSocket?.send(userData)
    }

    fun sendAnswer(mSdp: SessionDescription) {
        val userData = Gson().toJson(UserChat().apply {
            receiverId = "da5f5e28-6959-4605-b5b1-a4eae25bfd61"

            messageType = SDPType.SDP_ANSWER.name
            sdpType = mSdp.type.canonicalForm()
            sdpDescription = mSdp.description
        })
        webSocket?.send(userData)
    }

    fun sendIceCandidate(candidate: IceCandidate, mReciverId: String) {
        val userData = Gson().toJson(UserChat().apply {
            receiverId = mReciverId
            messageType = SDPType.ICE_CANDIDATE.name

            sdp = candidate.sdp
            sdpMid = candidate.sdpMid
            sdpMLineIndex = candidate.sdpMLineIndex
            serverUrl = candidate.serverUrl
        })
        webSocket?.send(userData)
    }
}

interface SignalingListener {
    fun setRemoteDescription(sdp: SessionDescription)
    fun onIceCandidateReceived(candidate: IceCandidate)
}