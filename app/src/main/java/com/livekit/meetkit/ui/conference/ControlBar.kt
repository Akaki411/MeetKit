/**
 * Нижняя панель управления звонком: микрофон, камера, шеринг экрана, чат, список участников и кнопка выхода.
 */
package com.livekit.meetkit.ui.conference

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livekit.meetkit.ui.icons.Tabler
import com.livekit.meetkit.ui.screens.DeviceChoice
import com.livekit.meetkit.ui.theme.*

private val BtnShape = RoundedCornerShape(12.dp)
private val ToggleShape = RoundedCornerShape(12.dp, 0.dp, 0.dp, 12.dp)
private val ChevronShape = RoundedCornerShape(0.dp, 12.dp, 12.dp, 0.dp)
private val ActiveFill = Color(0x2E4ADE80)
private val OffFill = Color(0x2EFF6352)
private val LocalBtnSize = compositionLocalOf { 40.dp }

@Composable
private fun ControlBtn(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    fill: Color? = null,
    tint: Color = TextPrimary,
    badge: Int = 0,
    enabled: Boolean = true,
    shape: Shape = BtnShape,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val base = fill ?: if (active) ActiveFill else Fill08
    Box(modifier = modifier.size(LocalBtnSize.current)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(if (pressed && enabled) base.copy(alpha = minOf(1f, base.alpha + 0.08f)) else base)
                .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon, contentDescription = description,
                tint = if (active) Green else tint,
                modifier = Modifier.size(20.dp)
            )
        }
        if (badge > 0) Badge(badge, Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp))
    }
}

@Composable
private fun Badge(count: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 17.6.dp, minHeight = 17.6.dp)
            .clip(RoundedCornerShape(8.8.dp))
            .background(Coral)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(if (count > 99) "99+" else "$count", color = TextPrimary, fontSize = 11.2.sp)
    }
}

@Composable
private fun MediaControl(
    enabled: Boolean,
    onIcon: ImageVector,
    offIcon: ImageVector,
    label: String,
    devices: List<DeviceChoice>,
    activeId: String?,
    onToggle: () -> Unit,
    onSelect: (DeviceChoice) -> Unit,
    pending: Boolean = false,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row {
            ControlBtn(
                icon = if (enabled) onIcon else offIcon,
                description = label,
                onClick = onToggle,
                enabled = !pending,
                shape = ToggleShape,
                fill = if (enabled) null else OffFill,
                tint = if (enabled) TextPrimary else CoralSoft,
            )
            Spacer(Modifier.width(1.dp))
            Box(
                modifier = Modifier
                    .height(LocalBtnSize.current).width(21.6.dp)
                    .clip(ChevronShape)
                    .background(Fill08)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { open = !open },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Tabler.ChevronUp, contentDescription = "$label — устройства", tint = TextPrimary,
                    modifier = Modifier.size(14.dp).rotate(if (open) 180f else 0f)
                )
            }
        }
        DeviceMenu(open = open, onDismiss = { open = false }, devices = devices, activeId = activeId, placeholder = label, onSelect = onSelect)
    }
}

@Composable
fun DeviceMenu(
    open: Boolean,
    onDismiss: () -> Unit,
    devices: List<DeviceChoice>,
    activeId: String?,
    placeholder: String,
    onSelect: (DeviceChoice) -> Unit,
) {
    DropdownMenu(
        expanded = open,
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1B1B1F),
        shape = RoundedCornerShape(9.6.dp),
        tonalElevation = 0.dp,
        modifier = Modifier.widthIn(min = 220.dp)
    ) {
        if (devices.isEmpty()) {
            DropdownMenuItem(text = { Text(placeholder, color = TextMuted, fontSize = 13.6.sp) }, onClick = onDismiss)
        }
        devices.forEach { device ->
            DropdownMenuItem(
                text = {
                    Text(
                        device.label, fontSize = 13.6.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        color = if (device.id == activeId) TextPrimary else Color85
                    )
                },
                modifier = Modifier.background(if (device.id == activeId) Fill10 else Color.Transparent),
                onClick = {
                    onSelect(device)
                    onDismiss()
                }
            )
        }
    }
}

@Composable
fun ControlBar(
    micEnabled: Boolean,
    cameraEnabled: Boolean,
    micPending: Boolean,
    cameraPending: Boolean,
    microphones: List<DeviceChoice>,
    cameras: List<DeviceChoice>,
    activeCameraId: String?,
    chatOpen: Boolean,
    unread: Int,
    participantsOpen: Boolean,
    participantCount: Int,
    isModerator: Boolean,
    onToggleMic: () -> Unit,
    onToggleCamera: () -> Unit,
    onSelectMic: (DeviceChoice) -> Unit,
    onSelectCamera: (DeviceChoice) -> Unit,
    onFlipCamera: () -> Unit,
    onToggleParticipants: () -> Unit,
    onToggleChat: () -> Unit,
    onShare: () -> Unit,
    onSettings: () -> Unit,
    onLeave: (endForAll: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var leaveMenu by remember { mutableStateOf(false) }
    BoxWithConstraints(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
        val showFlip = cameraEnabled && cameras.size > 1
        val squareButtons = if (showFlip) 6 else 5
        val blocks = if (showFlip) 8 else 7
        val fitted = (maxWidth - 16.dp - 16.dp - 45.2.dp - 5.6.dp * (blocks - 1)) / (squareButtons + 2)
        val buttonSize = fitted.coerceIn(34.dp, 40.dp)
        CompositionLocalProvider(LocalBtnSize provides buttonSize) {
            Row(
                modifier = Modifier
                    .widthIn(max = maxWidth - 16.dp)
                    .shadow(24.dp, RoundedCornerShape(16.dp), ambientColor = Color.Black, spotColor = Color.Black)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ControlBarBackground)
                    .horizontalScroll(rememberScrollState())
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(5.6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MediaControl(
                    enabled = micEnabled, onIcon = Tabler.Microphone, offIcon = Tabler.MicrophoneOff, label = "Микрофон",
                    devices = microphones, activeId = microphones.firstOrNull()?.id, onToggle = onToggleMic,
                    onSelect = onSelectMic, pending = micPending
                )
                MediaControl(
                    enabled = cameraEnabled, onIcon = Tabler.Video, offIcon = Tabler.VideoOff, label = "Камера",
                    devices = cameras, activeId = activeCameraId, onToggle = onToggleCamera,
                    onSelect = onSelectCamera, pending = cameraPending
                )
                if (showFlip) {
                    ControlBtn(Tabler.CameraRotate, "Сменить камеру", onFlipCamera)
                }
                ControlBtn(Tabler.Users, "Участники", onToggleParticipants, active = participantsOpen, badge = participantCount)
                ControlBtn(Tabler.Message, "Чат", onToggleChat, active = chatOpen, badge = if (chatOpen) 0 else unread)
                ControlBtn(Tabler.Share, "Поделиться", onShare)
                ControlBtn(Tabler.Settings, "Настройки", onSettings)

                Box {
                    ControlBtn(
                        Tabler.PhoneOff, "Выйти",
                        onClick = { if (isModerator) leaveMenu = !leaveMenu else onLeave(false) },
                        fill = Coral
                    )
                    DropdownMenu(
                        expanded = leaveMenu, onDismissRequest = { leaveMenu = false },
                        containerColor = Color(0xFF1B1B1F), shape = RoundedCornerShape(9.6.dp), tonalElevation = 0.dp
                    ) {
                        DropdownMenuItem(
                            text = { Text("Выйти из конференции", color = TextPrimary, fontSize = 14.4.sp) },
                            onClick = { leaveMenu = false; onLeave(false) }
                        )
                        DropdownMenuItem(
                            text = { Text("Завершить для всех", color = CoralSoft, fontSize = 14.4.sp) },
                            onClick = { leaveMenu = false; onLeave(true) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ScreenShareButton(active: Boolean, pending: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (active) Color(0xFF2E9E5B) else Color(0x8C000000))
            .clickable(enabled = !pending, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            if (active) Tabler.ScreenShareOff else Tabler.ScreenShare,
            contentDescription = "Демонстрация экрана",
            tint = TextPrimary,
            modifier = Modifier.size(22.dp)
        )
    }
}
