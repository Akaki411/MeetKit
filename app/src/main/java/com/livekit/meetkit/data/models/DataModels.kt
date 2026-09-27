/**
 * DTO и модели данных для взаимодействия с REST API сервера и отображения в UI.
 */
package com.livekit.meetkit.data.models

import com.google.gson.annotations.SerializedName

data class UserMe(
    val login: String? = null,
    val role: String? = "guest",
    val nickname: String? = null,
    val avatar: String? = null,
    @SerializedName("isAdmin") val isAdminField: Boolean? = false
) {
    val isAdmin: Boolean
        get() = isAdminField == true || role == "admin" || role == "owner"
}

data class RoomInfoResponse(
    val exists: Boolean = false,
    val adminOnly: Boolean = false,
    val requiresPassword: Boolean = false
)

data class ConnectionDetailsResponse(
    val serverUrl: String? = null,
    val roomName: String? = null,
    val participantToken: String? = null,
    val participantName: String? = null,
    val e2eePassphrase: String? = null,
    val error: String? = null
)

data class Attachment(
    val id: String = "",
    val url: String = "",
    val name: String = "",
    val type: String = "",
    val size: Long = 0
)

data class ReplySnippet(
    val id: String = "",
    val from: String = "",
    val text: String = ""
)

data class RecentRoom(
    val name: String,
    val ts: Long
)

data class AdminUser(
    val id: Int,
    val login: String,
    val role: String,
    val nickname: String? = null
)

data class AdminRoom(
    val id: Int,
    val name: String,
    val adminOnly: Boolean = false,
    val hasPassword: Boolean = false,
    val createdBy: String? = null
)

data class ActiveRoom(
    val name: String,
    val numParticipants: Int = 0,
    val createdAt: Long = 0
)

data class Participant(
    val identity: String,
    val name: String,
    val isAdmin: Boolean = false
)

data class Ban(
    val id: Int,
    val ip: String,
    val name: String? = null,
    val role: String? = null,
    val login: String? = null,
    val roomName: String? = null,
    val bannedBy: String? = null,
    val createdAt: String? = null
)
