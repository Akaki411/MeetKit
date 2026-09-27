/**
 * Боковая панель списка участников: статусы микрофонов, роли, локальный мьют и модерация.
 */
package com.livekit.meetkit.ui.conference

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livekit.meetkit.data.ApiClient
import com.livekit.meetkit.ui.components.rememberApiImage
import com.livekit.meetkit.ui.icons.Tabler
import com.livekit.meetkit.ui.theme.*
import io.livekit.android.room.Room
import io.livekit.android.room.participant.LocalParticipant
import io.livekit.android.room.participant.Participant
import io.livekit.android.room.track.Track
import kotlinx.coroutines.launch

val PanelBackground = Color(0xFF161618)

@Composable
fun ParticipantsPanel(
    room: Room,
    roomName: String,
    apiClient: ApiClient,
    localMuted: Set<String>,
    onToggleMute: (Participant) -> Unit,
    toast: ToastState,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val me = parseParticipantMeta(room.localParticipant.metadata)
    val participants = room.standardParticipants()

    Column(modifier = modifier.background(PanelBackground)) {
        HorizontalDivider(thickness = 1.dp, color = DarkCardBorder)
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(participants, key = { it.identity?.value ?: it.hashCode() }) { p ->
                ParticipantRow(
                    participant = p,
                    myMeta = me,
                    apiClient = apiClient,
                    muted = localMuted.contains(p.identity?.value),
                    onToggleMute = { onToggleMute(p) },
                    onModerate = { action ->
                        scope.launch {
                            val ok = apiClient.moderate(action, roomName, p.identity?.value ?: return@launch).isSuccess
                            if (!ok) {
                                toast.show(
                                    if (action == "kick") "Не удалось удалить участника" else "Не удалось забанить участника",
                                    error = true
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ParticipantRow(
    participant: Participant,
    myMeta: ParticipantMeta,
    apiClient: ApiClient,
    muted: Boolean,
    onToggleMute: () -> Unit,
    onModerate: (String) -> Unit,
) {
    val meta = parseParticipantMeta(participant.metadata)
    val name = participant.displayName
    val isLocal = participant is LocalParticipant
    val micMuted = participant.getTrackPublication(Track.Source.MICROPHONE)?.muted ?: true
    val canMod = !isLocal && canModerate(myMeta, meta)
    val avatar = rememberApiImage(apiClient, meta.avatar?.let { "/api/avatars/$it" })

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.2.dp))
            .padding(horizontal = 8.dp, vertical = 6.4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.4.dp)
    ) {
        Box(modifier = Modifier.size(33.6.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(BrandAccent, BrandAccentDark))),
                contentAlignment = Alignment.Center
            ) {
                if (avatar != null) {
                    Image(avatar, contentDescription = name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Text(initialsOf(name), color = TextPrimary, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            if (micMuted) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 3.dp, y = 3.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(PanelBackground)
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(Coral),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Tabler.MicrophoneOff, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(9.dp))
                }
            }
        }

        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.4.dp)
        ) {
            Text(
                name, color = TextPrimary, fontSize = 14.4.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (isLocal) Text("(вы)", color = TextMuted, fontSize = 12.sp)
        }

        when {
            meta.creator -> Mark("владелец", Tabler.Crown, Color(0x29FFC400), Color(0xFFFFD257))
            meta.admin -> Mark("админ", Tabler.Shield, Color(0x29FF6352), CoralSoft)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(3.2.dp)) {
            if (!isLocal) {
                RoundAction(
                    icon = if (muted) Tabler.VolumeOff else Tabler.Volume,
                    description = if (muted) "Включить звук для меня" else "Заглушить для меня",
                    active = muted,
                    onClick = onToggleMute
                )
            }
            if (canMod) {
                RoundAction(Tabler.UserX, "Удалить из звонка", onClick = { onModerate("kick") })
                RoundAction(Tabler.Ban, "Забанить по IP", danger = true, onClick = { onModerate("ban") })
            }
        }
    }
}

@Composable
private fun Mark(text: String, icon: ImageVector, container: Color, content: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(container)
            .padding(horizontal = 7.2.dp, vertical = 1.6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.2.dp)
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(12.dp))
        Text(text, color = content, fontSize = 10.9.sp)
    }
}

@Composable
private fun RoundAction(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    active: Boolean = false,
    danger: Boolean = false,
) {
    Box(
        modifier = Modifier
            .size(30.4.dp)
            .clip(CircleShape)
            .background(if (active) Color(0x2EFF6352) else Fill06)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon, contentDescription = description,
            tint = if (active || danger) CoralSoft else Color(0xB3FFFFFF),
            modifier = Modifier.size(16.dp)
        )
    }
}
