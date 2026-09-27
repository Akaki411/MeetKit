/**
 * Диалоговое окно настроек: выбор устройств ввода/вывода звука, камеры, фона и управления записью.
 */
package com.livekit.meetkit.ui.conference

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.livekit.meetkit.data.ApiClient
import com.livekit.meetkit.ui.icons.Tabler
import com.livekit.meetkit.ui.screens.DeviceChoice
import com.livekit.meetkit.ui.theme.*
import io.livekit.android.renderer.TextureViewRenderer
import io.livekit.android.room.Room
import io.livekit.android.room.track.Track
import io.livekit.android.room.track.VideoTrack
import kotlinx.coroutines.launch
import livekit.org.webrtc.RendererCommon

private val ModalBackground = Color(0xFF1B1B1F)
private val ToggleShape = RoundedCornerShape(9.6.dp)
private val RecordingColor = Color(0xE6F5333F)
private val RecordingText = Color(0xFFFF8A91)

@Composable
fun SettingsDialog(
    room: Room,
    roomName: String,
    apiClient: ApiClient,
    toast: ToastState,
    micEnabled: Boolean,
    cameraEnabled: Boolean,
    onToggleMic: () -> Unit,
    onToggleCamera: () -> Unit,
    cameras: List<DeviceChoice>,
    activeCameraId: String?,
    onSelectCamera: (DeviceChoice) -> Unit,
    noiseSuppression: Boolean,
    onNoiseSuppression: (Boolean) -> Unit,
    microphones: List<DeviceChoice>,
    onSelectMic: (DeviceChoice) -> Unit,
    audioOutputs: List<DeviceChoice>,
    activeAudioOutput: String?,
    onSelectAudioOutput: (DeviceChoice) -> Unit,
    backgroundSection: (@Composable () -> Unit)?,
    onClose: () -> Unit,
) {
    var tab by remember { mutableStateOf("media") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose)
            .systemBarsPadding()
            .imePadding()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(ModalBackground)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Настройки", color = TextPrimary, fontSize = 17.6.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(Fill06).clickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) { Icon(Tabler.X, contentDescription = "Закрыть настройки", tint = IconMuted, modifier = Modifier.size(16.dp)) }
            }

            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("media" to "Медиаустройства", "recording" to "Запись").forEach { (id, label) ->
                        Column(
                            modifier = Modifier
                                .width(IntrinsicSize.Max)
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { tab = id },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                label, color = if (tab == id) TextPrimary else IconMuted, fontSize = 15.2.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                            Box(modifier = Modifier.height(2.dp).fillMaxWidth().background(if (tab == id) Coral else Color.Transparent))
                        }
                    }
                }
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0x1AFFFFFF)))
            }

            if (tab == "media") {
                SettingsSection("Камера") {
                    CameraPreview(room, cameraEnabled)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.8.dp), verticalAlignment = Alignment.CenterVertically) {
                        ToggleButton(
                            icon = if (cameraEnabled) Tabler.Video else Tabler.VideoOff,
                            active = false, onClick = onToggleCamera
                        )
                        DeviceSelect(
                            devices = cameras, activeId = activeCameraId, placeholder = "Камера",
                            onSelect = onSelectCamera, modifier = Modifier.weight(1f)
                        )
                    }
                    backgroundSection?.invoke()
                }
                SettingsSection("Микрофон") {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.8.dp), verticalAlignment = Alignment.CenterVertically) {
                        ToggleButton(
                            icon = if (micEnabled) Tabler.Microphone else Tabler.MicrophoneOff,
                            active = false, onClick = onToggleMic
                        )
                        ToggleButton(
                            icon = Tabler.NoiseReduction, active = noiseSuppression,
                            onClick = { onNoiseSuppression(!noiseSuppression) }
                        )
                        DeviceSelect(
                            devices = microphones, activeId = microphones.firstOrNull()?.id, placeholder = "Микрофон",
                            onSelect = onSelectMic, modifier = Modifier.weight(1f)
                        )
                    }
                }
                SettingsSection("Динамик и наушники") {
                    DeviceSelect(
                        devices = audioOutputs, activeId = activeAudioOutput, placeholder = "Устройство вывода звука",
                        onSelect = onSelectAudioOutput, modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                RecordingTab(room, roomName, apiClient, toast)
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(9.6.dp)) {
        Text(title.uppercase(), color = IconMuted, fontSize = 12.8.sp, letterSpacing = 0.64.sp)
        content()
    }
}

@Composable
fun ToggleButton(
    icon: ImageVector,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    danger: Boolean = false,
    label: String? = null,
    enabled: Boolean = true,
) {
    val fill = when {
        danger -> Color(0x2EF5333F)
        active -> Color(0x294ADE80)
        else -> Fill06
    }
    val tint = when {
        danger -> RecordingText
        active -> Green
        else -> TextPrimary
    }
    Row(
        modifier = modifier
            .clip(ToggleShape)
            .background(fill)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.4.dp, vertical = 8.8.dp)
            .alpha(if (enabled) 1f else 0.5f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.4.dp)
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(18.dp))
        if (label != null) Text(label, color = tint, fontSize = 14.4.sp)
    }
}

@Composable
fun DeviceSelect(
    devices: List<DeviceChoice>,
    activeId: String?,
    placeholder: String,
    onSelect: (DeviceChoice) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    val label = devices.firstOrNull { it.id == activeId }?.label ?: devices.firstOrNull()?.label ?: placeholder
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(ToggleShape)
                .background(Fill06)
                .clickable { open = !open }
                .padding(horizontal = 12.8.dp, vertical = 8.8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(label, color = TextPrimary, fontSize = 14.4.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f))
            Icon(Tabler.ChevronDown, contentDescription = null, tint = TextLabel,
                modifier = Modifier.size(18.dp).rotate(if (open) 180f else 0f))
        }
        DeviceMenu(open, { open = false }, devices, activeId, placeholder, onSelect)
    }
}

@Composable
private fun CameraPreview(room: Room, cameraEnabled: Boolean) {
    val track = if (cameraEnabled) {
        room.localParticipant.getTrackPublication(Track.Source.CAMERA)?.track as? VideoTrack
    } else null
    if (track == null) return
    key(track) {
        AndroidView(
            factory = { ctx ->
                TextureViewRenderer(ctx).apply {
                    room.initVideoRenderer(this)
                    setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
                    setMirror(true)
                    track.addRenderer(this)
                }
            },
            onRelease = { renderer ->
                track.removeRenderer(renderer)
                renderer.release()
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 260.dp)
                .aspectRatio(16f / 9f)
                .clip(ToggleShape)
                .background(Color.Black)
        )
    }
}

@Composable
private fun RecordingTab(room: Room, roomName: String, apiClient: ApiClient, toast: ToastState) {
    val scope = rememberCoroutineScope()
    val recording = room.isRecording
    var processing by remember { mutableStateOf(false) }
    LaunchedEffect(recording) { processing = false }

    SettingsSection("Запись встречи") {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (recording) {
                Box(modifier = Modifier.size(8.8.dp).clip(CircleShape).background(RecordingColor))
            }
            Text(
                if (recording) "Эта встреча сейчас записывается" else "Активных записей этой встречи нет",
                color = IconMuted, fontSize = 14.4.sp
            )
        }
        ToggleButton(
            icon = Tabler.Video,
            active = false,
            danger = recording,
            label = if (recording) "Остановить запись" else "Начать запись",
            enabled = !processing,
            onClick = {
                processing = true
                scope.launch {
                    val ok = apiClient.record(if (recording) "stop" else "start", roomName).isSuccess
                    if (!ok) {
                        processing = false
                        toast.show("Не удалось выполнить запрос записи", error = true)
                    }
                }
            }
        )
    }
}
