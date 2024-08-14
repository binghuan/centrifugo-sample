package com.bh.centrifugo_android_client.vo

data class RtmChannelMsg(
    val account: String? = null,
    val id: String? = null,
    val timestamp: Long? = System.currentTimeMillis(),
    var name: String? = null,
    val text: String,
    val eventType: Int? = null,
    val eventId: String? = null,
    val agoraUid: Long? = null,
    val agoraAccount: String? = null,
    val userName: String? = null,
    val wuid: String? = null,
    val lang: String? = null,
    val type: String? = null,
    val recognizedAtTimeMillis: Long? = null,
)