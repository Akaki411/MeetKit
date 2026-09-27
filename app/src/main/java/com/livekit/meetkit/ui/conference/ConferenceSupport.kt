/**
 * Утилиты и хелперы состояния комнаты LiveKit: роли пользователей, плитки и всплывающие уведомления.
 */
package com.livekit.meetkit.ui.conference

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.livekit.meetkit.ui.screens.DeviceChoice
import com.livekit.meetkit.ui.theme.TextPrimary
import io.livekit.android.room.Room
import io.livekit.android.room.participant.Participant
import io.livekit.android.room.track.Track
import io.livekit.android.room.track.VideoTrack
import kotlinx.coroutines.delay

data class ParticipantMeta(
    val role: String = "guest",
    val admin: Boolean = false,
    val creator: Boolean = false,
    val login: String? = null,
    val avatar: String? = null,
)

private val gson = Gson()

fun parseParticipantMeta(metadata: String?): ParticipantMeta {
    if (metadata.isNullOrBlank()) return ParticipantMeta()
    return try {
        val json = gson.fromJson(metadata, JsonObject::class.java)
        fun str(key: String) = json.get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString
        val role = str("role") ?: if (json.get("admin")?.asBoolean == true) "admin" else "user"
        ParticipantMeta(
            role = role,
            admin = role == "admin" || role == "owner",
            creator = json.get("creator")?.asBoolean == true,
            login = str("login"),
            avatar = str("avatar"),
        )
    } catch (_: Exception) {
        ParticipantMeta()
    }
}

fun canModerate(actor: ParticipantMeta, target: ParticipantMeta): Boolean {
    fun authority(m: ParticipantMeta) = if (m.creator) 3 else if (m.admin) 2 else 0
    val a = authority(actor)
    return a >= 2 && a > authority(target)
}

fun initialsOf(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (parts.isEmpty()) return "?"
    if (parts.size == 1) return parts[0].take(2).uppercase()
    return (parts.first().take(1) + parts.last().take(1)).uppercase()
}

val Participant.displayName: String
    get() = name?.ifBlank { null } ?: identity?.value ?: "Участник"

@Composable
fun rememberRoomTick(room: Room): Int {
    var tick by remember(room) { mutableIntStateOf(0) }
    LaunchedEffect(room) {
        room.events.events.collect { tick++ }
    }
    return tick
}

fun Room.standardParticipants(): List<Participant> =
    listOf<Participant>(localParticipant) + remoteParticipants.values.filter { it.kind == Participant.Kind.STANDARD }

data class TileRef(val participant: Participant, val source: Track.Source, val track: VideoTrack?) {
    val key: String get() = "${participant.identity?.value}_${source.name}"
    val isScreenShare: Boolean get() = source == Track.Source.SCREEN_SHARE
}

fun Room.tiles(): List<TileRef> {
    val list = mutableListOf<TileRef>()
    for (p in standardParticipants()) {
        val cam = p.getTrackPublication(Track.Source.CAMERA)
        val camTrack = if (cam != null && !cam.muted) cam.track as? VideoTrack else null
        list += TileRef(p, Track.Source.CAMERA, camTrack)
        val share = p.getTrackPublication(Track.Source.SCREEN_SHARE)?.track as? VideoTrack
        if (share != null) list += TileRef(p, Track.Source.SCREEN_SHARE, share)
    }
    return list
}

class ToastState {
    var text by mutableStateOf<String?>(null)
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf(false)
        private set
    private var generation = 0

    suspend fun show(text: String, loading: Boolean = false, error: Boolean = false, durationMs: Long? = 3000) {
        val mine = ++generation
        this.text = text
        this.loading = loading
        this.error = error
        if (durationMs != null) {
            delay(durationMs)
            if (mine == generation) dismiss()
        }
    }

    fun dismiss() {
        text = null
        loading = false
        error = false
    }
}

@Composable
fun ToastHost(state: ToastState, modifier: Modifier = Modifier) {
    val text = state.text ?: return
    Box(modifier = modifier.fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.TopCenter) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (state.error) Color(0xE6F5333F) else Color(0xFF363636))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (state.loading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = TextPrimary, strokeWidth = 2.dp)
            }
            Text(text, color = TextPrimary, fontSize = 14.sp)
        }
    }
}

fun listMicrophones(context: Context): List<DeviceChoice> {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return listOf(DeviceChoice(null, "По умолчанию"))
    val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val inputs = audio.getDevices(AudioManager.GET_DEVICES_INPUTS)
    val labelled = inputs.mapNotNull { d ->
        val label = when (d.type) {
            AudioDeviceInfo.TYPE_BUILTIN_MIC -> "Встроенный микрофон"
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth: ${d.productName}"
            AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Проводная гарнитура"
            AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET -> "USB: ${d.productName}"
            else -> null
        }
        label?.let { DeviceChoice("${d.type}:${d.id}", it) }
    }
    return labelled.ifEmpty { listOf(DeviceChoice(null, "По умолчанию")) }
}
