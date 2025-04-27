package com.encept.websocket_client

import com.google.gson.annotations.SerializedName

class UserChat {
    @SerializedName("messageId")
    var messageId: String? = null

    @SerializedName("senderId")
    var senderId: String? = null

    @SerializedName("receiverId")
    var receiverId: String? = null

    @SerializedName("messageText")
    var messageText: String? = null

    @SerializedName("messageType")
    var messageType: String? = null

    @SerializedName("receiverType")
    var receiverType: String? = null

    @SerializedName("createdAt")
    var createdAt: String? = null

    @SerializedName("isRead")
    var isRead: Boolean? = null

    @SerializedName("isSaveMessage")
    var isSaveMessage: Boolean? = null

    @SerializedName("audioBytes")
    var audioBytes: ByteArray? = null

    @SerializedName("videoBytes")
    var videoBytes: ByteArray? = null

    @SerializedName("audioBase64String")
    var audioBase64String: String? = null

    @SerializedName("videoBase64String")
    var videoBase64String: String? = null

    @SerializedName("sdpType")
    var sdpType: String? = null

    @SerializedName("sdpDescription")
    var sdpDescription: String? = null

    //Ice part
    @SerializedName("sdp")
    var sdp: String? = null

    @SerializedName("sdpMid")
    var sdpMid: String? = null

    @SerializedName("sdpMLineIndex")
    var sdpMLineIndex: Int? = null

    @SerializedName("serverUrl")
    var serverUrl: String? = null
}

class IceCandidate {
    @SerializedName("sdp")
    var sdp: String? = null

    @SerializedName("sdpMid")
    var sdpMid: String? = null

    @SerializedName("sdpMLineIndex")
    var sdpMLineIndex: Int? = null

    @SerializedName("serverUrl")
    var serverUrl: String? = null
}