package com.encept.websocket_client

import com.google.gson.annotations.SerializedName

class RootJson {
    @SerializedName("errorStatusCode")
    var errorStatusCode: Int? = null

    @SerializedName("IsStatus")
    var IsStatus: Boolean? = null

    @SerializedName("messageAny")
    var messageAny: String? = null

    @SerializedName("errorShowType")
    var errorShowType: Boolean? = null

    @SerializedName("data")
    var data: UserChat? = null
}