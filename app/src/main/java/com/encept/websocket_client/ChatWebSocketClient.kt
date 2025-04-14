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
                Log.e("WebSocket", "onMessage " + text)
//                messageListener?.invoke(text)
                val json = JSONObject(text)
                when (json.getString("type")) {
                    "offer" -> {
                        val sdp = SessionDescription(
                            SessionDescription.Type.fromCanonicalForm(json.getString("messageType").lowercase()),
                            json.getString("sdp")
                        )
                        listener?.onRemoteSessionReceived(sdp)
                    }
                    "candidate" -> listener?.onIceCandidateReceived(
                        IceCandidate(
                            json.getString("sdpMid"),
                            json.getInt("sdpMLineIndex"),
                            json.getString("candidate")
                        )
                    )
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

    fun sendOffer(mSdp: String) {
        val userData = Gson().toJson(UserChat().apply {
            receiverId = "9a764f4e-4c7f-4fd5-acef-1915ae18e325"
            isSaveMessage = false
            messageText = "No"
            messageType = SDPType.SDP_OFFER.name
            sdp = mSdp
        })
        webSocket?.send(userData)
    }

    fun sendAnswer(mSdp: String) {
        val userData = Gson().toJson(UserChat().apply {
            receiverId = "9a764f4e-4c7f-4fd5-acef-1915ae18e325"
            isSaveMessage = false
            messageText = "No"
            messageType = SDPType.SDP_ANSWER.name
            sdp = mSdp
        })
        webSocket?.send(userData)
    }

    fun sendSessionDescription(sdp: SessionDescription) {
        val json = JSONObject().apply {
            put("messageType", sdp.type.canonicalForm())
            put("sdp", sdp.description)
        }
        webSocket?.send(json.toString())
    }

    fun sendIceCandidate(candidate: IceCandidate) {
        val userData = Gson().toJson(UserChat().apply {
            receiverId = "9a764f4e-4c7f-4fd5-acef-1915ae18e325"
            isSaveMessage = false
            messageText = "No"
            messageType = SDPType.ICE_CANDIDATE.name
            iceCandidate = candidate.sdp
        })
        webSocket?.send(userData)

        Log.e("WRA", "sendIceCandidate: " + userData)
    }
}

interface SignalingListener {
    fun onRemoteSessionReceived(sdp: SessionDescription)
    fun onIceCandidateReceived(candidate: IceCandidate)
}