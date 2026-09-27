/**
 * Карточка предпросмотра перед входом в комнату: выбор медиаустройств и ввод имени.
 */
package com.livekit.meetkit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.livekit.meetkit.ui.components.*
import com.livekit.meetkit.ui.icons.Tabler
import com.livekit.meetkit.ui.theme.*
import io.livekit.android.renderer.TextureViewRenderer
import io.livekit.android.room.Room
import io.livekit.android.room.track.LocalVideoTrack

data class DeviceChoice(val id: String?, val label: String)

private val PreviewShape = RoundedCornerShape(12.dp)
private val DeviceShape = RoundedCornerShape(8.dp)
private val DropdownBackground = Color(0xFF1B1B1F)
private val CameraOffColor = Color(0xFFE36D5B)
private val MicOffColor = Color(0xFFFF8A7A)
private val PlaceholderColor = Color(0xFF333333)

@Composable
fun PreJoinCard(
    room: Room,
    previewTrack: LocalVideoTrack?,
    micEnabled: Boolean,
    cameraEnabled: Boolean,
    onToggleMic: () -> Unit,
    onToggleCamera: () -> Unit,
    microphones: List<DeviceChoice>,
    cameras: List<DeviceChoice>,
    selectedCameraId: String?,
    onSelectCamera: (DeviceChoice) -> Unit,
    mirrorPreview: Boolean,
    nickname: String,
    onNicknameChange: (String) -> Unit,
    requiresPassword: Boolean,
    password: String,
    onPasswordChange: (String) -> Unit,
    error: String?,
    connecting: Boolean,
    onJoin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WebCard(
        modifier = modifier
            .widthIn(max = 420.dp)
            .fillMaxWidth(),
        padding = PaddingValues(24.dp),
        spacing = 14.4.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(PreviewShape)
                .background(DarkBackground),
            contentAlignment = Alignment.Center
        ) {
            if (cameraEnabled && previewTrack != null) {
                AndroidView(
                    factory = { ctx ->
                        TextureViewRenderer(ctx).apply {
                            room.initVideoRenderer(this)
                            setScalingType(livekit.org.webrtc.RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                            setMirror(mirrorPreview)
                            previewTrack.addRenderer(this)
                        }
                    },
                    onRelease = { renderer ->
                        previewTrack.removeRenderer(renderer)
                        renderer.release()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(Tabler.VideoOff, contentDescription = null, tint = PlaceholderColor, modifier = Modifier.size(40.dp))
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 9.6.dp, bottom = 9.6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x8C000000))
                    .padding(horizontal = 8.8.dp, vertical = 4.5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    if (micEnabled) Tabler.Microphone else Tabler.MicrophoneOff,
                    contentDescription = null,
                    tint = if (micEnabled) TextPrimary else MicOffColor,
                    modifier = Modifier.size(16.dp)
                )
                if (micEnabled) {
                    Box(
                        modifier = Modifier
                            .width(56.dp)
                            .height(5.6.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x2EFFFFFF))
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(9.6.dp)) {
            DeviceDropDown(
                enabled = micEnabled,
                onIcon = Tabler.Microphone,
                offIcon = Tabler.MicrophoneOff,
                choices = microphones,
                selectedId = null,
                placeholder = "Микрофон",
                onToggle = onToggleMic,
                onSelect = { },
                modifier = Modifier.weight(1f)
            )
            DeviceDropDown(
                enabled = cameraEnabled,
                onIcon = Tabler.Video,
                offIcon = Tabler.VideoOff,
                choices = cameras,
                selectedId = selectedCameraId,
                placeholder = "Камера",
                onToggle = onToggleCamera,
                onSelect = onSelectCamera,
                modifier = Modifier.weight(1f)
            )
        }

        WebField(
            value = nickname,
            onValueChange = onNicknameChange,
            placeholder = "Ваше имя",
            modifier = Modifier.fillMaxWidth()
        )

        if (requiresPassword) {
            WebField(
                value = password,
                onValueChange = onPasswordChange,
                placeholder = "Пароль комнаты",
                password = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (error != null) {
            Text(
                text = error,
                color = ErrorRed,
                fontSize = 14.4.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        WebButton(
            onClick = onJoin,
            modifier = Modifier.fillMaxWidth(),
            fill = Fill10,
            pressedFill = Color(0x29FFFFFF),
            contentPadding = PaddingValues(horizontal = 25.6.dp),
            enabled = !connecting && nickname.isNotBlank()
        ) {
            WebButtonText(if (connecting) "Подключение…" else "Войти")
        }
    }
}

@Composable
private fun DeviceDropDown(
    enabled: Boolean,
    onIcon: ImageVector,
    offIcon: ImageVector,
    choices: List<DeviceChoice>,
    selectedId: String?,
    placeholder: String,
    onToggle: () -> Unit,
    onSelect: (DeviceChoice) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    val noRipple = remember { MutableInteractionSource() }
    val activeLabel = choices.firstOrNull { it.id == selectedId }?.label
        ?: choices.firstOrNull()?.label
        ?: placeholder

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .glass(DeviceShape, Fill06)
                .padding(horizontal = 9.6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.6.dp)
        ) {
            Icon(
                imageVector = if (enabled) onIcon else offIcon,
                contentDescription = placeholder,
                tint = if (enabled) Color(0xB3FFFFFF) else CameraOffColor,
                modifier = Modifier
                    .size(18.dp)
                    .clickable(interactionSource = noRipple, indication = null, onClick = onToggle)
            )
            Text(
                text = activeLabel,
                color = TextPrimary,
                fontSize = 14.4.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .clickable(interactionSource = noRipple, indication = null) { open = !open }
            )
            Icon(
                imageVector = Tabler.ChevronDown,
                contentDescription = null,
                tint = TextLabel,
                modifier = Modifier
                    .size(18.dp)
                    .rotate(if (open) 180f else 0f)
                    .clickable(interactionSource = noRipple, indication = null) { open = !open }
            )
        }

        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            containerColor = DropdownBackground,
            shape = DeviceShape,
            tonalElevation = 0.dp
        ) {
            if (choices.isEmpty()) {
                DropdownMenuItem(text = { Text(placeholder, color = TextMuted, fontSize = 13.6.sp) }, onClick = { open = false })
            }
            choices.forEach { choice ->
                DropdownMenuItem(
                    text = {
                        Text(
                            choice.label,
                            color = if (choice.id == selectedId) TextPrimary else Color85,
                            fontSize = 13.6.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    onClick = {
                        onSelect(choice)
                        open = false
                    }
                )
            }
        }
    }
}
